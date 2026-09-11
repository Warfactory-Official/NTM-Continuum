package com.hbm.tileentity.machine.pwr;

import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.pwr.BlockPWR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * One cell of an assembled reactor. Assembly swaps every part for this block and parks the part's own
 * state here, so the running structure is a single block type that reads as one casing and the original
 * layout survives to be put back.
 *
 * <p>Where upstream restores that state from {@code breakBlock}, 26.1 runs the removal hook after the
 * block entity is gone, so the cell restores itself from its own tick instead: once the core is missing
 * or no longer assembled, the cell puts its original block back. The one cell that was actually broken
 * cannot do that — it drops its original block as an item instead, see {@link BlockPWR#getDrops}.
 *
 * <p>A cell that is not a port answers the radio with {@code null} rather than upstream's empty string,
 * which is this port's signal for "no such value" and keeps a reader from printing a blank line.
 */
public class TileEntityBlockPWR extends TileEntityLoadedBase
        implements IFluidReceiverMK2, IRORValueProvider, IRORInteractive {

    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];

    public @Nullable BlockState originalBlockState;
    public @Nullable BlockPos corePos;
    private @Nullable TileEntityPWRController cachedCore;

    public TileEntityBlockPWR(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_BLOCK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityBlockPWR cell) {
        if (level.getGameTime() % 20 != 0) return;
        cell.restoreIfOrphaned(level, pos);
    }

    /** Puts the original part back once the reactor this cell belonged to is gone or taken apart. */
    public void restoreIfOrphaned(Level level, BlockPos pos) {
        if (corePos == null) return;

        TileEntityPWRController controller = getCore();
        if (controller != null && controller.assembled) return;

        corePos = null;
        level.setBlockAndUpdate(pos, originalBlockState == null
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : originalBlockState);
    }

    public @Nullable TileEntityPWRController getCore() {
        if (corePos == null || level == null) return null;
        if (cachedCore != null && !cachedCore.isRemoved() && cachedCore.getBlockPos().equals(corePos)) {
            return cachedCore;
        }
        if (level.hasChunkAt(corePos) && level.getBlockEntity(corePos) instanceof TileEntityPWRController core) {
            cachedCore = core;
            return cachedCore;
        }
        cachedCore = null;
        return null;
    }

    /** Whether this cell is one of the assembly's fluid ports; non-ports pass nothing through. */
    private boolean isPort() {
        return getBlockState().getValue(BlockPWR.IO_ENABLED);
    }

    private @Nullable TileEntityPWRController activeCore() {
        return isPort() ? getCore() : null;
    }

    // --- fluid, delegated to the core ---

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        TileEntityPWRController core = activeCore();
        return core == null ? amount : core.transferFluid(type, pressure, amount);
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        TileEntityPWRController core = activeCore();
        return core == null ? 0 : core.getDemand(type, pressure);
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        TileEntityPWRController core = activeCore();
        return core != null && core.canConnect(type, dir);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        TileEntityPWRController core = activeCore();
        return core == null ? NO_TANKS : core.getAllTanks();
    }

    // --- redstone over radio, delegated to the core ---

    @Override
    public String[] getFunctionInfo() {
        TileEntityPWRController core = activeCore();
        return core == null ? new String[0] : core.getFunctionInfo();
    }

    @Override
    public String provideRORValue(String name) {
        TileEntityPWRController core = activeCore();
        return core == null ? null : core.provideRORValue(name);
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        TileEntityPWRController core = activeCore();
        return core == null ? null : core.runRORFunction(name, params);
    }

    // --- persistence ---

    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.storeNullable("originalBlockState", BlockState.CODEC, originalBlockState);
        if (corePos != null) output.store("corePos", BlockPos.CODEC, corePos);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        originalBlockState = input.read("originalBlockState", BlockState.CODEC).orElse(null);
        corePos = input.read("corePos", BlockPos.CODEC).orElse(null);
        cachedCore = null;
    }
}
