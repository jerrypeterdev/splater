# Splater

**The Ultimate Damage Tracker**

Splater is a RuneLite plugin for tracking your group's damage dealt and taken during bossing, raids and other fights.



## Features

- Overall and encounter totals for damage dealt and taken, including damage per second.
- Separate movable, resizable overlays with custom player colours and background opacity.
- Experimental special-attack icons and indicators for poison, venom, bleed and burn damage.

## Getting started

To set up Splater:

1. Search for **Splater** in RuneLite's Plugin Hub and install it on each player's client.
2. Open the Splater sidebar using its icon.
3. Enter the same private **Group Code** on each client and select **Join group**.
4. Start fighting! Players appear as their damage is recorded.

Your group code is saved and masked, but you must select **Join group** each time the plugin starts. Only share the code with people you trust. Splater uses RuneLite's party connection, so joining a Splater group changes any RuneLite Party you're already in.

## Using the tracker

Use the sidebar to view and reset overall and encounter totals separately. Reset overall totals at the start of a raid and encounter totals before each boss. Resets only affect your own client, and encounters aren't detected automatically.

Hold **Alt** to move or resize either overlay. In the sidebar, click a coloured circle to change a player's colour, or use **Settings** to show and hide overlays and adjust their background opacity.

## What the numbers mean

Splater counts supported NPC hitsplats from participating players, including hits on adds. Tracking is paused in the Wilderness and in recognised PvP areas and worlds. Damage from before joining or reconnecting isn't recovered, and unusual mechanics may not be recorded. Percentages are shares of recorded group damage.

Special-attack tracking is experimental. A coloured icon usually represents recorded positive damage; a grey icon represents a recorded zero. A question mark means the result or weapon couldn't be confirmed, not necessarily a miss. The scorching bow is an exception: its coloured icon shows that the special was used, regardless of arrow damage. Damage-effect indicators show that an effect caused damage, not whether it is currently active.

## Feedback

Found a problem? [Open an issue](https://github.com/jerrypeterdev/splater/issues) and describe what happened. If you include screenshots, hide your group code and private information.

Splater is available under the [BSD 2-Clause licence](LICENSE).
