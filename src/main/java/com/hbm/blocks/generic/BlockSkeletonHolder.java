package com.hbm.blocks.generic;

import com.hbm.tileentity.ModDungeonBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockSkeletonHolder}, "Oh, that's a dead guy": a skeleton slumped in a holder, drawn entirely by
 * {@code RenderSkeletonHolder}, holding one stack that sits across his lap. The same take/put rules as the
 * pedestal. 1.7 turned it on placement by the placer's yaw into metadata 5/3/4/2 for south/west/north/east -
 * a quarter turn counter-clockwise of the way the placer looks - and that is {@link #FACING} here.
 */
public class BlockSkeletonHolder extends CompatBlock implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlockSkeletonHolder(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getCounterClockWise());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySkeletonHolder(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return BlockPedestal.takeOrPlace(level, pos, player, hand);
    }

    /** 1.7 {@code TileEntitySkeletonHolder}: the held stack, never ticking. */
    public static class TileEntitySkeletonHolder extends TileEntityLoadedBase implements BlockPedestal.ItemHolder {

        private ItemStack item = ItemStack.EMPTY;

        public TileEntitySkeletonHolder(BlockPos pos, BlockState state) {
            super(ModDungeonBlockEntities.SKELETON_HOLDER.get(), pos, state);
        }

        @Override
        public ItemStack getItem() {
            return item;
        }

        @Override
        public void setItem(ItemStack stack) {
            this.item = stack == null ? ItemStack.EMPTY : stack;
            setChanged();
            if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            BlockPedestal.dropHeld(level, pos, item);
        }

        @Override
        protected void loadData(DataIn input) {
            super.loadData(input);
            this.item = input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        }

        @Override
        protected void saveData(DataOut output) {
            super.saveData(output);
            if (!item.isEmpty()) output.store("item", ItemStack.CODEC, item);
        }
    }

    /** Structure rotation and mirroring turn the placed facing, as vanilla's directional blocks do. */
    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
