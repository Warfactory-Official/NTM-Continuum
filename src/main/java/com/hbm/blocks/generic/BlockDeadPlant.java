package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * 1.7 {@code BlockDeadPlant}: a withered cross plant that only stands on grass, dirt, dead grass, oily or
 * dead dirt and drops nothing ({@code getItemDropped} returned null). The five {@code EnumDeadPlantType}
 * metas are flattened to {@code plant_dead_<type>}. Waterloggable (port addition, see {@link Waterlogging}).
 */
public class BlockDeadPlant extends BlockTerrainPlant implements SimpleWaterloggedBlock {

    public enum Type {
        GENERIC, GRASS, FLOWER, BIGFLOWER, FERN;

        public String id() { return "plant_dead_" + name().toLowerCase(Locale.ROOT); }
        public String texture() { return "hbm:blocks/plant_dead." + name().toLowerCase(Locale.ROOT); }
    }

    public final Type type;

    public BlockDeadPlant(Properties properties, Type type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(Waterlogging.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Waterlogging.WATERLOGGED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return Waterlogging.place(super.getStateForPlacement(context), context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Waterlogging.fluid(state);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        Waterlogging.tick(state, level, ticks, pos);
        return super.onUpdateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState);
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        return isDeadPlantGround(level.getBlockState(pos.below()));
    }

    /** 1.7 {@code getItemDropped -> null}: the creative item exists, but breaking one yields nothing. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }
}
