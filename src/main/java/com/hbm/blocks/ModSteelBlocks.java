package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockGrate;
import com.hbm.blocks.generic.BlockScaffold;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** Hand-registered stateful steel construction blocks from the old decorative block set. */
public final class ModSteelBlocks {

    private ModSteelBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final List<RegistryObject<? extends Block>> OBJ_BLOCKS = new ArrayList<>();

    private static final SoundType GRATE_SOUND = new SoundType(
            0.5F, 1.0F,
            SoundEvents.STONE_BREAK, ModSounds.METAL_BLOCK_EVENT, SoundEvents.STONE_PLACE,
            SoundEvents.STONE_HIT, SoundEvents.STONE_FALL);

    public static final RegistryObject<BlockGrate> steel_grate = BLOCKS.registerBlock("steel_grate", BlockGrate::new,
            p -> p.strength(2.0F, 5.0F).sound(GRATE_SOUND).noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    public static final RegistryObject<BlockScaffold> steel_scaffold = BLOCKS.registerBlock("steel_scaffold", BlockScaffold::new,
            p -> p.strength(5.0F, 15.0F).sound(SoundType.STONE).noOcclusion()
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    static {
        RegistryObject<BlockItem> grateItem = ITEMS.registerBlockItem("steel_grate", steel_grate, Compat::blockDescription);
        ModTabs.addTo(HbmTab.BLOCKS, grateItem);

        RegistryObject<BlockItem> scaffoldItem = ITEMS.registerBlockItem("steel_scaffold", steel_scaffold,
                Compat::blockDescription);
        ModTabs.addStacksTo(HbmTab.BLOCKS, () -> IntStream.range(0, BlockScaffold.Variant.values().length)
                .mapToObj(variant -> {
                    ItemStack stack = new ItemStack(scaffoldItem.get());
                    ModDataComponents.ITEM_VARIANT.set(stack, variant);
                    return stack;
                }).toList());

        OBJ_BLOCKS.add(steel_scaffold);
    }

    public static void init() {}
}
