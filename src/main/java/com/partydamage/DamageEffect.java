package com.partydamage;

import java.awt.Color;
import net.runelite.api.HitsplatID;

enum DamageEffect
{
    POISON(1, "P", new Color(67, 170, 76)),
    VENOM(2, "V", new Color(35, 119, 70)),
    BLEED(3, "B", new Color(190, 53, 53)),
    BURN(4, "F", new Color(235, 139, 38));

    final int code;
    final String symbol;
    final Color colour;
    DamageEffect(int code, String symbol, Color colour) { this.code = code; this.symbol = symbol; this.colour = colour; }
    static DamageEffect fromCode(int code)
    {
        for (DamageEffect effect : values()) if (effect.code == code) return effect;
        return null;
    }
    static DamageEffect fromHitsplat(int type)
    {
        if (type == HitsplatID.POISON) return POISON;
        if (type == HitsplatID.VENOM) return VENOM;
        if (type == HitsplatID.BLEED) return BLEED;
        if (type == HitsplatID.BURN) return BURN;
        return null;
    }
}
