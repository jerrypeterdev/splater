package com.partydamage;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EncounterSpecialsTest
{
    @Test public void overallAndEncounterSpecialsResetIndependently()
    {
        PartyDamagePlugin plugin = new PartyDamagePlugin();
        plugin.addSpec("Mouldy", 1, 12);
        assertEquals(1, plugin.specSnapshot().get("Mouldy").size());
        assertEquals(1, plugin.encounterSpecSnapshot().get("Mouldy").size());
        plugin.resetEncounter();
        assertTrue(plugin.encounterSpecSnapshot().isEmpty());
        assertEquals(1, plugin.specSnapshot().get("Mouldy").size());
        plugin.addSpec("Mouldy", 2, 0);
        plugin.reset();
        assertTrue(plugin.specSnapshot().isEmpty());
        assertEquals(1, plugin.encounterSpecSnapshot().get("Mouldy").size());
        assertFalse(plugin.encounterSnapshot().isEmpty());
    }
}
