package com.partydamage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.game.SpriteManager;
import net.runelite.api.gameval.SpriteID;

class PartyDamageOverlay extends Overlay
{
    private static final int WIDTH = 206, HEADER = 22, ROW = 20;
    private final PartyDamagePlugin plugin;
    private final SpecialIconCache icons;
    private final boolean taken;
    private final SpriteManager sprites;
    private final Map<DamageEffect, BufferedImage> effectSprites = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, RowValue> rowValues = new LinkedHashMap<String, RowValue>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, RowValue> eldest) { return size() > 128; }
    };
    private final Map<String, SpecialStrip> specialStrips = new LinkedHashMap<String, SpecialStrip>(32, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, SpecialStrip> eldest) { return size() > 128; }
    };

    private static final class SpecialStrip
    {
        final List<SpecResult> specs;
        final int count;
        final java.awt.Font font;
        final BufferedImage image;
        SpecialStrip(List<SpecResult> specs, int count, java.awt.Font font, BufferedImage image)
        { this.specs = specs; this.count = count; this.font = font; this.image = image; }
    }

    private static final class RowValue
    {
        final long damage;
        final long total;
        final double elapsed;
        final String text;
        RowValue(long damage, long total, double elapsed)
        {
            this.damage = damage;
            this.total = total;
            this.elapsed = elapsed;
            this.text = String.format(Locale.ROOT, "%,d (%.1f, %.1f%%)", damage, damage / elapsed,
                total == 0 ? 0 : damage * 100.0 / total);
        }
    }

    @Inject PartyDamageOverlay(PartyDamagePlugin plugin, SpecialIconCache icons, SpriteManager sprites)
    {
        this(plugin, icons, sprites, false);
    }
    PartyDamageOverlay(PartyDamagePlugin plugin, SpecialIconCache icons, SpriteManager sprites, boolean taken)
    {
        super(plugin);
        this.plugin = plugin;
        this.icons = icons;
        this.sprites = sprites;
        this.taken = taken;
        setPosition(taken ? OverlayPosition.TOP_RIGHT : OverlayPosition.TOP_LEFT);
        setResizable(true);
        setMinimumSize(32);
    }

    void loadEffectSprites()
    {
        if (!taken) return;
        sprites.getSpriteAsync(SpriteID.Hitmark.HITSPLAT_GREEN_POISON, 0, image -> effectSprites.put(DamageEffect.POISON, image));
        sprites.getSpriteAsync(SpriteID.Hitmark.HITSPLAT_DARK_GREEN_VENOM, 0, image -> effectSprites.put(DamageEffect.VENOM, image));
        sprites.getSpriteAsync(SpriteID.Hitmark.BURN_DAMAGE, 0, image -> effectSprites.put(DamageEffect.BURN, image));
    }

    @Override public Dimension render(Graphics2D graphics)
    {
        if (taken ? !plugin.showTakenOverlay() : !plugin.showOverlay()) return null;
        Graphics2D g = (Graphics2D) graphics.create();
        FontMetrics fm = g.getFontMetrics();
        int header = HEADER, row = ROW, iconRow = 24;
        Map<String, Long> data = taken ? plugin.overallTaken().snapshot() : plugin.snapshot();
        Map<String, List<SpecResult>> specs = taken || !plugin.showSpecialAttacks() ? java.util.Collections.emptyMap() : plugin.specSnapshot();
        Map<String, Set<DamageEffect>> effects = taken && plugin.showDamageEffects() ? plugin.overallTaken().effectSnapshot() : java.util.Collections.emptyMap();
        List<Map.Entry<String, Long>> rows = new ArrayList<>(data.entrySet());
        rows.sort(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
        long total = rows.stream().mapToLong(Map.Entry::getValue).sum();
        long max = rows.isEmpty() ? 1 : Math.max(1, rows.get(0).getValue());
        double elapsed = taken ? plugin.overallTaken().seconds() : plugin.elapsedSeconds();
        Dimension preferred = getPreferredSize();
        int width = preferred == null ? WIDTH : Math.max(160, preferred.width);
        int contentHeight = header;
        for (Map.Entry<String, Long> entry : rows)
            contentHeight += row + ((specs.containsKey(entry.getKey()) || effects.containsKey(entry.getKey())) ? iconRow : 0);
        boolean showIcons = taken ? plugin.showDamageEffects() : plugin.showSpecialAttacks();
        boolean firstHasIcons = !rows.isEmpty() && (specs.containsKey(rows.get(0).getKey()) || effects.containsKey(rows.get(0).getKey()));
        int height = autoHeight(contentHeight, rows.isEmpty(), showIcons, firstHasIcons);
        Shape previousClip = g.getClip();
        g.clipRect(0, 0, width, height);
        g.setColor(new Color(25, 25, 25, 220 * plugin.overlayOpacity() / 100));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        String title = taken ? "Damage Taken" : "Damage Dealt";
        int headerBaseline = (header - fm.getHeight()) / 2 + fm.getAscent() + 1;
        g.drawString(title, 6, headerBaseline);
        int y = header;
        for (int i = 0; i < rows.size(); i++)
        {
            Map.Entry<String, Long> entry = rows.get(i);
            g.setColor(plugin.colourFor(entry.getKey()));
            g.fillRect(0, y, (int) (width * (entry.getValue() / (double) max)), row - 1);
            g.setColor(Color.WHITE);
            String left = (i + 1) + ". " + entry.getKey();
            RowValue value = rowValues.get(entry.getKey());
            if (value == null || value.damage != entry.getValue() || value.total != total || value.elapsed != elapsed)
            {
                value = new RowValue(entry.getValue(), total, elapsed);
                rowValues.put(entry.getKey(), value);
            }
            String right = value.text;
            int padding = 5;
            int rightX = width - padding - fm.stringWidth(right);
            while (left.length() > 4 && fm.stringWidth(left) > rightX - 2 * padding) left = left.substring(0, left.length() - 1);
            int baseline = y + 15;
            g.drawString(left, padding, baseline);
            g.drawString(right, Math.max(padding, rightX), baseline);
            y += row;
            List<SpecResult> playerSpecs = specs.get(entry.getKey());
            if (playerSpecs != null)
            {
                drawSpecialStrip(g, entry.getKey(), playerSpecs, width, y);
                y += iconRow;
            }
            Set<DamageEffect> playerEffects = effects.get(entry.getKey());
            if (playerEffects != null)
            {
                int x = 4;
                int iconSize = 20;
                for (DamageEffect effect : DamageEffect.values())
                {
                    if (!playerEffects.contains(effect) || x + iconSize > width) continue;
                    BufferedImage sprite = effectSprites.get(effect);
                    if (sprite != null)
                    {
                        g.drawImage(sprite, x, y + 1, iconSize, iconSize, null);
                    }
                    else
                    {
                        g.setColor(effect.colour); g.fillOval(x + 1, y + 3, 16, 16);
                        g.setColor(Color.WHITE);
                        g.drawString(effect.symbol, x + 9 - fm.stringWidth(effect.symbol) / 2, y + 16);
                    }
                    x += 22;
                }
                y += iconRow;
            }
        }
        g.setClip(previousClip);
        g.dispose();
        return new Dimension(width, height);
    }

    static int autoHeight(int contentHeight, boolean empty, boolean showIcons, boolean firstHasIcons)
    {
        return contentHeight + (empty ? ROW : 0) + (showIcons && !firstHasIcons ? 24 : 0);
    }

    private void drawSpecialStrip(Graphics2D g, String name, List<SpecResult> specs, int width, int y)
    {
        int step = 25;
        int iconHeight = 24;
        int padding = 4;
        int count = Math.min(specs.size(), Math.max(0, (width - padding) / step));
        if (count == 0) return;
        SpecialStrip cached = specialStrips.get(name);
        if (cached != null && cached.specs == specs && cached.count == count && cached.font.equals(g.getFont()))
        {
            g.drawImage(cached.image, padding, y, count * step, iconHeight, null);
            return;
        }
        BufferedImage strip = new BufferedImage(count * 25, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D stripGraphics = strip.createGraphics();
        stripGraphics.setFont(g.getFont());
        stripGraphics.setRenderingHints(g.getRenderingHints());
        boolean ready = true;
        for (int i = 0; i < count; i++)
        {
            SpecResult spec = specs.get(i);
            BufferedImage icon = icons.get(spec.itemId, spec.isGrey());
            if (icon == null) { ready = false; continue; }
            int x = i * 25;
            stripGraphics.drawImage(icon, x, 0, null);
            if (spec.isUnknown())
            {
                stripGraphics.setColor(Color.BLACK); stripGraphics.drawString("?", x + 17, 23);
                stripGraphics.setColor(Color.WHITE); stripGraphics.drawString("?", x + 16, 22);
            }
        }
        stripGraphics.dispose();
        if (ready) specialStrips.put(name, new SpecialStrip(specs, count, g.getFont(), strip));
        g.drawImage(strip, padding, y, count * step, iconHeight, null);
    }
}
