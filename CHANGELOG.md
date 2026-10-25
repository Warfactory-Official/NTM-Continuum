# Changelog

Each release has a `## <version>` section; the release workflow uploads only the section matching
`mod.version` in gradle.properties.

## 1.0.2

Requires Golem Core 1.0.2. Use GemRender 0.1.18.

### Added
- Configurability with 1.7 defaults: every 1.7 machine config in `hbmMachines.json` (synced to clients); radiation walls
  and immunity tags, biome and dimension ambient radiation, mob transformations, cladding and gas mask blacklists as data;
  consumable treatments as the `hbm:consumable_treatment` item data map; every worldgen feature configurable.
- Looting mob drops and structure chest loot are loot tables (`hbm:chests/<pool>`), filled the way 1.7 fills them.
- Refined Storage importers, AE2 buses and item pipes can take items from HBM machines on NeoForge and Forge.
- Voxy support for HBM's placed fluids.
- Every 1.7 NEI tab has a recipe viewer category: ten new tabs (annihilator, ore slopper, outgasser, deuterium, grenade
  crafting, ZIRNOX, PWR, Watz, RBMK rod disassembly, waste decay) and the combination furnace, in 1.7's order in REI.
- "Press O to Duck!" as in 1.7.
- 1.7's coal dust and asbestos warnings, the reacher, explosion debris particles and the rarity table.
- Raw aluminium makes fireclay; `any_rubber` accepts latex.

### Fixed
- Nuked uranium ore turned into radon instead of sellafite ore; mined uranium ore and ancient scrap dropped nothing
  before 26.1.
- Held crates: crates picked up while one was open joined its stack and copied its contents (dupe); crates no longer
  take items that carry items.
- KubeJS machine outputs kept no item data (an empty Superb Warfare vehicle crate).
- Gem machines were invisible under a shader pack on 26.1; first-person guns were off screen under Iris on 1.21.1.
- Weapon mods, the AMR, StG 77, laser rifle and infinite water tank recipes were hidden in EMI.
- Crucible automation through its parts; the crucible slash no longer kills creative or spectator players.
- MK108 belt and Liberator animations, the B92 in hand below 1.21.4, helmet overlays below 26.1, chemthrower ammo name,
  1.7 absolute and creative damage flags.
- TELEX ghost text, the hotbar hidden by RBMK columns under ImmediatelyFast, and foods and cement eaten without the
  eating animation.
- The burner press takes coal coke again; asbestos ores and lab tiles outgas as in 1.7.
- Holo Inventory counted items twice at multiblock parts; cargo elevator picking as in 1.7.
- Wand, logic block, HIMARS rocket and jigsaw directions are saved by name.
- Plastic explosive, plane crash and torpedo explosions had no effects; the combine ball is exempt from nukes and 19 wood
  blocks burn as in 1.7.
- `machine_battery` is in no creative tab, as in 1.7.

### Changed
- Release jars are named without the version prefix.

## 1.0.1

Requires Golem Core 1.0.1. Use GemRender 0.1.18. Older versions are refused.

### Added
- AE2 support: pattern providers push into the seven 1.12.2 autocrafting machines, ME storage for the mass storage unit
  and the large arc furnace, radioactive storage cells as in 1.7, 528 explosive energistics and certus cluster recipes.
- Mining helmet lamp and occult shotgun flashlight cast real light through GemRender, with 1.7's block-light fallback
  without it.
- Mo' Bends support: HBM armour follows Mo' Bends animations (1.20.1 Forge, 1.21.1 NeoForge).
- Create Aeronautics / Sable: turbofans push sub-levels; FEL beams, blasts, black holes, sirens, designators, missiles,
  drones and turret aim work across sub-levels and the world; the laser detonator aims at the right spot. Blocks weigh
  iron x hardness / 5 and machines assemble as whole structures.
- TerraFirmaCraft worlds get bedrock ore and bedrock oil (GitHub issue 33).
- Holo Inventory Modern can read machine contents (GitHub issue 35).
- Block tooltips show "[Radiation Shielding]" and "Blast Resistance" as in 1.12.2.
- Machine outlines cover the whole multiblock.
- New bobbleheads: Golem, Norwood and SilentYeti.
- Jetpacks kick up block dust as in 1.7.
- "Cycle Tool Abilities" is a rebindable key.

### Fixed
- Blast resistance now uses 1.7's effective scale, and radiation-sealing blocks update the radiation walls when placed,
  removed or opened.
- Cables, ducts, box ducts, box cables and pneumatic tubes are drawn in the chunk mesh, so painted blocks are no longer too
  dark; coated ducts can be painted as in 1.7.
- Shift-click routes items into the right slots (batteries, fuels, inputs) in 25 machines.
- Watz: placement dupe and GUI tanks and gauge (GitHub issues 37, 38); crates emptied by a player break on 1.20.1
  (issue 39).
- Container titles, item icons with part textures, recipe numbers in JEI and ghost grids (GitHub issues 24-27).
- Fluid identifier sneak-click into barrels and tanks (issue 29); gluon gun ammo use with the daylight cycle off, flamer
  loop and fuel (issue 30); gluon gun tooltip damage (issue 40).
- Cargo elevator ownership, movement, riders, drops and saving.
- Waste drum voided stacked items when they cooled; machine inventories now respect their slot limits.
- Digamma particles turn into a quasar when dropped, as in 1.7; only the right waste items are protected while dropped.
- Pb-209, Au-198 and other 1.12.2-only powders had no radiation; any-material group tags use 1.7's order (technetium steel
  before cadmium steel).
- RBMK terminal legend, RoR terminal rotation, ReaSim boilers made no steam, AE2 cells on 1.20.1 Forge.
- Gas blocks no longer show an outline or block the crosshair; JEI hides 1.7's hidden items; molten metals and fluid icons
  look up all their recipes.
- Depth ores keep their floor through bedrock; Sable dupes, drills on sub-levels and ducts into foreign tanks.
- Disperser canister and glyphid gland icons; break particles for seven machines that showed the missing texture.
- RBMK lids and the RBMK tool are in the creative tabs; occult shotgun and assembly machine recipe ingredients.

## 1.0.0

Initial release

- Install a normal elytra in a chestplate at the armor modification table to glide and use firework boosts while wearing armor. The stored elytra keeps its durability and wears during flight.
