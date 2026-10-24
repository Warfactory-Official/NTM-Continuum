package com.hbm.inventory.fluid.tank;

import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** A kind-typed buffer which retains the exact resource it holds and an optional identifier pin. */
public class FluidTankNTM implements Cloneable {
    private FluidType type;
    private @Nullable FluidType pin;
    private @Nullable FluidType held;
    private @Nullable FluidType[] pinnedConnections;
    private transient @Nullable BlockEntity owner;
    private int fluid;
    private int maxFluid;
    private int pressure;

    public FluidTankNTM(FluidType type, int maxFluid) {
        this.type = type;
        this.maxFluid = maxFluid;
    }

    public byte getRedstoneComparatorPower() {
        if (getFill() == 0) return 0;
        var fraction = (double) getFill() / getMaxFill() * 15D;
        return (byte) Mth.clamp((int) fraction, 1, 15);
    }

    public FluidTankNTM withPressure(int pressure) {
        if (this.pressure != pressure) setFill(0);
        this.pressure = pressure;
        return this;
    }

    /** The stored kind/filter used by recipes and machine logic; held resources do not retype it. */
    public FluidType getTankType() { return type; }
    /** The held resource, else the identifier choice, else the native kind. */
    public FluidType getFluid() { return held != null ? held : pin != null ? pin : type; }
    public FluidType getDeclaredFluid() { return pin != null ? pin : type; }
    public boolean isStrict() { return pin != null; }

    public void bindOwner(BlockEntity owner) {
        if (this.owner != null && this.owner != owner) throw new IllegalStateException("Tank already has an owner");
        this.owner = owner;
    }

    /** Stable cached resource identities for the physical ports, independent of fill. */
    public FluidType[] connectionTypes() {
        if (pin != null) return pinnedConnections;
        if (type.isForeign() && Fluids.kindOf(type) != type) {
            // A formerly unshared declaration remains that exact filter after sharing metadata changes.
            if (pinnedConnections == null || pinnedConnections[0] != type) pinnedConnections = new FluidType[]{type};
            return pinnedConnections;
        }
        return Fluids.equivalentResources(type);
    }

    private void declaredChanged() {
        if (owner == null) return;
        owner.setChanged();
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        if (owner instanceof TileEntityLoadedBase loaded) loaded.refreshFluidPorts();
        for (var side : Direction.values()) BlockFluidDuct.portChanged(level, owner.getBlockPos().relative(side));
    }

    /** Code and recipe typing keeps existing contents and pins when the kind is unchanged. */
    public void setTankType(@Nullable FluidType incoming) {
        if (incoming == type) return;
        var kind = incoming == null ? Fluids.NONE : Fluids.kindOf(incoming);
        if (type == kind) return;
        type = kind;
        pin = null;
        pinnedConnections = null;
        held = null;
        fluid = 0;
        declaredChanged();
    }

    /** A player's identifier chooses one exact resource; incompatible contents are discarded. */
    public void setTankTypeByIdentifier(@Nullable FluidType incoming) {
        var exact = incoming == null || incoming == Fluids.NONE ? null : incoming;
        var kind = exact == null ? Fluids.NONE : Fluids.kindOf(exact);
        if (type == kind && pin == exact) return;
        if (type != kind || fluid > 0 && getFluid() != exact) {
            held = null;
            fluid = 0;
        }
        type = kind;
        pin = exact;
        pinnedConnections = pin == null ? null : new FluidType[]{pin};
        declaredChanged();
    }

    public boolean setType(NTMItemHandler slots, int in, int out) {
        var stack = slots.getStackInSlot(in);
        if (!(stack.getItem() instanceof IItemFluidId identifier)) return false;
        if (in != out && !slots.getStackInSlot(out).isEmpty()) return false;
        var chosen = identifier.getType(null, 0, 0, 0, stack);
        if (pin == chosen || chosen == Fluids.NONE && type == Fluids.NONE) return false;
        setTankTypeByIdentifier(chosen);
        if (in != out) {
            slots.setStackInSlot(out, stack.copy());
            slots.setStackInSlot(in, ItemStack.EMPTY);
        }
        return true;
    }

    public boolean accepts(net.minecraft.world.level.material.Fluid incoming) {
        return accepts(Fluids.fromVanillaExact(incoming));
    }
    public boolean provides(net.minecraft.world.level.material.Fluid incoming) {
        return provides(Fluids.fromVanillaExact(incoming));
    }
    public boolean joins(net.minecraft.world.level.material.Fluid incoming) {
        return joins(Fluids.fromVanillaExact(incoming));
    }

    public boolean accepts(@Nullable FluidType incoming) {
        if (incoming == null || incoming == Fluids.NONE || type == Fluids.NONE) return false;
        if (fluid > 0) return incoming == getFluid();
        return pin != null ? incoming == pin : incoming == type || Fluids.kindOf(incoming) == type;
    }
    public boolean provides(@Nullable FluidType incoming) {
        return incoming != null && type != Fluids.NONE && incoming == getFluid();
    }
    /** Physical connection ignores stored content; a strict identifier still limits the resource. */
    public boolean joins(@Nullable FluidType incoming) {
        if (incoming == null || incoming == Fluids.NONE || type == Fluids.NONE) return false;
        return pin != null ? incoming == pin : incoming == type || Fluids.kindOf(incoming) == type;
    }

    public void resetTank() {
        var changed = type != Fluids.NONE || pin != null;
        type = Fluids.NONE;
        pin = null;
        pinnedConnections = null;
        held = null;
        fluid = 0;
        pressure = 0;
        if (changed) declaredChanged();
    }
    public FluidTankNTM conform(FluidStack stack) {
        setTankType(stack.type);
        return withPressure(stack.pressure);
    }
    public int getFill() { return fluid; }
    public void setFill(int amount) {
        if (fluid == 0 && amount > 0 && type.isForeign()) held = type;
        fluid = Mth.clamp(amount, 0, maxFluid);
        if (fluid == 0) held = null;
    }
    public int getMaxFill() { return maxFluid; }
    public int getPressure() { return pressure; }
    public int changeTankSize(int size) {
        maxFluid = size;
        var excess = Math.max(0, fluid - maxFluid);
        if (excess > 0) setFill(maxFluid);
        return excess;
    }
    public int receive(FluidType incoming, int amount) {
        if (!accepts(incoming)) return 0;
        var accepted = Math.min(amount, maxFluid - fluid);
        if (accepted <= 0) return 0;
        if (fluid == 0) held = incoming.isForeign() || incoming != type ? incoming : null;
        fluid += accepted;
        return accepted;
    }
    /** Moves actual contents without changing either tank's typing or pressure setting. */
    public int transferTo(FluidTankNTM target, int amount) {
        var moved = target.receive(getFluid(), Math.min(amount, fluid));
        if (moved > 0) setFill(fluid - moved);
        return moved;
    }

    /** Capacity-proportional balancing with exact identity and integer conservation. */
    public int balanceWith(FluidTankNTM other) {
        if (getTankType() != other.getTankType()) return 0;
        var total = (long) fluid + other.fluid;
        if (total == 0) return 0;
        var resource = fluid > 0 ? getFluid() : other.getFluid();
        if (!accepts(resource) || !other.accepts(resource)) return 0;
        var capacity = (long) maxFluid + other.maxFluid;
        var wanted = (int) ((total * maxFluid + capacity - 1) / capacity);
        return wanted > fluid ? other.transferTo(this, wanted - fluid) : transferTo(other, fluid - wanted);
    }

    public int fill(FluidType incoming, int amount, boolean doFill) {
        if (incoming == null || incoming == Fluids.NONE || amount <= 0) return 0;
        if (type == Fluids.NONE) {
            var accepted = Math.min(maxFluid, amount);
            if (doFill && accepted > 0) {
                type = Fluids.kindOf(incoming);
                held = incoming.isForeign() || incoming != type ? incoming : null;
                fluid = accepted;
                declaredChanged();
            }
            return accepted;
        }
        if (!accepts(incoming)) return 0;
        return doFill ? receive(incoming, amount) : Math.min(amount, maxFluid - fluid);
    }

    private static @Nullable FluidType resource(String id) {
        var exact = id.isEmpty() ? Fluids.NONE : Fluids.byRegistryNameExact(id);
        return exact == Fluids.NONE ? null : exact;
    }
    private void restoreIdentities(@Nullable FluidType pin, @Nullable FluidType held, boolean missingHeld) {
        this.pin = pin;
        pinnedConnections = pin == null ? null : new FluidType[]{pin};
        this.held = held != null ? held : fluid > 0 && type.isForeign() ? type : null;
        if (missingHeld) fluid = 0;
        setFill(fluid);
        if (pin != null && this.held != null && pin != this.held) {
            throw new IllegalStateException("Tank contents do not match its exact identifier pin");
        }
    }
    public void writeToNBT(DataOut out, String key) {
        out.putInt(key, fluid);
        out.putInt(key + "_max", maxFluid);
        Fluids.write(out, key + "_type", type);
        out.putShort(key + "_p", (short) pressure);
        if (pin != null) Fluids.write(out, key + "_pin", pin);
        if (held != null) Fluids.write(out, key + "_held", held);
    }
    public void writeCarried(CompoundTag out, String key) {
        out.putInt(key, fluid);
        out.putInt(key + "_max", maxFluid);
        Fluids.write(out, key + "_type", type);
        if (pin != null) Fluids.write(out, key + "_pin", pin);
        if (held != null) Fluids.write(out, key + "_held", held);
    }
    public void readCarried(CompoundTag in, String key) {
        type = Fluids.readExact(in, key + "_type");
        fluid = Tags.getIntOr(in, key, 0);
        var heldId = Tags.getStringOr(in, key + "_held", "");
        var held = resource(heldId);
        restoreIdentities(resource(Tags.getStringOr(in, key + "_pin", "")), held, !heldId.isEmpty() && held == null);
    }
    public void readFromNBT(DataIn in, String key) {
        fluid = in.getIntOr(key, 0);
        var maximum = in.getIntOr(key + "_max", 0);
        if (maximum > 0) maxFluid = maximum;
        type = Fluids.readExact(in, key + "_type");
        pressure = in.getShortOr(key + "_p", (short) 0);
        var heldId = in.getStringOr(key + "_held", "");
        var held = resource(heldId);
        restoreIdentities(resource(in.getStringOr(key + "_pin", "")), held, !heldId.isEmpty() && held == null);
    }
    public void serialize(RegistryFriendlyByteBuf buffer) { serialize(buffer, fluid); }
    public void serialize(RegistryFriendlyByteBuf buffer, int fill) {
        serialize(buffer, fill, getFluid(), pin);
    }
    /** The resource and pin accompany a caller's pre-processing fill snapshot without allocating per tick. */
    public void serialize(RegistryFriendlyByteBuf buffer, int fill, FluidType resource, @Nullable FluidType snapshotPin) {
        packetSerialize(buffer, fill, resource, snapshotPin);
    }
    /** The same native tank payload in an independently encoded state unit. */
    public void packetSerialize(io.netty.buffer.ByteBuf buffer) { packetSerialize(buffer, fluid, getFluid(), pin); }
    private void packetSerialize(io.netty.buffer.ByteBuf buffer, int fill, FluidType resource, @Nullable FluidType snapshotPin) {
        buffer.writeInt(fill);
        buffer.writeInt(maxFluid);
        buffer.writeInt(Fluids.networkId(type));
        buffer.writeShort(pressure);
        buffer.writeInt(snapshotPin == null ? -1 : Fluids.networkId(snapshotPin));
        var snapshotHeld = fill > 0 && (resource.isForeign() || resource != type) ? resource : null;
        buffer.writeInt(snapshotHeld == null ? -1 : Fluids.networkId(snapshotHeld));
    }
    public void deserialize(RegistryFriendlyByteBuf buffer) { packetDeserialize(buffer); }
    public void packetDeserialize(io.netty.buffer.ByteBuf buffer) {
        fluid = buffer.readInt();
        maxFluid = buffer.readInt();
        type = Fluids.byNetworkIdExact(buffer.readInt());
        pressure = buffer.readShort();
        var pinId = buffer.readInt();
        var heldId = buffer.readInt();
        var held = heldId < 0 ? null : Fluids.byNetworkIdExact(heldId);
        restoreIdentities(pinId < 0 ? null : Fluids.byNetworkIdExact(pinId), held, heldId >= 0 && held == Fluids.NONE);
    }
    @Override public FluidTankNTM clone() {
        try {
            var copy = (FluidTankNTM) super.clone();
            copy.owner = null;
            return copy;
        } catch (CloneNotSupportedException error) { throw new AssertionError(error); }
    }
}
