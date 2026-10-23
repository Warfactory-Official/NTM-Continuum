package com.hbm.blocks;

import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.generic.BlockOutgas;
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

/** The six non-generated BlockOutgas registrations from old ModBlocks. */
public final class ModOutgasBlocks {
    private ModOutgasBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    public static final RegistryObject<BlockOutgas> brick_asbestos = block("brick_asbestos", HbmTab.BLOCKS,
            20, false, p -> p.requiresCorrectToolForDrops().strength(15.0F, 600.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockOutgas> tile_lab_broken = block("tile_lab_broken", HbmTab.BLOCKS,
            40, false, p -> p.requiresCorrectToolForDrops().strength(1.0F, 12.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    // 1.7 Material.cloth: the two asbestos blocks drop without a pickaxe, unlike the brick and tile.
    public static final RegistryObject<BlockOutgas> deco_asbestos = block("deco_asbestos", HbmTab.BLOCKS,
            40, false, p -> p.strength(5.0F, 6.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockOutgas> block_asbestos = block("block_asbestos", HbmTab.BLOCKS,
            4, false, p -> p.strength(10.0F, 9.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockOutgas> block_corium_cobble = block("block_corium_cobble", HbmTab.RESOURCES,
            1, true, p -> p.requiresCorrectToolForDrops().strength(100.0F, 3600.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockOutgas> ancient_scrap = block("ancient_scrap", HbmTab.RESOURCES,
            1, true, p -> p.requiresCorrectToolForDrops().strength(100.0F, 3600.0F).sound(SoundType.STONE))
            .transform(Presets.cubeAll())
            .loot(Loot.selfAlways())
            .tag(BlockTag.PICKAXE);

    private static Reg.Handle<BlockOutgas> block(String id, HbmTab tab, int rate, boolean neighbour,
                                                     UnaryOperator<BlockBehaviour.Properties> props) {
        Reg.Handle<BlockOutgas> outgas = REG.block(id,
                p -> new BlockOutgas(p, true, rate, true, neighbour),
                p -> props.apply(p).randomTicks());
        Reg.Handle<BlockItem> item = REG.blockItem(outgas,
                p -> Compat.blockDescription(p));
        ModTabs.addTo(tab, item);
        return outgas;
    }

    public static void init() {}
}
