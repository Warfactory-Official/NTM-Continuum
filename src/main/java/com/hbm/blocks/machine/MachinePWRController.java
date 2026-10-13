package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.util.OverheadMarkers;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.items.machine.ItemPWRPrinter;
import com.hbm.tileentity.machine.TileEntityBlockPWR;
import com.hbm.tileentity.machine.TileEntityPWRController;

/**
 * The PWR controller (1.7 {@code MachinePWRController}): a casing block with a front face. Right-clicking
 * an unassembled controller runs the structure scan; right-clicking an assembled one opens the reactor
 * menu, unless the held item is the printer (which then gets the click).
 *
 * <p>The scan is a six-way flood fill from the cell behind the controller, as in 1.7: casing blocks
 * (vessel, reflector, port) bound the fill, core blocks (fuel, control, channel, heat exchanger, heatsink,
 * neutron source) continue it, anything else is an error, reported as a red labelled in-world marker
 * ({@code RenderOverhead}) as in 1.7.
 */
public class MachinePWRController extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int MAX_SIZE = 4096;

    public MachinePWRController(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPWRController(pos, state);
    }

    /** 1.7 {@code onBlockPlacedBy}: the front faces the player, the reactor sits behind the block. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return activate(state, level, pos, player);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return activate(state, level, pos, player);
    }

    private InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof TileEntityPWRController controller)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!controller.assembled) {
            assemble(level, pos, state, player);
        } else {
            // 1.7: the printer falls through to its own onItemUse on an assembled reactor. PASS here reaches
            // the item's useOn; every other path consumes the click (IMPLEMENTATION.md §22).
            if (player.getMainHandItem().getItem() instanceof ItemPWRPrinter) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (player instanceof ServerPlayer serverPlayer) {
                Menus.openWithPos(serverPlayer, controller, pos);
            }
        }
        return InteractionResult.CONSUME;
    }

    // --- the structure scan ---

    private final Map<BlockPos, Block> assembly = new HashMap<>();
    private final Map<BlockPos, Block> fuelRods = new HashMap<>();
    private final Map<BlockPos, Block> sources = new HashMap<>();
    private boolean errored;

    public void assemble(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        assembly.clear();
        fuelRods.clear();
        sources.clear();
        assembly.put(pos, this);

        ForgeDirection dir = ForgeDirection.getOrientation(state.getValue(FACING)).getOpposite();

        errored = false;
        floodFill(level, pos.offset(dir.offsetX, 0, dir.offsetZ), player);

        if (fuelRods.isEmpty()) {
            sendError(pos, "Fuel rods required", player);
            errored = true;
        }
        if (sources.isEmpty()) {
            sendError(pos, "Neutron sources required", player);
            errored = true;
        }

        TileEntityPWRController controller = (TileEntityPWRController) level.getBlockEntity(pos);

        if (!errored) {
            Block shell = ModPwrBlocks.PWR_BLOCK.get();
            for (Map.Entry<BlockPos, Block> entry : assembly.entrySet()) {
                BlockPos cell = entry.getKey();
                Block block = entry.getValue();
                if (block == this) continue;

                boolean port = block == ModPwrBlocks.PWR_PORT.get();
                level.setBlock(cell, shell.defaultBlockState().setValue(BlockPWR.PORT, port), 3);

                if (level.getBlockEntity(cell) instanceof TileEntityBlockPWR pwr) {
                    pwr.block = block;
                    pwr.coreX = pos.getX();
                    pwr.coreY = pos.getY();
                    pwr.coreZ = pos.getZ();
                    pwr.setChanged();
                }
            }
            controller.setup(assembly, fuelRods);
        }
        controller.assembled = !errored;
        controller.setChanged();

        assembly.clear();
        fuelRods.clear();
        sources.clear();
    }

    private void floodFill(Level level, BlockPos pos, @Nullable Player player) {
        if (assembly.containsKey(pos)) return;
        if (assembly.size() >= MAX_SIZE) {
            errored = true;
            sendError(pos, "Max size exceeded", player);
            return;
        }

        Block block = level.getBlockState(pos).getBlock();

        if (isValidCasing(block)) {
            assembly.put(pos, block);
            return;
        }

        if (isValidCore(block)) {
            assembly.put(pos, block);
            if (block == ModPwrBlocks.PWR_FUEL.get()) fuelRods.put(pos, block);
            if (block == ModPwrBlocks.PWR_NEUTRON_SOURCE.get()) sources.put(pos, block);
            floodFill(level, pos.offset(1, 0, 0), player);
            floodFill(level, pos.offset(-1, 0, 0), player);
            floodFill(level, pos.offset(0, 1, 0), player);
            floodFill(level, pos.offset(0, -1, 0), player);
            floodFill(level, pos.offset(0, 0, 1), player);
            floodFill(level, pos.offset(0, 0, -1), player);
            return;
        }

        sendError(pos, "Non-reactor block", player);
        errored = true;
    }

    /** 1.7 sent a red, labelled marker particle; the label goes to chat here with the position. */
    public static void sendError(BlockPos pos, String message, @Nullable Player player) {
        // 1.7: a red RenderOverhead marker on the offending block, labelled, for five seconds within 128 blocks (G-C)
        if (player instanceof ServerPlayer serverPlayer) {
            OverheadMarkers.send(serverPlayer, pos, 0xff0000, message, 5_000, 128D);
        }
    }

    public static boolean isValidCore(Block block) {
        return block == ModPwrBlocks.PWR_FUEL.get()
                || block == ModPwrBlocks.PWR_CONTROL.get()
                || block == ModPwrBlocks.PWR_CHANNEL.get()
                || block == ModPwrBlocks.PWR_HEATEX.get()
                || block == ModPwrBlocks.PWR_HEATSINK.get()
                || block == ModPwrBlocks.PWR_NEUTRON_SOURCE.get();
    }

    public static boolean isValidCasing(Block block) {
        return block == ModPwrBlocks.PWR_CASING.get()
                || block == ModPwrBlocks.PWR_REFLECTOR.get()
                || block == ModPwrBlocks.PWR_PORT.get();
    }
}
