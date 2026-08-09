package com.hbm.blocks.machine.storage;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

/**
 * Hand-registered energy-storage machine blocks (the battery socket; future: capacitor banks, FENSU,
 * etc.). Separate from the generated {@link com.hbm.blocks.ModBlocks} and the general
 * {@link com.hbm.blocks.machine.ModMachineBlocks} so codegen never clobbers them.
 */
public final class ModBatteryBlocks {

    private ModBatteryBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<MachineBatterySocket> machine_battery_socket = BLOCKS.registerBlock(
            "machine_battery_socket", MachineBatterySocket::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    static {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem("machine_battery_socket", machine_battery_socket);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    public static void init() {}
}
