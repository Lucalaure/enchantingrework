# Changelog

All notable changes to Enchanting Rework. Versions follow [Semantic Versioning](https://semver.org).

## [1.0.0] - 2026-09-28

First release, for Minecraft 26.3 (Fabric).

### Enchanting table
- Three tier rows, unlocked by player level and bookshelves (tier I: level 5; tier II: level 15 + 8 shelves; tier III: level 30 + 15 shelves).
- Each tier caps enchantment levels at II / III / the enchantment's max and allows 0 / 1 / 2 random extras.
- **Catalyst slot:** 14 catalysts (quartz, redstone, obsidian, emerald, cobweb, blaze powder, glistering melon, fermented spider eye,
  slime ball, iron ingot, gunpowder, armadillo scute, feather, prismarine crystals) guarantee a themed enchantment.
  Rows I / II / III use 1 / 2 / 3 catalysts.
- **No catalyst:** vanilla random rolls, inside the tier system.
- **Bookshelf resonance:** enchanted books in chiseled bookshelves near the table raise the odds of their enchantments.
  Changing the books re-rolls the table; putting the same books back gives the same roll.
- **Re-enchanting:** at tier III, up to 3 table passes per item, each pass costing more.
- Treasure enchantments and curses never come from the table and never resonate.
- Chiseled bookshelves holding 3+ books count as bookshelves.

### Interface
- Catalyst items show their theme and possible enchantments in their tooltip.
- Row tooltips show tier requirements, costs, likely extras and which enchantments are resonating.
- The table's title bar shows your tier and bookshelf count.

### Configuration and data
- Every number is in `config/enchantingrework.json`; servers send their config to players on join.
- Catalysts are datapack entries and accept enchantment tags. Each theme includes an empty tag
  (e.g. `#enchantingrework:catalyst/edge`) that other mods and datapacks can add enchantments to.
