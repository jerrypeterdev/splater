package com.partydamage;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.swing.SwingUtilities;
import javax.inject.Inject;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Hitsplat;
import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

@PluginDescriptor(name = "Splater", description = "A ranked party damage overlay")
public class PartyDamagePlugin extends Plugin
{
    @Inject private Client client;
    @Inject private PartyService party;
    @Inject private WSClient wsClient;
    @Inject private ClientThread clientThread;
    @Inject private PartyDamageConfig config;
    @Inject private OverlayManager overlays;
    @Inject private PartyDamageOverlay overlay;
    @Inject private PartyDamageTakenOverlay takenOverlay;
    @Inject private PartyDamagePanel panel;
    @Inject private ClientToolbar toolbar;
    @Inject private ConfigManager configManager;
    @Inject private ItemManager itemManager;
    @Inject private SpecialAttackTracker specTracker;
    private NavigationButton navigation;

    private final Map<String, Long> damage = new HashMap<>();
    private final Map<String, Long> encounterDamage = new HashMap<>();
    private final DamageTakenSeries overallTaken = new DamageTakenSeries();
    private final DamageTakenSeries encounterTaken = new DamageTakenSeries();
    private final Map<String, List<SpecResult>> specials = new HashMap<>();
    private long startedNanos;
    private long lastHitNanos;
    private long encounterStartedNanos;
    private long encounterLastHitNanos;
    private volatile String joinedPassphrase;

    @Provides PartyDamageConfig provideConfig(ConfigManager manager) { return manager.getConfig(PartyDamageConfig.class); }

    @Override protected void startUp()
    {
        resetAll();
        wsClient.registerMessage(PartyDamageHitMessage.class);
        wsClient.registerMessage(PartyDamageSpecMessage.class);
        wsClient.registerMessage(PartyDamageTakenMessage.class);
        specTracker.reset();
        overlays.add(overlay);
        overlays.add(takenOverlay);
        takenOverlay.loadEffectSprites();
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        graphics.setColor(new Color(130, 89, 51));
        graphics.setStroke(new BasicStroke(3));
        graphics.drawLine(3, 13, 11, 5);
        graphics.setColor(new Color(190, 43, 34));
        graphics.fillRect(7, 1, 8, 5);
        graphics.dispose();
        navigation = NavigationButton.builder().tooltip("Splater").icon(icon).panel(panel).build();
        toolbar.addNavigation(navigation);
        NavigationButton originalNavigation = navigation;
        AsyncBufferedImage weaponIcon = itemManager.getImage(ItemID.DRAGON_WARHAMMER);
        weaponIcon.onLoaded(() -> SwingUtilities.invokeLater(() -> {
            if (navigation != originalNavigation) return;
            BufferedImage scaled = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.drawImage(weaponIcon, 0, 0, 16, 16, null);
            g.dispose();
            toolbar.removeNavigation(originalNavigation);
            navigation = NavigationButton.builder().tooltip("Splater").icon(scaled).panel(panel).build();
            toolbar.addNavigation(navigation);
        }));
        SwingUtilities.invokeLater(panel::start);
    }
    @Override protected void shutDown()
    {
        overlays.remove(overlay);
        overlays.remove(takenOverlay);
        NavigationButton removedNavigation = navigation;
        navigation = null;
        toolbar.removeNavigation(removedNavigation);
        SwingUtilities.invokeLater(panel::stop);
        wsClient.unregisterMessage(PartyDamageHitMessage.class);
        wsClient.unregisterMessage(PartyDamageSpecMessage.class);
        wsClient.unregisterMessage(PartyDamageTakenMessage.class);
        specTracker.reset();
        clientThread.invokeLater(() -> {
            if (joinedPassphrase != null && joinedPassphrase.equals(party.getPartyPassphrase())) party.changeParty(null);
            joinedPassphrase = null;
        });
        resetAll();
    }

    void joinGroup(String code)
    {
        String phrase = code.trim();
        if (phrase.isEmpty()) return;
        configManager.setConfiguration("partydamagemeter", "passphrase", phrase);
        clientThread.invokeLater(() -> {
            joinedPassphrase = phrase;
            if (!phrase.equals(party.getPartyPassphrase())) party.changeParty(phrase);
        });
    }

    void leaveGroup()
    {
        clientThread.invokeLater(() -> {
            if (joinedPassphrase != null && joinedPassphrase.equals(party.getPartyPassphrase())) party.changeParty(null);
            joinedPassphrase = null;
        });
    }

    private boolean hasJoinedGroup()
    {
        String joined = joinedPassphrase;
        return joined != null && party.isInParty() && joined.equals(party.getPartyPassphrase());
    }

    @Subscribe public void onPartyChanged(PartyChanged event) { resetAll(); }

    @Subscribe public void onHitsplatApplied(HitsplatApplied event)
    {
        specTracker.hit(event);
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer != null && event.getActor() == localPlayer && localPlayer.getName() != null
            && isDamageTaken(event.getHitsplat()) && event.getHitsplat().getAmount() > 0)
        {
            String name = localPlayer.getName();
            int amount = event.getHitsplat().getAmount();
            DamageEffect effect = DamageEffect.fromHitsplat(event.getHitsplat().getHitsplatType());
            addTaken(name, amount, effect);
            if (hasJoinedGroup() && party.getLocalMember() != null)
                party.send(new PartyDamageTakenMessage(name, amount, effect == null ? 0 : effect.code));
        }
        if (!(event.getActor() instanceof NPC) && !(event.getActor() instanceof Player)) return;
        if (event.getActor() == localPlayer || !event.getHitsplat().isMine()) return;
        Player player = client.getLocalPlayer();
        if (player == null || player.getName() == null) return;
        String name = player.getName();
        int amount = event.getHitsplat().getAmount();
        add(name, amount);
        if (hasJoinedGroup() && party.getLocalMember() != null)
            party.send(new PartyDamageHitMessage(name, amount));
    }

    private static boolean isDamageTaken(Hitsplat hit)
    {
        int type = hit.getHitsplatType();
        return hit.isMine() || hit.isOthers() || type == HitsplatID.POISON || type == HitsplatID.VENOM
            || type == HitsplatID.BLEED || type == HitsplatID.BURN || type == HitsplatID.DISEASE;
    }

    @Subscribe public void onPartyDamageTakenMessage(PartyDamageTakenMessage event)
    {
        PartyMember local = party.getLocalMember();
        if (!hasJoinedGroup() || local == null || local.getMemberId() == event.getMemberId() || party.getMemberById(event.getMemberId()) == null) return;
        if (event.name() == null || event.name().isEmpty() || event.name().length() > 12
            || event.amount() <= 0 || event.amount() > 10000) return;
        addTaken(event.name(), event.amount(), DamageEffect.fromCode(event.effect()));
    }
    private void addTaken(String name, int amount, DamageEffect effect)
    {
        long now = System.nanoTime();
        overallTaken.add(name, amount, effect, now);
        encounterTaken.add(name, amount, effect, now);
    }

    @Subscribe public void onVarbitChanged(VarbitChanged event) { specTracker.energyChanged(event); }
    @Subscribe public void onGameTick(GameTick event) { specTracker.tick(event); }

    void recordLocalSpec(int itemId, int amount)
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getName() == null) return;
        addSpec(player.getName(), itemId, amount);
        if (hasJoinedGroup() && party.getLocalMember() != null)
            party.send(new PartyDamageSpecMessage(player.getName(), itemId, amount));
    }

    @Subscribe public void onPartyDamageSpecMessage(PartyDamageSpecMessage event)
    {
        PartyMember local = party.getLocalMember();
        if (!hasJoinedGroup() || local == null || local.getMemberId() == event.getMemberId() || party.getMemberById(event.getMemberId()) == null) return;
        if (event.name() == null || event.name().isEmpty() || event.name().length() > 12
            || event.itemId() <= 0 || event.itemId() >= 100000 || event.damage() < -1 || event.damage() > 10000) return;
        addSpec(event.name(), event.itemId(), event.damage());
    }
    private synchronized void addSpec(String name, int itemId, int amount)
    {
        damage.putIfAbsent(name, 0L);
        List<SpecResult> list = specials.computeIfAbsent(name, k -> new ArrayList<>());
        list.add(new SpecResult(itemId, amount));
        if (list.size() > 8) list.remove(0);
    }
    synchronized Map<String, List<SpecResult>> specSnapshot()
    {
        Map<String, List<SpecResult>> copy = new HashMap<>();
        specials.forEach((name, list) -> copy.put(name, new ArrayList<>(list)));
        return copy;
    }

    @Subscribe public void onPartyDamageHitMessage(PartyDamageHitMessage event)
    {
        PartyMember local = party.getLocalMember();
        if (!hasJoinedGroup() || local == null || local.getMemberId() == event.getMemberId()) return;
        PartyMember sender = party.getMemberById(event.getMemberId());
        String name = event.getName();
        if (sender != null && name != null && !name.isEmpty() && name.length() <= 12 && event.getAmount() >= 0 && event.getAmount() <= 10000)
            add(name, event.getAmount());
    }

    private synchronized void add(String name, int hit)
    {
        if (hit < 0) return;
        long now = System.nanoTime();
        if (startedNanos == 0) startedNanos = now;
        lastHitNanos = now;
        damage.merge(name, (long) hit, Long::sum);
        encounterDamage.merge(name, (long) hit, Long::sum);
        if (encounterStartedNanos == 0) encounterStartedNanos = now;
        encounterLastHitNanos = now;
    }

    synchronized void reset() { damage.clear(); specials.clear(); startedNanos = 0; lastHitNanos = 0; }
    synchronized void resetEncounter() { encounterDamage.clear(); encounterStartedNanos = 0; encounterLastHitNanos = 0; }
    private synchronized void resetAll() { reset(); resetEncounter(); overallTaken.reset(); encounterTaken.reset(); }
    synchronized Map<String, Long> snapshot() { return new HashMap<>(damage); }
    synchronized Map<String, Long> encounterSnapshot() { return new HashMap<>(encounterDamage); }
    String savedGroupCode()
    {
        String saved = configManager.getConfiguration("partydamagemeter", "passphrase");
        return saved == null ? "" : saved;
    }
    DamageTakenSeries overallTaken() { return overallTaken; }
    DamageTakenSeries encounterTaken() { return encounterTaken; }
    boolean showOverlay() { return config.showOverlay(); }
    void setShowOverlay(boolean show) { configManager.setConfiguration("partydamagemeter", "showOverlay", show); }
    boolean showTakenOverlay() { return config.showTakenOverlay(); }
    void setShowTakenOverlay(boolean show) { configManager.setConfiguration("partydamagemeter", "showTakenOverlay", show); }
    int overlayOpacity() { return Math.max(0, Math.min(100, config.overlayOpacity())); }
    void setOverlayOpacity(int value) { configManager.setConfiguration("partydamagemeter", "overlayOpacity", Math.max(0, Math.min(100, value))); }
    void resetOverlaySize(boolean taken)
    {
        clientThread.invokeLater(() -> {
            PartyDamageOverlay selected = taken ? takenOverlay : overlay;
            selected.setPreferredSize(null);
            overlays.saveOverlay(selected);
        });
    }
    Color colourFor(String name)
    {
        String stored = configManager.getConfiguration("partydamagecolors", colourKey(name));
        if (stored != null)
        {
            try { return new Color(Integer.parseInt(stored), true); }
            catch (NumberFormatException ignored) { }
        }
        return Color.getHSBColor((name.toLowerCase(java.util.Locale.ROOT).hashCode() & 0xffff) / 65536f, .52f, .65f);
    }
    void setColour(String name, Color color)
    {
        configManager.setConfiguration("partydamagecolors", colourKey(name), Integer.toString(color.getRGB()));
    }
    private String colourKey(String name)
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(name.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }
    synchronized double elapsedSeconds()
    {
        return startedNanos == 0 ? 1 : Math.max(1, (lastHitNanos - startedNanos) / 1_000_000_000.0);
    }
    synchronized double encounterElapsedSeconds()
    {
        return encounterStartedNanos == 0 ? 1 : Math.max(1, (encounterLastHitNanos - encounterStartedNanos) / 1_000_000_000.0);
    }
}
