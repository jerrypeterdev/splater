package com.partydamage;

import net.runelite.client.party.messages.PartyMemberMessage;

public class PartyDamageTakenMessage extends PartyMemberMessage
{
    private final String name;
    private final int amount;
    private final int effect;
    PartyDamageTakenMessage(String name, int amount, int effect) { this.name = name; this.amount = amount; this.effect = effect; }
    String name() { return name; }
    int amount() { return amount; }
    int effect() { return effect; }
}
