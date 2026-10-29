package com.hbm.tileentity;

import io.netty.buffer.ByteBuf;
import dev.golem.core.data.CompatBlockEntity;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import com.hbm.uninos.networkproviders.PowerNetProvider;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.lib.Library;
import com.hbm.packet.BlockEntitySyncPayload;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.net.GolemSync;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;

/**
 * Root of HBM's BlockEntity tower, ported from 1.12.2's {@code TileEntityLoadedBase}. Provides:
 *
 * <ul>
 *   <li>the {@link ILoadedTile} "is this tile loaded" flag,</li>
 *   <li>the {@code muffled} sound flag (persisted),</li>
 *   <li>vanilla chunk-load sync ({@link #getUpdateTag}/{@link #getUpdatePacket} via
 *       {@code saveCustomOnly}, so all persistent fields reach the client on chunk load), and</li>
 *   <li>the per-tick / on-change "BufPacket" sync via {@link #networkPackNT()} /
 *       {@link #networkPackMK2()}, the 26.1 replacement for the old off-thread {@code BufPacket}.</li>
 * </ul>
 *
 * <p>The buf sync runs <em>synchronously</em> on the server thread (the old off-thread packet pool has
 * no cross-loader equivalent and was a frequent source of CMEs), and the chunk-load path uses vanilla
 * NBT rather than the {@code hbmSync} byte[] trick (which can't survive {@code DataOut}).
 */
public class TileEntityLoadedBase extends CompatBlockEntity implements ILoadedTile, IBufPacketReceiver, GraphResident {

    public boolean isLoaded = true;
    public boolean muffled = false;

    /** Set by subclasses when a synced field changes; gates {@link #networkPackMK2()}. */
    protected boolean hasDataChanged = true;
    /** FNV-1a hash of the last buf payload sent, so identical packets aren't re-broadcast. */
    private long lastPackedBufHash = 0L;

    public TileEntityLoadedBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TilePort[] powerPorts;
    public TilePort[] fluidInPorts;
    public TilePort[] fluidOutPorts;

    /** Power ports and, where the tile has tanks of that role, fluid in and fluid out ports. */
    public void setupAllPorts(PortDef[] ports) {
        if (ports == null || ports.length == 0) return;
        this.setupPowerPorts(ports);
        this.setupFluidPorts(ports);
    }

    /** Separate power ports, no passthrough. */
    public void setupPowerPorts(PortDef[] ports) {
        if (powerPorts != null) return;
        powerPorts = TilePort.manyToMany(this, ports);
        for (TilePort port : powerPorts) {
            port.setupType(PowerNetProvider.THE_PROVIDER);
        }
    }

    public void setupFluidPorts(PortDef[] ports) {
        if (fluidInPorts == null && this instanceof IFluidStandardReceiverMK2 receiver) this.setupFluidInPorts(receiver.getReceivingTanks(), PortDef.combine(ports));
        if (fluidOutPorts == null && this instanceof IFluidStandardSenderMK2 sender) this.setupFluidOutPorts(sender.getSendingTanks(), PortDef.combine(ports));
    }

    public void setupFluidInPorts(FluidTankNTM[] tanks, PortDef ports) {
        if (fluidInPorts != null) return;
        fluidInPorts = TilePort.oneToMany(this, tanks.length, ports);
        for (int i = 0; i < fluidInPorts.length; i++) {
            fluidInPorts[i].setupFluids(tanks[i], level);
        }
    }

    public void setupFluidOutPorts(FluidTankNTM[] tanks, PortDef ports) {
        if (fluidOutPorts != null) return;
        fluidOutPorts = TilePort.oneToMany(this, tanks.length, ports);
        for (int i = 0; i < fluidOutPorts.length; i++) {
            fluidOutPorts[i].setupFluids(tanks[i], level);
        }
    }

    public void updateAllPorts() {
        if (powerPorts != null) for (TilePort port : powerPorts) port.update(level);
        if (fluidInPorts != null) for (TilePort port : fluidInPorts) port.update(level);
        if (fluidOutPorts != null) for (TilePort port : fluidOutPorts) port.update(level);
    }

    /** Fluid in, fluid out. */
    public void updatePortFIFO() {
        this.updateAllPorts();
        if (this instanceof IFluidStandardReceiverMK2 receiver) this.receiveFluid(receiver.getReceivingTanks());
        if (this instanceof IFluidStandardSenderMK2 sender) this.provideFluid(sender.getSendingTanks());
    }

    /** Power in, fluid in, fluid out. */
    public void updatePortPIFIFO() { this.updatePortFIFO(); this.receivePower(); }
    /** Power out, fluid in, fluid out. */
    public void updatePortPOFIFO() { this.updatePortFIFO(); this.providePower(); }

    public void receivePower() { if (powerPorts == null) return; if (this instanceof IEnergyReceiverMK2) for (TilePort port : powerPorts) port.checkSubscribe(level); }
    public void providePower() { if (powerPorts == null) return; if (this instanceof IEnergyProviderMK2) for (TilePort port : powerPorts) port.checkProvide(level); }

    public void provideFluid(FluidTankNTM[] tanks) { provideFluid(tanks, this.fluidOutPorts); }
    public void provideFluid(FluidTankNTM[] tanks, TilePort[] ports) {
        if (ports == null || ports.length != tanks.length) return;
        for (int i = 0; i < ports.length; i++) {
            ports[i].setupFluids(tanks[i], level);
            if (!ports[i].needsRebuild) ports[i].checkProvide(level);
        }
    }

    public void receiveFluid(FluidTankNTM[] tanks) { receiveFluid(tanks, this.fluidInPorts); }
    public void receiveFluid(FluidTankNTM[] tanks, TilePort[] ports) {
        if (ports == null || ports.length != tanks.length) return;
        for (int i = 0; i < ports.length; i++) {
            ports[i].setupFluids(tanks[i], level);
            if (!ports[i].needsRebuild) ports[i].checkSubscribe(level);
        }
    }

    /** Applies a changed tank declaration to every real port and its exact-resource equivalents. */
    public void refreshFluidPorts() {
        if (this instanceof IFluidStandardReceiverMK2 receiver && fluidInPorts != null) {
            var tanks = receiver.getReceivingTanks();
            if (tanks.length == fluidInPorts.length) for (var index = 0; index < tanks.length; index++) fluidInPorts[index].setupFluids(tanks[index], level);
        }
        if (this instanceof IFluidStandardSenderMK2 sender && fluidOutPorts != null) {
            var tanks = sender.getSendingTanks();
            if (tanks.length == fluidOutPorts.length) for (var index = 0; index < tanks.length; index++) fluidOutPorts[index].setupFluids(tanks[index], level);
        }
        updateAllPorts();
        if (this instanceof IFluidStandardReceiverMK2 receiver) receiveFluid(receiver.getReceivingTanks());
        if (this instanceof IFluidStandardSenderMK2 sender) provideFluid(sender.getSendingTanks());
    }

    public void destroyAllPorts() {
        disablePorts(powerPorts);
        disablePorts(fluidInPorts);
        disablePorts(fluidOutPorts);
        powerPorts = null;
        fluidInPorts = null;
        fluidOutPorts = null;
    }

    private void disablePorts(TilePort[] ports) {
        if (ports != null && level != null) for (TilePort port : ports) if (port != null) port.disableIfPresent(level);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (this instanceof IFluidUserMK2 user) for (var tank : user.getAllTanks()) tank.bindOwner(this);
        GraphResident.onLoad(this);
    }

    @Override
    public boolean isLoaded() {
        return isLoaded && !isRemoved();
    }

    /** Marks the chunk dirty for saving without forcing a block update (old {@code markChanged}). */
    public void markChanged() {
        setChanged();
    }

    private static final int SAVE_INTERVAL = 100;

    public void dataChanged() {
        this.hasDataChanged = true;
    }

    public float getVolume(float baseVolume) {
        return muffled ? baseVolume * 0.1F : baseVolume;
    }

    public void setMuffled(boolean muffled) {
        this.muffled = muffled;
        dataChanged();
    }

    // --- persistence ---

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("muffled", muffled);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        muffled = input.getBooleanOr("muffled", false);
        hasDataChanged = true;
    }

    // --- chunk-load sync (vanilla NBT path) ---

    /** Machines drive their GUI from persisted fields, so the whole tag goes out on chunk load. */
    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- per-tick / on-change "BufPacket" sync ---

    /**
     * Compiles this tile's {@link #serialize} payload and publishes it to every player tracking the
     * chunk. No-op on the client, and no-op while nobody is tracking: the payload is not even built
     * then. Skips the broadcast when the payload is
     * byte-identical to the previous one; {@link GolemSync} replays that last payload to players who
     * arrive later, so skipping an unchanged send does not strand them.
     */
    public void networkPackNT() {
        networkPackNT(true);
    }

    /** Callers that notify every persistent mutation can synchronize without the periodic save backstop. */
    protected void networkPackNT(boolean periodicSave) {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        // Machines change progress, fuel, heat, power and tanks every tick without marking the chunk
        // unsaved, so a chunk that is otherwise unmodified would save and unload stale state.
        if (periodicSave && Math.floorMod(serverLevel.getGameTime() + worldPosition.hashCode(), SAVE_INTERVAL) == 0)
            setChanged();
        if (!GolemSync.isWatched(serverLevel, worldPosition)) return;

        byte[] data = compilePayload();
        long hash = Library.fnv1a64(data);
        if (hash == lastPackedBufHash) return;
        lastPackedBufHash = hash;

        GolemSync.publish(serverLevel, worldPosition, BlockEntitySyncPayload.CODEC,
                new BlockEntitySyncPayload(worldPosition, data));
    }

    /**
     * As {@link #networkPackNT()} but skips compilation entirely unless {@link #dataChanged()} was set.
     * A change raised while nobody watches keeps its flag rather than clearing it, so the first tick
     * with an audience sends the state that changed in the dark.
     */
    public void networkPackMK2() {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (!hasDataChanged) return;
        setChanged();
        if (!GolemSync.isWatched(serverLevel, worldPosition)) return;

        byte[] data = compilePayload();
        GolemSync.publish(serverLevel, worldPosition, BlockEntitySyncPayload.CODEC,
                new BlockEntitySyncPayload(worldPosition, data));
        hasDataChanged = false;
    }

    @Override
    public void setRemoved() {
        releaseSync();
        // also a chunk unload: 1.7 dropped the port nodes in onChunkUnload and invalidate alike
        disablePorts(powerPorts);
        disablePorts(fluidInPorts);
        disablePorts(fluidOutPorts);
        super.setRemoved();
    }

    /** Releases this tile's current native publication entry before its lifecycle ends. */
    public void releaseSync() {
        if (level instanceof ServerLevel serverLevel) GolemSync.retract(serverLevel, worldPosition);
    }

    protected void writeMuffled(ByteBuf output) { output.writeBoolean(muffled); }
    protected void readMuffled(ByteBuf input) { muffled = input.readBoolean(); }

    private byte[] compilePayload() {
        var buf = GolemBuffers.wrap(Unpooled.buffer(), level);
        try {
            serialize(buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }

    /**
     * Writes per-tick sync data. The base writes the muffled flag so noisy machines that call
     * {@code super.serialize} stay in sync; quiet machines may ignore it. Counterpart of
     * {@link #deserialize(RegistryFriendlyByteBuf)}.
     */
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        writeMuffled(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        readMuffled(buf);
    }
}
