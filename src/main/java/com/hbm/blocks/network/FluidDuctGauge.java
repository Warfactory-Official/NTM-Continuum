package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.network.TileEntityPipeGauge;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * An inline fluid gauge: a full-cube duct segment that shows what the pipe network is moving.
 * Ported from 1.12.2's {@code FluidDuctGauge}.
 *
 * <p>Typed by right-clicking with a fluid identifier, exactly as an ordinary duct is, and picked up by
 * the duct block's shift-click type-spreading walk because its block entity is a duct.
 *
 * <p><b>Deviation from the fork.</b> There the block draws through a hand-built baked model that tints a
 * paintable overlay with the carried fluid's colour. This port draws a plain cube instead: the tint
 * mechanism available here colours from the block state, and a duct's fluid lives in its block entity,
 * so that colour is not reachable without a renderer of its own. The fluid is still named in the look
 * overlay, in its own colour, so nothing is lost but the paint.
 */
public class FluidDuctGauge extends dev.golem.core.world.CompatBlock implements EntityBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public FluidDuctGauge(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPipeGauge(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof IItemFluidId ident)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityPipeGauge gauge) {
            FluidType type = ident.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            if (type != gauge.getFluidType()) {
                if (player.isShiftKeyDown()) {
                    BlockFluidDuct.spreadType(level, pos, type, gauge.getFluidType(), 256);
                } else {
                    gauge.setFluidType(type);
                    level.updateNeighborsAt(pos, this);
                }
                player.sendSystemMessage(Component.literal("Duct fluid: ").append(type.getDisplayName())
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityPipeGauge gauge)) return null;
        FluidType type = gauge.getFluidType();
        List<String> text = new ArrayList<>();
        text.add("&[" + type.getColor() + "&]" + type.getDisplayNameString());
        text.add(String.format(Locale.US, "%,d", gauge.lastSecond) + " mB/s");
        return new ILookOverlay.Data("Fluid Gauge", 0xFFFF00, 0x404000, text);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.PIPE_GAUGE.get(), TileEntityPipeGauge::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
