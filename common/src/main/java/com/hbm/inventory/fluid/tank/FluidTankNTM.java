package com.hbm.inventory.fluid.tank;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

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

    public void resetTank() {
        this.type = Fluids.NONE;
        this.fluid = 0;
        this.pressure = 0;
    }

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

    public void writeToNBT(ValueOutput nbt, String s) {
        nbt.putInt(s, fluid);
        nbt.putInt(s + "_max", maxFluid);
        nbt.putInt(s + "_type", type.getID());
        nbt.putShort(s + "_p", (short) pressure);
    }

    public void readFromNBT(ValueInput nbt, String s) {
        fluid = nbt.getIntOr(s, 0);
        int max = nbt.getIntOr(s + "_max", 0);
        if (max > 0) maxFluid = max;
        fluid = Mth.clamp(fluid, 0, maxFluid);
        type = Fluids.fromID(nbt.getIntOr(s + "_type", 0));
        this.pressure = nbt.getShortOr(s + "_p", (short) 0);
    }

    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeInt(fluid);
        buf.writeInt(maxFluid);
        buf.writeInt(type.getID());
        buf.writeShort(pressure);
    }

    public void deserialize(RegistryFriendlyByteBuf buf) {
        fluid = buf.readInt();
        maxFluid = buf.readInt();
        type = Fluids.fromID(buf.readInt());
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
