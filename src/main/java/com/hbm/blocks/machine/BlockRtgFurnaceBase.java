package com.hbm.blocks.machine;

import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The shared shell of the two deprecated 1.7 RTG furnaces: a horizontally facing furnace block whose old
 * {@code _off}/{@code _on} block swap ({@code updateBlockState}) is the {@link #LIT} property, so the
 * block entity survives the transition and the inventory never has to be rescued.
 */
public abstract class BlockRtgFurnaceBase extends CompatBlock implements TickingBlock, WorldlyContainerHolder {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected BlockRtgFurnaceBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WorldlyContainer container ? container : null;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container container
                ? AbstractContainerMenu.getRedstoneSignalFromContainer(container) : 0;
    }

    /** The old onBlockAdded facing correction (the RTG furnace lacked it; the blast furnace has it). */
    protected void correctDefaultDirection(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        Direction facing = state.getValue(FACING);
        boolean north = Compat.isSolidRender(level.getBlockState(pos.north()), level, pos.north());
        boolean south = Compat.isSolidRender(level.getBlockState(pos.south()), level, pos.south());
        boolean west = Compat.isSolidRender(level.getBlockState(pos.west()), level, pos.west());
        boolean east = Compat.isSolidRender(level.getBlockState(pos.east()), level, pos.east());

        if (north && !south) facing = Direction.SOUTH;
        if (south && !north) facing = Direction.NORTH;
        if (west && !east) facing = Direction.EAST;
        if (east && !west) facing = Direction.WEST;

        if (facing != state.getValue(FACING)) level.setBlock(pos, state.setValue(FACING, facing), 2);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** 1.7 {@code updateBlockState}: only the property changes, so the block entity stays put. */
    public static void updateBlockState(boolean lit, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(LIT)) return;
        BlockState updated = state.setValue(LIT, lit);
        if (updated != state) level.setBlock(pos, updated, 2);
    }
}
