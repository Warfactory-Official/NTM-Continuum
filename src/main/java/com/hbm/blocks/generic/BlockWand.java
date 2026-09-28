package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * 1.7 {@code BlockWand} ({@code wand_air}): a structure-editing marker that a structure save writes as the block it
 * stands for ({@link #exportAs}, air), so a saved structure explicitly clears that cell when it is built. Glass-like,
 * a 1..15 px box without collision, drawn inside and out (1.7 {@code RenderBlockWand} rendered the box twice, once
 * with {@code renderFromInside}); faces against another wand block are skipped, as its {@code shouldSideBeRendered}.
 */
public class BlockWand extends CompatBlock {

    private static final VoxelShape SHAPE = Block.box(1, 1, 1, 15, 15, 15);

    public final Supplier<Block> exportAs;

    public BlockWand(Properties properties, Supplier<Block> exportAs) {
        super(properties);
        this.exportAs = exportAs;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this);
    }
}
