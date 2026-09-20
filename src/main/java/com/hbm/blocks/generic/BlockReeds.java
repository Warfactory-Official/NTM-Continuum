package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code BlockReeds} ({@code plant_reeds}): a reed that stands on water and drops a stick. The old
 * {@code RenderReeds} drew crossed squares down through the whole water column below the block; a JSON
 * model can reach one block down at most, so {@code depth} records how much of the column shows —
 * 1 (top only), 2 (top + bottom cap one below) or 3 (top + mid section one below, column continues).
 */
public class BlockReeds extends BlockTerrainPlant {

    public static final IntegerProperty DEPTH = IntegerProperty.create("depth", 1, 3);

    public BlockReeds(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(DEPTH, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEPTH);
    }

    private static boolean isWater(BlockState state) {
        return state.is(Blocks.WATER);
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return isWater(level.getBlockState(pos.below()));
    }

    /** 1.7 {@code checkAndDropBlock} only sets air. */
    @Override
    protected boolean dropsWhenUnsupported(BlockState state) {
        return false;
    }

    /** The water column depth below, as the old renderer measured it, clamped to what the model can show. */
    public static int depth(LevelReader level, BlockPos pos) {
        int depth = 0;
        for (int y = pos.getY() - 1; y > level.getMinY(); y--) {
            if (!isWater(level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())))) break;
            depth = pos.getY() - y;
            if (depth >= 3) break;
        }
        return Math.max(1, depth);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!isWater(level.getBlockState(pos.below()))) return null;
        return defaultBlockState().setValue(DEPTH, depth(level, pos));
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, dev.golem.core.world.BlockTickScheduler ticks,
                                       BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        BlockState result = super.onUpdateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState);
        if (result != state || direction != Direction.DOWN) return result;
        return state.setValue(DEPTH, depth(level, pos));
    }

    /** 1.7 {@code getItemDropped -> Items.stick}. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.STICK));
    }
}
