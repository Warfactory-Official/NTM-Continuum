package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * 1.7 {@code BlockBiomeStone} ({@code stone_biome}): the layered biome stone. {@code EnumBiomeType} is
 * flattened to {@code stone_biome_<type>}. The old {@code getIcon} showed the plain side texture when the
 * block above was the same stone (a continuous face) and the "layer" texture with its grass rim
 * otherwise; {@code covered} carries that choice into the blockstate.
 */
public class BlockBiomeStone extends CompatBlock {

    public enum Type {
        DESERT, WOODLAND;

        public String id() { return "stone_biome_" + name().toLowerCase(Locale.ROOT); }
        public String suffix() { return "." + name().toLowerCase(Locale.ROOT); }
    }

    public static final BooleanProperty COVERED = BooleanProperty.create("covered");

    public final Type type;

    public BlockBiomeStone(Properties properties, Type type) {
        super(properties);
        this.type = type;
        registerDefaultState(getStateDefinition().any().setValue(COVERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COVERED);
    }

    private boolean coveredAt(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(this);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(COVERED, coveredAt(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        if (direction != Direction.UP) return state;
        return state.setValue(COVERED, neighbourState.is(this));
    }
}
