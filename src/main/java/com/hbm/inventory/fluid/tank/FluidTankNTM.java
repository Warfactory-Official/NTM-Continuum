package com.hbm.inventory.fluid.tank;

import dev.golem.core.data.Tags;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.items.machine.IItemFluidId;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.NonNull;

/**
 * HBM's fluid tank: a typed, pressurised fill buffer used by every fluid machine/pipe. It holds a
 * {@link FluidType}, a fill amount, a capacity, and a pressure level.
 *
 * <p>Slimmer than the 1.12.2 original, which implemented Forge's {@code IFluidHandler}/{@code IFluidTank},
 * carried GL tank rendering and handled canister item fill/unload: the network and machines only need the
 * typed buffer and its persistence. Persistence is via {@code DataOut}/{@code DataIn} (NBT) and
 * {@code RegistryFriendlyByteBuf} (the per-tick BufPacket sync).
 *
 * @author hbm, mlbv
 */
public class FluidTankNTM implements Cloneable {

    @NonNull
    private FluidType type;
    private int fluid;
    private int maxFluid;
    private int pressure = 0;

    public FluidTankNTM(@NonNull FluidType type, int maxFluid) {
        this.type = type;
        this.maxFluid = maxFluid;
    }

    public byte getRedstoneComparatorPower() {
        if (getFill() == 0) return 0;
        double frac = (double) getFill() / (double) getMaxFill() * 15D;
        return (byte) Mth.clamp((int) frac, 1, 15);
    }

    public FluidTankNTM withPressure(int pressure) {
        if (this.pressure != pressure) this.setFill(0);
        this.pressure = pressure;
        return this;
    }

    @NonNull
    public FluidType getTankType() {
        return type;
    }

    public void setTankType(FluidType type) {
        if (type == null) type = Fluids.NONE;
        if (this.type == type) return;
        this.type = type;
        this.setFill(0);
    }

    /** 1.7 {@code setType(in, out, slots)}: an identifier in {@code in} retypes the tank, then moves to {@code out}. */
    public boolean setType(NTMItemHandler slots, int in, int out) {
        ItemStack stack = slots.getStackInSlot(in);
        if (!(stack.getItem() instanceof IItemFluidId id)) return false;
        if (in != out && !slots.getStackInSlot(out).isEmpty()) return false;
        FluidType newType = id.getType(null, 0, 0, 0, stack);
        if (type == newType) return false;
        type = newType;
        fluid = 0;
        if (in != out) {
            slots.setStackInSlot(out, stack.copy());
            slots.setStackInSlot(in, ItemStack.EMPTY);
        }
        return true;
    }

    public void resetTank() {
        this.type = Fluids.NONE;
        this.fluid = 0;
        this.pressure = 0;
    }

    /** Changes type and pressure based on a fluid stack, useful for changing tank types based on recipes. */
    public FluidTankNTM conform(FluidStack stack) {
        this.setTankType(stack.type);
        this.withPressure(stack.pressure);
        return this;
    }

    public int getFill() { return fluid; }

    public void setFill(int i) { fluid = i; }

    public int getMaxFill() { return maxFluid; }

    public int getPressure() { return pressure; }

    public int changeTankSize(int size) {
        maxFluid = size;
        if (fluid > maxFluid) {
            int dif = fluid - maxFluid;
            fluid = maxFluid;
            return dif;
        }
        return 0;
    }

    /**
     * Fills this tank with {@code amount} of {@code incomingType}, returns how much was accepted. If the
     * tank is empty it adopts the incoming type; otherwise the types must match.
     */
    public int fill(FluidType incomingType, int amount, boolean doFill) {
        if (incomingType == null || incomingType == Fluids.NONE || amount <= 0) return 0;
        if (this.type == Fluids.NONE) {
            int toTransfer = Math.min(getMaxFill(), amount);
            if (doFill) {
                this.type = incomingType;
                this.setFill(toTransfer);
            }
            return toTransfer;
        } else {
            if (!this.type.equals(incomingType)) return 0;
            int toTransfer = Math.min(getMaxFill() - getFill(), amount);
            if (doFill && toTransfer > 0) setFill(getFill() + toTransfer);
            return toTransfer;
        }
    }

    // --- persistence (NBT) ---

    /** Writes fill/max/type/pressure under keys prefixed by {@code s}. */
    public void writeToNBT(DataOut nbt, String s) {
        nbt.putInt(s, fluid);
        nbt.putInt(s + "_max", maxFluid);
        Fluids.write(nbt, s + "_type", type); // registry name, never the numeric id
        nbt.putShort(s + "_p", (short) pressure);
    }

    public void readFromNBT(DataIn nbt, String s) {
        fluid = nbt.getIntOr(s, 0);
        int max = nbt.getIntOr(s + "_max", 0);
        if (max > 0) maxFluid = max;
        fluid = Mth.clamp(fluid, 0, maxFluid);
        type = Fluids.read(nbt, s + "_type");
        this.pressure = nbt.getShortOr(s + "_p", (short) 0);
    }

    // --- per-tick sync ---

    public void serialize(RegistryFriendlyByteBuf buf) {
        serialize(buf, fluid);
    }

    /** Same wire format as {@link #serialize(RegistryFriendlyByteBuf)} but with a caller-supplied fill (a pre-tick snapshot). */
    public void serialize(RegistryFriendlyByteBuf buf, int fill) {
        buf.writeInt(fill);
        buf.writeInt(maxFluid);
        buf.writeInt(Fluids.networkId(type));
        buf.writeShort(pressure);
    }

    public void deserialize(RegistryFriendlyByteBuf buf) {
        fluid = buf.readInt();
        maxFluid = buf.readInt();
        type = Fluids.byNetworkId(buf.readInt());
        pressure = buf.readShort();
    }

    @Override
    public FluidTankNTM clone() {
        try {
            return (FluidTankNTM) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
