package com.hbm.main;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.creativetabs.ModTabs;
import com.hbm.entity.ModEntities;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.sound.ModSounds;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModBombItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.tileentity.ModBlockEntities;

/**
 * Common bootstrap entry point. Loads the content classes so their {@code DeferredRegister}s are
 * populated. Must be invoked by every loader <em>before</em> registration is flushed (for NeoForge,
 * before {@code RegisterEvent} fires; for Fabric, before {@code flushAll}).
 */
public final class MainRegistry {

    private MainRegistry() {}

    /**
     * The "polaroid" — a per-launch easter-egg roll, ported from 1.12.2's {@code MainRegistry}. Rolled
     * once at bootstrap as {@code rand.nextInt(18) + 1}, <b>re-rolling 4 and 9</b> (upstream skips
     * those two ids). Only one consumer is ported so far: on a world where this lands on 11, a
     * digamma-wavelength FEL beam leaves spreading {@code digamma_matter} instead of a plain
     * {@code fire_digamma}. Upstream also honoured a {@code generalOverride} config, which the port
     * has no config file for — set the field directly if you want to force an id.
     */
    public static int polaroidID = 1;

    private static void rollPolaroid() {
        java.util.Random rand = new java.util.Random();
        do {
            polaroidID = rand.nextInt(18) + 1;
        } while (polaroidID == 4 || polaroidID == 9);
    }

    /** {@code config/hbm.cfg}, 1.7's {@code event.getSuggestedConfigurationFile()}. */
    public static java.nio.file.Path configFile() {
        return dev.golem.core.platform.ConfigFiles.configDir().resolve("hbm.cfg");
    }

    /**
     * 1.7 {@code MainRegistry.loadConfig}: every {@code loadFromConfig} in 1.7's order, then {@code config.save()}, which
     * writes the file with any missing key at its default. Runs first in {@link #bootstrap}, before anything is
     * registered, as 1.7's ran in pre-init (History §19ej-8).
     */
    public static void loadConfig(com.hbm.config.Configuration config) {
        config.load();

        com.hbm.config.GeneralConfig.loadFromConfig(config);
        com.hbm.config.WorldConfig.loadFromConfig(config);
        com.hbm.config.MachineConfig.loadFromConfig(config);
        com.hbm.config.BombConfig.loadFromConfig(config);
        com.hbm.config.RadiationConfig.loadFromConfig(config);
        com.hbm.config.PotionConfig.loadFromConfig(config);
        com.hbm.config.ToolConfig.loadFromConfig(config);
        com.hbm.config.WeaponConfig.loadFromConfig(config);
        com.hbm.config.MobConfig.loadFromConfig(config);
        com.hbm.config.StructureConfig.loadFromConfig(config);

        config.save();
    }

    public static void bootstrap() {
        loadConfig(new com.hbm.config.Configuration(configFile()));
        // Golem Core recipe flags for the config branches of 1.7's crafting recipes ("golem:requires")
        com.hbm.inventory.recipes.loader.RecipeConditions.init();
        // The fluid registry is a plain static map (not an MC registry); populate it first so any
        // fluid tile/recipe that references Fluids.* during registration finds the types ready.
        rollPolaroid();
        Fluids.init();
        Fluids.declareVanilla(); // every FluidType as a vanilla Fluid for other mods (History section 19ek-fluids)
        com.hbm.inventory.material.Mats.init(); // material registry (tag-based ore-dict replacement)
        // Order matters: blocks before items (block items reference blocks), tabs last.
        ModBlocks.init();
        com.hbm.items.ModItemModels.declare();
        com.hbm.inventory.material.ModMaterialTags.declare();
        com.hbm.lib.ModDamageTypes.declare();
        com.hbm.world.gen.ModBiomeModifiers.declare();
        com.hbm.world.gen.LegacyDungeonDeclarations.declare(); // WG-D: legacy dungeon structures + scattered-block features (before BiomeFeatures.install)
        com.hbm.entity.ModMobSpawns.init(); // 1.7 EntityMappings natural spawns + placements (§19ej-7; before BiomeFeatures.install)
        dev.golem.core.world.BiomeFeatures.install(); // Fabric: the same biome modifiers through BiomeModifications (no-op elsewhere)
        com.hbm.inventory.recipes.ModCraftingRecipes.declare();
        com.hbm.blocks.machine.ModArmorTableBlocks.init();
        com.hbm.blocks.machine.ModWeaponTableBlocks.init();
        com.hbm.blocks.ModBeaconBlocks.init(); // plain beacon-base storage blocks
        ModMachineBlocks.init();   // machine blocks (must precede block entities, which reference them)
        com.hbm.radio.ModRadioContent.init(); // Redstone-over-Radio blocks, block entities, and menus
        com.hbm.blocks.ModConveyorContent.init(); // conveyor belts, cranes, piston inserter, wand
        com.hbm.blocks.ModPneumaticContent.init(); // pneumatic tube storage system
        com.hbm.blocks.ModDroneContent.init(); // delivery drone blocks, linker, request network sweep
        com.hbm.blocks.ModProductionContent.init(); // production machines A: rotary furnace, thresher, microwave, autocrafter, conveyor press
        com.hbm.blocks.ModProduction2Content.init(); // production machines B: excavator, ore slopper, annihilator, old boiler, RTG furnaces
        com.hbm.blocks.ModEnergyNetContent.init(); // pylons/wires, cable variants, legacy storage tiers, HEV battery, tesla
        com.hbm.blocks.ModDoorContent.init(); // generic OBJ doors, the old blast door, mod doors and the steel trapdoor
        com.hbm.blocks.ModSecurityContent.init(); // barbed wire, spikes, fences, barricade, locksmith table, padlocks
        com.hbm.blocks.ModPwrContent.init(); // pressurized water reactor: parts, controller, fuel rods, printer
        com.hbm.blocks.ModPowerGenContent.init(); // Leviathan turbine, LPW-2, geothermal exchanger, IGen memorial, siren + tracks
        com.hbm.blocks.ModDecoContent.init(); // structural/decorative blocks: pipes, pillars, lamps, spotlights, bricks, asphalt, crates, poles
        com.hbm.blocks.ModTerrainContent.init(); // natural/terrain/hazard blocks: plants, frozen ground, biome stone, fissure, volcano, geysers, vents, storage blocks
        com.hbm.assemblyfactory.AssemblyFactoryRegistrations.init();
        com.hbm.chemicalfactory.ChemicalFactoryRegistrations.init();
        com.hbm.blocks.machine.AutosawFeature.init();
        com.hbm.blocks.machine.solar.ModSolarBlocks.init();
        com.hbm.items.solar.ModSolarItems.init();
        com.hbm.tileentity.machine.solar.ModSolarBlockEntities.init();
        com.hbm.smoke.SmokeChainRegistration.init();
        com.hbm.blocks.machine.pile.ModPileBlocks.init();
        com.hbm.blocks.machine.dfc.ModDFCBlocks.init();
        com.hbm.items.ModSatelliteItems.init();
        com.hbm.blocks.ModSatelliteContent.init(); // satellites: ID manager, cargo pad, orbital laser, /ntmsatellites
        com.hbm.blocks.machine.soyuz.ModSoyuzBlocks.init();
        com.hbm.foundry.FoundryExpansion.init();
        com.hbm.blocks.machine.DiFurnaceContent.init();
        ModBatteryBlocks.init();   // energy-storage machine blocks (battery socket)
        com.hbm.blocks.turret.ModTurretBlocks.init(); // turret pad cell and the turrets on it
        ModStorageBlocks.init();   // storage crates (iron / steel / desh)
        ModBombBlocks.init();      // bomb blocks (also precede block entities)
        com.hbm.blocks.ModBombs2Content.init(); // special bombs: custom nuke, F.L.E.I.J.A., Blue Rinse, Prototype, balefire bomb, thermo/EMP/float/flame war
        com.hbm.blocks.ModRailBlocks.init(); // the folded rails the ntm-next trains run on
        com.hbm.blocks.ModCustomMissileContent.init(); // custom missile parts, assembly station, compact launcher, launch table, large/rusted pads
        com.hbm.blocks.ModRadarUtilityContent.init(); // radars, radar screen/linker, forcefield, teleporter/anchor, orbus, silo hatch, Soyuz capsule
        com.hbm.blocks.ModDecoCrateContent.init(); // storage crates, bobbleheads/plushies/snowglobes, radios, emitters, coils, cap blocks, absorbers, glyphid hive blocks
        com.hbm.blocks.ModGapsBContent.init(); // G-B: crates opened from the hand (CRATE_OPEN_HELD menus)
        com.hbm.items.ModRenderInputContent.init(); // G-C: tool animation cues, equip edge, rendering/input gap content
        com.hbm.blocks.ModIcfContent.init(); // ICF reactor, seed, vessel components, laser controller/array, pellet maker, fuel pellet
        com.hbm.config.MachineDynConfig.initialize(); // 1.7 hbmConfig/hbmMachines.json (icfLaser)
        com.hbm.blocks.ModEnergyBedrockContent.init(); // boxcable, coated wire, charging station, HE/RF converters, bedrock ore grades
        com.hbm.blocks.ModDungeonContent.init(); // dungeon/structure bricks, keyholes, pedestal, skeleton holder, loot pile, old lantern, action blocks, cart rails, rebar + placer
        com.hbm.blocks.generic.ModStructureWandBlocks.init(); // structure wand blocks: wand_air/loot/jigsaw/logic/tandem/structure (NBTStructure editing)
        com.hbm.blocks.ModHazardBlocks.init(); // radioactive / rad-resistant blocks (radiation emitters)
        com.hbm.blocks.ModFalloutBlocks.init(); // fallout layer + waste/sellafite terrain (nuke aftermath)
        com.hbm.blocks.ModSteelBlocks.init();   // stateful steel grate + OBJ scaffold
        com.hbm.blocks.ModDecoBlocks.init();    // decorative DecoBlock set (steel structures, sat dishes, boxcar/boat)
        com.hbm.blocks.ModAnvilBlocks.init();   // the 12 tiered anvils (must precede block entities)
        com.hbm.blocks.ModOilBlocks.init();     // special oil blocks (ore_oil deposit / oil_pipe / oil_spill)
        com.hbm.blocks.ModNuclearBlocks.init(); // uranium outgassing ores and radon gases
        com.hbm.blocks.ModOutgasBlocks.init(); // asbestos, corium cobble, tomb scrap, and lab outgas blocks
        com.hbm.blocks.ModOreBlocks.init();     // clusters, depth rock/ores, resource stone, cave spikes, gas pockets
        com.hbm.blocks.ModGlassBlocks.init();   // the BlockNTMGlass family (glass_quartz so far)
        com.hbm.blocks.ModLadderBlocks.init();  // vanilla LadderBlock family, including ladder_red_top
        com.hbm.blocks.machine.rbmk.ModRBMKBlocks.init(); // reactor columns/peripherals before their block items and BE types
        ModDataComponents.init();  // custom item data components (referenced by hand items below)
        com.hbm.items.special.ItemHot.init(); // declare the hot-item component before Fabric's registry flush
        com.hbm.blocks.material.MaterialCompletionRegistrations.init();
        com.hbm.inventory.recipes.ModRecipes.init(); // custom crafting serializers
        ModItems.init();
        // After ModItems: its container items name ModItems empties as craft remainders at registration time.
        com.hbm.blocks.ModFluidNetContent.init(); // fluid-network ducts, barrels, foundry tank, fluid blocks, containers
        com.hbm.items.ModEntityItems.init(); // spawn items, carts, drones, trains, grenades, dispersers (entity port)
        com.hbm.items.ModGunSupportItems.init(); // ammo container and stack-backed ammo/casing bags
        com.hbm.items.ModJetpackDependencyItems.init(); // hot steel and the functional xenon thruster part
        com.hbm.items.ModAbilityToolItems.init(); // ability tools/weapons, tool sets, meteorite sword chain
        com.hbm.items.ModMeleeKitItems.init(); // special melee weapons, novelty tools, starter kits, toolbox, loot, Bobmazon
        com.hbm.items.ModSingularityItems.init(); // dropped singularity capsules and Spark progression
        com.hbm.items.ModSelfchargerItems.init(); // deprecated powered-armour auxiliary batteries
        com.hbm.items.ModBatterySCItems.init(); // 1.7 ItemBatterySC radiovoltaic batteries (battery_sc_<variant>)
        com.hbm.items.ModRtgContent.init(); // RTG pellet state, block entity, and menu registrations
        com.hbm.items.ModChemicalDyeItems.init(); // flattened chemical-dye and crayon variants
        com.hbm.items.ModSpawnEggs.init(); // spawn eggs for the 29 1.7 addMob mobs (§19ej-7)
        com.hbm.items.ModStorageDrumItems.init();
        com.hbm.items.ModFoodItems.init();
        com.hbm.items.ModConsumableTailItems.init();
        com.hbm.items.ModFinalConsumableItems.init(); // food, consumables, records, batteries, loose materials, 5G/muffler upgrades
        com.hbm.items.ModCatalystItems.init();
        ModMedicineItems.init();
        com.hbm.items.ModRBMKItems.init(); // RBMK rods/pellets and reactor tools
        com.hbm.items.ModZirnoxItems.init(); // component-backed reactor fuel and spent fuel
        ModMachineItems.init();    // hand-registered machine items (templates)
        com.hbm.items.ModCasingItems.init(); // metal variants of the old metadata casing item
        com.hbm.items.machine.ModStampItems.init(); // press stamps (dies)
        com.hbm.items.machine.ModMoldItems.init();  // casting molds (foundry mold/basin) + mold_base
        com.hbm.items.ModAutogenItems.init();        // auto-generated metal-shape items (cast plate, shell, …)
        com.hbm.items.ModIntermediateItems.init();   // recipe intermediates (circuit / item_expensive / chunk_ore)
        ModIngredientItems.init(); // escaped metadata families: generic parts, ash, and plate fuel
        ModBatteryItems.init();    // modern battery / capacitor items (the socket's storage medium)
        com.hbm.items.ModUpgradeItems.init(); // machine upgrade modules (speed/effect/power/overdrive)
        com.hbm.items.ModFuelItems.init();  // the ItemFuel family (furnace-burnable; burn times wired per loader)
        com.hbm.items.ModOilItems.init();   // oil-chain items (oil_tar variants + oil_detector)
        com.hbm.items.ModToolItems.init();  // in-world construction tools (blowtorch, boltgun)
        com.hbm.items.ModWatzItems.init();  // Watz fuel pellets (one item per type, fresh + depleted)
        com.hbm.blocks.ModFallingBlocks.init(); // the BlockFallingBase family (gravity blocks)
        ModFluidItems.init();      // fluid-container items (fluid_bucket, one per fluid)
        ModBombItems.init();       // hand-registered bomb components
        com.hbm.items.ModGearItems.init(); // radiation gear (geiger/dosimeter + hazmat armor)
        com.hbm.items.ModArmorItems.init(); // modelled power armour (T-51b)
        com.hbm.items.ModArmorSetBContent.init(); // final armor B: remaining ModItemsArmor ids, IV bags, statue_elb_f
        com.hbm.items.ModArmorSetContent.init(); // final armor sets A: alloy, cmb, bismuth, euphemium, zirconium, dnt, AJR/AJRO, HEV, Fau, trenchmaster
        com.hbm.items.ModArmorModules.init(); // complete armor modules (storage, cladding, inserts)
        com.hbm.items.ModArmorModItems.init(); // remaining ItemMod* modules, PaA hazmat, liquidator, mask of infamy
        com.hbm.blocks.ModUtilityToolContent.init(); // utility/diagnostic tools, pipettes, guide/lore books, holotapes, tablets, Lemegeton, legacy manual block
        com.hbm.blocks.ModCustomMachineContent.init(); // custom machine system: controllers from hbmCustomMachines.json, cm_* components, anchor, structure wand
        com.hbm.blocks.ModNuclearMiscContent.init(); // radiolysis RTG, legacy breeder/research reactors + controller, legacy graphite pile, breeding/pile rods, spent plate fuel
        com.hbm.blocks.ModExplosivesContent.init(); // landmines, timed charges, det blocks, plastic explosive blocks, duds, multi purpose bomb, detonators
        // Ammo, factories, and gun suppliers must all be declared before any loader flushes ITEMs.
        com.hbm.items.weapon.sedna.factory.GunCatalogue.init();
        com.hbm.tileentity.machine.pile.TileEntityPileCore.initDebris();
        com.hbm.items.ModMissileItems.init(); // launchable missiles + the designators that aim them
        com.hbm.items.ModWandItems.init(); // creative wand_k / wand_s / wand_d
        com.hbm.advancement.AdvancementCommandContent.init(); // 1.7 achievements as advancements (triggers, icons) + ntm* commands
        com.hbm.world.gen.ModFeatures.init(); // worldgen feature types (1.7 ores, strata, MapGen terrain, surface features)
        com.hbm.handler.BossSpawnHandler.init(); // 1.7 meteor strikes and showers, rolled every level tick
        com.hbm.handler.SpecialPlayers.init(); // 1.7 onPlayerRespawn: Dr_Nostalgia's hat and Beta
        com.hbm.extprop.HbmPlayerProps.init(); // 1.7 onPlayerClone: reputation, shield, fbi/rad marks survive respawn
        com.hbm.world.gen.NTMWorldGenerator.init(); // NBT structures: hbm:nbt structure type, piece type, 1.7 spawn registrations
        com.hbm.world.gen.NTMComponentStructures.init(); // features/bunker/meteor_dungeon spawns + hardcoded piece types (WG-C)
        com.hbm.world.gen.LegacyDungeonStructure.init(); // WG-D: 1.7 HbmWorldGen legacy dungeons (hbm:legacy_dungeon) + scattered-block feature types
        com.hbm.world.biome.ModBiomes.init(); // crater biome keys, backed by worldgen biome JSON
        ModBlockEntities.init();
        com.hbm.tileentity.ModRBMKBlockEntities.init();
        ModMenus.init();
        com.hbm.inventory.ModGunSupportMenus.init();
        com.hbm.inventory.ModRBMKMenus.init();
        ModEntities.init();        // entity types (the explosion + fallout entities)
        com.hbm.entity.ModRBMKEntities.init();
        com.hbm.entity.missile.ModMissileEntities.init(); // the missiles
        com.hbm.entity.NtmToolEntityCatalog.init(); // NI4NI's throwable coin, separate from ModEntities
        com.hbm.entity.ModNtmEntities.init(); // the ntm-next entity port (mobs, vehicles, effects, remaining projectiles)
        com.hbm.particle.HbmParticles.init();
        com.hbm.potion.ModEffects.init(); // status effects (radiation)
        ModSounds.init();          // sound events (assembly machine motor/strike/start/stop)
        ModTabs.init();
        com.hbm.handler.neutron.NeutronLifecycle.init();
        com.hbm.handler.pollution.PollutionLifecycle.init();   // soot/poison field: diffusion, decay and blight
        com.hbm.main.ModEventHandlerMobs.init();     // 1.7 ModEventHandler mob death drops, decorateMob, addAITasks (§19ej-7)
        com.hbm.main.ModEventHandlerImpact.init();   // Tom impact aftermath: dust/fire decay, burning, spawns (§19ej-6)
        com.hbm.main.ModEventHandlerHooks.init();    // food eaten, block break, sleep, potential spawns (§19ek-hooks)
        com.hbm.handler.ImpactSky.register();         // Tom impact sky darkening (1.7 WorldProviderNTM)
        com.hbm.handler.radiation.RadiationFog.init(); // 1.7 radFog, read from the chunk radiation (§19ej-6)
        com.hbm.world.chunk.ChunkLoadingManager.init();   // forced-chunk bookkeeping + restart reconciliation
        dev.golem.core.platform.Lifecycle.onLevelUnload((server, level) -> com.hbm.handler.JetpackHandler.clear(level));
        dev.golem.core.platform.Lifecycle.onServerTick(server ->
                server.getAllLevels().forEach(com.hbm.handler.JetpackHandler::cleanup));
        // ntm-next entity port: item pools resolve their registry ids once the server holds registries; the
        // TOM impact gate is republished for off-thread worldgen readers; per-server caches clear on stop.
        dev.golem.core.platform.Lifecycle.onServerStarted(server -> {
            com.hbm.itempool.ItemPool.load(server.registryAccess());
            com.hbm.saveddata.TomSaveData.publish(server);
        });
        dev.golem.core.platform.Lifecycle.onServerStopping(server -> {
            com.hbm.saveddata.TomSaveData.onServerStopping();
            com.hbm.tileentity.network.RequestNetwork.onServerStopping();
            com.hbm.tileentity.network.RTTYSystem.onServerStopping();
            com.hbm.util.ChunkUtil.onServerStopped();
        });
        // Needs both the tool items and the convertible blocks, so it runs after every registry.
        com.hbm.handler.NTMToolHandler.register();
        // Machines and container items on the loaders' fluid capabilities (History section 19ek-fluids).
        com.hbm.capability.ForeignFluids.register();
        // 1.7's OpenComputers components as CC: Tweaked peripherals, when CC is loaded (History section 19el-computers)
        com.hbm.compat.computercraft.ComputerCraft.init();
    }
}
