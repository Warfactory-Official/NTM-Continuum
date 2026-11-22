# Create: Fission Works - handoff (2026-11-13, second update)

Goal: Create addon (NeoForge 1.21.1). Two reactors, HBM-inspired:
1. Hall: wrench-formed 7x7 flux reactor with rods, groups, AZ-5, console (neutronics side).
2. ZIRNOX: fixed gas-cooled reactor block: CO2 + water tanks, steam out, heat, pressure, meltdown, GUI, fuel rods.
Targets: NeoForge 21.1.219, Minecraft 1.21.1, Create 6.0.10, Java 21.

## Status (real NeoForge/Create compile NOT done: sandbox has no network; stub compile passes, see tools/stubcheck.sh)
| Piece | State |
|---|---|
| sim/Reactor.java (hall flux) | compiled, 19 tests pass |
| sim/Zirnox.java (24 slots, tanks, heat, pressure, burnup, decay heat, meltdown) | compiled, 21 tests pass |
| Hall, PanelBlock, RodBlock, FuelBlock, ReactorBE, ConsoleScreen | written, uncompiled |
| ZirnoxBlock, ZirnoxBE (ports, steam push, commands, meltdown), FuelRodItem, ModFluids, Caps, FluidClient, ZirnoxScreen | written, uncompiled |
| Textures (PIL), models, lang, loot, tags | generated; check_mod clean; contact sheets and GUI mocks seen, in-game not seen |
| build.gradle | Curse Maven Create 6.0.10 verified; other coordinates from memory |

## Next steps
0. Real compile, on a machine with internet: install JDK 21 and Gradle 8.10+, in this folder run `gradle wrapper --gradle-version 8.10.2`, then `./gradlew compileJava 2>&1 | grep -E "error:" | head -40`. Send the errors back.
1. (same, with wrapper) Add a Gradle wrapper (`gradle wrapper --gradle-version 8.10.2`). Run `./gradlew compileJava -q 2>&1 | grep -E "error:|warning:" | head -30`. Fix, log in .create-knowledge/gotchas.md.
2. Check in Create source: AllItems.WRENCH.isIn and the wrench tag (Wrench.java).
3. `./gradlew runClient`. ZIRNOX test: place block, hold ZIRNOX rods (creative tab) and click channels, add CO2 canister x4 and water buckets, START. Turbine stand-in: pipe the top port into any tank, or watch steam fill and melt.
4. If gas fluids crash at startup: register a LiquidBlock and bucket for each (see gotchas).
5. Wave 2: gas compressor (CO2 source), turbine stages (super dense to dense to steam to low pressure), condenser, spent-fuel reprocessing, recipes, Ponder, goggles info.
6. Hall: decay heat and coolant; link hall to a steam loop.

## Known risks (unverified guesses)
- FluidType.Properties methods, BaseFlowingFluid.Properties constructor, RegisterClientExtensionsEvent.registerFluidType.
- DeferredRegister.Items.registerItem / registerSimpleItem signatures; Item.Properties.durability on a custom Item.
- level.getCapability(FluidHandler.BLOCK, pos, side), registerBlockEntity lambda form.
- Block.useWithoutItem is called when useItemOn passes, with any item in hand (so rod-in-hand opens the GUI, then click a channel).
- Meltdown calls level.explode with ExplosionInteraction.BLOCK; no radiation or fallout yet.

## Commands
- Offline internal check: `./tools/stubcheck.sh` (stubs are from memory; a pass does not prove the real API calls exist)
- Sim tests: `java -m jdk.compiler/com.sun.tools.javac.Main -d /tmp/o src/main/java/fissionworks/sim/*.java src/test/java/fissionworks/sim/*.java && java -cp /tmp/o fissionworks.sim.ZirnoxTest` (and ReactorTest)
- Asset check: `python scripts/check_mod.py .`

## File map
- sim/Zirnox.java: tune K_COOL, PRESSURE_K, POWER, LIFE, caps here; sim/Reactor.java: hall flux
- ZirnoxBlock / ZirnoxBE: block, ticking, ports (water left, CO2 right, steam top), commands OP_*
- client/ZirnoxScreen.java: console; Net.java + Console.java: one packet for both consoles
- ModFluids / FluidClient / Caps: gases and pipe access
- .create-knowledge/, .create-study/, scripts/, CREATE-EXPERT-SKILL.md: notes and tools

## Open questions
- Hall height and size fixed (7x7, one block tall). ZIRNOX 3D model is a plain cube with per-face textures; a bigger multi-part model later?
