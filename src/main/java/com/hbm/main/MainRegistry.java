package com.hbm.main;

import com.hbm.advancement.AdvancementCommandContent;
import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.ModBeaconBlocks;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModBombs2Content;
import com.hbm.blocks.ModConveyorContent;
import com.hbm.blocks.ModCustomMachineContent;
import com.hbm.blocks.ModCustomMissileContent;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModDecoCrateContent;
import com.hbm.blocks.ModDoorContent;
import com.hbm.blocks.ModDroneContent;
import com.hbm.blocks.ModDungeonContent;
import com.hbm.blocks.ModEnergyBedrockContent;
import com.hbm.blocks.ModEnergyNetContent;
import com.hbm.blocks.ModExplosivesContent;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModFluidNetContent;
import com.hbm.blocks.ModGapsBContent;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModIcfContent;
import com.hbm.blocks.ModLadderBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModNuclearMiscContent;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModOutgasBlocks;
import com.hbm.blocks.ModPneumaticContent;
import com.hbm.blocks.ModPowerGenContent;
import com.hbm.blocks.ModProduction2Content;
import com.hbm.blocks.ModProductionContent;
import com.hbm.blocks.ModPwrContent;
import com.hbm.blocks.ModRadarUtilityContent;
import com.hbm.blocks.ModRailBlocks;
import com.hbm.blocks.ModSatelliteContent;
import com.hbm.blocks.ModSecurityContent;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.ModTerrainContent;
import com.hbm.blocks.ModUtilityToolContent;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.generic.ModStructureWandBlocks;
import com.hbm.blocks.machine.ModArmorTableBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.ModWeaponTableBlocks;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.capability.ForeignFluids;
import com.hbm.capability.ForeignItems;
import com.hbm.compat.HbmBlockInfo;
import com.hbm.compat.computercraft.ComputerCraft;
import com.hbm.handler.ae2.AE2CompatHandler;
import com.hbm.config.BombConfig;
import com.hbm.config.Configuration;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MachineConfig;
import com.hbm.config.MobConfig;
import com.hbm.config.PotionConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.config.StructureConfig;
import com.hbm.config.ToolConfig;
import com.hbm.config.WeaponConfig;
import com.hbm.config.WorldConfig;
import com.hbm.creativetabs.ModTabs;
import com.hbm.entity.ModEntities;
import com.hbm.entity.ModMobSpawns;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.ModRBMKEntities;
import com.hbm.entity.missile.ModMissileEntities;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.handler.ImpactSky;
import com.hbm.handler.JetpackHandler;
import com.hbm.handler.NTMToolHandler;
import com.hbm.handler.SpecialPlayers;
import com.hbm.handler.neutron.NeutronLifecycle;
import com.hbm.handler.pollution.PollutionLifecycle;
import com.hbm.handler.radiation.RadiationFog;
import com.hbm.handler.radiation.RadiationWalls;
import com.hbm.inventory.ModGunSupportMenus;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.ModRBMKMenus;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.ModMaterialTags;
import com.hbm.inventory.recipes.ModCraftingRecipes;
import com.hbm.inventory.recipes.ModRecipes;
import com.hbm.inventory.recipes.loader.RecipeConditions;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModArmorModules;
import com.hbm.items.ModArmorSetBContent;
import com.hbm.items.ModArmorSetContent;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModBatterySCItems;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModCatalystItems;
import com.hbm.items.ModChemicalDyeItems;
import com.hbm.items.ModConsumableTailItems;
import com.hbm.items.ModEntityItems;
import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModGearItems;
import com.hbm.items.ModGunSupportItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItemModels;
import com.hbm.items.ModJetpackDependencyItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.ModMissileItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.ModRenderInputContent;
import com.hbm.items.ModRtgContent;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.ModSelfchargerItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.ModSpawnEggs;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.ModToolItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.ModWandItems;
import com.hbm.items.ModWatzItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ModMoldItems;
import com.hbm.items.machine.ModStampItems;
import com.hbm.items.special.ItemHot;
import com.hbm.items.weapon.sedna.factory.GunCatalogue;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.HbmParticles;
import com.hbm.potion.ModEffects;
import com.hbm.saveddata.TomSaveData;
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
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.RequestNetwork;
import com.hbm.util.ChunkUtil;
import com.hbm.world.biome.ModBiomes;
import com.hbm.world.chunk.ChunkLoadingManager;
import com.hbm.world.gen.LegacyDungeonDeclarations;
import com.hbm.world.gen.LegacyDungeonStructure;
import com.hbm.world.gen.ModBiomeModifiers;
import com.hbm.world.gen.ModFeatures;
import com.hbm.world.gen.NTMComponentStructures;
import com.hbm.world.gen.NTMWorldGenerator;
import dev.golem.core.platform.ConfigFiles;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.BiomeFeatures;
import java.nio.file.Path;
import java.util.Random;

/**
 * Common bootstrap entry point. Loads the content classes so their {@code DeferredRegister}s are
 * populated. Must be invoked by every loader <em>before</em> registration is flushed (for NeoForge,
 * before {@code RegisterEvent} fires; for Fabric, before {@code flushAll}).
 */
public final class MainRegistry {

    private MainRegistry() {}

    /**
     * The "polaroid": a per-launch easter-egg roll, ported from 1.12.2's {@code MainRegistry}. Rolled
     * once at bootstrap as {@code rand.nextInt(18) + 1}, <b>re-rolling 4 and 9</b> (upstream skips
     * those two ids). Only one consumer is ported: on a world where this lands on 11, a
     * digamma-wavelength FEL beam leaves spreading {@code digamma_matter} instead of a plain
     * {@code fire_digamma}. Upstream also honoured a {@code generalOverride} config, which the port
     * has no config file for: set the field directly if you want to force an id.
     */
    public static int polaroidID = 1;

    private static void rollPolaroid() {
        Random rand = new Random();
        do {
            polaroidID = rand.nextInt(18) + 1;
        } while (polaroidID == 4 || polaroidID == 9);
    }

    /** {@code config/hbm.cfg}, 1.7's {@code event.getSuggestedConfigurationFile()}. */
    public static Path configFile() {
        return ConfigFiles.configDir().resolve("hbm.cfg");
    }

    /**
     * 1.7 {@code MainRegistry.loadConfig}: every {@code loadFromConfig} in 1.7's order, then {@code config.save()}, which
     * writes the file with any missing key at its default. Runs first in {@link #bootstrap}, before anything is
     * registered, as 1.7's ran in pre-init.
     */
    public static void loadConfig(Configuration config) {
        config.load();

        GeneralConfig.loadFromConfig(config);
        WorldConfig.loadFromConfig(config);
        MachineConfig.loadFromConfig(config);
        BombConfig.loadFromConfig(config);
        RadiationConfig.loadFromConfig(config);
        PotionConfig.loadFromConfig(config);
        ToolConfig.loadFromConfig(config);
        WeaponConfig.loadFromConfig(config);
        MobConfig.loadFromConfig(config);
        StructureConfig.loadFromConfig(config);

        config.save();
    }

    public static void bootstrap() {
        loadConfig(new Configuration(configFile()));
        // Golem Core recipe flags for the config branches of 1.7's crafting recipes ("golem:requires")
        RecipeConditions.init();
        // The fluid registry is a plain static map (not an MC registry); populate it first so any
        // fluid tile/recipe that references Fluids.* during registration finds the types ready.
        rollPolaroid();
        Fluids.init();
        Fluids.declareVanilla(); // every FluidType as a vanilla Fluid for other mods
        Mats.init(); // material registry (tag-based ore-dict replacement)
        // Order matters: blocks before items (block items reference blocks), tabs last.
        ModBlocks.init();
        ModItemModels.declare();
        ModMaterialTags.declare();
        ModDamageTypes.declare();
        ModBiomeModifiers.declare();
        LegacyDungeonDeclarations.declare(); // WG-D: legacy dungeon structures + scattered-block features (before BiomeFeatures.install)
        ModMobSpawns.init(); // 1.7 EntityMappings natural spawns + placements (before BiomeFeatures.install)
        BiomeFeatures.install(); // Fabric: the same biome modifiers through BiomeModifications (no-op elsewhere)
        ModCraftingRecipes.declare();
        ModArmorTableBlocks.init();
        ModWeaponTableBlocks.init();
        ModBeaconBlocks.init(); // plain beacon-base storage blocks
        ModMachineBlocks.init();   // machine blocks, Redstone-over-Radio included (must precede block entities, which reference them)
        ModConveyorContent.init(); // conveyor belts, cranes, piston inserter, wand
        ModPneumaticContent.init(); // pneumatic tube storage system
        ModDroneContent.init(); // delivery drone blocks, linker, request network sweep
        ModProductionContent.init(); // production machines A: rotary furnace, thresher, microwave, autocrafter, conveyor press
        ModProduction2Content.init(); // production machines B: excavator, ore slopper, annihilator, old boiler, RTG furnaces
        ModEnergyNetContent.init(); // pylons/wires, cable variants, legacy storage tiers, HEV battery, tesla
        ModDoorContent.init(); // generic OBJ doors, the old blast door, mod doors and the steel trapdoor
        ModSecurityContent.init(); // barbed wire, spikes, fences, barricade, locksmith table, padlocks
        ModPwrContent.init(); // pressurized water reactor: parts, controller, fuel rods, printer
        ModPowerGenContent.init(); // Leviathan turbine, LPW-2, geothermal exchanger, IGen memorial, siren + tracks
        ModDecoContent.init(); // structural/decorative blocks: pipes, pillars, lamps, spotlights, bricks, asphalt, crates, poles
        ModTerrainContent.init(); // natural/terrain/hazard blocks: plants, frozen ground, biome stone, fissure, volcano, geysers, vents, storage blocks
        ModPileBlocks.init();
        ModDFCBlocks.init();
        ModSatelliteItems.init();
        ModSatelliteContent.init(); // satellites: ID manager, cargo pad, orbital laser, /ntmsatellites
        ModBatteryBlocks.init();   // energy-storage machine blocks (battery socket)
        ModTurretBlocks.init(); // turret pad cell and the turrets on it
        ModStorageBlocks.init();   // storage crates (iron / steel / desh)
        ModBombBlocks.init();      // bomb blocks (also precede block entities)
        ModBombs2Content.init(); // special bombs: custom nuke, F.L.E.I.J.A., Blue Rinse, Prototype, balefire bomb, thermo/EMP/float/flame war
        ModRailBlocks.init(); // the folded rails the ntm-next trains run on
        ModCustomMissileContent.init(); // custom missile parts, assembly station, compact launcher, launch table, large/rusted pads
        ModRadarUtilityContent.init(); // radars, radar screen/linker, forcefield, teleporter/anchor, orbus, silo hatch, Soyuz capsule
        ModDecoCrateContent.init(); // storage crates, bobbleheads/plushies/snowglobes, radios, emitters, coils, cap blocks, absorbers, glyphid hive blocks
        ModGapsBContent.init(); // G-B: crates opened from the hand (CRATE_OPEN_HELD menus)
        ModRenderInputContent.init(); // G-C: tool animation cues, equip edge, rendering/input gap content
        ModIcfContent.init(); // ICF reactor, seed, vessel components, laser controller/array, pellet maker, fuel pellet
        ModEnergyBedrockContent.init(); // boxcable, coated wire, charging station, HE/RF converters, bedrock ore grades
        ModDungeonContent.init(); // dungeon/structure bricks, keyholes, pedestal, skeleton holder, loot pile, old lantern, action blocks, cart rails, rebar + placer
        ModStructureWandBlocks.init(); // structure wand blocks: wand_air/loot/jigsaw/logic/tandem/structure (NBTStructure editing)
        ModHazardBlocks.init(); // radioactive / rad-resistant blocks (radiation emitters)
        ModFalloutBlocks.init(); // fallout layer + waste/sellafite terrain (nuke aftermath)
        ModSteelBlocks.init();   // stateful steel grate + OBJ scaffold
        ModDecoBlocks.init();    // decorative DecoBlock set (steel structures, sat dishes, boxcar/boat)
        ModAnvilBlocks.init();   // the 12 tiered anvils (must precede block entities)
        ModOilBlocks.init();     // special oil blocks (ore_oil deposit / oil_pipe / oil_spill)
        ModNuclearBlocks.init(); // uranium outgassing ores and radon gases
        ModOutgasBlocks.init(); // asbestos, corium cobble, tomb scrap, and lab outgas blocks
        ModOreBlocks.init();     // clusters, depth rock/ores, resource stone, cave spikes, gas pockets
        ModGlassBlocks.init();   // the BlockNTMGlass family (glass_quartz so far)
        ModLadderBlocks.init();  // vanilla LadderBlock family, including ladder_red_top
        ModRBMKBlocks.init(); // reactor columns/peripherals before their block items and BE types
        ModDataComponents.init();  // custom item data components (referenced by hand items below)
        ItemHot.init(); // declare the hot-item component before Fabric's registry flush
        ModRecipes.init(); // custom crafting serializers
        ModItems.init();
        // After ModItems: its container items name ModItems empties as craft remainders at registration time.
        ModFluidNetContent.init(); // fluid-network ducts, barrels, foundry tank, fluid blocks, containers
        ModEntityItems.init(); // spawn items, carts, drones, trains, grenades, dispersers (entity port)
        ModGunSupportItems.init(); // ammo container and stack-backed ammo/casing bags
        ModJetpackDependencyItems.init(); // hot steel and the functional xenon thruster part
        ModAbilityToolItems.init(); // ability tools/weapons, tool sets, meteorite sword chain
        ModMeleeKitItems.init(); // special melee weapons, novelty tools, starter kits, toolbox, loot, Bobmazon
        ModSingularityItems.init(); // dropped singularity capsules and Spark progression
        ModSelfchargerItems.init(); // deprecated powered-armour auxiliary batteries
        ModBatterySCItems.init(); // 1.7 ItemBatterySC radiovoltaic batteries (battery_sc_<variant>)
        ModRtgContent.init(); // RTG pellet state, block entity, and menu registrations
        ModChemicalDyeItems.init(); // flattened chemical-dye and crayon variants
        ModSpawnEggs.init(); // spawn eggs for the 29 1.7 addMob mobs
        ModStorageDrumItems.init();
        ModFoodItems.init();
        ModConsumableTailItems.init();
        ModFinalConsumableItems.init(); // food, consumables, records, batteries, loose materials, 5G/muffler upgrades
        ModCatalystItems.init();
        ModMedicineItems.init();
        ModRBMKItems.init(); // RBMK rods/pellets and reactor tools
        ModZirnoxItems.init(); // component-backed reactor fuel and spent fuel
        ModMachineItems.init();    // hand-registered machine items (templates)
        ModCasingItems.init(); // metal variants of the old metadata casing item
        ModStampItems.init(); // press stamps (dies)
        ModMoldItems.init();  // casting molds (foundry mold/basin) + mold_base
        ModAutogenItems.init();        // auto-generated metal-shape items (cast plate, shell, …)
        ModIntermediateItems.init();   // recipe intermediates (circuit / item_expensive / chunk_ore)
        ModIngredientItems.init(); // escaped metadata families: generic parts, ash, and plate fuel
        ModBatteryItems.init();    // modern battery / capacitor items (the socket's storage medium)
        ModUpgradeItems.init(); // machine upgrade modules (speed/effect/power/overdrive)
        ModFuelItems.init();  // the ItemFuel family (furnace-burnable; burn times wired per loader)
        ModOilItems.init();   // oil-chain items (oil_tar variants + oil_detector)
        ModToolItems.init();  // in-world construction tools (blowtorch, boltgun)
        ModWatzItems.init();  // Watz fuel pellets (one item per type, fresh + depleted)
        ModFallingBlocks.init(); // the BlockFallingBase family (gravity blocks)
        ModFluidItems.init();      // fluid-container items (fluid_bucket, one per fluid)
        ModBombItems.init();       // hand-registered bomb components
        ModGearItems.init(); // radiation gear (geiger/dosimeter + hazmat armor)
        ModArmorItems.init(); // modelled power armour (T-51b)
        ModArmorSetBContent.init(); // final armor B: remaining ModItemsArmor ids, IV bags, statue_elb_f
        ModArmorSetContent.init(); // final armor sets A: alloy, cmb, bismuth, euphemium, zirconium, dnt, AJR/AJRO, HEV, Fau, trenchmaster
        ModArmorModules.init(); // complete armor modules (storage, cladding, inserts)
        ModArmorModItems.init(); // remaining ItemMod* modules, PaA hazmat, liquidator, mask of infamy
        ModUtilityToolContent.init(); // utility/diagnostic tools, pipettes, guide/lore books, holotapes, tablets, Lemegeton, legacy manual block
        ModCustomMachineContent.init(); // custom machine system: controllers from hbmCustomMachines.json, cm_* components, anchor, structure wand
        ModNuclearMiscContent.init(); // radiolysis RTG, legacy breeder/research reactors + controller, legacy graphite pile, breeding/pile rods, spent plate fuel
        ModExplosivesContent.init(); // landmines, timed charges, det blocks, plastic explosive blocks, duds, multi purpose bomb, detonators
        // Ammo, factories, and gun suppliers must all be declared before any loader flushes ITEMs.
        GunCatalogue.init();
        TileEntityPileCore.initDebris();
        ModMissileItems.init(); // launchable missiles + the designators that aim them
        ModWandItems.init(); // creative wand_k / wand_s / wand_d
        AdvancementCommandContent.init(); // 1.7 achievements as advancements (triggers, icons) + ntm* commands
        ModFeatures.init(); // worldgen feature types (1.7 ores, strata, MapGen terrain, surface features)
        BossSpawnHandler.init(); // 1.7 meteor strikes and showers, rolled every level tick
        SpecialPlayers.init(); // 1.7 onPlayerRespawn: Dr_Nostalgia's hat and Beta
        HbmPlayerProps.init(); // 1.7 onPlayerClone: reputation, shield, fbi/rad marks survive respawn
        NTMWorldGenerator.init(); // NBT structures: hbm:nbt structure type, piece type, 1.7 spawn registrations
        NTMComponentStructures.init(); // features/bunker/meteor_dungeon spawns + hardcoded piece types (WG-C)
        LegacyDungeonStructure.init(); // WG-D: 1.7 HbmWorldGen legacy dungeons (hbm:legacy_dungeon) + scattered-block feature types
        ModBiomes.init(); // crater biome keys, backed by worldgen biome JSON
        ModBlockEntities.init();
        ModRBMKBlockEntities.init();
        ModMenus.init();
        ModGunSupportMenus.init();
        ModRBMKMenus.init();
        ModEntities.init();        // entity types (the explosion + fallout entities)
        ModRBMKEntities.init();
        ModMissileEntities.init(); // the missiles
        ModNtmEntities.init(); // the ntm-next entity port (mobs, vehicles, effects, remaining projectiles)
        HbmParticles.init();
        ModEffects.init(); // status effects (radiation)
        ModSounds.init();          // sound events (assembly machine motor/strike/start/stop)
        ModTabs.init();
        NeutronLifecycle.init();
        RBMKDials.init();            // the RBMK dials as /gamerule world rules
        PollutionLifecycle.init();   // soot/poison field: diffusion, decay and blight
        ModEventHandlerMobs.init();     // 1.7 ModEventHandler mob death drops, decorateMob, addAITasks
        ModEventHandlerImpact.init();   // Tom impact aftermath: dust/fire decay, burning, spawns
        ModEventHandlerHooks.init();    // food eaten, block break, sleep, potential spawns
        ImpactSky.register();         // Tom impact sky darkening (1.7 WorldProviderNTM)
        RadiationFog.init(); // 1.7 radFog, read from the chunk radiation
        RadiationWalls.init(); // the hbm:radiation_resistant tag as radiation walls
        ChunkLoadingManager.init();   // forced-chunk bookkeeping + restart reconciliation
        Lifecycle.onLevelUnload((server, level) -> JetpackHandler.clear(level));
        Lifecycle.onServerTick(server ->
                server.getAllLevels().forEach(JetpackHandler::cleanup));
        // ntm-next entity port: item pools resolve their registry ids once the server holds registries; the
        // TOM impact gate is republished for off-thread worldgen readers; per-server caches clear on stop.
        Lifecycle.onServerStarted(server -> {
            ItemPool.load(server.registryAccess());
            TomSaveData.publish(server);
        });
        Lifecycle.onServerStopping(server -> {
            TomSaveData.onServerStopping();
            RequestNetwork.onServerStopping();
            RTTYSystem.onServerStopping();
            ChunkUtil.onServerStopped();
        });
        // Needs both the tool items and the convertible blocks, so it runs after every registry.
        NTMToolHandler.register();
        // Machines and container items on the loaders' fluid capabilities.
        ForeignFluids.register();
        // Machine and crate contents as a read-only view for other mods' viewers
        ForeignItems.register();
        // 1.7's OpenComputers components as CC: Tweaked peripherals, when CC is loaded
        ComputerCraft.init();
        // AE2 pattern providers push jobs into the recipe machines, when AE2 is loaded
        AE2CompatHandler.init();
        // Buffers, tanks and multiblock cores in a block-info overlay (Jade), when one is loaded
        HbmBlockInfo.init();
    }
}
