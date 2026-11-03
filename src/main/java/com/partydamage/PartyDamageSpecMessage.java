package com.partydamage;

import net.runelite.client.party.messages.PartyMemberMessage;

public class PartyDamageSpecMessage extends PartyMemberMessage
{
    private final String name;
    private final int itemId;
    private final int damage;
    private final int version;
    PartyDamageSpecMessage(String name, int itemId, int damage)
    { this.name = name; this.itemId = itemId; this.damage = damage; this.version = 1; }
    int version() { return version; }
    String name() { return name; }
    int itemId() { return itemId; }
    int damage() { return damage; }
}
