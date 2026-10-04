package com.hbm.client;

import com.hbm.client.particle.FlameCreator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

/**
 * Client-only half of the flamer rounds' 1.7 {@code XFactoryFlamer.LAMBDA_FIRE} / {@code LAMBDA_BALEFIRE}: every
 * client tick a flame is composed an eighth of a block under the projectile while the local player is within 100
 * blocks. Kept out of the factory so a dedicated server never resolves client symbols.
 */
public final class FlamerTrailClientEffects {

    private FlamerTrailClientEffects() {}

    public static void trail(Entity bullet, int meta) {
        if (!(bullet.level() instanceof ClientLevel client)) return;
        var me = Minecraft.getInstance().player;
        if (me == null || me.distanceTo(bullet) >= 100) return;
        FlameCreator.composeEffectClient(client, bullet.getX(), bullet.getY() - 0.125, bullet.getZ(), meta);
    }
}
