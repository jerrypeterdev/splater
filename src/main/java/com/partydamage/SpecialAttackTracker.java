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

    private static final class ObservedHit
    {
        final Actor actor;
        final int tick;
        final int amount;
        ObservedHit(Actor actor, int tick, int amount) { this.actor = actor; this.tick = tick; this.amount = amount; }
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
        clientThread.invokeLater(() -> {
            Player player = client.getLocalPlayer();
            ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
            if (player == null || equipment == null || plugin.isPvpArea()) return;
            if (!(player.getInteracting() instanceof NPC)) { target = null; return; }
            Item item = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
            if (item == null || item.getId() <= 0) return;
            if (target != null) plugin.recordLocalSpec(weapon, -1);
            if (!supported(item.getId()))
            {
                target = null;
                plugin.recordLocalSpec(item.getId(), -1);
                return;
            }
            target = player.getInteracting();
            weapon = item.getId();
            expectedTick = usedTick + ((weapon == ItemID.ELDER_MAUL || weapon == ItemID.ELDER_MAUL_ORNAMENT) ? 2 : 1);
            observed = -1;
            matches = 0;
        });
    }

    void hit(HitsplatApplied event)
    {
        if (!PartyDamagePlugin.isNpcDamage(event.getActor(), event.getHitsplat().isMine())) return;
        int tick = client.getTickCount();
        recentHits.add(new ObservedHit(event.getActor(), tick, event.getHitsplat().getAmount()));
        recentHits.removeIf(hit -> hit.tick < tick - 4);
    }

    void tick(GameTick event)
    {
        if (plugin.isPvpArea()) { reset(); return; }
        if (lastEnergy < 0) lastEnergy = client.getVarpValue(VarPlayerID.SA_ENERGY);
        int tick = client.getTickCount();
        if (target == null || tick <= expectedTick + 1) return;
        int selectedResult = -1;
        int candidates = 0;
        for (int candidateTick = expectedTick - 1; candidateTick <= expectedTick + 1; candidateTick++)
        {
            observed = -1;
            matches = 0;
            for (ObservedHit hit : recentHits)
            {
                if (hit.actor == target && hit.tick == candidateTick)
                {
                    if (observed < 0) observed = 0;
                    observed += hit.amount;
                    matches++;
                }
            }
            int result = classify(weapon, matches, observed);
            LOG.debug("Special tracker item={} tickOffset={} matchingHits={} damage={} candidate={}",
                weapon, candidateTick - expectedTick, matches, observed, result >= 0);
            if (result >= 0) { selectedResult = result; candidates++; }
        }
        plugin.recordLocalSpec(weapon, candidates == 1 ? selectedResult : -1);
        target = null;
        recentHits.removeIf(hit -> hit.tick < tick - 4);
    }

    static boolean supported(int id)
    {
        return expectedHits(id) > 1 || id == ItemID.DRAGON_WARHAMMER || id == ItemID.DRAGON_WARHAMMER_ORNAMENT
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
        return count == expectedHits(id) && damage >= 0 ? damage : -1;
    }
    static int expectedHits(int id)
    {
        if (id == ItemID.DRAGON_CLAWS || id == ItemID.DRAGON_CLAWS_ORNAMENT) return 4;
        if (id == ItemID.BONE_CLAWS) return 3;
        if (id == ItemID.DRAGON_DAGGER || id == ItemID.DRAGON_DAGGER_P
            || id == ItemID.DRAGON_DAGGER_P_ || id == ItemID.DRAGON_DAGGER_P__
            || id == ItemID.ABYSSAL_DAGGER || id == ItemID.ABYSSAL_DAGGER_P
            || id == ItemID.ABYSSAL_DAGGER_P_ || id == ItemID.ABYSSAL_DAGGER_P__) return 2;
        return 1;
    }
}
