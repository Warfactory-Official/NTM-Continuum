package com.hbm.blocks.machine.storage;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemBatteryREDD;
import com.hbm.items.machine.ItemFENSU;
import dev.golem.core.world.Compat;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
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

    public static final RegistryObject<MachineFENSU> machine_fensu = BLOCKS.registerBlock(
            "machine_fensu", MachineFENSU::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE).noOcclusion()
                    .isSuffocating((state, world, pos) -> false)
                    .isViewBlocking((state, world, pos) -> false));

    public static final RegistryObject<MachineBatteryREDD> machine_battery_redd = BLOCKS.registerBlock(
            "machine_battery_redd", MachineBatteryREDD::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .isSuffocating((state, world, pos) -> false)
                    .isViewBlocking((state, world, pos) -> false));

    static {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem("machine_battery_socket", machine_battery_socket);
        ModTabs.addTo(HbmTab.MACHINES, item);
        RegistryObject<ItemFENSU> fensuItem = ITEMS.registerItem("machine_fensu",
                p -> new ItemFENSU(machine_fensu.get(), p), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, fensuItem);
        RegistryObject<ItemBatteryREDD> batteryReddItem = ITEMS.registerItem("machine_battery_redd",
                p -> new ItemBatteryREDD(machine_battery_redd.get(), p), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, batteryReddItem);
    }

    public static void init() {}
}
