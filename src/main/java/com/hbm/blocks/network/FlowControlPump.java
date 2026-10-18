package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.network.TileEntityFlowControlPump;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.7 {@code FlowControlPump}: three blocks across the facing, the pump in the middle and a fluid port on each end.
 * An identifier sets the fluid; a plain click opens the settings screen.
 */
public class FlowControlPump extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    /** Installed by the client registrations; the screen is not a container. */
    public static Consumer<TileEntityFlowControlPump> OPEN_SCREEN = pump -> {};

    public FlowControlPump(Properties properties) {
        super(properties);
    }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityFlowControlPump(pos, state); }
    @Override public int[] getDimensions() { return new int[] {0, 0, 0, 0, 1, 1}; }
    @Override public int getOffset() { return 0; }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityFlowControlPump pump
                && IEnergyConnectorBlock.isPort(PortDef.connections(pump.getInPort(), pump.getOutPort()), pos, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction out = sideOf(facing);
        visitor.cell(core.relative(out), 1 << out.ordinal());
        visitor.cell(core.relative(out.getOpposite()), 1 << out.getOpposite().ordinal());
    }

    /** 1.7 stamps a fluid proxy on both end blocks. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_FLUID_IN);
    }

    /** The outlet end: 1.7 {@code dir.getRotation(DOWN)}. */
    static Direction sideOf(Direction facing) {
        return facing.getCounterClockWise();
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof IItemFluidId id)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFlowControlPump pump)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            FluidType type = id.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            pump.tank[0].setTankType(type);
            pump.setChanged();
            player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(type.getDisplayName()).append(Component.literal("!")));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(core) instanceof TileEntityFlowControlPump pump)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) OPEN_SCREEN.accept(pump);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFlowControlPump pump)) return null;
        List<String> text = new ArrayList<>();
        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + pump.tank[0].getTankType().getDisplayNameString()
                + " (" + pump.tank[0].getPressure() + " PU): " + BobMathUtil.format(pump.bufferSize) + "mB/t" + ChatFormatting.RED + " ->");
        text.add("Priority: " + ChatFormatting.YELLOW + pump.priority.name());
        if (pump.redstone) text.add(ChatFormatting.RED + "Disabled by redstone");
        if (pump.tank[0].getFill() > 0) text.add(BobMathUtil.format(pump.tank[0].getFill()) + "mB buffered");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {1, 0, 0, ForgeDirection.EAST.ordinal()},
                {-1, 0, 0, ForgeDirection.WEST.ordinal()},
        }, dir);
    }
}
