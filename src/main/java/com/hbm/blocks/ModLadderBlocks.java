package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockNTMLadder;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

/** The twelve HBM ladder blocks, backed by vanilla {@link net.minecraft.world.level.block.LadderBlock}. */
public final class ModLadderBlocks {

    private ModLadderBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockNTMLadder> ladder_sturdy = ladder("ladder_sturdy", false);
    public static final RegistryObject<BlockNTMLadder> ladder_iron = ladder("ladder_iron", false);
    public static final RegistryObject<BlockNTMLadder> ladder_gold = ladder("ladder_gold", false);
    public static final RegistryObject<BlockNTMLadder> ladder_aluminium = ladder("ladder_aluminium", false);
    public static final RegistryObject<BlockNTMLadder> ladder_copper = ladder("ladder_copper", false);
    public static final RegistryObject<BlockNTMLadder> ladder_titanium = ladder("ladder_titanium", false);
    public static final RegistryObject<BlockNTMLadder> ladder_lead = ladder("ladder_lead", false);
    public static final RegistryObject<BlockNTMLadder> ladder_cobalt = ladder("ladder_cobalt", false);
    public static final RegistryObject<BlockNTMLadder> ladder_steel = ladder("ladder_steel", false);
    public static final RegistryObject<BlockNTMLadder> ladder_tungsten = ladder("ladder_tungsten", false);
    public static final RegistryObject<BlockNTMLadder> ladder_red = ladder("ladder_red", false);
    public static final RegistryObject<BlockNTMLadder> ladder_red_top = ladder("ladder_red_top", true);

    private static RegistryObject<BlockNTMLadder> ladder(String id, boolean top) {
        RegistryObject<BlockNTMLadder> block = BLOCKS.registerBlock(id,
                p -> new BlockNTMLadder(p, top),
                p -> p.strength(0.25F, 2.0F).sound(SoundType.LADDER).noOcclusion().noCollision());
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, Compat::blockDescription);
        ModTabs.addTo(HbmTab.BLOCKS, item);
        return block;
    }

    public static void init() {}
}
