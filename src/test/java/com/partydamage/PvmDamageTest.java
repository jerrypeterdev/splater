package com.partydamage;

import java.lang.reflect.Proxy;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PvmDamageTest
{
    private static Actor actor(Class<?> type)
    {
        return (Actor) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> null);
    }

    @Test public void onlyOurNpcHitsAreTracked()
    {
        assertTrue(PartyDamagePlugin.isNpcDamage(actor(NPC.class), true));
        assertFalse(PartyDamagePlugin.isNpcDamage(actor(NPC.class), false));
        assertFalse(PartyDamagePlugin.isNpcDamage(actor(Player.class), true));
        assertFalse(PartyDamagePlugin.isNpcDamage(null, true));
    }
}
