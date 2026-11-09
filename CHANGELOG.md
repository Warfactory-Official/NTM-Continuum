# Changelog

Each release has a `## <version>` section; the release workflow uploads only the section matching
`mod.version` in gradle.properties.

## 1.0.5

Requires Golem Core 1.0.5. Use GemRender 0.1.20; older versions are refused.

### Added
- Native EMI support on 1.21.1 and 1.20.1: every HBM recipe category with recipe ids, favourites and the recipe tree
  (GitHub issues 113, 168). Fluid recipes show when looking up a fluid in EMI, JEI and REI (GitHub issue 162).
- Item tags generated from the game: `hbm:guns`, `ammo`, `food`, `armor`, `bombs`, `missiles`, `machines`,
  `hbm:hazardous/<type>`, `hbm:blast_resistant/<tier>`, `hbm:radiation_shielding` and the `c:` / `forge:` convention
  tags (GitHub issue 132).
- HBM fluids join the `c:` fluid tags (crude oil, ethanol, diesel, hydrogen, steam, plant oil and more), so other mods'
  tagged fluids work in HBM tanks and recipes and HBM's work in theirs (GitHub issue 156).
- Every 1.7 structure and scattered site can be found with `/locate` and switched in `hbm.cfg`, including the silo,
  blueprint chest, locked safe, lantern, broadcaster, capsule, glyphid hive and fallen meteorite (GitHub issue 131).
- RBMK columns show 1.7's DODD diagnostic overlay (client config, on by default) (GitHub issues 147, 165).
- Smoking glyphid meat in a smoker (GitHub issue 141).
- Ability tools drop ore experience like a vanilla pickaxe; silk touch and the processing abilities drop none (GitHub
  issue 145).
- Power armour VATS health bars and thermal sight, and the HUD and jetpack toggle messages (GitHub issue 163).
- Fluid ducts, pipes and pneumatic tubes use 1.7's pipe sounds (GitHub issue 146).
- Turrets eject 1.7's spent casings and muzzle flashes (GitHub issue 159).
- An oil derrick destroyed by an explosion with fluid inside blows up, as in 1.7.
- `hbm.cfg`: `1.91_sableHardnessMass` (off: Sable's default block mass), `1.92_sableBlockMasses` (per-block masses) and
  `1.93_deImportBackup` (skip the world backup before a DE import).

### Changed
- Every MK4 gun scatters with 1.7's spread; shots had been 133 times too accurate (GitHub issue 115).
- Recipe machines take ingredient i of the selected recipe in input slot i only, as in 1.7, so one ingredient can no
  longer fill every slot and stall the machine; old saves keep working (GitHub issue 126).
- Powered and fueled suits spend charge through 1.7's armour wear (damage / 4 per piece), lasting about four times as
  long; the Taurun suit is unbreakable as in 1.7 (GitHub issue 140).
- The Goalkeeper CIWS has 1.7's flat 250-block range, and the ABM climbs and locks on as in 1.7 (GitHub issues 117, 119).
- 39 items 1.7 keeps out of its creative tabs are hidden from the tabs and recipe viewers; the turret AI chip, grenade
  parts, drones and carts are listed (GitHub issues 117, 122).
- Every HBM block in a structure turns with the structure; structure bobbleheads and plushies are random (GitHub issue
  161).
- The mining helmet and flashlight fallback light spreads like a placed torch.
- Gun zoom eases in and out like 1.7, and scope overlays cover wide screens (GitHub issue 152).
- Stereo sounds fade with distance as in 1.7; the chemical plant, chemical factory, ore acidizer and assembly factory
  loops use 1.7's volumes (GitHub issue 166).
- Armour pieces with their own 3D model, such as the Taurun suit, hide the matching skin outer layer (GitHub issue
  127).
- The universal fluid bucket is no longer listed, and fluid pickers list HBM and addon fluids only (GitHub issue 156).
- DE world import follows NTM: DE 1.0.0-beta.14; DE's Create motors are refunded.

### Fixed
- Reopening a world with an assembled PWR crashed it, and the world could not load again (GitHub issue 111).
- Desert Atom barrels gave almost no radiation (GitHub issue 110).
- Ducks and Quackos took fall damage (GitHub issue 109).
- Crater biome names were missing (GitHub issue 108).
- Centrifuge and crystallizer showed a certus ore row with an empty input with AE2 installed (GitHub issue 114).
- Vein mining stopped at bent or connected cables, tubes, fences and walls (GitHub issue 120).
- The fan ignored redstone changes on the client (GitHub issue 123).
- A Watz filled with poisonous mud never melted down (GitHub issue 124).
- The assembly factory and precision assembler ignored recipe selection (GitHub issue 125).
- The plasma forge took plasma on the wrong side of the fusion reactor (GitHub issue 128).
- Welders, soldering stations, the Tauon turret, blast furnace, crucible, large arc furnace, turbofan and Maxwell never
  showed their 1.7 effects (GitHub issue 133).
- Conveyor and crane sides could not be changed with a screwdriver (GitHub issue 134).
- Foundry mold melt z-fought (GitHub issue 135); the electrolyser poured without a stream (GitHub issue 136).
- Beam and laser visuals steered with pitch; four grenades were missing on clients; additive effects were plain alpha on
  1.20.1 (GitHub issue 137).
- Folly sight overlays, jingle and rings, plasma blast particles and fog on beam effects (GitHub issue 138).
- Conveyor inserters and other mods' funnels could not reach the firebox, oven and other multiblock inventories; heat
  machines read heat from any cell (GitHub issue 142).
- HBM blocks were missing on Xaero's and vanilla maps (GitHub issue 144).
- Chicago pile loader, vent and control rod animations never ran; pile rods stack to 64 (GitHub issue 148).
- Steam clouds stuck inside blocks on Sable ships (GitHub issue 149).
- Bullet tracers were brown, projectiles jittered, recoil snapped, and the sawblade was not 1.7's (GitHub issue 150).
- Quitting a world with fluid ducts on a Sable ship aborted the game; Chicago piles ejected rods on chunk unload (GitHub
  issue 151).
- SPAS-12 and Congo Lake shells were miscoloured (GitHub issue 153).
- Fusion reactor parts were silent and the plasma forge striker stopped (GitHub issue 154).
- The lever-action rifle and several other guns and machines played the wrong sounds (GitHub issue 155).
- Two "Ethanol Bucket"s, colourless buckets for other mods' fluids, and fluid lists full of foreign fluids (GitHub issue
  156).
- After a respawn, casts and battery sockets showed empty; vault locks ignored the offhand and desh screwdriver; AppleSkin
  icons were erased on Fabric; broken multiblocks dropped working machines or their parts; glyphids hit from too far
  (GitHub issue 157).
- The mining helmet light did nothing under Oculus or 26.1 Iris shader packs (GitHub issue 158).
- Artillery shells flew 1.12.2's course and missed; turret casings were dark on 1.21.1 and 26.1 (GitHub issue 159).
- The charge thrower's hook let go the tick it hit and was forgotten every tick below 26.1 (GitHub issue 160).
- Silo hatches, Methusalem turrets, launch pads, bunkers and offices generated without their multiblock cells, and
  some structure blocks faced backwards (GitHub issue 161).
- Bobbleheads, plushies and snow globes dropped nothing when broken by hand (GitHub issue 167).
- HBM tools played a block's break sound and particles twice, and the drill gun three times (GitHub issue 169).
- Particles flashed at full size, radiation fog had hard edges, liquefactor and solidifier glass was invisible, the
  plasma forge tube was see-through and arc furnace electrodes did not glow (GitHub issue 166).
- Neutron-activated items re-equipped in hand every tick (NTM: Space issue 6).
- The ore slopper's recipe listed every bedrock ore in one row (NTM: Space issue 7).

## 1.0.4

Requires Golem Core 1.0.4. Use GemRender 0.1.20; older versions are refused.

### Added
- OpenComputers: Rebooted support on 1.21.1 NeoForge: every block CC: Tweaked can reach is an OpenComputers component,
  through cables and Adapters, with 1.7's methods.
- Land-protection support: explosions, bombs, missiles, fallout, hazard blocks and clouds, and spreading effects ask
  protection mods (WarForge claims and safe zones) before changing blocks or hurting entities, and carry the player
  who set them off.
- The advanced and high-performance hazmat hoods blur by wear, as in 1.7.
- Upstream 1.7 X5863: the HIMRAAL, a nine-barrel micro rocket launcher (full auto, or the whole drum at once with the
  secondary trigger), and micro rockets from the ammo press. The HIMRAAL is found in weapon crates and C-130 supply
  drops.

### Changed
- Upstream 1.7 X5863: the Panzerschreck reloads in 85 ticks instead of 50; mobs no longer spawn wearing legacy alloy
  armor; updated Russian localization and manual pages.

### Fixed
- Landmines, AT mines, broadcasters, safes, capsules, the chlorine geyser and the radiation hotspot crater generated
  with their `hbm.cfg` spawn setting at 0 next to mods that re-place features near water, such as Streams Reflowing
  (GitHub issue 102).
- Helmet and gas mask overlays covered the hotbar and crosshair (GitHub issue 106).
- JEI recipe registration took many seconds in large packs (18 s in All the Mods 10, now 0.2 s) (GitHub issue 107).
- The electrolyser refused crystals placed by hand, and a second crystal type did nothing after the first; taking a
  module out of the armor table left it on the armor (GitHub issue 101). 63 more menus checked placed items against
  the menu's slot number instead of the container's.
- The geiger counter HUD barely moved; it is 1.7's accumulated radiation bar again (GitHub issue 99).
- Radiation, asbestos, digamma and other damage with no attacker knocked players in a random direction.
- The condenser and Leviathan item icons were invisible below 26.1 with NTM: Space installed (GitHub issue 98).
- A crash ("Index out of bounds") after a Tom impact with C2ME installed.
- Chunk radiation cost up to 10 ms per server tick once the field spread over the loaded area; it is about 2 ms, with
  the same results.
- Map mods such as Xaero's minimap spent time looking up the crater biomes' grass colour.
- QMAW logged a warning for every other mod's manual file; it reads only HBM's manuals.
- The calculator's QMAW search listed template pages, and a middle click on a search result deleted a history entry
  (upstream 1.7 X5863).

## 1.0.3

Requires Golem Core 1.0.3. Use GemRender 0.1.20; older versions are refused.

### Added
- A NTM: DE world Converter. You can convert worlds on first load (1.21.1 NeoForge): blocks, machines, items, players,
  advancements, duct networks, satellites and Sable airships, with a backup first. Anything that cannot be converted is
  kept in `data/hbm_de_leftovers.dat` but the converter covers 99.99% so nothing should be missed.
- 1.7 X5860: the fluid burner's overdrive slot and burn-rate slider, the calculator as a QMAW browser, Redstone-over-Radio
  on every launcher, the Lambda and Soyuz pads and the satellite linker, and the spy satellite's `findplayer`.
- Rock mill rows for sculk and sculk veins.
- Paint and rebar take the colour of the clicked concrete.
- The Clawd bobblehead, made in the assembly machine.
- A creative battery in the battery socket shows the chuddie figure holding an energy ball.
- Lootr support: structure chests and barrels keep a loot table, so Lootr converts them.
- Search bars match through Just Enough Characters (pinyin) when it is installed.
- Plushie compression and battery socket centrepieces for addons.
- Updated Japanese translation (thanks Twister716).

### Fixed
- A server whose `hbm.cfg` recipe switches differed from the client's hid recipe rows in the viewer and machine selectors.
- Wind charges destroyed multiblock machines on 1.21.1.
- The 1.21.1 client hung on world exit while terrain was still generating.
- The templates tab listed the uncraftable multi fluid identifier instead of 1.7's craftable one; JEI and REI find its
  crafting recipe from any listed identifier.
- RBMK cover panels sat inside the top of structural, storage, boiler, heater and other non-fuel columns, and every
  cover panel looked like a thin floating plate (GitHub issue 94).
- The Tom crater only cleared up to y 256 and filled down to y 0; it now uses the world's full height (GitHub issue 95).
- The client failed to load ("Missing key ... minecraft:fluid / hbm:...") next to a mod that registers a fluid early,
  such as Create: Estrogen, on NeoForge and Forge.
- Volcanic lava makes 1.7's basalt ores again and turns any diamond ore, deepslate included, into gem ore.
- Guns in the offhand were drawn inside out.
- The Torex shock ring follows the terrain again; rotated multiblocks kept unrotated hitboxes, and the ore slopper could
  not be placed facing along X.
- The Target Pistol's durability and ammo HUD was missing.
- Other mods' pipes could not reach 1.20.1 Forge multiblocks through their ports.
- The supply crate drops itself with its contents; the B92 is 1.7's model in 1.7's hand position.
- The catalytic cracker's hitbox, and the swapped ducrete and ducrete tile names.
- The blinking CRT draws one frame of its strip, the splitter item shows both lanes, and the crucible and seven other
  machines hold one item per slot as in 1.7. Material and shape names are translatable.
- Untyped tanks and barrels take the fluid of a container in their load slot; foundry mold melt draws with depth.
- A machine port change could load unloading chunks and hang the server.
- 18 duplicate crafting and smelting recipes; structure loot counts on 1.20.1 were capped at the stack size.
- 1.7 X5860's fixes: pneumatic storage dupes and desyncs, wand, radar and balefire hardening, fluid tank connections on
  its long sides only, the Orbus top, the ore slopper, turbofan redstone and the electrolyser's input-only ports.
- Text and stack panels drawn over a menu's slot items sat under them before 26.1.
- The particle source fired a whole stacked input as one particle; it takes one capsule per shot.
- The fluid burner's tooltip still said it is set with a screwdriver.
- A crate opened from the hand could take the wrong layout or lock the wrong hotbar slot if the selection changed while
  it opened.
- Barrels placed from an item, or reloaded below 1.20.5, could keep an earlier exact fluid or identifier pin.
- Dropped-item effects (rags, singularities, cells, antimatter, the demon core) ran from a scan of every loaded entity on
  Fabric and 1.20.1 Forge; they run after each item's own tick.
- The refueler was a 12-pixel slab instead of 1.7's 4-pixel wall.
- Other players' jetpacks showed a stale state after coming into view, and their thrust key was not synced.
- A cargo elevator placed from a picked-up stack builds all its courses, and needs room for them.
- Large explosions had no shockwave; it is drawn 2 blocks up and fades as in 1.7.
- The hand drill removes a burner or electric press's dummy blocks, bomber and C-130 loops play under the Players
  volume, gneiss gas holds 50 mB of petroleum in 528 mode, the sawmill takes only single items from automation and
  jetpack exhaust moves as in 1.7.

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
