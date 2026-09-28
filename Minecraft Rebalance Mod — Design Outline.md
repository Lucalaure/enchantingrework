# Minecraft Rebalance Mod — Design Outline

Sep 26, 2026 · @Seeklab

## Vision and scope

The mod finishes Minecraft's half-built systems so that animals, farming and food matter for the whole game, not just the first night. Today most players find one reliable food, automate it, and never think about eating again; most animals are scenery once the player has a cow pen.

**Design pillars**

- **Every mob earns its slot.** Each animal gives the player something no other source does.
- **Variety beats volume.** A varied diet and a varied farm are rewarded more than one huge automated farm.
- **Upkeep, not punishment.** New obligations are light, predictable, visible before they bite, and automatable with effort.
- **Feels like vanilla.** Reuse existing items and blocks first, keep Mojang's art style, no tech-mod menus.
- **Tunable.** Every number lives in config or datapack JSON, and each module (animals, cooking, balance) can be switched off on its own.

**Target.** Java Edition on the 26.x drop line; the fourth 2026 drop (Java 26.4) is due in Q4 2026 and adds ice caves and a Freezing effect, which the polar bear design uses. Loader (Fabric or NeoForge) is still open.

**Out of scope for v1:** new dimensions, new biomes, redstone or tech blocks.

## Polar bears

Polar bears become the cold-biome companion: a befriendable mount that is fast on snow and in water, sheds fur for cold protection, and catches fish. Today they can't be bred or tamed, drop only fish, and exist mainly to punish players who wander near a cub.

**Befriending.** Feed a calm adult raw cod or salmon several times without hitting it; after a random number of feeds (like taming a wolf) it becomes *bonded* to that player. Cubs bond faster. Hostility towards strangers near cubs stays as it is.

**Breeding.** Two bonded bears breed on salmon. Cubs inherit their parents' bond.

**Mount.** A saddled, bonded bear can be ridden.

- Faster than a horse on snow, packed ice and powder snow, and never sinks into powder snow
- A strong swimmer: keeps full speed in water and doesn't dismount the rider, making it the first real water mount
- Slower than a horse on ordinary ground, so it stays a regional specialist, not a new default
- Immune to Freezing; its rider gets Freezing reduced while mounted, which makes it the natural ice-cave companion once 26.4 lands

**Fur.** Brushing a bonded bear yields polar fur on a cooldown, the same pattern as armadillo scutes. Fur is used to:

- Line any chestplate at the smithing table, giving Freezing and powder-snow protection without wearing leather
- Craft a fur bedroll: sleep anywhere once without resetting spawn, useful for long expeditions

**Fishing partner.** A bonded bear told to stay near water catches a fish every few minutes and leaves it on the bank or in an adjacent chest. It is a slow, steady fish supply that feeds into the cooking system.

**Guarding.** A bonded bear attacks hostile mobs that damage its owner, like a wolf, but won't follow the player through doors or into tight spaces because of its size.

## Animal husbandry

Farm animals get a hunger level and need a minimum amount of food each in-game day to survive; well-fed animals produce more, starving ones stop producing and eventually die. Right now a cow in a 1x1 hole lives forever on nothing, so animals cost nothing to keep and a pen never needs a second thought.

**Hunger states.** Each animal holds a food value that drains once per in-game day (20 real minutes).

| State | What happens | How the player can tell |
| --- | --- | --- |
| Well-fed | Can breed; bonus yield (see table below) | Hearts particles occasionally, healthy idle animations |
| Fed | Normal behaviour and yields | Nothing special |
| Hungry | Can't breed; no wool regrowth, eggs or milk | Complaining sounds, head-down idle animation |
| Starving | Loses 1 heart per in-game day until it dies | Thinner model, sounds get more frequent |

Starvation is deliberately slow: an animal goes from Fed to dead in several in-game days, so a missed afternoon never wipes a farm.

**Ways to feed.**

- **Grazing.** Cows, sheep, goats and horses eat grass blocks and tall grass, turning grass into dirt as sheep already do. A big enough pasture feeds a small herd for free.
- **Feeding trough.** A new block that holds feed and can be filled by hoppers. Animals within a few blocks eat from it when hungry. This is the automation path: automated animals need an automated crop supply behind them.
- **Hand feeding.** Using feed on an animal, as with breeding today.

**Diets.**

| Animal | Eats | Well-fed bonus |
| --- | --- | --- |
| Cow, mooshroom | Grass, wheat, hay bales | Extra chance of leather; milk also gives a short Regeneration buff |
| Sheep | Grass, wheat | Wool regrows faster; small chance of 2 wool per shear |
| Goat | Grass, wheat, hay bales | Milk as cow; horn drop chance up |
| Pig | Carrots, potatoes, beetroot, spoiled food | Extra meat on slaughter |
| Chicken | Seeds, sweet berries | Eggs laid more often |
| Rabbit | Carrots, dandelions | Rabbit's foot drop chance up |
| Polar bear | Raw fish | Fur cooldown shorter |

Pigs eating spoiled food gives them a real role as the farm's waste disposal (see Cooking and nutrition).

**Crowding.** Each animal wants a small amount of space. Above a density threshold, animals eat faster and lose their well-fed bonus. This discourages 1x1 cow stacking without banning compact farms outright.

**Which mobs are excluded.** Wild, untouched animals in the world don't starve, so natural spawns are never depleted. Hunger only starts once an animal has been bred, led, fed or penned by a player. Pets (wolves, cats, parrots) are left alone in v1.

## Cooking and nutrition

Food is split into five groups, eating the same thing repeatedly gives less and less, and multi-ingredient meals cooked in a new pot are the efficient way to stay topped up. The goal is that a player with three or four farms eats better than a player with one huge farm, without making a single-food diet fatal.

**Food groups.** Every food belongs to one or more groups. Each group has its own bar, visible in a new tab of the inventory screen.

| Group | Examples | If kept high | If it runs empty |
| --- | --- | --- | --- |
| Protein | Meat, fish, eggs | Small attack speed bonus | Mining speed slightly lower |
| Grain | Bread, cookies, cake | Hunger drains more slowly | Hunger drains faster |
| Produce | Carrots, potatoes, beetroot, apples, melon, berries | Faster natural regeneration | Natural regeneration slower |
| Dairy and sweet | Milk, honey, sugar, pumpkin pie | Shorter negative effect durations | No penalty |
| Fungi | Mushrooms, mushroom and suspicious stew | Small resistance to poison and wither | No penalty |

Penalties are mild slowdowns, never damage. Three or more groups kept high gives **Well Nourished**: two extra hearts of max health. All five gives a small extra bonus to saturation.

**Food fatigue.** Each item remembers how often it was eaten recently. The fifth golden carrot in a row restores much less saturation than the first, and recovers as the player eats other things. This directly targets the one-food meta without banning any food.

**Cooking pot.** A new block placed over a campfire or on top of a furnace. It takes up to four ingredients plus a bowl and produces a meal.

- Meals cover several food groups at once, stack to 16 and restore more than their ingredients would separately
- Examples: beef stew (beef, carrot, potato, mushroom), fish chowder (cod or salmon, milk, potato), berry porridge (wheat, sweet berries, honey), rabbit stew moved here from the crafting table
- The pot can be fed by hoppers, so meals can be automated, but only with several farms feeding it

**Spoilage (optional module, off by default).** Raw meat and fish slowly spoil in inventories and chests. Cold biomes, ice or snow blocks next to a chest, and cooking all stop it. Spoiled food can be composted or fed to pigs. This is the most divisive idea in the doc, so it ships as a toggle.

**Underused foods get a job.** Beetroot, dried kelp, glow berries, pumpkin pie, chorus fruit and honey each get at least one pot recipe or a food group that makes them worth growing.

## Other abandoned features

Four more half-finished pieces fit the same themes and are cheap to add once the core systems exist; they're candidates, ranked by how well they support the rest of the mod.

| Feature | Problem today | Proposal | Priority |
| --- | --- | --- | --- |
| Bats | Pure ambience; guano was proposed years ago and never shipped | Bats roosting above a block leave guano, a stronger bone meal that feeds the crop farms behind troughs and pots | v1 |
| Fletching table | Only a villager job block; no player use | Arrow workbench: craft arrows in bulk, make tipped arrows from a single potion, add feathered arrows that fly further (chicken feathers give chickens more value) | v1 |
| Luck | The Luck attribute exists but the Potion of Luck is creative-only | Make it brewable from a well-fed rabbit's foot plus a new ingredient; Luck improves fishing, chest loot and polar bear catches | v1 |
| Pandas | Mostly decorative | Befriended pandas harvest bamboo nearby and occasionally sneeze up a mushroom or seeds, a slow forager | Stretch |

### Fletching table

The fletching table becomes the archery workbench: cheaper arrows, cheaper tipped arrows, specialty arrows, and bow repair without XP. It gets a four-slot screen: tip, shaft, fletching, and an optional potion slot.

- **Bulk arrows.** Flint, stick and feather make 8 arrows here instead of 4 at the crafting table.
- **Tipped arrows from one potion.** 8 arrows plus any drinkable potion makes 8 tipped arrows. Vanilla needs a lingering potion, which means dragon's breath, so tipped arrows are rarely worth making today.
- **Restringing.** A bow or crossbow plus string restores a quarter of its durability, with no XP cost and no anvil penalty.

**Specialty arrows.** Swapping the tip or fletching changes how the arrow behaves; each uses materials that currently have few uses.

| Arrow | Swap in | Effect |
| --- | --- | --- |
| Soaring | Phantom membrane as fletching | Much less drop over distance |
| Gust | Breeze rod as shaft | Knockback burst on impact |
| Tidal | Prismarine shard as tip | Keeps full speed underwater |
| Shattering | Amethyst shard as tip | Extra damage; the arrow breaks and can't be picked up |

Feathers matter more as a result, which makes well-fed chickens (see Animal husbandry) worth keeping. The fletcher villager can sell specialty arrows at master level.

### Luck

The Luck effect becomes brewable and nudges loot rarity, so an explorer can prepare for a big structure the way they'd prepare Fire Resistance for the Nether. Today Luck exists as an attribute and a creative-only potion, and only fishing reads it.

**Brewing.** An awkward potion plus a four-leaf clover makes Potion of Luck (5:00). Redstone extends it to 8:00; glowstone makes Luck II (1:30). Four-leaf clovers are a rare drop from breaking wildflowers, so they reward walking through meadows and flower forests.

**What Luck affects.**

- **Fishing:** unchanged from vanilla, and stacks with Luck of the Sea
- **Structure chests:** loot is generated when a chest is first opened, so Luck at that moment shifts modifier rolls towards Rare and Epic
- **Variant drops:** higher chance that a variant drops a modified item
- **Polar bear catches:** a bonded bear fishing near a lucky owner brings back rarer fish and treasure

Luck shifts the odds modestly and never guarantees an Epic, so drinking one before every chest stays optional rather than mandatory.

## Wider balance changes

A small set of balance changes supports the food and animal systems; each is its own toggle so players can take the mod's systems without its opinions.

- **Golden carrots and golden apples.** Keep them strong, but food fatigue applies. Golden carrots stay the best single item, just not the only thing worth eating.
- **Villager food trades.** Farmer and butcher trades for cooked food are tuned down so buying food from villagers doesn't replace running farms. Selling crops to villagers still works.
- **Natural regeneration.** Tied to the Produce group as well as saturation, so healing well means eating well.
- **Breeding cooldown.** Only well-fed animals can breed, replacing the flat cooldown as the thing that limits farm growth.
- **Horse feed.** Horses, donkeys and mules join the hunger system and gain a short speed buff when well-fed on hay, giving them an edge over their current flat stats.

Left alone on purpose: iron and raid farms, and elytra. They're common balance complaints but don't touch this mod's themes, and Mojang may change them in upcoming drops.

## How the systems interlock

The three core ideas are one loop, not three features: the animal hunger system creates demand for crops, and the cooking system rewards the variety that demand produces.

&#91;embedded content: how the systems feed each other · 9 parts\]

A player who automates only one crop can keep animals alive but can't cook balanced meals; the Well Nourished buff needs several farms working together. Spoiled food closes a second, smaller loop by going back to pigs.

## Mob progression

Hostile mobs get slowly stronger as a world ages and the further players travel from spawn, rare named variants appear with a special trait, and fair kills give enough XP that XP farms stop being necessary. Today a zombie on day 500 is the same zombie as on day 1, so combat stops mattering once the player has diamond gear.

**What drives the scaling.** Each hostile mob rolls a *tier* when it spawns, from two inputs:

- **World age:** one tier per 20 in-game days, starting after day 10 so the first week is untouched
- **Distance from spawn:** one extra tier per 2,000 blocks, so explorers meet tougher mobs before homebodies do

The Nether adds one tier and the End two. Vanilla's regional difficulty stays in place and keeps controlling gear and enchantment chances on mobs.

**What each tier gives** (starting values, all in config):

| Stat | Per tier | Cap |
| --- | --- | --- |
| Max health | +8% | +60% |
| Attack damage | +5% | +40% |
| Armor | +1 point | +6 |
| Knockback resistance | +3% | +20% |
| XP dropped | +15% | +100% |

Movement speed, creeper explosion size and skeleton accuracy never scale: faster or better-aimed mobs feel unfair rather than tougher, and bigger explosions just grief builds. Bosses and passive mobs don't scale. Game difficulty sets the caps: Easy halves them, Hard raises them by half.

**Special variants.** From day 20, each hostile spawn has a small chance (about 3%) to be a variant. A variant has one trait, a coloured name tag, a faint particle aura, and drops five times normal XP plus a chance at a modified item (see Loot modifiers).

| Variant | Trait | Tell |
| --- | --- | --- |
| Brute | Double health, strong knockback, slightly slower | Larger model, red name |
| Frostbound | Hits apply Freezing | Frost particles, pale blue name |
| Venomous | Hits apply Poison | Green drip particles |
| Warded | Takes half damage from projectiles | Faint shield shimmer |
| Vengeful | Calls nearby mobs of its type when hurt | Angry particles, dark red name |
| Volatile (creepers only) | Leaves lingering Weakness cloud; no extra block damage | Crackling sparks |

One trait per mob in v1; stacking traits is a later option.

**XP without farms.** Hostile XP roughly doubles at base and grows with tier, but only for *fair kills*: the player dealt most of the damage, the mob didn't come from a spawner, and it wasn't killed by fall, suffocation or entity cramming. Farm kills still give vanilla XP. Without that rule, higher XP would make XP farms stronger, not obsolete.

## Loot modifiers

Weapons and tools found in structure chests or dropped by variants carry one to three random modifiers that crafting and enchanting can't produce, and harder structures roll better ones. That makes a stronghold or ancient city worth raiding for gear, not just for its one-off loot.

**Rarity.** Uses vanilla's existing rarity colours so items read at a glance.

| Rarity | Modifiers | Best source |
| --- | --- | --- |
| Uncommon (yellow) | 1 minor | Villages, shipwrecks, ruined portals, mineshafts |
| Rare (aqua) | 2 minor, or 1 major | Desert and jungle temples, outposts, woodland mansions, variant drops |
| Epic (purple) | 2 major plus 1 unique | Strongholds, bastions, ancient cities, end cities, ominous trial vaults |

**Example modifiers.** All values are percentages so they still matter on netherite.

| Slot | Minor | Major | Unique (epic only) |
| --- | --- | --- | --- |
| Swords and axes | Keen: +crit damage · Swift: +attack speed | Sundering: ignores part of armor · Hunter's: bonus damage against variants | Vampiric: heals a little on each hit |
| Bows and crossbows | Taut: +arrow speed · Steady: less spread | Frostbite: arrows apply Freezing · Piercing volley: arrows pass through one extra mob | Homing: arrows curve slightly towards the target |
| Pickaxes and shovels | Hasty: +mining speed · Delver's: extra speed below y 0 | Reach: +1 block interaction range · Prospector's: nearby ore glints when mining | Earthmover: mines a 3x1 line |
| Axes and hoes (as tools) | Woodsman's: +speed on logs · Tended: hoes use less durability | Harvester's: hoes auto-replant crops | Feller: breaks a whole tree trunk |

**How they fit with vanilla.**

- Modifiers are separate from enchantments and stack with them; a modified sword can still be enchanted
- Anvil repairs, Mending and the netherite upgrade keep modifiers, so a great diamond find is never a dead end
- Combining two items in an anvil keeps the left item's modifiers only
- **Reforging:** at the smithing table, an echo shard rerolls one modifier on an item. It can't raise rarity, so exploring stays the only way to get epic gear

Harvester's and Prospector's tie back into farming and cooking; Hunter's and variant drops tie into Mob progression.

## Enchanting

The enchanting table uses four levers. XP level sets what you can reach, lapis sets the strength, a catalyst item chooses the main enchantment, and enchanted books in chiseled bookshelves steer the random extras. Players get real control, but they earn it by gathering materials and building a library, not by picking from a menu. Today the table shows one hint per option and re-rolls everything else, so most players skip it for librarian villagers.

**1. XP level sets the tier.** The player's current level and the number of bookshelves around the table decide which tier is open.

| Tier | Player level needed | Bookshelves | Max level of main enchantment | Random extras |
| --- | --- | --- | --- | --- |
| I | 5 | 0 | II | None |
| II | 15 | 8 | III | Up to 1 |
| III | 30 | 15 | Enchantment's max (IV or V) | Up to 2 |

**2. Lapis sets the level.** Each lapis adds one level to the main enchantment, up to the tier cap: 5 lapis and tier III gives Sharpness V. The table spends 2 XP levels per enchantment level, so Sharpness V costs 10 levels. That's steeper than vanilla, and it's affordable because of the higher fair-kill XP from Mob progression.

**3. A catalyst chooses the enchantment.** A new third slot takes one catalyst, which is used up. Each catalyst is a *theme*, and the item being enchanted decides which enchantment in that theme it becomes. The table shows the exact result (name, level and cost) before the player commits.

| Catalyst | Theme | Result by item |
| --- | --- | --- |
| Quartz | Edge | Sword and axe: Sharpness · Bow: Power · Crossbow: Piercing |
| Redstone | Speed | Tools: Efficiency · Crossbow: Quick Charge · Fishing rod: Lure |
| Obsidian | Endurance | Any item: Unbreaking |
| Emerald | Fortune | Tools: Fortune · Sword: Looting · Fishing rod: Luck of the Sea |
| Cobweb | Delicacy | Tools: Silk Touch |
| Blaze powder | Fire | Sword: Fire Aspect · Bow: Flame · Armor: Fire Protection |
| Glistering melon | Holy | Sword and axe: Smite |
| Fermented spider eye | Venom | Sword and axe: Bane of Arthropods |
| Slime ball | Force | Sword: Knockback · Bow: Punch |
| Iron ingot | Guard | Armor: Protection |
| Gunpowder | Blast | Armor: Blast Protection |
| Armadillo scute | Deflect | Armor: Projectile Protection |
| Feather | Air | Boots: Feather Falling |
| Prismarine crystals | Water | Helmet: Respiration · Boots: Depth Strider · Trident: Impaling |

With no catalyst, the table does a cheap vanilla-style random roll for 1 to 3 levels. The gamble stays available for anyone who wants it.

**4. Bookshelf resonance steers the extras.** Enchanted books placed in chiseled bookshelves within range of the table make their enchantments more likely to appear as random extras. The books aren't used up. This turns the enchanting room into a library the player builds up over a world's lifetime, and it gives chiseled bookshelves, which barely do anything today, a real job. Extras are never guaranteed: resonance only raises the odds. The table shows the single most likely extra as a hint.

**Re-enchanting.** At tier III, an already enchanted item can go back on the table to add one more main enchantment. Each pass costs more, like the anvil's repair penalty, and an item takes three table passes at most.

**Kept out of the table.** Treasure enchantments stay exploration-only: Mending, Frost Walker, Soul Speed, Swift Sneak, Wind Burst and curses. They don't resonate either, so a single Mending book can't spread through a library. Enchantments and loot modifiers stack freely on the same item.

## Open questions and milestones

The biggest open decision is what happens to animal hunger in unloaded chunks; the rest can be settled during playtesting.

**Open questions**

- [ ] Unloaded chunks: does hunger pause (vanilla-like, easy) or catch up on reload from troughs (realistic, harder, risks surprise deaths)?
- [ ] Loader: Fabric or NeoForge?
- [ ] Should wild animals that a player has only walked near ever join the hunger system?
- [ ] Is spoilage worth building at all, or cut from v1 entirely?
- [ ] Multiplayer: whose bond does a polar bear cub inherit when parents are bonded to different players?
- [ ] Should mob tier also read the player's gear, so a fresh player in an old world isn't overwhelmed?
- [ ] Should loot modifiers extend to armor in v1, or stay on weapons and tools?
- [ ] Librarian book trades now undercut the enchanting table: raise their prices, limit them to treasure enchantments, or leave them as they are?

**Milestones**

1. **Animals.** Hunger states, grazing, feeding trough, diets. Playtest a survival world for a week of real play before moving on.
2. **Cooking.** Food groups, food fatigue, cooking pot and the first 15 to 20 recipes.
3. **Mob progression.** Tiers, variants and fair-kill XP. Tune caps in a long-running test world.
4. **Loot modifiers.** Rarity, modifier pools, structure loot tables, reforging.
5. **Enchanting.** Tiers, lapis levels, catalyst slot, bookshelf resonance, re-enchanting. Balance XP costs against milestone 3.
6. **Polar bears.** Bonding, riding, fur, fishing partner. Line up with Java 26.4 so ice caves and Freezing exist.
7. **Rounding out.** Bats and guano, fletching table and specialty arrows, Luck potion, balance toggles.
8. **Stretch.** Pandas, spoilage module, stacked variant traits.
