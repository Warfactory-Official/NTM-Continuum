package com.hbm.client.particle;

import com.hbm.packet.PlasmaBlastPayload;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

/** Client half of {@link PlasmaBlastPayload}: the shockwave particle, kept out of the common packet class. */
public final class PlasmaBlastClient {

    private PlasmaBlastClient() {}

    public static void handle(Level level, PlasmaBlastPayload payload) {
        if (!(level instanceof ClientLevel client)) return;
        Particles.add(new ParticlePlasmaBlast(client,
                payload.x(), payload.y(), payload.z(), payload.r(), payload.g(), payload.b(),
                payload.pitch(), payload.yaw(), payload.scale()));
    }
}
