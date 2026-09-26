package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockOutgas;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

/** The six non-generated BlockOutgas registrations from old ModBlocks. */
public final class ModOutgasBlocks {
    private ModOutgasBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockOutgas> brick_asbestos = block("brick_asbestos", HbmTab.BLOCKS,
            20, false, p -> p.strength(15.0F, 40.0F).sound(SoundType.STONE));
    public static final RegistryObject<BlockOutgas> tile_lab_broken = block("tile_lab_broken", HbmTab.BLOCKS,
            40, false, p -> p.strength(1.0F, 20.0F).sound(SoundType.STONE));
    public static final RegistryObject<BlockOutgas> deco_asbestos = block("deco_asbestos", HbmTab.BLOCKS,
            40, false, p -> p.strength(5.0F, 10.0F).sound(SoundType.STONE));
    public static final RegistryObject<BlockOutgas> block_asbestos = block("block_asbestos", HbmTab.BLOCKS,
            4, false, p -> p.strength(10.0F, 10.0F).sound(SoundType.STONE));
    public static final RegistryObject<BlockOutgas> block_corium_cobble = block("block_corium_cobble", HbmTab.RESOURCES,
            1, true, p -> p.strength(100.0F, 6000.0F).sound(SoundType.STONE));
    public static final RegistryObject<BlockOutgas> ancient_scrap = block("ancient_scrap", HbmTab.RESOURCES,
            1, true, p -> p.strength(100.0F, 6000.0F).sound(SoundType.STONE));

    private static RegistryObject<BlockOutgas> block(String id, HbmTab tab, int rate, boolean neighbour,
                                                     UnaryOperator<BlockBehaviour.Properties> props) {
        RegistryObject<BlockOutgas> outgas = BLOCKS.registerBlock(id,
                p -> new BlockOutgas(p, true, rate, true, neighbour),
                p -> props.apply(p).requiresCorrectToolForDrops().randomTicks());
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, outgas,
                p -> dev.golem.core.world.Compat.blockDescription(p));
        ModTabs.addTo(tab, item);
        return outgas;
    }

    public static void init() {}
}
