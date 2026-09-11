package com.hbm.blocks.machine.pwr;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.pwr.TileEntityBlockPWR;
import com.hbm.tileentity.machine.pwr.TileEntityPWRController;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The reactor's face. Right-clicking an unassembled controller flood-fills outward from the block
 * behind it: core parts let the fill continue, casing parts stop it, and anything else fails the
 * assembly. A structure that holds together is then swapped over to {@link BlockPWR} in one pass.
 *
 * <p>Where upstream marks a rejected block with a floating marker particle, this port reports the
 * problem and its position as a chat message, the same substitution the Chicago Pile's assembly makes.
 */
public class MachinePWRController extends CompatBlock implements EntityBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final int MAX_SIZE = 4096;

    public MachinePWRController(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPWRController(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.PWR_CONTROLLER.get(), TileEntityPWRController::serverTick);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof TileEntityPWRController controller)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!controller.assembled) {
            assemble(level, pos, state, player);
        } else if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, controller, pos);
        }
        return InteractionResult.SUCCESS;
    }

    public void assemble(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        Assembly assembly = new Assembly(level, player);
        assembly.parts.put(pos, state);
        Direction dir = state.getValue(FACING).getOpposite();
        assembly.floodFill(pos.relative(dir));

        if (assembly.fuelRods.isEmpty()) {
            sendError(level, pos, "Fuel rods required", player);
            assembly.errored = true;
        }
        if (assembly.sources.isEmpty()) {
            sendError(level, pos, "Neutron sources required", player);
            assembly.errored = true;
        }

        if (!(level.getBlockEntity(pos) instanceof TileEntityPWRController controller)) return;

        if (!assembly.errored) {
            for (Map.Entry<BlockPos, BlockState> entry : assembly.parts.entrySet()) {
                BlockPos partPos = entry.getKey();
                BlockState original = entry.getValue();
                if (original.getBlock() == this) continue;

                boolean port = original.getBlock() == ModPWRBlocks.pwr_port.get();
                level.setBlock(partPos, ModPWRBlocks.pwr_block.get().defaultBlockState()
                        .setValue(BlockPWR.IO_ENABLED, port), Block.UPDATE_ALL);

                if (level.getBlockEntity(partPos) instanceof TileEntityBlockPWR cell) {
                    cell.originalBlockState = original;
                    cell.corePos = pos;
                    cell.setChanged();
                }
            }
            controller.setup(assembly.parts, assembly.fuelRods);
        }

        controller.assembled = !assembly.errored;
        controller.setChanged();
    }

    /** One assembly attempt: the flood fill's frontier plus the two part kinds the reactor must have. */
    private static final class Assembly {

        private final Level level;
        private final @Nullable Player player;
        private final Map<BlockPos, BlockState> parts = new HashMap<>();
        private final Map<BlockPos, BlockState> fuelRods = new HashMap<>();
        private final Map<BlockPos, BlockState> sources = new HashMap<>();
        private boolean errored;

        private Assembly(Level level, @Nullable Player player) {
            this.level = level;
            this.player = player;
        }

        private void floodFill(BlockPos pos) {
            if (parts.containsKey(pos) || errored) return;
            if (parts.size() >= MAX_SIZE) {
                sendError(level, pos, "Max size exceeded (" + MAX_SIZE + ")", player);
                errored = true;
                return;
            }

            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();

            if (isValidCasing(block)) {
                parts.put(pos, state);
                return;
            }

            if (isValidCore(block)) {
                parts.put(pos, state);
                if (block == ModPWRBlocks.pwr_fuelrod.get()) fuelRods.put(pos, state);
                if (block == ModPWRBlocks.pwr_neutron_source.get()) sources.put(pos, state);

                for (Direction side : Direction.values()) {
                    floodFill(pos.relative(side));
                }
                return;
            }

            sendError(level, pos, "Invalid block in structure: " + state.getBlock().getName().getString(), player);
            errored = true;
        }
    }

    public static void sendError(Level level, BlockPos pos, String message, @Nullable Player player) {
        if (player == null) return;
        player.sendSystemMessage(Component.literal(message + " at " + pos.toShortString())
                .withStyle(ChatFormatting.RED));
    }

    private static boolean isValidCore(Block block) {
        return block == ModPWRBlocks.pwr_fuelrod.get() || block == ModPWRBlocks.pwr_control.get()
                || block == ModPWRBlocks.pwr_channel.get() || block == ModPWRBlocks.pwr_heatex.get()
                || block == ModPWRBlocks.pwr_heatsink.get() || block == ModPWRBlocks.pwr_neutron_source.get();
    }

    private static boolean isValidCasing(Block block) {
        return block == ModPWRBlocks.pwr_casing.get() || block == ModPWRBlocks.pwr_reflector.get()
                || block == ModPWRBlocks.pwr_port.get();
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityPWRController controller)) return null;

        List<String> text = new ArrayList<>();
        if (!controller.assembled) {
            text.add("§cNot assembled");
        } else {
            text.add("Fuel rods: " + controller.rodCount);
            text.add("Connections: " + controller.connections + " (" + controller.connectionsControlled + " controlled)");
            text.add("Flux: " + (int) controller.flux);
            text.add("Core: " + controller.coreHeat + " / " + controller.coreHeatCapacity + " TU");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
