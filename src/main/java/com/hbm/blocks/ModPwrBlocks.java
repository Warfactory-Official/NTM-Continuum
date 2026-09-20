package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.DescBlockItem;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.machine.MachinePWRController;

/**
 * Feature-local blocks of the PWR. 1.7 {@code ModBlocks}: every part is iron, 5.0/10, on the machine tab and
 * carries {@code ITooltipProvider}'s description lines; the assembled shell {@code pwr_block} is 15.0/10 with no
 * tab and no item.
 */
public final class ModPwrBlocks {
    private ModPwrBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<Block> PWR_FUEL = part("pwr_fuel");
    public static final RegistryObject<Block> PWR_CONTROL = part("pwr_control");
    public static final RegistryObject<Block> PWR_CHANNEL = part("pwr_channel");
    public static final RegistryObject<Block> PWR_HEATEX = part("pwr_heatex");
    public static final RegistryObject<Block> PWR_HEATSINK = part("pwr_heatsink");
    public static final RegistryObject<Block> PWR_NEUTRON_SOURCE = part("pwr_neutron_source");
    public static final RegistryObject<Block> PWR_REFLECTOR = part("pwr_reflector");
    public static final RegistryObject<Block> PWR_CASING = part("pwr_casing");
    public static final RegistryObject<Block> PWR_PORT = part("pwr_port");
    public static final RegistryObject<MachinePWRController> PWR_CONTROLLER = BLOCKS.registerBlock(
            "pwr_controller", MachinePWRController::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<BlockPWR> PWR_BLOCK = BLOCKS.registerBlock(
            "pwr_block", BlockPWR::new,
            p -> p.strength(15.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    static {
        descItem("pwr_fuel", PWR_FUEL, 2);
        descItem("pwr_control", PWR_CONTROL, 2);
        descItem("pwr_channel", PWR_CHANNEL, 2);
        descItem("pwr_heatex", PWR_HEATEX, 2);
        descItem("pwr_heatsink", PWR_HEATSINK, 3);
        descItem("pwr_neutron_source", PWR_NEUTRON_SOURCE, 3);
        descItem("pwr_reflector", PWR_REFLECTOR, 3);
        descItem("pwr_casing", PWR_CASING, 2);
        descItem("pwr_port", PWR_PORT, 2);
        descItem("pwr_controller", PWR_CONTROLLER, 3);
    }

    private static RegistryObject<Block> part(String name) {
        return BLOCKS.registerSimpleBlock(name,
                p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    /** 1.7 ITooltipProvider blocks: a block item carrying the {@code block.hbm.<id>.desc.N} lines. */
    private static void descItem(String name, RegistryObject<? extends Block> block, int lines) {
        RegistryObject<DescBlockItem> item = ITEMS.registerItem(name,
                p -> new DescBlockItem(block.get(), p, name, lines), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    public static void init() {}
}
