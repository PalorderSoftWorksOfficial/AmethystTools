# AmethystTools

A Fabric mod for Minecraft 1.21.11 that adds 3x3 amethyst mining tools with a simple in-game menu.

## Commands

`/amethysttools`

Opens the tool menu.

`/amethysttools <tier> <type>`

Gives the tool to the player running the command.

`/amethysttoolsreload`

Reloads the configuration.

`/amethysttoolsgive <player> <tier> <type>`

Gives a tool to another player.

## Tiers

Wooden
Stone
Iron
Copper
Golden
Diamond
Netherite

## Tool Types

Pickaxe
Shovel
Axe
Hoe
Sword

## Enchantments

The tools are normal vanilla items, so every vanilla enchantment works on them: Fortune and Silk Touch
apply to every block the tool breaks (not only the block you aimed at), and Efficiency, Unbreaking,
Mending and the rest behave as usual. Enchant them at an enchanting table, on an anvil, or with
`/enchant`.

Everything the mod adds is data driven: the tools are vanilla items, the menu is a vanilla chest
GUI and the enchantments ride Minecraft's normal datapack registry sync, so **vanilla clients can
join a server with this mod installed without installing anything themselves**. Every enchantment
description also carries a `fallback`, so a player without the resource pack still reads "Blocks"
and "Vein Miner" instead of raw translation keys.

Five custom enchantments are added:

`amethysttools:blocks` (max level 5)

Grows the mined area. A plain tool mines 3x3, Blocks I mines 4x4, Blocks II mines 5x5 and every
further level adds another row and column, so Blocks V mines 8x8.

`amethysttools:veinminer` (max level 5)

Breaks the whole vein connected to the block you mined. The vein is capped at 16 blocks per level
(Vein Miner V mines up to 80 connected blocks), and by default only ore blocks are affected.

`amethysttools:depth` (max level 5)

Pushes the mined cut one more block into the wall you are facing per level, and deepens the
vertical cut from 2 to 2+level blocks.

`amethysttools:resonance` (max level 5)

Every extra block the tool breaks has a 10% chance per level to chime back an amethyst shard.

`amethysttools:experience` (max level 5)

Adds 1 experience per level to every block the tool breaks, using the vanilla block experience
effect, so Silk Touch still zeroes it.

## Configuration

`config/amethysttools.properties` also controls the enchantments:

`enchant.blocks-enabled=true`

`enchant.blocks-size-per-level=1` (extra rows and columns per Blocks level)

`enchant.veinminer-enabled=true`

`enchant.veinminer-ores-only=true`

`enchant.veinminer-blocks-per-level=16`

`enchant.depth-enabled=true`

`enchant.resonance-enabled=true`

`enchant.resonance-chance-per-level=10` (percent per Resonance level)

The blocks Vein Miner treats as ore can be extended with the `amethysttools:veinminer_ores` block tag.

## Features

3x3 block breaking, scalable with the Blocks enchantment
Vein mining with the Vein Miner enchantment
Five custom enchantments: Blocks, Vein Miner, Depth, Resonance and Experience
Vanilla client friendly, no client mod required
Directional mining based on where the player is looking
Fortune and Silk Touch support for every block the tool breaks
Tool menu with tier and type selection
Custom item tagging so the tools survive renaming and inventory movement

## Installation

Place the built JAR into the `mods` folder on a Fabric 1.21.11 server or client.

## Notes

Copper tools are available in this port.
