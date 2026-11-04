package com.partydamage;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class OverlayHeightTest
{
    @Test public void heightFitsRowsAndEnabledIconSpace()
    {
        assertEquals(66, PartyDamageOverlay.autoHeight(22, true, true, false));
        assertEquals(42, PartyDamageOverlay.autoHeight(22, true, false, false));
        assertEquals(66, PartyDamageOverlay.autoHeight(42, false, true, false));
        assertEquals(66, PartyDamageOverlay.autoHeight(66, false, true, true));
        assertEquals(42, PartyDamageOverlay.autoHeight(42, false, false, false));
        assertEquals(110, PartyDamageOverlay.autoHeight(110, false, true, true));
    }
}
