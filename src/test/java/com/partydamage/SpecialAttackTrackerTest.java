package com.partydamage;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.HitsplatID;
import net.runelite.api.Actor;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class SpecialAttackTrackerTest
{
    @Test public void scorchingBowDisplaysUseAndWarhammerUsesObservedDamage()
    {
        for (int value : new int[]{-1, 0, 32})
        {
            SpecResult bow = new SpecResult(ItemID.SCORCHING_BOW, value);
            assertFalse(bow.isGrey());
            assertFalse(bow.isUnknown());
            assertEquals("Scorching bow special used", bow.tooltip());
        }
        assertFalse(new SpecResult(ItemID.DRAGON_WARHAMMER, 32).isGrey());
        assertTrue(new SpecResult(ItemID.DRAGON_WARHAMMER, 0).isGrey());
        assertTrue(new SpecResult(ItemID.DRAGON_WARHAMMER, -1).isUnknown());
        assertEquals(0, SpecialAttackTracker.classify(ItemID.DRAGON_WARHAMMER, 1, 0));
        assertEquals(25, SpecialAttackTracker.classify(ItemID.DRAGON_WARHAMMER, 1, 25));
    }
    @Test public void scorchingBowMatchesArrowWithoutCountingBurn()
    {
        Actor target = (Actor) Proxy.newProxyInstance(Actor.class.getClassLoader(), new Class<?>[]{Actor.class}, (proxy, method, args) -> null);
        assertTrue(SpecialAttackTracker.supported(ItemID.SCORCHING_BOW));
        assertTrue(SpecialAttackTracker.isRanged(ItemID.SCORCHING_BOW));
        assertEquals(30, SpecialAttackTracker.matchHits(ItemID.SCORCHING_BOW, target, 12, Arrays.asList(
            new SpecialAttackTracker.ObservedHit(target, 12, 30),
            new SpecialAttackTracker.ObservedHit(target, 12, 1, HitsplatID.BURN))));
        assertEquals(0, SpecialAttackTracker.matchHits(ItemID.SCORCHING_BOW, target, 12, Arrays.asList(
            new SpecialAttackTracker.ObservedHit(target, 12, 0),
            new SpecialAttackTracker.ObservedHit(target, 12, 1, HitsplatID.BURN))));
        assertEquals(-1, SpecialAttackTracker.matchHits(ItemID.SCORCHING_BOW, target, 12,
            Collections.singletonList(new SpecialAttackTracker.ObservedHit(target, 12, 1, HitsplatID.BURN))));
    }
    @Test public void recognisesBlowpipeAndKistenSpecials()
    {
        assertTrue(SpecialAttackTracker.supported(ItemID.TOXIC_BLOWPIPE_LOADED));
        assertTrue(SpecialAttackTracker.supported(ItemID.TOXIC_BLOWPIPE_LOADED_ORNAMENT));
        assertTrue(SpecialAttackTracker.isRanged(ItemID.TOXIC_BLOWPIPE_LOADED));
        assertTrue(SpecialAttackTracker.supported(ItemID.CRIMSON_KISTEN));
        assertEquals(1, SpecialAttackTracker.expectedHits(ItemID.CRIMSON_KISTEN));
        assertEquals(25, SpecialAttackTracker.classify(ItemID.CRIMSON_KISTEN, 1, 25));
        assertEquals(0, SpecialAttackTracker.classify(ItemID.TOXIC_BLOWPIPE_LOADED, 1, 0));
        assertFalse(SpecialAttackTracker.supported(ItemID.VENATOR_BOW));
    }
    @Test public void multiHitResultsNeedAllHits()
    {
        assertEquals(19, SpecialAttackTracker.classify(ItemID.DRAGON_DAGGER_P, 2, 19));
        assertEquals(0, SpecialAttackTracker.classify(ItemID.ABYSSAL_DAGGER, 2, 0));
        assertEquals(31, SpecialAttackTracker.classify(ItemID.DRAGON_CLAWS, 4, 31));
        assertEquals(7, SpecialAttackTracker.classify(ItemID.BONE_CLAWS, 3, 7));
        assertEquals(12, SpecialAttackTracker.classify(ItemID.DRAGON_CLAWS, 2, 12));
        assertEquals(-1, SpecialAttackTracker.classify(ItemID.DRAGON_DAGGER, 3, 12));
        assertEquals(-1, SpecialAttackTracker.classify(ItemID.BGS, 0, -1));
    }

    @Test public void matchesDaggerHitsAcrossAdjacentTicksWithoutGuessingExtraHits()
    {
        Actor target = (Actor) Proxy.newProxyInstance(Actor.class.getClassLoader(), new Class<?>[]{Actor.class}, (proxy, method, args) -> null);
        Actor other = (Actor) Proxy.newProxyInstance(Actor.class.getClassLoader(), new Class<?>[]{Actor.class}, (proxy, method, args) -> null);
        SpecialAttackTracker.ObservedHit first = new SpecialAttackTracker.ObservedHit(target, 10, 5);
        SpecialAttackTracker.ObservedHit second = new SpecialAttackTracker.ObservedHit(target, 11, 8);
        SpecialAttackTracker.ObservedHit unrelated = new SpecialAttackTracker.ObservedHit(other, 11, 20);
        assertEquals(13, SpecialAttackTracker.matchHits(ItemID.DRAGON_DAGGER, target, 10, Arrays.asList(first, second, unrelated)));
        assertEquals(5, SpecialAttackTracker.matchHits(ItemID.DRAGON_DAGGER, target, 10, Collections.singletonList(first)));
        assertEquals(-1, SpecialAttackTracker.matchHits(ItemID.DRAGON_DAGGER, target, 10, Arrays.asList(first, second, first)));
        assertEquals(0, SpecialAttackTracker.matchHits(ItemID.DRAGON_DAGGER, target, 10, Arrays.asList(
            new SpecialAttackTracker.ObservedHit(target, 10, 0), new SpecialAttackTracker.ObservedHit(target, 10, 0))));
    }

    @Test public void supportsRangedAndDoesNotGuessMissingZeroHits()
    {
        Actor target = (Actor) Proxy.newProxyInstance(Actor.class.getClassLoader(), new Class<?>[]{Actor.class}, (proxy, method, args) -> null);
        assertEquals(110, SpecialAttackTracker.matchHits(ItemID.ZARYTE_XBOW, target, 12,
            Collections.singletonList(new SpecialAttackTracker.ObservedHit(target, 14, 110))));
        assertEquals(10, SpecialAttackTracker.matchHits(ItemID.TONALZTICS_OF_RALOS_CHARGED, target, 12,
            Collections.singletonList(new SpecialAttackTracker.ObservedHit(target, 12, 10))));
        assertEquals(-1, SpecialAttackTracker.matchHits(ItemID.DRAGON_DAGGER, target, 12,
            Collections.singletonList(new SpecialAttackTracker.ObservedHit(target, 12, 0))));
        assertEquals(-1, SpecialAttackTracker.matchHits(ItemID.ZARYTE_XBOW, target, 12,
            Collections.singletonList(new SpecialAttackTracker.ObservedHit(target, 15, 110))));
    }
}
