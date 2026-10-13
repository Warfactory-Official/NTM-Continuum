package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

import java.util.List;
import java.util.function.Supplier;

/**
 * Cave spike, the 1.12.2 {@code BlockStalagmite}, scattered by the sulfur/asbestos cave feature.
 * A cross-model, non-solid, walk-through decoration that needs a solid face to hang off: stalagmites
 * attach to the block below, stalactites to the block above, and either pops when its anchor goes.
 *
 * <p>The 1.12.2 block was two registrations ({@code stalagmite}/{@code stalactite}) × two
 * {@code EnumStalagmiteType} metas; here they are four ({@code stalagmite_sulfur},
 * {@code stalagmite_asbestos}, {@code stalactite_sulfur}, {@code stalactite_asbestos}).
 *
 * <p>Drops one {@code sulfur} or {@code powder_asbestos}; Silk Touch returns the spike itself.
 */
public class BlockStalagmite extends CompatBlock {

    /** Face the spike hangs from: {@code DOWN} for a stalagmite (stands on the floor), {@code UP} for a stalactite. */
    private final Direction anchor;
    private final Supplier<ItemStack> drop;

    public BlockStalagmite(Properties properties, Direction anchor, Supplier<ItemStack> drop) {
        super(properties);
        this.anchor = anchor;
        this.drop = drop;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();   // old getCollisionBoundingBox -> NULL_AABB (passable)
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.relative(anchor);
        return level.getBlockState(support).isFaceSturdy(level, support, anchor.getOpposite());
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // 1.7 BlockStalagmite.renderAsNormalBlock() is false, so it can never be silk-harvested.
        ItemStack stack = drop.get();
        return stack.isEmpty() ? List.of() : List.of(stack);
    }
}
