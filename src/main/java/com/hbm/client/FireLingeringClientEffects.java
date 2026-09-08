package com.hbm.client;

import com.hbm.client.particle.FlameCreator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

/**
 * Client-only flames for {@link com.hbm.entity.effect.EntityFireLingering}, kept out of the entity so a
 * dedicated server never resolves {@code net.minecraft.client} symbols when it class-loads it. Each
 * client raises its own flames from the entity's own tick, so the fire costs no packets.
 */
public final class FireLingeringClientEffects {

    private FireLingeringClientEffects() {}

    public static void flame(Level level, double x, double y, double z, int meta) {
        if (level instanceof ClientLevel client) FlameCreator.composeEffectClient(client, x, y, z, meta);
    }
}
