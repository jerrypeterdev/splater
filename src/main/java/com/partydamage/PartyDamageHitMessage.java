package com.partydamage;

import net.runelite.client.party.messages.PartyMemberMessage;

public class PartyDamageHitMessage extends PartyMemberMessage
{
    private final String name;
    private final int amount;
    private final int version;

    PartyDamageHitMessage(String name, int amount) { this.name = name; this.amount = amount; this.version = 1; }
    int version() { return version; }
    public String getName() { return name; }
    public int getAmount() { return amount; }
}
