package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.MachineCapacitor;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModEnergyNetBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A passive HE store connected only through its mounted face. */
public class TileEntityMachineCapacitor extends TileEntityLoadedBase implements IEnergyReceiverMK2,
        IEnergyProviderMK2, IRORValueProvider {
    private static final String[] ROR = {PREFIX_VALUE + "fill", PREFIX_VALUE + "fillpercent"};
    private long power;
    private long maxPower;
    private long received, sent, lastReceived, lastSent;
    private long counted = Long.MIN_VALUE;

    public TileEntityMachineCapacitor(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.CAPACITOR.get(), pos, state);
        maxPower = ((MachineCapacitor) state.getBlock()).maxPower;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCapacitor capacitor) {
        capacitor.networkPackNT();
        capacitor.rollover();
    }

    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower / 100L; }
    @Override public long getProviderSpeed() { return maxPower / 300L; }
    @Override public ConnectionPriority getPriority() { return ConnectionPriority.LOW; }
    @Override public boolean bufferedEndpoint() { return true; }

    @Override
    public boolean canConnect(ForgeDirection side) {
        return side.toDirection() == getBlockState().getValue(MachineCapacitor.FACING);
    }

    @Override
    public void setPower(long value) {
        var bounded = IBatteryItem.clampCharge(value, maxPower);
        if (bounded == power) return;
        power = bounded;
        setChanged();
        dataChanged();
    }

    @Override
    public long transferPower(long amount, boolean simulate) {
        if (amount < 0) throw new IllegalArgumentException("Negative energy transfer");
        var next = IBatteryItem.adjustCharge(power, amount, maxPower, false);
        var accepted = next - power;
        if (!simulate && accepted > 0) {
            rollover();
            setPower(next);
            received = IBatteryItem.adjustCharge(received, accepted, Long.MAX_VALUE, false);
        }
        return amount - accepted;
    }

    @Override
    public void usePower(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative energy debit");
        var taken = Math.min(amount, power);
        if (taken == 0) return;
        rollover();
        sent = IBatteryItem.adjustCharge(sent, taken, Long.MAX_VALUE, false);
        setPower(power - taken);
    }

    private void rollover() {
        var now = level.getGameTime();
        if (now == counted) return;
        var previous = now == counted + 1;
        lastReceived = previous ? received : 0;
        lastSent = previous ? sent : 0;
        received = sent = 0;
        counted = now;
    }

    public long powerReceivedThisTick() { rollover(); return received; }
    public long powerSentThisTick() { rollover(); return sent; }
    public long lastPowerReceived() { rollover(); return lastReceived; }
    public long lastPowerSent() { rollover(); return lastSent; }
    public CapacitorCharge carriedState() { return new CapacitorCharge(power, maxPower); }

    public void restore(CapacitorCharge state) {
        applyState(state);
        setChanged();
        dataChanged();
    }

    private void applyState(CapacitorCharge state) {
        if (state.maxPower() <= 0) throw new IllegalArgumentException("A capacitor needs positive capacity");
        maxPower = state.maxPower();
        power = IBatteryItem.clampCharge(state.power(), maxPower);
    }

    @Override public String[] getFunctionInfo() { return ROR; }
    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name)) return Long.toString(power);
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return Integer.toString(CapacitorCharge.fillPercent(power, maxPower));
        return null;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeLong(power);
        buffer.writeLong(maxPower);
        buffer.writeLong(received);
        buffer.writeLong(sent);
        buffer.writeLong(lastReceived);
        buffer.writeLong(lastSent);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        var stored = buffer.readLong();
        applyState(new CapacitorCharge(stored, buffer.readLong()));
        received = buffer.readLong();
        sent = buffer.readLong();
        lastReceived = buffer.readLong();
        lastSent = buffer.readLong();
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
        applyState(new CapacitorCharge(input.getLongOr("power", 0), input.getLongOr("maxPower", maxPower)));
    }
}
