package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.machine.MachineSatDock;
import com.hbm.blocks.machine.MachineSatLinker;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * The satellite family's blocks, as 1.7 {@code ModBlocks} declared them: both {@code Material.iron}, hardness 5,
 * resistance 10, on the missile tab. {@code dummy_plate_cargo} is folded into {@code sat_dock}'s own cells.
 */
public final class ModSatelliteBlocks {

    private ModSatelliteBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<MachineSatLinker> machine_satlinker =
            BLOCKS.registerBlock("machine_satlinker", MachineSatLinker::new,
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    /** Renderer-drawn: the block model is empty, so nothing may cull or suffocate against it. */
    public static final RegistryObject<MachineSatDock> sat_dock =
            BLOCKS.registerBlock("sat_dock", MachineSatDock::new,
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                            .isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false));

    static {
        item("machine_satlinker", machine_satlinker);
        item("sat_dock", sat_dock);
    }

    private static void item(String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block, Compat::blockDescription);
        ModTabs.addTo(HbmTab.MISSILES, item);
    }

    public static void init() {}
}
