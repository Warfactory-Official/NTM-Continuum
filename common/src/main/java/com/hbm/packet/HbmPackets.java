package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.IBufPacketReceiver;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class HbmPackets {

    private HbmPackets() {}

    public static void handleSync(Level level, BlockEntitySyncPayload payload) {
        if (level == null) return;
        BlockEntity be = level.getBlockEntity(payload.pos());
        if (be instanceof IBufPacketReceiver receiver) {
            RegistryFriendlyByteBuf buf =
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), level.registryAccess());
            try {
                receiver.deserialize(buf);
            } catch (Exception e) {
                Constants.LOG.warn("Failed to apply BlockEntitySyncPayload at {}: {}", payload.pos(), e.toString());
            }
        }
    }

    public static void handleControl(ServerPlayer player, ControlEventPayload payload) {
        if (player == null) return;
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(payload.pos());
        if (be instanceof IControlReceiver receiver
                && player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) <= 64.0
                && receiver.hasPermission(player)) {
            try {
                receiver.receiveControl(payload.data());
            } catch (Exception e) {
                Constants.LOG.warn("Failed to apply ControlEventPayload at {}: {}", payload.pos(), e.toString());
            }
        }
    }
}
