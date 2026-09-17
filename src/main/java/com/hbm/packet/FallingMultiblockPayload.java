package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.entity.item.EntityFallingMultiblock;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * S2C: member list and carried core block entity of an {@link EntityFallingMultiblock}; entity-data
 * serializers cannot carry it (ntm-next's payload of the same name, on Golem's codec).
 */
public record FallingMultiblockPayload(int entityId, long corePos, long[] members, @Nullable CompoundTag carried)
        implements GolemPayload {

    public static final GolemCodec<FallingMultiblockPayload> CODEC = GolemCodec.of(FallingMultiblockPayload.class,
            Ids.of(Constants.MOD_ID, "falling_multiblock"),
            (payload, buffer) -> {
                buffer.writeVarInt(payload.entityId);
                buffer.writeLong(payload.corePos);
                buffer.writeVarInt(payload.members.length);
                for (long member : payload.members) buffer.writeLong(member);
                buffer.writeBoolean(payload.carried != null);
                if (payload.carried != null) buffer.writeNbt(payload.carried);
            },
            buffer -> {
                int entityId = buffer.readVarInt();
                long corePos = buffer.readLong();
                long[] members = new long[buffer.readVarInt()];
                for (int i = 0; i < members.length; i++) members[i] = buffer.readLong();
                CompoundTag carried = buffer.readBoolean() ? buffer.readNbt() : null;
                return new FallingMultiblockPayload(entityId, corePos, members, carried);
            });

    /** Sends the entity's members to everyone tracking the chunk it is in. */
    public static void send(EntityFallingMultiblock entity, long corePos, long[] members, @Nullable CompoundTag carried) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        GolemNetwork.sendToTracking(level, entity.blockPosition(), CODEC,
                new FallingMultiblockPayload(entity.getId(), corePos, members, carried));
    }

    public static void handleClient(Level level, FallingMultiblockPayload payload) {
        Entity entity = level.getEntity(payload.entityId);
        if (entity instanceof EntityFallingMultiblock falling) {
            falling.setMembersFromNetwork(payload.members, payload.corePos, payload.carried);
        }
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
