<p align="center"><img src="docs/logo.png" width="160" alt="Enchanting Rework logo"></p>

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
2. **An enchanting template guarantees the enchantment.** A third slot (where the book used to float) takes one template.
   The item decides which enchantment of the template's theme it gets, and **every row** gives that enchantment. You pick the row:
   tier I gives level II, tier II level III, tier III the enchantment's max, with 0 / 1 / 2 chances at a random extra.
3. **Templates, lapis and XP pay for it.** A guaranteed enchant uses up one template, plus lapis that climbs with the
   enchantment level (1 / 2 / 3 / 5 / 8 for levels I–V, `materialCostByLevel`) and 2 XP levels per enchantment level
   (Sharpness V = 1 Edge template + 8 lapis + 10 levels). Every row's green number is the level you need.
   **Without a template, the rows are vanilla random rolls inside the tier system**: row N unlocks with tier N and caps every
   enchantment it rolls at that tier's level, bookshelves set each row's power, rows cost 1 / 2 / 3 levels and lapis, and
   hovering a row shows the vanilla clue ("Sharpness III . . . ?").
4. **Bookshelf resonance steers the odds.** Enchanted books in chiseled bookshelves near the table raise the odds of their enchantments showing up,
   both in no-catalyst rolls and as catalyst extras. Resonance only changes *which* enchantments appear; the tier still caps their level.
   The books aren't used up. The table shows the most likely extra as a hint.

**One trip to the table:** an item can only be enchanted at the table once. Already enchanted items show
"Already enchanted"; the anvil still works as in vanilla.

**Advancements:** *By Design* (enchant an item using an Enchanting Template) and *Well Read* (enchant an item while
enchanted books resonate from nearby chiseled bookshelves), both following vanilla's *Enchanter*. Datapacks can make
their own with the `enchantingrework:table_enchant` trigger (`catalyst`: true/false, `resonant_books`: a range).

**Kept out of the table:** treasure enchantments (Mending, Frost Walker, Soul Speed, Swift Sneak, Wind Burst, curses) never come from the table and never resonate.

## Enchanting templates

You never find templates: you **inscribe** them from an enchanted book at a crafting table. The book goes in the middle and
**comes back**, so one book of a theme lets you make that theme's templates forever. Each craft makes one template.

```
 M  A  M        M = theme material  (×4)
 A  B  A        A = amethyst shard  (×3)
 M  C  M        B = enchanted book  (kept)
                C = core            (×1)
```

| Template | Book (any level) | Material | Core | Gives |
| --- | --- | --- | --- | --- |
| Air | Feather Falling | Feather | Paper | Feather Falling |
| Force | Knockback, Punch | Slime ball | Paper | Knockback · Punch |
| Blast | Blast Protection | Gunpowder | Paper | Blast Protection |
| Deflect | Projectile Protection | Armadillo scute | Paper | Projectile Protection |
| Endurance | Unbreaking | Obsidian | Diamond | Unbreaking |
| Speed | Efficiency, Quick Charge, Lure | Block of Redstone | Diamond | Efficiency · Quick Charge · Lure |
| Edge | Sharpness, Power, Piercing | Block of Quartz | Diamond | Sharpness · Power · Piercing |
| Guard | Protection | Block of Iron | Diamond | Protection |
| Fire | Fire Aspect, Flame, Fire Protection | Blaze powder | Diamond | Fire Aspect · Flame · Fire Protection |
| Water | Respiration, Depth Strider, Impaling | Prismarine crystals | Diamond | Respiration · Depth Strider · Impaling |
| Holy | Smite | Glistering melon slice | Diamond | Smite |
| Venom | Bane of Arthropods | Fermented spider eye | Diamond | Bane of Arthropods |
| Fortune | Fortune, Looting, Luck of the Sea | Emerald | Echo shard | Fortune · Looting · Luck of the Sea |
| Delicacy | Silk Touch | Cobweb | Echo shard | Silk Touch |

- The material picks the theme, so a book with several enchantments can make a template for any theme it fits
  (a Sharpness + Unbreaking book makes Edge with quartz, Endurance with obsidian).
- Treasure books (Mending, Frost Walker, Soul Speed, Swift Sneak, Wind Burst) and curses can't be inscribed.
- Templates are named for what they grant ("Edge Enchantment") and laid out like vanilla smithing templates.
  Enchanted books list the templates they can make ("Can inscribe: Edge, Endurance").
- With [JEI](https://modrinth.com/mod/jei) installed, inscribing recipes appear in JEI's normal **Crafting** tab.
  Looking up any enchanted book (any level, any number of enchantments) shows every template it can make, with that
  book in the middle slot. Each template also has an info page. JEI is optional.

### Datapacks

Templates are catalysts, defined as datapack entries in `data/<namespace>/enchantingrework/catalyst/<name>.json`:

```json
{
  "item": "enchantingrework:edge_enchanting_template",
  "theme": "edge",
  "enchantments": ["minecraft:sharpness", "minecraft:power", "minecraft:piercing", "#enchantingrework:catalyst/edge"],
  "inscription": { "material": "minecraft:quartz", "core": "minecraft:diamond", "filler": "minecraft:amethyst_shard" }
}
```

The first enchantment in the list that supports the item wins; books take the first entry. Entries can be enchantment
tags (`"#mymod:blades"`); a tag expands to its members in order. `inscription` is optional; leave it out and the catalyst
item isn't craftable (any item can be a catalyst).

**Adding enchantments from other mods:** every built-in theme ends with an empty tag named after it, for example
`#enchantingrework:catalyst/edge`. Add your enchantment to it in a datapack or mod:

`data/enchantingrework/tags/enchantment/catalyst/edge.json`:

```json
{ "values": ["mymod:serration"] }
```

It becomes a fallback for items none of the vanilla Edge enchantments fit, and books holding it can be inscribed into Edge
templates. A brand-new catalyst's theme name shows as-is unless you add an `enchantingrework.theme.<theme>` translation.

## Config

Every number lives in `config/enchantingrework.json` (created on first launch): tier requirements and caps, XP per level,
extra chance, templates per enchant, lapis cost curve, resonance radius and weight, and chiseled bookshelf counting.

## Code map

- `table/EnchantingLogic` – tiers, catalyst preview, extras, resonance, gamble roll
- `table/ReworkedEnchantmentMenu` – slots (item, lapis, catalyst), syncs the preview to the client, performs the enchant
- `mixin/EnchantingTableBlockMixin` – makes the enchanting table open the reworked menu
- `catalyst/Catalyst` – the synced `enchantingrework:catalyst` datapack registry
- `template/EnchantingTemplates`, `template/InscribeTemplateRecipe` – the template items and the inscribing recipe
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
