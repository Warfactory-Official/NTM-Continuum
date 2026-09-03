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

    public static void bootstrap() {
        // The fluid registry is a plain static map (not an MC registry); populate it first so any
        // fluid tile/recipe that references Fluids.* during registration finds the types ready.
        rollPolaroid();
        Fluids.init();
        com.hbm.inventory.material.Mats.init(); // material registry (tag-based ore-dict replacement)
        // Order matters: blocks before items (block items reference blocks), tabs last.
        ModBlocks.init();
        com.hbm.blocks.machine.ModArmorTableBlocks.init();
        com.hbm.blocks.ModBeaconBlocks.init(); // plain beacon-base storage blocks
        ModMachineBlocks.init();   // machine blocks (must precede block entities, which reference them)
        com.hbm.assemblyfactory.AssemblyFactoryRegistrations.init();
        com.hbm.chemicalfactory.ChemicalFactoryRegistrations.init();
        com.hbm.blocks.machine.AutosawFeature.init();
        com.hbm.blocks.machine.solar.ModSolarBlocks.init();
        com.hbm.items.solar.ModSolarItems.init();
        com.hbm.tileentity.machine.solar.ModSolarBlockEntities.init();
        com.hbm.smoke.SmokeChainRegistration.init();
        com.hbm.foundry.FoundryExpansion.init();
        com.hbm.blocks.machine.DiFurnaceContent.init();
        ModBatteryBlocks.init();   // energy-storage machine blocks (battery socket)
        ModStorageBlocks.init();   // storage crates (iron / steel / desh)
        ModBombBlocks.init();      // bomb blocks (also precede block entities)
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
        com.hbm.blocks.material.MaterialCompletionRegistrations.init();
        com.hbm.inventory.recipes.ModRecipes.init(); // custom crafting serializers
        ModItems.init();
        com.hbm.items.ModChemicalDyeItems.init(); // flattened chemical-dye and crayon variants
        com.hbm.items.ModStorageDrumItems.init();
        com.hbm.items.ModFoodItems.init();
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
        com.hbm.items.ModArmorModules.init(); // complete armor modules (storage, cladding, inserts)
        com.hbm.items.ModAmmoItems.init();  // Sedna ammunition (one item per old ammo_standard variant)
        // The bullet configs must exist before a gun's magazine names them; the gun items themselves are
        // built lazily at registration, well after this.
        com.hbm.items.weapon.sedna.factory.GunFactory.init();
        com.hbm.items.ModGunItems.init();   // the Sedna guns
        com.hbm.items.ModMissileItems.init(); // launchable missiles + the designators that aim them
        com.hbm.world.gen.ModFeatures.init(); // worldgen features (oil bubble)
        com.hbm.world.biome.ModBiomes.init(); // crater biome keys, backed by worldgen biome JSON
        ModBlockEntities.init();
        com.hbm.tileentity.ModRBMKBlockEntities.init();
        ModMenus.init();
        com.hbm.inventory.ModRBMKMenus.init();
        ModEntities.init();        // entity types (the explosion + fallout entities)
        com.hbm.entity.ModRBMKEntities.init();
        com.hbm.entity.missile.ModMissileEntities.init(); // the missiles
        com.hbm.particle.HbmParticles.init();
        com.hbm.potion.ModEffects.init(); // status effects (radiation)
        ModSounds.init();          // sound events (assembly machine motor/strike/start/stop)
        ModTabs.init();
        com.hbm.handler.neutron.NeutronLifecycle.init();
        com.hbm.world.chunk.ChunkLoadingManager.init();   // forced-chunk bookkeeping + restart reconciliation
        // Needs both the tool items and the convertible blocks, so it runs after every registry.
        com.hbm.handler.NTMToolHandler.register();
    }
}
