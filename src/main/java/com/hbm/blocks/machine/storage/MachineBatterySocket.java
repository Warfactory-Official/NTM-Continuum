package com.hbm.blocks.machine.storage;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Battery Socket: a 2x2 folded multiblock (dims {1,0,1,0,1,0}), drawn by the
 * {@code BatterySocketRenderer}. Its core is {@link TileEntityBatterySocket}, which exposes the inserted
 * battery item's charge to the power grid.
 */
public class MachineBatterySocket extends BlockMultiblockMachine implements IEnergyConnectorBlock, ILookOverlay {

    public MachineBatterySocket(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBatterySocket(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    // --- comparator output from the carried battery's fill (works from any cell via findCore) ---

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityBatterySocket socket ? socket.getComparatorPower() : 0;
    }

    /** Look-overlay: the carried battery item's charge / max (or "Empty" when no battery is inserted). */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityBatterySocket socket)) return null;

        // Read the synced carried stack, not getPower()/getMaxPower(). Those read the slot-0 inventory
        // item, which is only synced to the client while the container is open (so the HUD would show a
        // frozen charge with no GUI open). syncStack is broadcast every tick via the BufPacket.
        long power = TileEntityBatterySocket.powerFromStack(socket.syncStack);
        long max = TileEntityBatterySocket.maxPowerFromStack(socket.syncStack);
        List<String> text = new ArrayList<>();
        if (max <= 0) {
            text.add("§7Empty");
        } else {
            text.add(Library.getShortNumber(power) + "/" + Library.getShortNumber(max) + " HE");
            if (socket.delta == 0) text.add("§e-- §r0HE/s");
            else if (socket.delta > 0) text.add("§a-> §r" + Library.getShortNumber(socket.delta) + "HE/s");
            else text.add("§c<- §r" + Library.getShortNumber(-socket.delta) + "HE/s");
            double frac = (double) power / (double) max;
            text.add("&[" + Library.getColorProgress(frac) + "&]    " + Library.getPercentage(frac) + "%");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    /** The four cells of the 2x2 each turn the two faces of its own corner outward. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(back), MASK_NORTH | MASK_EAST);
        visitor.cell(core.relative(rot), MASK_SOUTH | MASK_WEST);
        visitor.cell(core.relative(back).relative(rot), MASK_NORTH | MASK_WEST);
    }

    @Override public int coreMask() { return MASK_SOUTH | MASK_EAST; }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(back), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(back).relative(rot), MASK_ALL, domains);
    }
}
