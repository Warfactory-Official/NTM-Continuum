package com.hbm.tileentity.network.pneumatic;

import net.minecraft.core.Direction;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.StackCache.CacheSlot;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import com.hbm.inventory.ISlotMonitorProvider;
import com.hbm.inventory.SlotMonitor;
import com.hbm.inventory.StackCache;
import com.hbm.uninos.PneumaticGraph;
import com.hbm.uninos.networkproviders.PneumaticNetwork;

/** 1.7 {@code TileEntityPneumaticStorageBase}: an air-powered storage bank exposed to the network as monitors. */
public abstract class TileEntityPneumaticStorageBase extends TileEntityMachineBase
        implements IFluidStandardReceiverMK2, ISlotMonitorProvider, IControlReceiver, MenuProvider {

    public final FluidTankNTM compair;
    public final SlotMonitor[] monitors;

    protected boolean wasAvailable = false;
    /** The net last joined; a rebuilt net re-offers every monitor to the caches that now share it. */
    private @Nullable PneumaticNetwork lastNet;

    protected TileEntityPneumaticStorageBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
        this.compair = new FluidTankNTM(Fluids.AIR, 4_000).withPressure(1);
        this.monitors = new SlotMonitor[slots];

        for (int i = 0; i < monitors.length; i++) this.monitors[i] = new SlotMonitor(i, this);
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "pressure")) {
            int pressure = this.compair.getPressure() + 1;
            if (pressure > 5) pressure = 1;
            this.compair.setTankType(Fluids.AIR);
            this.compair.withPressure(pressure);
            for (SlotMonitor monitor : this.monitors) monitor.availabilityHasChanged();
        }
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;

        boolean isAvailable = this.isAvailable();

        if (isAvailable != wasAvailable) {
            this.wasAvailable = isAvailable;
            for (SlotMonitor monitor : monitors) monitor.availabilityHasChanged();
        }

        PneumaticNetwork net = PneumaticNetwork.at(sl, worldPosition);
        if (net != lastNet) {
            lastNet = net;
            for (SlotMonitor monitor : monitors) monitor.availabilityHasChanged();
        }
        if (net != null) net.addStorage(this);

        if (this.compair.getFill() > 0) {
            // 1.7: int division, so the ceil it wraps is the identity
            int consumption = this.compair.getFill() * 9 / this.compair.getMaxFill() + 1;
            this.compair.setFill(Math.max(this.compair.getFill() - consumption, 0));
        }

        this.updateMonitors();
        this.syncState();
    }

    public boolean isAvailable() {
        return !this.isRemoved() && this.compair.getFill() > 0;
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) PneumaticGraph.addNode(server, worldPosition);
    }

    /** 1.7 splits this across invalidate() and onChunkUnload(); setRemoved fires for both. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        for (SlotMonitor monitor : this.monitors) {
            for (CacheSlot cache : List.copyOf(monitor.viewedBy)) cache.removeMonitor(monitor);
            monitor.viewedBy.clear();
        }
        if (level instanceof ServerLevel sl) {
            PneumaticNetwork net = PneumaticNetwork.at(sl, worldPosition);
            if (net != null) net.storages.remove(this);
        }
        PneumaticGraph.dropCell(this, worldPosition);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        compair.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        compair.deserialize(buf);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.compair.readFromNBT(input, "tank");
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        this.compair.writeToNBT(output, "tank");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null && type == Fluids.AIR;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{compair};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{compair};
    }

    @Override
    public SlotMonitor[] getMonitors() {
        return monitors;
    }

    @Override
    public ItemStack getSlotAt(int index) {
        return inventory.getStackInSlot(index);
    }

    @Override
    public @Nullable PneumaticNetwork getRelevantNetwork() {
        if (!(level instanceof ServerLevel sl)) return null;
        return PneumaticNetwork.at(sl, worldPosition);
    }

    @Override
    public boolean isAvailableToCache(StackCache cache) {
        if (!isAvailable()) return false;
        int range = TileEntityPneumoTube.getRangeFromPressure(this.compair.getPressure());
        return worldPosition.distSqr(cache.pos) <= (double) range * range;
    }
}
