package com.hbm.blocks.generic;

import com.hbm.blocks.ModDecoBlocks;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** The old 16 metadata concrete colours, retained as one block plus a state property. */
public final class BlockConcreteColored extends CompatBlock {

    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public BlockConcreteColored(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLOR, DyeColor.WHITE));
    }

    public static DyeColor color(int variant) {
        DyeColor[] values = DyeColor.values();
        return values[Math.floorMod(variant, values.length)];
    }

    /** Legacy EnumDyeColor name used by block-item and stair registry ids (LIGHT_GRAY was "silver"). */
    public static String legacyName(int variant) {
        return Math.floorMod(variant, DyeColor.values().length) == 8
                ? "silver" : color(variant).getSerializedName();
    }

    public static int variant(BlockState state) {
        return state.getValue(COLOR).getId();
    }

    public BlockState stateForVariant(int variant) {
        return defaultBlockState().setValue(COLOR, color(variant));
    }

    public BlockState stateForItem(ItemStack stack) {
        return stateForVariant(stack.getItem() instanceof MaterialCompletionBlockItem item ? item.variant() : 0);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return stateForItem(context.getItemInHand());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(ModDecoBlocks.concreteColoredItem(state.getValue(COLOR).getId()));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return ModDecoBlocks.concreteColoredItem(variant(state));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }
}
