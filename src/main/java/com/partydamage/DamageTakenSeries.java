package com.partydamage;

import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.Set;
import java.util.Collections;

final class DamageTakenSeries
{
    private final Map<String, Long> damage = new HashMap<>();
    private final Map<String, EnumSet<DamageEffect>> effects = new HashMap<>();
    private long first, last;
    private Map<String, Long> damageView;
    private Map<String, Set<DamageEffect>> effectView;

    synchronized void add(String player, int amount, DamageEffect effect, long now)
    {
        if (first == 0) first = now;
        last = now;
        damage.merge(player, (long) amount, Long::sum);
        damageView = null;
        effectView = null;
        if (effect != null) effects.computeIfAbsent(player, k -> EnumSet.noneOf(DamageEffect.class)).add(effect);
    }
    synchronized void reset() { damage.clear(); effects.clear(); damageView = null; effectView = null; first = 0; last = 0; }
    synchronized Map<String, Long> snapshot()
    {
        if (damageView == null) damageView = Collections.unmodifiableMap(new HashMap<>(damage));
        return damageView;
    }
    synchronized Map<String, Set<DamageEffect>> effectSnapshot()
    {
        if (effectView == null)
        {
            Map<String, Set<DamageEffect>> copy = new HashMap<>();
            effects.forEach((name, values) -> copy.put(name, Collections.unmodifiableSet(EnumSet.copyOf(values))));
            effectView = Collections.unmodifiableMap(copy);
        }
        return effectView;
    }
    synchronized double seconds() { return first == 0 ? 1 : Math.max(1, (last - first) / 1_000_000_000.0); }
}
