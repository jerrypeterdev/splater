package com.partydamage;

import javax.inject.Inject;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

final class PartyDamageTakenOverlay extends PartyDamageOverlay
{
    @Inject PartyDamageTakenOverlay(PartyDamagePlugin plugin, ItemManager items, SpriteManager sprites)
    {
        super(plugin, items, sprites, true);
    }
}
