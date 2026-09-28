package com.hbm.blocks.machine;

import com.hbm.blocks.ModIcfBlocks;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityBlockICF;
import com.hbm.tileentity.machine.TileEntityICFController;
import com.hbm.util.BobMathUtil;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * The ICF laser controller (1.7 {@code MachineICFController}): an ICF casing block with a front face. The laser
 * leaves through the front; the array sits behind it. Right-clicking an unassembled controller runs the array
 * scan; there is no menu.
 *
 * <p>The scan is 1.7's six-way flood fill from the cell behind the controller: laser casings and ports bound the
 * fill, cells, flash tubes, capacitors and turbochargers continue it, anything else is an error. 1.7 reported
 * errors as red, labelled in-world markers ({@code RenderOverhead}), as here.
 */
public class MachineICFController extends CompatBlock implements TickingBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int MAX_SIZE = 1024;

    public MachineICFController(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityICFController(pos, state);
    }

    /** 1.7 {@code onBlockPlacedBy}: the front faces the player. */
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

    /** 1.7 {@code onBlockActivated}: true on the client, false (falls through) sneaking, else assemble if needed. */
    private InteractionResult activate(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.getBlockEntity(pos) instanceof TileEntityICFController controller && !controller.assembled) {
            assemble(level, pos, state, player);
        }
        return InteractionResult.CONSUME;
    }

    // --- the array scan ---

    private final Map<BlockPos, Block> assembly = new HashMap<>();
    private final HashSet<BlockPos> casings = new HashSet<>();
    private final HashSet<BlockPos> ports = new HashSet<>();
    private final HashSet<BlockPos> cells = new HashSet<>();
    private final HashSet<BlockPos> emitters = new HashSet<>();
    private final HashSet<BlockPos> capacitors = new HashSet<>();
    private final HashSet<BlockPos> turbochargers = new HashSet<>();
    private boolean errored;

    public void assemble(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        clearScan();
        assembly.put(pos, Blocks.AIR); // 1.7 assembly.put(pos, 0): the controller's own cell stops the fill

        ForgeDirection dir = ForgeDirection.getOrientation(state.getValue(FACING)).getOpposite();

        errored = false;
        floodFill(level, pos.offset(dir.offsetX, 0, dir.offsetZ), player);
        assembly.remove(pos);

        TileEntityICFController controller = (TileEntityICFController) level.getBlockEntity(pos);

        if (!errored && controller != null) {
            Block shell = ModIcfBlocks.icf_block.get();
            for (Map.Entry<BlockPos, Block> entry : assembly.entrySet()) {
                BlockPos cell = entry.getKey();
                level.setBlock(cell, shell.defaultBlockState().setValue(BlockICF.PORT, ports.contains(cell)), 3);

                if (level.getBlockEntity(cell) instanceof TileEntityBlockICF icf) {
                    icf.block = entry.getValue();
                    icf.coreX = pos.getX();
                    icf.coreY = pos.getY();
                    icf.coreZ = pos.getZ();
                    icf.setChanged();
                }
            }

            controller.setup(ports, cells, emitters, capacitors, turbochargers);
            controller.setChanged();
        }
        if (controller != null) controller.assembled = !errored;

        clearScan();
    }

    private void clearScan() {
        assembly.clear();
        casings.clear();
        ports.clear();
        cells.clear();
        emitters.clear();
        capacitors.clear();
        turbochargers.clear();
    }

    private void floodFill(Level level, BlockPos pos, @Nullable Player player) {
        if (assembly.containsKey(pos)) return;
        if (assembly.size() >= MAX_SIZE) {
            errored = true;
            sendError(pos, "Max size exceeded", player);
            return;
        }

        Block block = level.getBlockState(pos).getBlock();

        boolean validCasing = false;
        boolean validCore = false;

        if (block == ModIcfBlocks.icf_laser_component_casing.get()) { casings.add(pos); validCasing = true; }
        if (block == ModIcfBlocks.icf_laser_component_port.get()) { ports.add(pos); validCasing = true; }
        if (block == ModIcfBlocks.icf_laser_component_cell.get()) { cells.add(pos); validCore = true; }
        if (block == ModIcfBlocks.icf_laser_component_emitter.get()) { emitters.add(pos); validCore = true; }
        if (block == ModIcfBlocks.icf_laser_component_capacitor.get()) { capacitors.add(pos); validCore = true; }
        if (block == ModIcfBlocks.icf_laser_component_turbo.get()) { turbochargers.add(pos); validCore = true; }

        if (validCasing) {
            assembly.put(pos, block);
            return;
        }

        if (validCore) {
            assembly.put(pos, block);
            floodFill(level, pos.offset(1, 0, 0), player);
            floodFill(level, pos.offset(-1, 0, 0), player);
            floodFill(level, pos.offset(0, 1, 0), player);
            floodFill(level, pos.offset(0, -1, 0), player);
            floodFill(level, pos.offset(0, 0, 1), player);
            floodFill(level, pos.offset(0, 0, -1), player);
            return;
        }

        sendError(pos, "Non-laser block", player);
        errored = true;
    }

    /** 1.7 sent a red, labelled marker particle to the player who ran the scan. */
    private static void sendError(BlockPos pos, String message, @Nullable Player player) {
        // 1.7: a red RenderOverhead marker on the offending block, labelled, for five seconds within 128 blocks (G-C)
        if (player instanceof ServerPlayer serverPlayer) {
            com.hbm.util.OverheadMarkers.send(serverPlayer, pos, 0xff0000, message, 5_000, 128D);
        }
    }

    /** 1.7 {@code printHook}: the buffered and maximum laser power. */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityICFController icf)) return null;
        List<String> text = new ArrayList<>();
        text.add(BobMathUtil.getShortNumber(icf.getPower()) + "/" + BobMathUtil.getShortNumber(icf.getMaxPower()) + "HE");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
