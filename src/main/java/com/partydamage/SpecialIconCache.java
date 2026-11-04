package com.partydamage;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.color.ColorSpace;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

/** Loads and converts each item icon once instead of converting it on every frame. */
@Singleton
final class SpecialIconCache
{
    private static final int MAX_ITEMS = 128;
    @Inject private ItemManager items;
    @Inject private ClientThread clientThread;
    private final Map<Integer, IconPair> cache = Collections.synchronizedMap(new LinkedHashMap<Integer, IconPair>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, IconPair> eldest) { return size() > MAX_ITEMS; }
    });
    private final Set<Integer> loading = Collections.synchronizedSet(new HashSet<>());

    BufferedImage get(int itemId, boolean grey)
    {
        IconPair pair = cache.get(itemId);
        if (pair != null) return grey ? pair.grey : pair.colour;
        if (items != null && clientThread != null && loading.add(itemId))
        {
            clientThread.invokeLater(() -> {
                AsyncBufferedImage image = items.getImage(itemId);
                if (image == null) { loading.remove(itemId); return; }
                image.onLoaded(() -> {
                    try { cache.put(itemId, createIcons(image)); }
                    finally { loading.remove(itemId); }
                });
            });
        }
        return null;
    }

    static IconPair createIcons(BufferedImage image)
    {
        BufferedImage colour = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = colour.createGraphics();
        graphics.drawImage(image, 0, 0, 24, 24, null);
        graphics.dispose();
        BufferedImage grey = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(colour, null);
        return new IconPair(colour, grey);
    }

    static final class IconPair
    {
        final BufferedImage colour;
        final BufferedImage grey;
        IconPair(BufferedImage colour, BufferedImage grey) { this.colour = colour; this.grey = grey; }
    }
}
