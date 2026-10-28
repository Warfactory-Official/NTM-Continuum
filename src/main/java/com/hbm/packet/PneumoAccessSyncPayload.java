package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.world.ItemStacks;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.inventory.container.ContainerPneumoStorageAccess;

/**
 * Server to client: what changed in one access terminal's view of the network. 1.7
 * {@code ContainerPneumoStorageAccess.pushNewItem}/{@code updateCount}, whose deltas rode the shared
 * {@code ContainerNBTCommsPacket}.
 *
 * <p>1.7 keys a type by a hash of (item, meta, nbt) that both sides re-derive; the cache here is keyed by the stack
 * itself, so the menu hands out a per-menu id and the wire carries the type once. The deltas are compiled into a
 * registry-aware buffer on the sending side (like {@code BlockEntitySyncPayload}) so item stacks encode on every
 * node.
 */
public record PneumoAccessSyncPayload(int containerId, byte[] data) implements GolemPayload {

    public static final GolemCodec<PneumoAccessSyncPayload> CODEC = GolemCodec.of(PneumoAccessSyncPayload.class,
            Ids.of(Constants.MOD_ID, "pneumo_access_sync"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.containerId);
                buffer.writeByteArray(payload.data);
            },
            buffer -> new PneumoAccessSyncPayload(buffer.readVarInt(), buffer.readByteArray()));

    /** @param display the type, once, when the client has not been told this id yet; null updates the count alone */
    public record Delta(int id, @Nullable ItemStack display, long amount) {

        /** 1.7 {@code DeltaType.RESET}: the client forgets every type it was told about. */
        public static final Delta RESET = new Delta(-1, null, 0);

        public boolean isReset() {
            return id < 0;
        }
    }

    public static PneumoAccessSyncPayload of(Level level, int containerId, List<Delta> deltas) {
        var buf = GolemBuffers.wrap(Unpooled.buffer(), level);
        try {
            buf.writeVarInt(deltas.size());
            for (Delta delta : deltas) {
                buf.writeVarInt(delta.id());
                buf.writeBoolean(delta.display() != null);
                if (delta.display() != null) ItemStacks.writeOptional(buf, delta.display());
                buf.writeVarLong(delta.amount());
            }
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return new PneumoAccessSyncPayload(containerId, data);
        } finally {
            buf.release();
        }
    }

    public static void handleClient(PneumoAccessSyncPayload payload, GolemNetwork.ClientContext context) {
        Player player = context.player();
        Level level = context.level();
        if (player == null || level == null) return;
        if (!(player.containerMenu instanceof ContainerPneumoStorageAccess menu)) return;
        if (menu.containerId != payload.containerId) return;

        var buf = GolemBuffers.wrap(Unpooled.wrappedBuffer(payload.data), level);
        try {
            int size = buf.readVarInt();
            List<Delta> deltas = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int id = buf.readVarInt();
                ItemStack display = buf.readBoolean() ? ItemStacks.readOptional(buf) : null;
                deltas.add(new Delta(id, display, buf.readVarLong()));
            }
            menu.applyDeltas(deltas);
        } finally {
            buf.release();
        }
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
