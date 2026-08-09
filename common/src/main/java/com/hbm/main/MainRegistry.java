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
import com.hbm.tileentity.ModBlockEntities;

/**
 * Common bootstrap entry point. Loads the content classes so their {@code DeferredRegister}s are
 * populated. Must be invoked by every loader <em>before</em> registration is flushed (for NeoForge,
 * before {@code RegisterEvent} fires; for Fabric, before {@code flushAll}).
 */
public final class MainRegistry {

    private MainRegistry() {}

    public static void bootstrap() {
        // The fluid registry is a plain static map (not an MC registry); populate it first so any
        // fluid tile/recipe that references Fluids.* during registration finds the types ready.
        Fluids.init();
        com.hbm.inventory.material.Mats.init(); // material registry (tag-based ore-dict replacement)
        // Order matters: blocks before items (block items reference blocks), tabs last.
        ModBlocks.init();
        ModMachineBlocks.init();   // machine blocks (must precede block entities, which reference them)
        ModBatteryBlocks.init();   // energy-storage machine blocks (battery socket)
        ModStorageBlocks.init();   // storage crates (iron / steel / desh)
        ModBombBlocks.init();      // bomb blocks (also precede block entities)
        com.hbm.blocks.ModHazardBlocks.init(); // radioactive / rad-resistant blocks (radiation emitters)
        com.hbm.blocks.ModFalloutBlocks.init(); // fallout layer + waste/sellafite terrain (nuke aftermath)
        com.hbm.blocks.ModDecoBlocks.init();    // decorative DecoBlock set (steel structures, sat dishes, boxcar/boat)
        com.hbm.blocks.ModAnvilBlocks.init();   // the 12 tiered anvils (must precede block entities)
        com.hbm.blocks.ModOilBlocks.init();     // special oil blocks (ore_oil deposit / oil_pipe / oil_spill)
        ModDataComponents.init();  // custom item data components (referenced by hand items below)
        ModItems.init();
        ModMachineItems.init();    // hand-registered machine items (templates)
        com.hbm.items.machine.ModStampItems.init(); // press stamps (dies)
        com.hbm.items.machine.ModMoldItems.init();  // casting molds (foundry mold/basin) + mold_base
        com.hbm.items.ModAutogenItems.init();        // auto-generated metal-shape items (cast plate, shell, …)
        com.hbm.items.ModIntermediateItems.init();   // recipe intermediates (circuit / item_expensive / chunk_ore)
        ModBatteryItems.init();    // modern battery / capacitor items (the socket's storage medium)
        com.hbm.items.ModUpgradeItems.init(); // machine upgrade modules (speed/effect/power/overdrive)
        com.hbm.items.ModFuelItems.init();  // the ItemFuel family (furnace-burnable; burn times wired per loader)
        com.hbm.items.ModOilItems.init();   // oil-chain items (oil_tar variants + oil_detector)
        com.hbm.blocks.ModFallingBlocks.init(); // the BlockFallingBase family (gravity blocks)
        ModFluidItems.init();      // fluid-container items (fluid_bucket, one per fluid)
        ModBombItems.init();       // hand-registered bomb components
        com.hbm.items.ModGearItems.init(); // radiation gear (geiger/dosimeter + hazmat armor)
        com.hbm.world.gen.ModFeatures.init(); // worldgen features (oil bubble)
        ModBlockEntities.init();
        ModMenus.init();
        ModEntities.init();        // entity types (the explosion + fallout entities)
        com.hbm.potion.ModEffects.init(); // status effects (radiation)
        ModSounds.init();          // sound events (assembly machine motor/strike/start/stop)
        ModTabs.init();
    }
}
