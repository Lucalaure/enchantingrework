# Enchanting Rework

A Fabric mod for Minecraft 26.3 that implements the **Enchanting** section of
`Minecraft Rebalance Mod — Design Outline.md`.

## Installing

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3 and put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder.
2. Download `enchantingrework-<version>.jar` from the [releases page](https://github.com/Lucalaure/enchantingrework/releases) and put it in `mods` too.
3. For multiplayer, the server and every player need the mod.

## How it works

The enchanting table runs on four levers:

1. **Three tier rows, like vanilla.** The table always shows three options, one per tier. Tier I needs level 5, tier II needs level 15
   and 8 shelves, tier III needs level 30 and 15 shelves. A row you haven't unlocked is greyed out, and its tooltip shows what it needs.
2. **A catalyst guarantees the enchantment.** A third slot (where the book used to float) takes one catalyst. The item decides which
   enchantment of the theme it gets, and **every row** gives that enchantment. You pick the row:
   tier I gives level II, tier II level III, tier III the enchantment's max, with 0 / 1 / 2 chances at a random extra.
3. **Lapis, catalysts and XP pay for it.** A catalyst row uses the same number of lapis and catalysts, climbing with the
   enchantment level: 1 / 2 / 3 / 5 / 8 for levels I–V (`materialCostByLevel`). It also costs 2 XP levels per enchantment
   level (Sharpness V = 8 quartz + 8 lapis + 10 levels). **Without a catalyst, the rows are vanilla random rolls inside the tier system**: row N unlocks
   with tier N and caps every enchantment it rolls at that tier's level, bookshelves set each row's power, rows cost 1 / 2 / 3 levels
   and lapis, and hovering a row shows the vanilla clue ("Sharpness III . . . ?").

**Bookshelves:** normal bookshelves count as in vanilla, and so do chiseled bookshelves holding at least 3 books (`chiseledShelfMinBooks`).
The table's title bar always shows your tier and how many shelves it sees.
4. **Bookshelf resonance steers the odds.** Enchanted books in chiseled bookshelves near the table raise the odds of their enchantments showing up,
   both in no-catalyst rolls and as catalyst extras. Resonance only changes *which* enchantments appear; the tier still caps their level.
   The books aren't used up. The table shows the most likely extra as a hint.

**Re-enchanting:** at tier III, an enchanted item can go back on the table to add or upgrade one main enchantment. Each pass costs more
(anvil-style doubling penalty), and an item takes at most three passes. Items show their pass count in the tooltip.

**Kept out of the table:** treasure enchantments (Mending, Frost Walker, Soul Speed, Swift Sneak, Wind Burst, curses) never come from the table and never resonate.

## Catalysts

| Catalyst | Theme | Result |
| --- | --- | --- |
| Quartz | Edge | Sharpness · Power · Piercing |
| Redstone | Speed | Efficiency · Quick Charge · Lure |
| Obsidian | Endurance | Unbreaking |
| Emerald | Fortune | Fortune · Looting · Luck of the Sea |
| Cobweb | Delicacy | Silk Touch |
| Blaze powder | Fire | Fire Aspect · Flame · Fire Protection |
| Glistering melon | Holy | Smite |
| Fermented spider eye | Venom | Bane of Arthropods |
| Slime ball | Force | Knockback · Punch |
| Iron ingot | Guard | Protection |
| Gunpowder | Blast | Blast Protection |
| Armadillo scute | Deflect | Projectile Protection |
| Feather | Air | Feather Falling |
| Prismarine crystals | Water | Respiration · Depth Strider · Impaling |

Catalysts are datapack entries, so packs can add or change them:
`data/<namespace>/enchantingrework/catalyst/<name>.json`

```json
{
  "item": "minecraft:quartz",
  "theme": "edge",
  "enchantments": ["minecraft:sharpness", "minecraft:power", "minecraft:piercing"]
}
```

The first enchantment in the list that supports the item wins; books take the first entry. Entries can be enchantment
tags (`"#mymod:blades"`); a tag expands to its members in order.

**Adding enchantments from other mods:** every built-in catalyst ends with an empty tag named after its theme, for example
`#enchantingrework:catalyst/edge`. To add your enchantment to the Edge theme, add it to that tag in a datapack or mod:

`data/enchantingrework/tags/enchantment/catalyst/edge.json`:

```json
{ "values": ["mymod:serration"] }
```

It becomes a fallback for items none of the vanilla Edge enchantments fit. To make a brand-new catalyst, add a new catalyst file;
its theme name shows as-is unless you add an `enchantingrework.theme.<theme>` translation.

## Config

Every number lives in `config/enchantingrework.json` (created on first launch): tier requirements and caps, XP per level,
extra chance, re-enchant penalty and pass limit, resonance radius and weight, and the no-catalyst gamble.

## Code map

- `table/EnchantingLogic` – tiers, catalyst preview, extras, resonance, gamble roll
- `table/ReworkedEnchantmentMenu` – slots (item, lapis, catalyst), syncs the preview to the client, performs the enchant
- `mixin/EnchantingTableBlockMixin` – makes the enchanting table open the reworked menu
- `catalyst/Catalyst` – the synced `enchantingrework:catalyst` datapack registry
- `client/screen/ReworkedEnchantmentScreen` – the table UI

## Releasing

Either way, GitHub Actions builds the mod and attaches the jar to the release (GitHub also always adds
"Source code" archives; those can't be removed).

- **From the command line:** set `version` in `gradle.properties`, add a matching `## [x.y.z]` section to
  [CHANGELOG.md](CHANGELOG.md), commit, then `git tag vX.Y.Z && git push origin vX.Y.Z`. The release is created
  for you with that changelog section as its notes.
- **From the GitHub website:** create and publish a release as usual. The jar appears on it a couple of minutes later.

## Building

Requires JDK 25. `./gradlew build` (jar in `build/libs`), `./gradlew runClient` to play in a dev instance.
