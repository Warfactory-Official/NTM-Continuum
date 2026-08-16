package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Snow-style 1–8 layer block — port of the 1.12.2 {@code BlockLayering} (oil_spill): 2px per layer,
 * survives only on a solid full face (or a full 8-layer stack of itself), pops to air when support is
 * lost, drops nothing (old {@code getItemDropped} = AIR; empty loot table). Not replaceable and no
 * stack-on-place behavior — worldgen is the only placer (old Material.GROUND was non-replaceable too).
 */
public class BlockLayering extends dev.golem.core.world.CompatBlock {

    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        for (int i = 0; i < 8; i++) {
            SHAPES[i] = Block.box(0, 0, 0, 16, 2 * (i + 1), 16);
        }
    }

    public BlockLayering(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.ICE) || below.is(Blocks.PACKED_ICE)) return false;
        if (below.getBlock() == this) return below.getValue(LAYERS) == 8;
        return below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighborPos, BlockState neighborState) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
}
