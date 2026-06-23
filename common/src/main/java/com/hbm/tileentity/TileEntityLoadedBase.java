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

public class TileEntityLoadedBase extends BlockEntity implements ILoadedTile, IBufPacketReceiver {

    public boolean isLoaded = true;
    public boolean muffled = false;

    protected boolean hasDataChanged = true;

    private long lastPackedBufHash = 0L;

    public TileEntityLoadedBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean isLoaded() {
        return isLoaded && !isRemoved();
    }

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

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {

        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void networkPackNT() {
        if (level == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        byte[] data = compilePayload();
        long hash = Library.fnv1a64(data);
        if (hash == lastPackedBufHash) return;
        lastPackedBufHash = hash;

        Services.PLATFORM.sendToTracking(serverLevel, worldPosition, new BlockEntitySyncPayload(worldPosition, data));
    }

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

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(muffled);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        muffled = buf.readBoolean();
    }
}
