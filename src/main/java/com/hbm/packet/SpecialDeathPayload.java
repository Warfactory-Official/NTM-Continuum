package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: 1.12.2 {@code PacketSpecialDeath}: an entity id, an effect id and a float array. Effect 0 is the gluon gun's
 * kill (the victim falls apart into its model's cubes), effect 1 a gluon hit (hurt flash and hurt sound), effect 3 the
 * cut mob ({@code nx, ny, nz, d, Float.intBitsToFloat(tex)}: the plane in the victim's space and the cap texture);
 * effect 4 (bullet gibs, with an extra hit list) is not sent by anything ported.
 */
public record SpecialDeathPayload(int entityId, int effectId, float[] auxData) implements GolemPayload {

    public static final int GLUON = 0;
    public static final int GLUON_HIT = 1;
    public static final int SLICED = 3;

    public static final GolemCodec<SpecialDeathPayload> CODEC = GolemCodec.of(SpecialDeathPayload.class,
            Ids.of(Constants.MOD_ID, "special_death"),
            (payload, buffer) -> {
                buffer.writeInt(payload.entityId);
                buffer.writeInt(payload.effectId);
                buffer.writeByte(payload.auxData.length);
                for (float f : payload.auxData) buffer.writeFloat(f);
            },
            buffer -> {
                int entityId = buffer.readInt();
                int effectId = buffer.readInt();
                int length = buffer.readByte();
                float[] aux = new float[length];
                for (int i = 0; i < length; i++) aux[i] = buffer.readFloat();
                return new SpecialDeathPayload(entityId, effectId, aux);
            });

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
