package com.partydamage;

import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;

final class DamageTakenSeries
{
    private final Map<String, Long> damage = new HashMap<>();
    private final Map<String, EnumSet<DamageEffect>> effects = new HashMap<>();
    private long first, last;

    synchronized void add(String player, int amount, DamageEffect effect, long now)
    {
        if (first == 0) first = now;
        last = now;
        damage.merge(player, (long) amount, Long::sum);
        if (effect != null) effects.computeIfAbsent(player, k -> EnumSet.noneOf(DamageEffect.class)).add(effect);
    }
    synchronized void reset() { damage.clear(); effects.clear(); first = 0; last = 0; }
    synchronized Map<String, Long> snapshot() { return new HashMap<>(damage); }
    synchronized Map<String, Set<DamageEffect>> effectSnapshot()
    {
        Map<String, Set<DamageEffect>> copy = new HashMap<>();
        effects.forEach((name, values) -> copy.put(name, EnumSet.copyOf(values)));
        return copy;
    }
    synchronized double seconds() { return first == 0 ? 1 : Math.max(1, (last - first) / 1_000_000_000.0); }
}
