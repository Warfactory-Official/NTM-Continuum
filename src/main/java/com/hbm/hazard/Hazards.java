package com.hbm.hazard;

/**
 * Post-registration hazard setup entrypoint. Must run <b>after</b> all items/blocks are registered
 * (so {@link HazardRegistry#registerItems()} can resolve items from the registry), i.e. from each
 * loader's post-registration hook (NeoForge {@code FMLCommonSetupEvent}, Fabric after
 * {@code DeferredRegister.flushAll}).
 */
public final class Hazards {

    private Hazards() {}

    private static boolean done = false;

    public static void init() {
        if (done) return;
        done = true;
        com.hbm.items.ModRBMKItems.linkPellets();
        HazardRegistry.registerTrafos();
        HazardRegistry.registerItems();
        com.hbm.items.ModGearItems.registerHazards(); // hazmat resistance coefficients + full-set membership
        com.hbm.items.ModArmorItems.configure();      // power-armour set bonuses, rad resistance, hazard classes
        com.hbm.blocks.ModHazardBlocks.initHazards(); // block radiation values (reads block-item RADIATION hazard)
        com.hbm.tileentity.bomb.TileEntityLaunchPadBase.registerLaunchables(); // missile item -> entity table
    }
}
