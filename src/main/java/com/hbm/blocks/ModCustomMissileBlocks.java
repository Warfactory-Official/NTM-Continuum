package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.bomb.CompactLauncher;
import com.hbm.blocks.bomb.LaunchPadLarge;
import com.hbm.blocks.bomb.LaunchPadRusted;
import com.hbm.blocks.bomb.LaunchTable;
import com.hbm.blocks.machine.MachineMissileAssembly;
import com.hbm.blocks.machine.struct.BlockStruct;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import com.hbm.tileentity.machine.TileEntityMultiblock;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

/**
 * The custom missile system's blocks and the two erector/silo pads, as 1.7 {@code ModBlocks} declared them: all
 * {@code Material.iron}, hardness 5, resistance 10, on the missile tab.
 *
 * <p>Not registered: {@code dummy_plate_compact_launcher}, {@code dummy_port_compact_launcher},
 * {@code dummy_plate_launch_table} and {@code dummy_port_launch_table}. They were upstream's dummy blocks for the
 * two launchers and are folded into the launchers' own cells here, as every other port multiblock folds its
 * dummies (see {@link CompactLauncher} and {@link LaunchTable}).
 */
public final class ModCustomMissileBlocks {

    private ModCustomMissileBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static final UnaryOperator<BlockBehaviour.Properties> IRON =
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    /** Renderer-drawn machines: the block model is empty, so nothing may cull or suffocate against it. */
    private static final UnaryOperator<BlockBehaviour.Properties> RENDERED =
            p -> IRON.apply(p).noOcclusion().isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false);

    public static final RegistryObject<BlockStruct> struct_launcher_core = BLOCKS.registerBlock("struct_launcher_core",
            p -> new BlockStruct(p, TileEntityMultiblock::new, () -> ModCustomMissileBlockEntities.STRUCT_LAUNCHER_CORE.get()),
            p -> IRON.apply(p).noOcclusion());
    public static final RegistryObject<BlockStruct> struct_launcher_core_large = BLOCKS.registerBlock("struct_launcher_core_large",
            p -> new BlockStruct(p, TileEntityMultiblock::new, () -> ModCustomMissileBlockEntities.STRUCT_LAUNCHER_CORE_LARGE.get()),
            p -> IRON.apply(p).noOcclusion());

    public static final RegistryObject<MachineMissileAssembly> machine_missile_assembly =
            BLOCKS.registerBlock("machine_missile_assembly", MachineMissileAssembly::new, RENDERED);
    public static final RegistryObject<CompactLauncher> compact_launcher =
            BLOCKS.registerBlock("compact_launcher", CompactLauncher::new, RENDERED);
    public static final RegistryObject<LaunchTable> launch_table =
            BLOCKS.registerBlock("launch_table", LaunchTable::new, RENDERED);
    public static final RegistryObject<LaunchPadRusted> launch_pad_rusted =
            BLOCKS.registerBlock("launch_pad_rusted", LaunchPadRusted::new, RENDERED);
    public static final RegistryObject<LaunchPadLarge> launch_pad_large =
            BLOCKS.registerBlock("launch_pad_large", LaunchPadLarge::new, RENDERED);

    static {
        // 1.7 ModBlocks registration order on the missile tab
        item("struct_launcher_core", struct_launcher_core);
        item("struct_launcher_core_large", struct_launcher_core_large);
        item("machine_missile_assembly", machine_missile_assembly);
        item("launch_pad_rusted", launch_pad_rusted);
        item("launch_pad_large", launch_pad_large);
        item("compact_launcher", compact_launcher);
        item("launch_table", launch_table);
    }

    private static void item(String name, RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block, Compat::blockDescription);
        ModTabs.addTo(HbmTab.MISSILES, item);
    }

    public static void init() {}
}
