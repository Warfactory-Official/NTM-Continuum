package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The thin, height-selectable steel grate from the 1.12.2 {@code BlockGrate}. */
public class BlockGrate extends CompatBlock {

    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 0, 9);

    public BlockGrate(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HEIGHT, 0));
    }

    public static double yOffset(int height) {
        return height == 9 ? -0.125D : height * 0.125D;
    }

    private VoxelShape shape(BlockState state) {
        int height = state.getValue(HEIGHT);
        int fromY = height == 9 ? -2 : height * 2;
        return Block.box(0, fromY, 0, 16, fromY + 2, 16);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return shape(state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) return defaultBlockState().setValue(HEIGHT, 7);
        if (face == Direction.UP) return defaultBlockState().setValue(HEIGHT, 0);

        Vec3 click = context.getClickLocation();
        double localY = click.y - context.getClickedPos().getY();
        return defaultBlockState().setValue(HEIGHT, Mth.floor(localY * 8.0D));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer == null || !placer.isShiftKeyDown()) return;

        int height = state.getValue(HEIGHT);
        if (height == 0 && hasClearanceBelow(level, pos)) {
            level.setBlock(pos, state.setValue(HEIGHT, 9), 3);
        } else if (height == 7 && hasClearanceAbove(level, pos)) {
            level.setBlock(pos, state.setValue(HEIGHT, 8), 3);
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        super.onBlockRemoved(state, level, pos, movedByPiston);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level.isClientSide()) return;

        int height = state.getValue(HEIGHT);
        if ((height == 8 && !hasClearanceAbove(level, pos))
                || (height == 9 && !hasClearanceBelow(level, pos))) {
            level.destroyBlock(pos, true);
        }
    }

    private static boolean hasClearanceBelow(LevelReader level, BlockPos pos) {
        BlockPos supportPos = pos.below();
        BlockState support = level.getBlockState(supportPos);
        if (support.isAir()) return false;
        VoxelShape collision = support.getCollisionShape(level, supportPos, CollisionContext.empty());
        return collision.isEmpty() || collision.max(Direction.Axis.Y) < 0.95D;
    }

    private static boolean hasClearanceAbove(LevelReader level, BlockPos pos) {
        BlockPos supportPos = pos.above();
        BlockState support = level.getBlockState(supportPos);
        if (support.isAir()) return false;
        VoxelShape collision = support.getCollisionShape(level, supportPos, CollisionContext.empty());
        return collision.isEmpty() || collision.min(Direction.Axis.Y) > 0.05D;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEIGHT);
    }
}
