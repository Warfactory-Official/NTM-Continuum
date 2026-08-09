package com.hbm.blocks.machine.storage;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Battery Socket: a small 2×2 dummyable multiblock (dims {1,0,1,0,1,0} + 3 extra cells), drawn by the
 * {@code BatterySocketRenderer}. Its core is {@link TileEntityBatterySocket}, which exposes the inserted
 * battery item's charge to the power grid. Implements {@link IEnergyConnectorBlock} so cables connect to
 * any footprint face (the core's BlockEntity isn't present at the dummy cells).
 */
public class MachineBatterySocket extends BlockDummyable implements IEnergyConnectorBlock, ILookOverlay {

    public MachineBatterySocket(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityBatterySocket(pos, state);
    }

    /** Cables connect only at the socket's true power ports (its {@code getConPos()} positions), not every footprint face. */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityBatterySocket te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        this.makeExtra(world, x - dir.offsetX, y, z - dir.offsetZ);
        this.makeExtra(world, x + rot.offsetX, y, z + rot.offsetZ);
        this.makeExtra(world, x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ);
    }

    // --- comparator output from the carried battery's fill (works from any cell via findCore) ---

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return findCoreTE(level, pos) instanceof TileEntityBatterySocket socket ? socket.getComparatorPower() : 0;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.BATTERY_SOCKET.get(), TileEntityBatterySocket::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    /** Look-overlay: the carried battery item's charge / max (or "Empty" when no battery is inserted). */
    @Override
    public void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos) {
        if (!(findCoreTE(level, pos) instanceof TileEntityBatterySocket socket)) return;

        // Read the SYNCED carried stack, not getPower()/getMaxPower() — those read the slot-0 inventory
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
        ILookOverlay.printGeneric(graphics, this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
