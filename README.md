# Enchanting Rework

A Fabric mod for Minecraft 26.3 that implements the **Enchanting** section of
`Minecraft Rebalance Mod — Design Outline.md`. The enchanting table runs on four levers:

1. **Three tier rows, like vanilla.** The table always shows three options, one per tier. Tier I needs level 5, tier II needs level 15
   and 8 shelves, tier III needs level 30 and 15 shelves. A row you haven't unlocked is greyed out, and its tooltip shows what it needs.
2. **A catalyst guarantees the enchantment.** A third slot (where the book used to float) takes one catalyst. The item decides which
   enchantment of the theme it gets, and **every row** gives that enchantment. You pick the row:
   tier I gives level II, tier II level III, tier III the enchantment's max, with 0 / 1 / 2 chances at a random extra.
3. **Lapis and XP pay for it.** A row costs lapis equal to the enchantment level, plus 2 XP levels per enchantment level
   (Sharpness V = 5 lapis, 10 levels). **Without a catalyst, the table works exactly like vanilla**: bookshelves set each row's
   level requirement, rows cost 1 / 2 / 3 levels and lapis, and hovering a row shows the vanilla clue ("Sharpness III . . . ?").

**Bookshelves:** normal bookshelves count as in vanilla, and so do chiseled bookshelves holding at least 3 books (`chiseledShelfMinBooks`).
The table's title bar always shows your tier and how many shelves it sees.
4. **Bookshelf resonance steers the extras.** Enchanted books in chiseled bookshelves near the table raise the odds of their enchantments showing up as extras.
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

The first enchantment in the list that supports the item wins; books take the first entry.

## Config

Every number lives in `config/enchantingrework.json` (created on first launch): tier requirements and caps, XP per level,
extra chance, re-enchant penalty and pass limit, resonance radius and weight, and the no-catalyst gamble.

## Code map

- `table/EnchantingLogic` – tiers, catalyst preview, extras, resonance, gamble roll
- `table/ReworkedEnchantmentMenu` – slots (item, lapis, catalyst), syncs the preview to the client, performs the enchant
- `mixin/EnchantingTableBlockMixin` – makes the enchanting table open the reworked menu
- `catalyst/Catalyst` – the synced `enchantingrework:catalyst` datapack registry
- `client/screen/ReworkedEnchantmentScreen` – the table UI

## Building

Requires JDK 25. `./gradlew build` (jar in `build/libs`), `./gradlew runClient` to play in a dev instance.
