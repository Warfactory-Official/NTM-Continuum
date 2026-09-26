package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockGasRadon;
import com.hbm.blocks.generic.BlockGasMeltdown;
import com.hbm.blocks.generic.BlockGasAsbestos;
import com.hbm.blocks.generic.BlockGasMonoxide;
import com.hbm.blocks.generic.BlockGasRadonTomb;
import com.hbm.blocks.generic.BlockOutgas;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/** Non-generated nuclear resources and gas blocks. */
public final class ModNuclearBlocks {
    private ModNuclearBlocks() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockOutgas> ore_uranium = ore("ore_uranium", 5F, 20);
    public static final RegistryObject<BlockOutgas> ore_uranium_scorched = ore("ore_uranium_scorched", 5F, 15);
    public static final RegistryObject<BlockOutgas> ore_gneiss_uranium = ore("ore_gneiss_uranium", 1.5F, 20);
    public static final RegistryObject<BlockOutgas> ore_gneiss_uranium_scorched = ore("ore_gneiss_uranium_scorched", 1.5F, 20);
    public static final RegistryObject<BlockOutgas> ore_nether_uranium = ore("ore_nether_uranium", 0.4F, 20);
    public static final RegistryObject<BlockOutgas> ore_nether_uranium_scorched = ore("ore_nether_uranium_scorched", 0.4F, 20);
    public static final RegistryObject<BlockGasRadon> gas_radon = BLOCKS.registerBlock("gas_radon",
            p -> new BlockGasRadon(p.noOcclusion().noCollision().replaceable().randomTicks(), 0.05F, false));
    public static final RegistryObject<BlockGasRadon> gas_radon_dense = BLOCKS.registerBlock("gas_radon_dense",
            p -> new BlockGasRadon(p.noOcclusion().noCollision().replaceable().randomTicks(), 0.5F, true));
    public static final RegistryObject<BlockGasMeltdown> gas_meltdown = BLOCKS.registerBlock("gas_meltdown",
            p -> new BlockGasMeltdown(p.noOcclusion().noCollision().replaceable().randomTicks()));
    public static final RegistryObject<BlockGasRadonTomb> gas_radon_tomb = BLOCKS.registerBlock("gas_radon_tomb",
            p -> new BlockGasRadonTomb(p.noOcclusion().noCollision().replaceable().randomTicks()));
    public static final RegistryObject<BlockGasMonoxide> gas_monoxide = BLOCKS.registerBlock("gas_monoxide",
            p -> new BlockGasMonoxide(p.noOcclusion().noCollision().replaceable().randomTicks()));
    public static final RegistryObject<BlockGasAsbestos> gas_asbestos = BLOCKS.registerBlock("gas_asbestos",
            p -> new BlockGasAsbestos(p.noOcclusion().noCollision().replaceable().randomTicks()));

    private static RegistryObject<BlockOutgas> ore(String id, float hardness, int rate) {
        return BLOCKS.registerBlock(id, p -> new BlockOutgas(p, true, rate, true),
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
