package com.partydamage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.color.ColorSpace;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.api.gameval.SpriteID;

class PartyDamageOverlay extends Overlay
{
    private static final int WIDTH = 280, HEADER = 22, ROW = 20;
    private final PartyDamagePlugin plugin;
    private final ItemManager items;
    private final boolean taken;
    private final SpriteManager sprites;
    private final Map<DamageEffect, BufferedImage> effectSprites = new java.util.concurrent.ConcurrentHashMap<>();

    @Inject PartyDamageOverlay(PartyDamagePlugin plugin, ItemManager items, SpriteManager sprites)
    {
        this(plugin, items, sprites, false);
    }
    PartyDamageOverlay(PartyDamagePlugin plugin, ItemManager items, SpriteManager sprites, boolean taken)
    {
        super(plugin);
        this.plugin = plugin;
        this.items = items;
        this.sprites = sprites;
        this.taken = taken;
        setPosition(taken ? OverlayPosition.TOP_RIGHT : OverlayPosition.TOP_LEFT);
        setResizable(true);
        setMinimumSize(160);
    }

    void loadEffectSprites()
    {
        if (!taken) return;
        sprites.getSpriteAsync(SpriteID.Hitmark.HITSPLAT_GREEN_POISON, 0, image -> effectSprites.put(DamageEffect.POISON, image));
        sprites.getSpriteAsync(SpriteID.Hitmark.HITSPLAT_DARK_GREEN_VENOM, 0, image -> effectSprites.put(DamageEffect.VENOM, image));
        sprites.getSpriteAsync(SpriteID.Hitmark.BURN_DAMAGE, 0, image -> effectSprites.put(DamageEffect.BURN, image));
    }

    @Override public Dimension render(Graphics2D g)
    {
        if (taken ? !plugin.showTakenOverlay() : !plugin.showOverlay()) return null;
        Map<String, Long> data = taken ? plugin.overallTaken().snapshot() : plugin.snapshot();
        Map<String, List<SpecResult>> specs = taken ? java.util.Collections.emptyMap() : plugin.specSnapshot();
        Map<String, Set<DamageEffect>> effects = taken ? plugin.overallTaken().effectSnapshot() : java.util.Collections.emptyMap();
        List<Map.Entry<String, Long>> rows = new ArrayList<>(data.entrySet());
        rows.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
        long total = rows.stream().mapToLong(Map.Entry::getValue).sum();
        long max = rows.isEmpty() ? 1 : Math.max(1, rows.get(0).getValue());
        double elapsed = taken ? plugin.overallTaken().seconds() : plugin.elapsedSeconds();
        Dimension preferred = getPreferredSize();
        int width = preferred == null ? WIDTH : Math.max(160, preferred.width);
        int contentHeight = HEADER;
        for (Map.Entry<String, Long> entry : rows)
            contentHeight += ROW + ((specs.containsKey(entry.getKey()) || effects.containsKey(entry.getKey())) ? 24 : 0);
        int height = preferred == null ? contentHeight : Math.max(32, preferred.height);
        Shape previousClip = g.getClip();
        g.clipRect(0, 0, width, height);
        g.setColor(new Color(25, 25, 25, 220 * plugin.overlayOpacity() / 100));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int headerBaseline = (HEADER - fm.getHeight()) / 2 + fm.getAscent() + 1;
        g.drawString(taken ? "Damage Taken" : "Damage Dealt", 6, headerBaseline);
        int y = HEADER;
        for (int i = 0; i < rows.size(); i++)
        {
            Map.Entry<String, Long> entry = rows.get(i);
            g.setColor(plugin.colourFor(entry.getKey()));
            g.fillRect(0, y, (int) (width * (entry.getValue() / (double) max)), ROW - 1);
            g.setColor(Color.WHITE);
            String left = (i + 1) + ". " + entry.getKey();
            String right = String.format(Locale.ROOT, "%,d (%.1f, %.1f%%)", entry.getValue(), entry.getValue() / elapsed, total == 0 ? 0 : entry.getValue() * 100.0 / total);
            int rightX = width - 5 - fm.stringWidth(right);
            while (left.length() > 4 && fm.stringWidth(left) > rightX - 10) left = left.substring(0, left.length() - 1);
            g.drawString(left, 5, y + 15);
            g.drawString(right, Math.max(5, rightX), y + 15);
            y += ROW;
            List<SpecResult> playerSpecs = specs.get(entry.getKey());
            if (playerSpecs != null)
            {
                int x = 4;
                for (SpecResult spec : playerSpecs)
                {
                    if (x + 24 > width) break;
                    BufferedImage icon = items.getImage(spec.itemId);
                    if (icon != null)
                    {
                        if (spec.damage <= 0)
                            icon = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(icon, null);
                        g.drawImage(icon, x, y, 24, 24, null);
                        if (spec.damage < 0)
                        {
                            g.setColor(Color.BLACK); g.drawString("?", x + 17, y + 23);
                            g.setColor(Color.WHITE); g.drawString("?", x + 16, y + 22);
                        }
                    }
                    x += 25;
                }
                y += 24;
            }
            Set<DamageEffect> playerEffects = effects.get(entry.getKey());
            if (playerEffects != null)
            {
                int x = 4;
                for (DamageEffect effect : DamageEffect.values())
                {
                    if (!playerEffects.contains(effect) || x + 20 > width) continue;
                    BufferedImage sprite = effectSprites.get(effect);
                    if (sprite != null)
                    {
                        g.drawImage(sprite, x, y + 1, 20, 20, null);
                    }
                    else
                    {
                        g.setColor(effect.colour); g.fillOval(x + 1, y + 3, 16, 16);
                        g.setColor(Color.WHITE);
                        g.drawString(effect.symbol, x + 9 - fm.stringWidth(effect.symbol) / 2, y + 16);
                    }
                    x += 22;
                }
                y += 24;
            }
        }
        g.setClip(previousClip);
        return new Dimension(width, height);
    }
}
