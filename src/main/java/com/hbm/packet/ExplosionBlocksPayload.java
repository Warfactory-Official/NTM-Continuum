package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.world.SubLevels;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code ExplosionVanillaNewTechnologyCompressedAffectedBlockPositionDataForClientEffectsAndParticleHandlingPacket}:
 * the centre as floats and each block as three signed bytes from the truncated centre, so offsets past 127 wrap
 * as they did in 1.7.
 */
public record ExplosionBlocksPayload(double x, double y, double z, float size, List<BlockPos> blocks) implements GolemPayload {

    public static final double RANGE = 250D;
    // port: vanilla drops a clientbound custom payload over 1 MiB; 1.7 sent the whole list
    private static final int MAX_BLOCKS = ((1 << 20) - 64) / 3;

    public static final GolemCodec<ExplosionBlocksPayload> CODEC = GolemCodec.of(ExplosionBlocksPayload.class,
            Ids.of(Constants.MOD_ID, "explosion_blocks"),
            (payload, buffer) -> {
                buffer.writeFloat((float) payload.x);
                buffer.writeFloat((float) payload.y);
                buffer.writeFloat((float) payload.z);
                buffer.writeFloat(payload.size);
                buffer.writeInt(payload.blocks.size());
                int i = (int) payload.x;
                int j = (int) payload.y;
                int k = (int) payload.z;
                for (BlockPos pos : payload.blocks) {
                    buffer.writeByte(pos.getX() - i);
                    buffer.writeByte(pos.getY() - j);
                    buffer.writeByte(pos.getZ() - k);
                }
            },
            buffer -> {
                double x = buffer.readFloat();
                double y = buffer.readFloat();
                double z = buffer.readFloat();
                float size = buffer.readFloat();
                int count = buffer.readInt();
                List<BlockPos> blocks = new ArrayList<>(count);
                int i = (int) x;
                int j = (int) y;
                int k = (int) z;
                for (int n = 0; n < count; n++) {
                    blocks.add(new BlockPos(buffer.readByte() + i, buffer.readByte() + j, buffer.readByte() + k));
                }
                return new ExplosionBlocksPayload(x, y, z, size, blocks);
            });

    public static void send(ServerLevel level, double x, double y, double z, float size, List<BlockPos> blocks) {
        List<BlockPos> sent = blocks.size() > MAX_BLOCKS ? blocks.subList(0, MAX_BLOCKS) : blocks;
        ExplosionBlocksPayload payload = new ExplosionBlocksPayload(x, y, z, size, sent);
        Vec3 centre = new Vec3(x, y, z);
        for (ServerPlayer player : level.players()) {
            if (SubLevels.distanceSq(level, centre, player.position()) <= RANGE * RANGE) {
                GolemNetwork.sendToPlayer(player, CODEC, payload);
            }
        }
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
