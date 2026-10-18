package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 1.7 {@code BlockNetworkSeparator}: three blocks wide and two high, a power port on each lower end. */
public class BlockNetworkSeparator extends BlockMultiblockMachine implements IEnergyConnectorBlock, ILookOverlay {

    /** Installed by the client registrations; the screen is not a container. */
    public static Consumer<TileEntityNetworkSeparator> OPEN_SCREEN = separator -> {};

    public BlockNetworkSeparator(Properties properties) {
        super(properties);
    }

    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityNetworkSeparator(pos, state); }
    @Override public int[] getDimensions() { return new int[] {1, 0, 0, 0, 1, 1}; }
    @Override public int getOffset() { return 0; }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityNetworkSeparator sep
                && IEnergyConnectorBlock.isPort(PortDef.connections(sep.getInPort()[0], sep.getOutPort()[0]), pos, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction out = FlowControlPump.sideOf(facing);
        visitor.cell(core.relative(out), 1 << out.ordinal());
        visitor.cell(core.relative(out.getOpposite()), 1 << out.getOpposite().ordinal());
    }

    /** 1.7 stamps a power proxy on every block but the core. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_POWER_IN);
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(level.getBlockEntity(core) instanceof TileEntityNetworkSeparator sep)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) OPEN_SCREEN.accept(sep);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityNetworkSeparator sep)) return null;
        List<String> text = new ArrayList<>();
        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + Library.getShortNumber(sep.deltaTick[19]) + "HE/t" + ChatFormatting.RED + " ->");
        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + Library.getShortNumber(sep.deltaSecond) + "HE/s" + ChatFormatting.RED + " ->");
        text.add("Configured: " + Library.getShortNumber(sep.configuredLimit) + "HE/t");
        text.add("Priority: " + ChatFormatting.YELLOW + sep.priority.name());
        if (sep.redstone) text.add(ChatFormatting.RED + "Disabled by redstone");
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
