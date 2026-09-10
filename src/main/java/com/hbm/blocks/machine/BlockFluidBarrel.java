package com.hbm.blocks.machine;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityBarrel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A one-block fluid drum, ported from 1.12.2's {@code BlockFluidBarrel}.
 *
 * <p>Six drums differ only in capacity, so one block entity type serves all of them and the capacity
 * rides on the block. The four horizontal connection flags exist purely so the renderer can bridge a row
 * of drums with a connector piece; nothing reads them for behaviour.
 */
public class BlockFluidBarrel extends dev.golem.core.world.CompatBlock implements EntityBlock, ILookOverlay {

    public static final BooleanProperty CONN_POS_X = BooleanProperty.create("conn_pos_x");
    public static final BooleanProperty CONN_NEG_X = BooleanProperty.create("conn_neg_x");
    public static final BooleanProperty CONN_POS_Z = BooleanProperty.create("conn_pos_z");
    public static final BooleanProperty CONN_NEG_Z = BooleanProperty.create("conn_neg_z");

    private final int capacity;

    public BlockFluidBarrel(int capacity, Properties properties) {
        super(properties);
        this.capacity = capacity;
        registerDefaultState(stateDefinition.any()
                .setValue(CONN_POS_X, false).setValue(CONN_NEG_X, false)
                .setValue(CONN_POS_Z, false).setValue(CONN_NEG_Z, false));
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBarrel(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONN_POS_X, CONN_NEG_X, CONN_POS_Z, CONN_NEG_Z);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks,
                                       BlockPos pos, Direction direction, BlockPos neighbourPos,
                                       BlockState neighbourState) {
        return direction.getAxis().isHorizontal() ? connections(state, level, pos) : state;
    }

    private BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(CONN_POS_X, connects(level, pos.east()))
                .setValue(CONN_NEG_X, connects(level, pos.west()))
                .setValue(CONN_POS_Z, connects(level, pos.south()))
                .setValue(CONN_NEG_Z, connects(level, pos.north()));
    }

    private boolean connects(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == this;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityBarrel barrel) {
            player.openMenu(barrel);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityBarrel barrel)) return 0;
        FluidTankNTM tank = barrel.tank;
        if (tank.getMaxFill() <= 0 || tank.getFill() <= 0) return 0;
        return Math.max(1, (int) ((long) tank.getFill() * 15 / tank.getMaxFill()));
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityBarrel barrel)) return null;
        FluidTankNTM tank = barrel.tank;
        List<String> text = new ArrayList<>();
        text.add("&[" + tank.getTankType().getColor() + "&]" + tank.getTankType().getDisplayNameString());
        text.add(tank.getFill() + " / " + tank.getMaxFill() + " mB");
        return new ILookOverlay.Data(getName().getString(), 0xFFFF00, 0x404000, text);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.BARREL.get(), TileEntityBarrel::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
