package com.partydamage;

import javax.inject.Inject;
import net.runelite.client.game.SpriteManager;

final class PartyDamageTakenOverlay extends PartyDamageOverlay
{
    @Inject PartyDamageTakenOverlay(PartyDamagePlugin plugin, SpecialIconCache icons, SpriteManager sprites)
    {
        super(plugin, icons, sprites, true);
    }
}
