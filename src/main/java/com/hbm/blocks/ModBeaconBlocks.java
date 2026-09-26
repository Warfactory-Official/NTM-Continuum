package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockBeaconable;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** The eleven plain {@code BlockBeaconable} storage blocks from old HBM. */
public final class ModBeaconBlocks {

    private ModBeaconBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockBeaconable> block_cadmium = beacon("block_cadmium", 5.0F, 30.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_bismuth = beacon("block_bismuth", 5.0F, 30.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_coltan = beacon("block_coltan", 5.0F, 30.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_tantalium = beacon("block_tantalium", 5.0F, 30.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_niobium = beacon("block_niobium", 5.0F, 30.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_lanthanium = beacon("block_lanthanium", 5.0F, 30.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_actinium = beacon("block_actinium", 5.0F, 30.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_polymer = beacon("block_polymer", 3.0F, 10.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_bakelite = beacon("block_bakelite", 3.0F, 10.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_rubber = beacon("block_rubber", 3.0F, 10.0F, SoundType.METAL);
    public static final RegistryObject<BlockBeaconable> block_zirconium = beacon("block_zirconium", 5.0F, 10.0F, SoundType.METAL);

    private static RegistryObject<BlockBeaconable> beacon(String id, float hardness, float resistance, SoundType sound) {
        RegistryObject<BlockBeaconable> block = BLOCKS.registerBlock(id, BlockBeaconable::new,
                p -> p.strength(hardness, resistance).sound(sound).requiresCorrectToolForDrops());
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, Compat::blockDescription);
        ModTabs.addTo(HbmTab.BLOCKS, item);
        return block;
    }

    public static void init() {}
}
