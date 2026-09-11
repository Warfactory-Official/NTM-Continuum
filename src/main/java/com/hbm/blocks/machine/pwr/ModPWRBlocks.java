package com.hbm.blocks.machine.pwr;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ItemEnumMulti;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.util.Arrays;

/** The pressurised water reactor's parts, its controller, the assembled cell and the fuel pellets. */
public final class ModPWRBlocks {

    private ModPWRBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<Block> pwr_fuelrod = part("pwr_fuelrod");
    public static final RegistryObject<Block> pwr_control = part("pwr_control");
    public static final RegistryObject<Block> pwr_channel = part("pwr_channel");
    public static final RegistryObject<Block> pwr_heatex = part("pwr_heatex");
    public static final RegistryObject<Block> pwr_heatsink = part("pwr_heatsink");
    public static final RegistryObject<Block> pwr_neutron_source = part("pwr_neutron_source");
    public static final RegistryObject<Block> pwr_reflector = part("pwr_reflector");
    public static final RegistryObject<Block> pwr_casing = part("pwr_casing");
    public static final RegistryObject<Block> pwr_port = part("pwr_port");

    public static final RegistryObject<MachinePWRController> pwr_controller = BLOCKS.registerBlock(
            "pwr_controller", MachinePWRController::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<BlockItem> pwr_controller_item =
            ITEMS.registerBlockItem("pwr_controller", pwr_controller);

    /** No item form: the cell only ever exists inside an assembled reactor. */
    public static final RegistryObject<BlockPWR> pwr_block = BLOCKS.registerBlock(
            "pwr_block", BlockPWR::new,
            p -> p.strength(15.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<ItemPWRFuel> pwr_fuel =
            ITEMS.registerItem("pwr_fuel", ItemPWRFuel::new, p -> p.stacksTo(16));
    public static final RegistryObject<ItemEnumMulti> pwr_fuel_hot =
            ITEMS.registerItem("pwr_fuel_hot", p -> new ItemEnumMulti(EnumPWRFuel.class, true, false, p),
                    p -> p.stacksTo(16));
    public static final RegistryObject<ItemEnumMulti> pwr_fuel_depleted =
            ITEMS.registerItem("pwr_fuel_depleted", p -> new ItemEnumMulti(EnumPWRFuel.class, true, false, p),
                    p -> p.stacksTo(16));

    static {
        ModTabs.addTo(HbmTab.MACHINES, pwr_controller_item);
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> Arrays.stream(EnumPWRFuel.VALUES)
                .map(type -> pwr_fuel.get().of(type)).toList());
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> Arrays.stream(EnumPWRFuel.VALUES)
                .map(type -> pwr_fuel_hot.get().of(type)).toList());
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> Arrays.stream(EnumPWRFuel.VALUES)
                .map(type -> pwr_fuel_depleted.get().of(type)).toList());
    }

    private static RegistryObject<Block> part(String name) {
        RegistryObject<Block> block = BLOCKS.registerBlock(name, Block::new,
                p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
        ModTabs.addTo(HbmTab.MACHINES, ITEMS.registerBlockItem(name, block));
        return block;
    }

    /** Class-load hook so the registers are populated during bootstrap. */
    public static void init() {}
}
