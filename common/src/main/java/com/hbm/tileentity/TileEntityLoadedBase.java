package com.hbm.tileentity;

import com.hbm.api.tile.ILoadedTile;
import com.hbm.lib.Library;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.platform.Services;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
 * <p>Unlike the old class, the buf sync runs <em>synchronously</em> on the server thread (the old
 * off-thread packet pool has no cross-loader equivalent and was a frequent source of CMEs), and the
 * chunk-load path uses vanilla NBT rather than the {@code hbmSync} byte[] trick (which can't survive
 * {@code ValueOutput}, see IMPLEMENTATION.md §20).
 */
public class TileEntityLoadedBase extends BlockEntity implements ILoadedTile, IBufPacketReceiver {

    public boolean isLoaded = true;
    public boolean muffled = false;

    /** Set by subclasses when a synced field changes; gates {@link #networkPackMK2()}. */
    protected boolean hasDataChanged = true;
    /** FNV-1a hash of the last buf payload sent, so identical packets aren't re-broadcast. */
    private long lastPackedBufHash = 0L;

    public TileEntityLoadedBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isLoaded() {
        return isLoaded && !isRemoved();
    }

    /** Marks the chunk dirty for saving without forcing a block update (old {@code markChanged}). */
    public void markChanged() {
        setChanged();
    }

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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("muffled", muffled);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        muffled = input.getBooleanOr("muffled", false);
        hasDataChanged = true;
    }

    // --- chunk-load sync (vanilla NBT path) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Sync all persistent (saveAdditional) fields to the client on chunk load. Display-only fields
        // that aren't persisted are handled by the per-tick buf packet instead.
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- per-tick / on-change "BufPacket" sync ---

    /**
     * Compiles this tile's {@link #serialize} payload and ships it to every player tracking the chunk.
     * No-op on the client. Skips the broadcast when the payload is byte-identical to the previous one.
     */
    public void networkPackNT() {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        byte[] data = compilePayload();
        long hash = Library.fnv1a64(data);
        if (hash == lastPackedBufHash) return;
        lastPackedBufHash = hash;

        Services.PLATFORM.sendToTracking(serverLevel, worldPosition, new BlockEntitySyncPayload(worldPosition, data));
    }

    /** As {@link #networkPackNT()} but skips compilation entirely unless {@link #dataChanged()} was set. */
    public void networkPackMK2() {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;
        if (!hasDataChanged) return;

        byte[] data = compilePayload();
        Services.PLATFORM.sendToTracking(serverLevel, worldPosition, new BlockEntitySyncPayload(worldPosition, data));
        hasDataChanged = false;
    }

    private byte[] compilePayload() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
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
        buf.writeBoolean(muffled);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        muffled = buf.readBoolean();
    }
}
