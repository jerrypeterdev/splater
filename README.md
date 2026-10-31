# Splater

A party damage dealt and taken tracker.

An experimental RuneLite overlay inspired by Details. It ranks damage against NPCs, with total damage, DPS and each player's share of the recorded total.

## Setup

This is source code, not an installable Plugin Hub release. To test it, install a JDK and Gradle and run `gradle run` from this folder. Jagex account users can follow [RuneLite's development login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Each participant must run a compatible version of Splater. Open its sidebar, enter the same private group code and select Join group. For development convenience the code is saved in RuneLite settings and shown as masked text, but users must click Join group on every plugin start; it never joins at startup. No separate Party or DPS Counter plugin is required. The plugin uses RuneLite's shared party connection, so joining a group changes any RuneLite Party already joined, including one managed by the Party plugin. Leaving the group or disabling this plugin leaves the connection if it joined it. Share codes only with trusted group members.

The damage-dealt and damage-taken overlays can be moved and resized independently using RuneLite's overlay controls. Open the Splater sidebar using its dragon warhammer icon to show or hide either overlay, see totals, reset, and choose a colour for each player after they have recorded a hit. Colours are saved locally and do not change other players' displays. Party changes also clear the display. Resets are not shared: agree when to reset if comparing displays.

The sidebar tracks total damage and encounter damage independently. Reset total damage at the beginning of a raid, and reset encounter at the beginning of each boss or room. Each tracker includes NPC and player hits until its own reset; encounters and raids are not detected automatically. The in-game overlays use the respective overall trackers. Resetting total damage also clears the displayed special-attack icons; resetting an encounter does not. Each client resets its own counters.

The sidebar also tracks overall and encounter damage taken independently, including common damage, poison, venom, bleed and burn hitsplats on the local player. Each player must run a version with damage-taken sharing to contribute; earlier versions continue to share damage dealt but cannot report damage taken. Healing and non-health drains are excluded. Each counter has its own reset, which is local to that client. Damage-per-second values use the interval between the first and most recent recorded hit, with a one-second minimum, and pause after the latest hit. Special mechanics may use other hitsplat types and require in-game verification.

The damage-taken overlay shows one badge per damage-over-time type observed since the overall damage-taken reset. Poison, venom and burn use their game hitsplat sprites; bleed uses a B badge until its sprite can be identified. A letter badge also appears temporarily if a sprite is unavailable. Badges record that damage occurred, not whether the effect is currently active. Other party members need a compatible version to send effect details; older versions still send ordinary damage amounts without an effect badge.

Hold Alt to move or resize the overlay. Its height clips rows if made too short. The experimental special-attack display identifies an equipped item when special energy decreases and shares it with the group. This observation can be wrong when equipment is switched quickly or energy changes for another reason. Selected single-hit weapons attempt to match a target hitsplat: a positive number gives a coloured icon and zero a grey icon. A grey icon with a question mark means the result or weapon use could not be confirmed, including unsupported multi-hit, delayed or non-damaging specials; it is not a miss. These observations should not be used as proof of a player's actions. Results are not recovered after reconnecting and are reset locally with the meter. All participants must update to share special results. PvP and newly added weapons need in-game verification.

Experimental multi-hit matching covers dragon and abyssal daggers (two hits), burning claws (three) and dragon claws (four), when the hits arrive together at the expected tick. Missing or extra hits result in an unconfirmed icon. Other multi-hit, ranged, delayed and non-damaging specials are not fully classified. Normal attacks are counted independently for every individual hitsplat and do not require a weapon list.

## Measurement limits

The meter counts your own hitsplats on NPCs and other players and damage messages from other participants after you start recording. It does not recover earlier hits when you join late or reconnect, and people without this plugin or with a different passphrase are missing. Percentages are shares of recorded damage, not of the boss's health. All NPCs are included, including raid adds and unrelated NPCs; there is no automatic fight detection. Hitsplats may not represent all damage sources or effective boss HP, and special encounters need in-game verification. DPS is total damage divided by the elapsed time from the first to the most recent recorded hit, with a one-second minimum; gaps between fights count. The timer stops advancing after the last hit.

This prototype has not yet been tested in a live party or reviewed for the Plugin Hub.

