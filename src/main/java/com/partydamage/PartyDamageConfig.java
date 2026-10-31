package com.partydamage;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("partydamagemeter")
public interface PartyDamageConfig extends Config
{
    @ConfigItem(keyName = "showOverlay", name = "Show overlay", description = "Show the damage overlay", hidden = true)
    default boolean showOverlay() { return true; }

    @ConfigItem(keyName = "showTakenOverlay", name = "Show damage taken", description = "Show damage taken overlay", hidden = true)
    default boolean showTakenOverlay() { return true; }

    @ConfigItem(keyName = "overlayOpacity", name = "Overlay opacity", description = "Overlay opacity", hidden = true)
    default int overlayOpacity() { return 100; }
}
