package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.machine.BlockConverterHeRf;
import com.hbm.blocks.machine.BlockConverterRfHe;
import com.hbm.blocks.machine.Charger;
import com.hbm.blocks.network.PowerCableBox;
import com.hbm.blocks.network.WireCoated;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

/**
 * The energy extras 1.7 kept in {@code ModBlocks}: the boxed and the coated red copper cable, the charging station
 * and the two RF converters. All five are {@code Material.iron} at 5.0 hardness / 10.0 resistance, which in 1.7
 * needed a pickaxe to drop.
 *
 * <p>{@code red_cable_box} was one block with sizes 0..4 in metadata; it is flattened to
 * {@code red_cable_box_<size>} like {@code fluid_duct_box}.
 */
public final class ModEnergyBedrockBlocks {

    private ModEnergyBedrockBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static UnaryOperator<BlockBehaviour.Properties> iron() {
        return p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    @SuppressWarnings("unchecked")
    public static final RegistryObject<PowerCableBox>[] red_cable_box = new RegistryObject[5];

    static {
        for (int size = 0; size < 5; size++) {
            final int s = size;
            red_cable_box[size] = BLOCKS.registerBlock("red_cable_box_" + size, p -> new PowerCableBox(p, s),
                    p -> iron().apply(p).noOcclusion());
        }
    }

    public static final RegistryObject<WireCoated> red_wire_coated =
            BLOCKS.registerBlock("red_wire_coated", WireCoated::new, iron());
    /** 1.7 blockTab; renderer-drawn, so nothing may cull or suffocate against it. */
    public static final RegistryObject<Charger> charger = BLOCKS.registerBlock("charger", Charger::new,
            p -> iron().apply(p).noOcclusion().isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false));
    public static final RegistryObject<BlockConverterHeRf> machine_converter_he_rf =
            BLOCKS.registerBlock("machine_converter_he_rf", BlockConverterHeRf::new, iron());
    public static final RegistryObject<BlockConverterRfHe> machine_converter_rf_he =
            BLOCKS.registerBlock("machine_converter_rf_he", BlockConverterRfHe::new, iron());

    static {
        // 1.7 ModBlocks order: red_wire_coated, red_cable_box (machineTab) ... converters (machineTab); charger (blockTab)
        item(HbmTab.MACHINES, "red_wire_coated", red_wire_coated);
        for (int size = 0; size < 5; size++) item(HbmTab.MACHINES, "red_cable_box_" + size, red_cable_box[size]);
        item(HbmTab.MACHINES, "machine_converter_he_rf", machine_converter_he_rf);
        item(HbmTab.MACHINES, "machine_converter_rf_he", machine_converter_rf_he);
        item(HbmTab.BLOCKS, "charger", charger);
    }

    private static void item(HbmTab tab, String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block, Compat::blockDescription);
        ModTabs.addTo(tab, item);
    }

    public static Block[] cableBoxes() {
        Block[] blocks = new Block[5];
        for (int i = 0; i < 5; i++) blocks[i] = red_cable_box[i].get();
        return blocks;
    }

    public static void init() {}
}
