package com.partydamage;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SpecialAttackTrackerTest
{
    @Test public void multiHitResultsNeedAllHits()
    {
        assertEquals(19, SpecialAttackTracker.classify(ItemID.DRAGON_DAGGER_P, 2, 19));
        assertEquals(0, SpecialAttackTracker.classify(ItemID.ABYSSAL_DAGGER, 2, 0));
        assertEquals(31, SpecialAttackTracker.classify(ItemID.DRAGON_CLAWS, 4, 31));
        assertEquals(7, SpecialAttackTracker.classify(ItemID.BONE_CLAWS, 3, 7));
        assertEquals(-1, SpecialAttackTracker.classify(ItemID.DRAGON_CLAWS, 2, 12));
        assertEquals(-1, SpecialAttackTracker.classify(ItemID.DRAGON_DAGGER, 3, 12));
        assertEquals(-1, SpecialAttackTracker.classify(ItemID.BGS, 0, -1));
    }
}
