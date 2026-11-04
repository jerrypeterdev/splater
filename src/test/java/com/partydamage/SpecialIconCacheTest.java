package com.partydamage;

import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class SpecialIconCacheTest
{
    @Test public void greyIconKeepsTransparencyAndDoesNotChangeOriginal()
    {
        BufferedImage source = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(4, 4, 0xffff0000);
        SpecialIconCache.IconPair pair = SpecialIconCache.createIcons(source);
        assertNotSame(pair.colour, pair.grey);
        assertEquals(0xffff0000, pair.colour.getRGB(4, 4));
        assertEquals(0, pair.grey.getRGB(0, 0) >>> 24);
        int pixel = pair.grey.getRGB(4, 4);
        assertEquals((pixel >> 16) & 255, (pixel >> 8) & 255);
        assertEquals((pixel >> 8) & 255, pixel & 255);
    }
}
