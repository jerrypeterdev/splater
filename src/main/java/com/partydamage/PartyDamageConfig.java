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

    @ConfigItem(keyName = "showSpecialAttacks", name = "Show special attacks", description = "Show special attack icons", hidden = true)
    default boolean showSpecialAttacks() { return true; }

    @ConfigItem(keyName = "showDamageEffects", name = "Show damage effects", description = "Show poison, venom, bleed and burn icons", hidden = true)
    default boolean showDamageEffects() { return true; }

    @ConfigItem(keyName = "showSidebarSpecialAttacks", name = "Sidebar special attacks", description = "Show special attacks in the sidebar", hidden = true)
    default boolean showSidebarSpecialAttacks() { return true; }

    @ConfigItem(keyName = "showSidebarDamageEffects", name = "Sidebar damage effects", description = "Show damage effects in the sidebar", hidden = true)
    default boolean showSidebarDamageEffects() { return true; }
}
