package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityFluidPump;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.BobMathUtil;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The 1.7 {@code FluidPump} (flow control pump): a one-block directional throttle drawn as the
 * {@code fluid_diode.obj}. An identifier sets its fluid; a plain click opens its settings screen
 * (throughput, pressure, priority); sneaking does nothing, as in 1.7.
 */
public class FluidPump extends CompatBlock implements TickingBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Installed by the client registrations; opens the pump's settings screen. */
    public static Consumer<TileEntityFluidPump> OPEN_SCREEN = pump -> {};

    public FluidPump(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // drawn by the BER (fluid_diode.obj)
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // 1.7: yaw quadrant 0/1/2/3 -> meta 2/5/3/4 = N/E/S/W, i.e. the block faces away from the placer.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidPump(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.getItem() instanceof IItemFluidId ident && level.getBlockEntity(pos) instanceof TileEntityFluidPump pump) {
            if (!level.isClientSide()) {
                FluidType type = ident.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                pump.tank[0].setTankType(type);
                pump.setChanged();
                level.updateNeighborsAt(pos, this);
                if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(type.getDisplayName()).append(Component.literal("!")));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityFluidPump pump) OPEN_SCREEN.accept(pump);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFluidPump pump)) return null;
        List<String> text = new ArrayList<>();
        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + pump.tank[0].getTankType().getDisplayNameString()
                + " (" + pump.tank[0].getPressure() + " PU): " + BobMathUtil.format(pump.bufferSize) + "mB/t" + ChatFormatting.RED + " ->");
        text.add("Priority: " + ChatFormatting.YELLOW + pump.priority.name());
        if (pump.tank[0].getFill() > 0) text.add(BobMathUtil.format(pump.tank[0].getFill()) + "mB buffered");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
