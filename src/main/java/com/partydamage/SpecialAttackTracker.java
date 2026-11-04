package com.partydamage;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Actor;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Records recognised hits and keeps unconfirmed energy-spend observations separate. */
final class SpecialAttackTracker
{
    private static final Logger LOG = LoggerFactory.getLogger(SpecialAttackTracker.class);
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private PartyDamagePlugin plugin;
    private int lastEnergy = -1;
    private Actor target;
    private int weapon;
    private int expectedTick;
    private int observed = -1;
    private int matches;
    private final List<ObservedHit> recentHits = new ArrayList<>();

    static final class ObservedHit
    {
        final Actor actor;
        final int tick;
        final int amount;
        final int type;
        ObservedHit(Actor actor, int tick, int amount) { this(actor, tick, amount, -1); }
        ObservedHit(Actor actor, int tick, int amount, int type) { this.actor = actor; this.tick = tick; this.amount = amount; this.type = type; }
    }

    void reset() { target = null; lastEnergy = -1; observed = -1; matches = 0; recentHits.clear(); }

    void energyChanged(VarbitChanged event)
    {
        if (plugin.isPvpArea()) { reset(); return; }
        if (event.getVarpId() != VarPlayerID.SA_ENERGY) return;
        int energy = event.getValue();
        int previous = lastEnergy;
        lastEnergy = energy;
        if (previous < 0 || energy >= previous) return;
        final int usedTick = client.getTickCount();
        Player localPlayer = client.getLocalPlayer();
        final Actor initialTarget = localPlayer == null ? null : localPlayer.getInteracting();
        clientThread.invokeLater(() -> {
            Player player = client.getLocalPlayer();
            ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
            if (player == null || equipment == null || plugin.isPvpArea()) return;
            Actor candidateTarget = initialTarget instanceof NPC ? initialTarget : player.getInteracting();
            if (!(candidateTarget instanceof NPC)) { target = null; return; }
            Item item = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
            if (item == null || item.getId() <= 0) return;
            if (target != null) plugin.recordLocalSpec(weapon, -1);
            if (item.getId() == ItemID.SCORCHING_BOW)
            {
                target = null;
                plugin.recordLocalSpec(item.getId(), 0);
                return;
            }
            if (!supported(item.getId()))
            {
                target = null;
                plugin.recordLocalSpec(item.getId(), -1);
                return;
            }
            target = candidateTarget;
            weapon = item.getId();
            expectedTick = usedTick + ((weapon == ItemID.ELDER_MAUL || weapon == ItemID.ELDER_MAUL_ORNAMENT || isRanged(weapon)) ? 2 : 1);
            observed = -1;
            matches = 0;
        });
    }

    void hit(HitsplatApplied event)
    {
        if (!PartyDamagePlugin.isNpcDamage(event.getActor(), event.getHitsplat().isMine())) return;
        int tick = client.getTickCount();
        recentHits.add(new ObservedHit(event.getActor(), tick, event.getHitsplat().getAmount(), event.getHitsplat().getHitsplatType()));
        recentHits.removeIf(hit -> hit.tick < tick - 6);
    }

    void tick(GameTick event)
    {
        if (plugin.isPvpArea()) { reset(); return; }
        if (lastEnergy < 0) lastEnergy = client.getVarpValue(VarPlayerID.SA_ENERGY);
        int tick = client.getTickCount();
        if (target == null || tick <= expectedTick + (isRanged(weapon) ? 2 : 1)) return;
        int result = matchHits(weapon, target, expectedTick, recentHits);
        LOG.debug("Special tracker item={} expectedTick={} result={}", weapon, expectedTick, result);
        plugin.recordLocalSpec(weapon, result);
        target = null;
        recentHits.removeIf(hit -> hit.tick < tick - 6);
    }

    static int matchHits(int weapon, Actor target, int expectedTick, List<ObservedHit> hits)
    {
        int count = 0;
        int damage = 0;
        int tolerance = isRanged(weapon) ? 2 : 1;
        for (ObservedHit hit : hits)
        {
            if (hit.actor == target && hit.tick >= expectedTick - tolerance && hit.tick <= expectedTick + tolerance
                && !(weapon == ItemID.SCORCHING_BOW && hit.type == HitsplatID.BURN))
            {
                count++;
                damage += hit.amount;
            }
        }
        return classify(weapon, count, count == 0 ? -1 : damage);
    }

    static boolean supported(int id)
    {
        return isRanged(id) || expectedHits(id) > 1 || id == ItemID.CRIMSON_KISTEN || id == ItemID.DRAGON_WARHAMMER || id == ItemID.DRAGON_WARHAMMER_ORNAMENT
            || id == ItemID.BGS || id == ItemID.BGSG
            || id == ItemID.ELDER_MAUL || id == ItemID.ELDER_MAUL_ORNAMENT
            || id == ItemID.ARCLIGHT || id == ItemID.EMBERLIGHT || id == ItemID.DARKLIGHT
            || id == ItemID.AGS || id == ItemID.AGSG || id == ItemID.SGS || id == ItemID.SGSG
            || id == ItemID.ZGS || id == ItemID.ZGSG || id == ItemID.ANCIENT_GODSWORD
            || id == ItemID.BRAIN_ANCHOR || id == ItemID.GRANITE_HAMMER
            || id == ItemID.DTTD_BONE_DAGGER || id == ItemID.DTTD_BONE_DAGGER_P
            || id == ItemID.DTTD_BONE_DAGGER_P_ || id == ItemID.DTTD_BONE_DAGGER_P__
            || id == ItemID.DRAGON_SCIMITAR || id == ItemID.DRAGON_SCIMITAR_ORNAMENT
            || id == ItemID.ABYSSAL_WHIP || id == ItemID.ABYSSAL_WHIP_LAVA || id == ItemID.ABYSSAL_WHIP_ICE;
    }
    static int classify(int id, int count, int damage)
    {
        if (count > 0 && count <= expectedHits(id) && damage > 0) return damage;
        return count == expectedHits(id) && damage == 0 ? 0 : -1;
    }
    static boolean isRanged(int id)
    {
        return id == ItemID.SCORCHING_BOW || id == ItemID.TOXIC_BLOWPIPE_LOADED || id == ItemID.TOXIC_BLOWPIPE_LOADED_ORNAMENT
            || id == ItemID.ZARYTE_XBOW || id == ItemID.TONALZTICS_OF_RALOS_CHARGED
            || id == ItemID.TONALZTICS_OF_RALOS_UNCHARGED;
    }
    static int expectedHits(int id)
    {
        if (id == ItemID.DRAGON_CLAWS || id == ItemID.DRAGON_CLAWS_ORNAMENT) return 4;
        if (id == ItemID.BONE_CLAWS) return 3;
        if (id == ItemID.TONALZTICS_OF_RALOS_CHARGED
            || id == ItemID.DRAGON_DAGGER || id == ItemID.DRAGON_DAGGER_P
            || id == ItemID.DRAGON_DAGGER_P_ || id == ItemID.DRAGON_DAGGER_P__
            || id == ItemID.ABYSSAL_DAGGER || id == ItemID.ABYSSAL_DAGGER_P
            || id == ItemID.ABYSSAL_DAGGER_P_ || id == ItemID.ABYSSAL_DAGGER_P__) return 2;
        return 1;
    }
}
