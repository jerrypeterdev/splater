package com.partydamage;

import net.runelite.api.gameval.ItemID;

final class SpecResult
{
    final int itemId;
    final int damage;
    SpecResult(int itemId, int damage) { this.itemId = itemId; this.damage = damage; }
    boolean isScorchingBow() { return itemId == ItemID.SCORCHING_BOW; }
    boolean isUnknown() { return !isScorchingBow() && damage < 0; }
    boolean isGrey() { return !isScorchingBow() && damage <= 0; }
    String tooltip() { return isScorchingBow() ? "Scorching bow special used" : isUnknown() ? "Special attack result unconfirmed" : "Special attack damage: " + damage; }
}
