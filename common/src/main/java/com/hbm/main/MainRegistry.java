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

public final class MainRegistry {

    private MainRegistry() {}

    public static void bootstrap() {

        Fluids.init();
        com.hbm.inventory.material.Mats.init();

        ModBlocks.init();
        ModMachineBlocks.init();
        ModBatteryBlocks.init();
        ModStorageBlocks.init();
        ModBombBlocks.init();
        ModDataComponents.init();
        ModItems.init();
        ModMachineItems.init();
        com.hbm.items.machine.ModStampItems.init();
        com.hbm.items.ModAutogenItems.init();
        ModBatteryItems.init();
        ModFluidItems.init();
        ModBombItems.init();
        ModBlockEntities.init();
        ModMenus.init();
        ModEntities.init();
        ModSounds.init();
        ModTabs.init();
    }
}
