package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockGasRadon;
import com.hbm.blocks.generic.BlockGasMeltdown;
import com.hbm.blocks.generic.BlockOutgas;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Non-generated nuclear resources and gas blocks. */
public final class ModNuclearBlocks {
    private ModNuclearBlocks() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockOutgas> ore_uranium = ore("ore_uranium", 5F);
    public static final RegistryObject<BlockOutgas> ore_uranium_scorched = ore("ore_uranium_scorched", 5F);
    public static final RegistryObject<BlockOutgas> ore_gneiss_uranium = ore("ore_gneiss_uranium", 1.5F);
    public static final RegistryObject<BlockOutgas> ore_gneiss_uranium_scorched = ore("ore_gneiss_uranium_scorched", 1.5F);
    public static final RegistryObject<BlockOutgas> ore_nether_uranium = ore("ore_nether_uranium", 0.4F);
    public static final RegistryObject<BlockOutgas> ore_nether_uranium_scorched = ore("ore_nether_uranium_scorched", 0.4F);
    public static final RegistryObject<BlockGasRadon> gas_radon = BLOCKS.registerBlock("gas_radon",
            p -> new BlockGasRadon(p.noOcclusion().noCollision().replaceable().randomTicks(), 0.05F, false));
    public static final RegistryObject<BlockGasRadon> gas_radon_dense = BLOCKS.registerBlock("gas_radon_dense",
            p -> new BlockGasRadon(p.noOcclusion().noCollision().replaceable().randomTicks(), 0.5F, true));
    public static final RegistryObject<BlockGasMeltdown> gas_meltdown = BLOCKS.registerBlock("gas_meltdown",
            p -> new BlockGasMeltdown(p.noOcclusion().noCollision().replaceable().randomTicks()));

    private static RegistryObject<BlockOutgas> ore(String id, float hardness) {
        return BLOCKS.registerBlock(id, BlockOutgas::new,
                p -> p.strength(hardness, 10F).sound(SoundType.STONE).requiresCorrectToolForDrops().randomTicks());
    }

    static {
        for (RegistryObject<? extends Block> block : java.util.List.of(ore_uranium, ore_uranium_scorched,
                ore_gneiss_uranium, ore_gneiss_uranium_scorched, ore_nether_uranium, ore_nether_uranium_scorched)) {
            RegistryObject<BlockItem> item = ITEMS.registerBlockItem(block.getId().getPath(), block::get);
            ModTabs.addTo(HbmTab.RESOURCES, item);
        }
    }
    public static void init() {}
}
