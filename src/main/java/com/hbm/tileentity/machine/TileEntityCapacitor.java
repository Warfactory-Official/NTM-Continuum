package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.MachineCapacitor;
import com.hbm.blocks.machine.MachineCapacitorBus;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block-scale power buffer that charges from the face it points away from and discharges along a run
 * of {@link MachineCapacitorBus} blocks. Ported from 1.12.2's nested
 * {@code MachineCapacitor.TileEntityCapacitor}.
 *
 * <p>The bus walk is the whole of its wiring: from the block behind the capacitor it steps bus to bus
 * while every bus points the same way, and provides power at the far end. A run that turns a corner is
 * rejected outright rather than followed, which is what makes the bus a directed output pipe rather
 * than a network.
 *
 * <p>Its capacity comes from the block, not the constructor, because one block entity type backs all
 * five metals; it is still written to disk so a capacitor whose block is somehow gone reports honestly
 * rather than as a zero-capacity buffer.
 *
 * <p><b>Deviation from the fork.</b> No {@code IPersistentNBT}: this port does not carry that mechanism
 * on any machine, so a broken capacitor drops empty like every other machine here rather than keeping
 * its charge in the item.
 */
public class TileEntityCapacitor extends TileEntityLoadedBase
        implements IEnergyProviderMK2, IEnergyReceiverMK2, IRORValueProvider {

    public long power;
    protected long maxPower;
    public long powerReceived;
    public long powerSent;
    public long lastPowerReceived;
    public long lastPowerSent;

    public TileEntityCapacitor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAPACITOR.get(), pos, state);
        this.maxPower = capacityOf(state);
    }

    private static long capacityOf(BlockState state) {
        return state.getBlock() instanceof MachineCapacitor capacitor ? capacitor.getCapacity() : 0L;
    }

    private ForgeDirection facing() {
        BlockState state = getBlockState();
        if (!state.hasProperty(MachineCapacitor.FACING)) return ForgeDirection.UP;
        return ForgeDirection.getOrientation(state.getValue(MachineCapacitor.FACING));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCapacitor cap) {
        ForgeDirection opp = cap.facing();
        ForgeDirection dir = opp.getOpposite();

        BlockPos end = pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
        ForgeDirection run = null;

        while (level.getBlockState(end).getBlock() instanceof MachineCapacitorBus) {
            ForgeDirection current = ForgeDirection.getOrientation(
                    level.getBlockState(end).getValue(MachineCapacitorBus.FACING));
            if (run == null) run = current;

            if (run != current) {
                end = null;
                break;
            }
            end = end.offset(current.offsetX, current.offsetY, current.offsetZ);
        }

        if (end != null && run != null) {
            cap.tryProvide(level, end.getX(), end.getY(), end.getZ(), run);
        }

        cap.networkPackNT();

        cap.lastPowerSent = cap.powerSent;
        cap.lastPowerReceived = cap.powerReceived;
        cap.powerSent = 0;
        cap.powerReceived = 0;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (power <= 0) return power;

        long currentPower = getPower();
        long capacity = Math.max(getMaxPower() - currentPower, 0L);
        if (capacity <= 0) return power;

        long accepted = Math.min(power, capacity);
        if (!simulate) {
            setPower(currentPower + accepted);
            powerReceived += accepted;
        }
        return power - accepted;
    }

    @Override
    public void usePower(long power) {
        if (power <= 0) return;
        long used = Math.min(getPower(), power);
        if (used <= 0) return;
        powerSent += used;
        setPower(getPower() - used);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public long getProviderSpeed() {
        return getMaxPower() / 300;
    }

    @Override
    public long getReceiverSpeed() {
        return getMaxPower() / 100;
    }

    @Override
    public ConnectionPriority getPriority() {
        return ConnectionPriority.LOW;
    }

    @Override
    public void setPower(long power) {
        long newPower = maxPower > 0 ? Math.max(0L, Math.min(power, maxPower)) : Math.max(0L, power);
        if (this.power == newPower) return;
        this.power = newPower;
        if (level != null && !level.isClientSide()) markChanged();
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir == facing();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeLong(powerReceived);
        buf.writeLong(powerSent);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        powerReceived = buf.readLong();
        powerSent = buf.readLong();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", capacityOf(getBlockState()));
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fillpercent").equals(name)) {
            return "" + (maxPower > 0 ? power * 100 / maxPower : 0);
        }
        return null;
    }
}
