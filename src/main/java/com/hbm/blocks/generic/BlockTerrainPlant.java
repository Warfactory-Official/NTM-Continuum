package com.hbm.blocks.generic;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModFallingBlocks;
import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared shell for the 1.7 plant blocks ({@code BlockDeadPlant}, {@code BlockNTMFlower},
 * {@code BlockTallPlant}, {@code BlockReeds}): a full-cube selection box, no collision, and the old
 * {@code canBlockStay / onNeighborBlockChange -> checkAndDropBlock} pair. A plant whose ground
 * disappears drops its loot ({@link #dropsWhenUnsupported}) and becomes air.
 */
public abstract class BlockTerrainPlant extends CompatBlock {

    protected BlockTerrainPlant(Properties properties) {
        super(properties);
    }

    /** The 1.7 {@code canPlaceBlockOn} of the flowers: grass, dirt, farmland, dead dirt, oily dirt. */
    public static boolean isFlowerGround(BlockState below) {
        Block b = below.getBlock();
        return b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.FARMLAND
                || b == ModFallingBlocks.dirt_dead.get() || b == ModFallingBlocks.dirt_oily.get();
    }

    /** The 1.7 {@code canPlaceBlockOn} of the dead plants: grass, dirt, dead grass, oily dirt, dead dirt. */
    public static boolean isDeadPlantGround(BlockState below) {
        Block b = below.getBlock();
        return b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == ModFalloutBlocks.waste_earth.get()
                || b == ModFallingBlocks.dirt_oily.get() || b == ModFallingBlocks.dirt_dead.get();
    }

    public static boolean isDeadDirt(BlockState below) {
        Block b = below.getBlock();
        return b == ModFallingBlocks.dirt_dead.get() || b == ModFallingBlocks.dirt_oily.get();
    }

    /** Whether the block at {@code pos} can stay with the given state. */
    protected abstract boolean canStay(BlockState state, LevelReader level, BlockPos pos);

    /** 1.7 {@code checkAndDropBlock} calls {@code dropBlockAsItem} for most plants; reeds and vines do not. */
    protected boolean dropsWhenUnsupported(BlockState state) {
        return true;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canStay(state, level, pos);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        if (!canStay(state, level, pos)) {
            if (dropsWhenUnsupported(state) && level instanceof net.minecraft.server.level.ServerLevel server) {
                dropResources(state, server, pos);
            }
            // air, or the water a waterlogged plant was holding
            return state.getFluidState().createLegacyBlock();
        }
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // old getCollisionBoundingBoxFromPool -> null
    }
}
