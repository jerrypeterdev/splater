package com.partydamage;

import net.runelite.api.HitsplatID;
import org.junit.Test;
import static org.junit.Assert.*;

public class DamageTakenSeriesTest
{
    @Test public void effectDamageIsCountedOnceAndResetWithSeries()
    {
        DamageTakenSeries series = new DamageTakenSeries();
        series.add("player", 4, DamageEffect.fromHitsplat(HitsplatID.POISON), 100);
        series.add("player", 3, DamageEffect.fromHitsplat(HitsplatID.POISON), 200);
        series.add("player", 2, DamageEffect.fromHitsplat(HitsplatID.BURN), 300);
        assertEquals(Long.valueOf(9), series.snapshot().get("player"));
        assertEquals(2, series.effectSnapshot().get("player").size());
        assertTrue(series.effectSnapshot().get("player").contains(DamageEffect.POISON));
        assertEquals(DamageEffect.BURN, DamageEffect.fromCode(DamageEffect.BURN.code));
        assertNull(DamageEffect.fromCode(0));
        series.reset();
        assertTrue(series.snapshot().isEmpty());
        assertTrue(series.effectSnapshot().isEmpty());
    }

    @Test public void cachedSnapshotsStayStableAndRefreshAfterChanges()
    {
        DamageTakenSeries series = new DamageTakenSeries();
        series.add("player", 4, DamageEffect.POISON, 100);
        java.util.Map<String, Long> before = series.snapshot();
        java.util.Map<String, java.util.Set<DamageEffect>> effects = series.effectSnapshot();
        assertSame(before, series.snapshot());
        assertSame(effects, series.effectSnapshot());
        series.add("player", 2, DamageEffect.BURN, 200);
        assertEquals(Long.valueOf(4), before.get("player"));
        assertEquals(Long.valueOf(6), series.snapshot().get("player"));
        assertFalse(effects.get("player").contains(DamageEffect.BURN));
        assertTrue(series.effectSnapshot().get("player").contains(DamageEffect.BURN));
        series.reset();
        assertTrue(series.snapshot().isEmpty());
        assertTrue(series.effectSnapshot().isEmpty());
    }
}
