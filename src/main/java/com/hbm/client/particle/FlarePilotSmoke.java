package com.hbm.client.particle;

import dev.golem.core.render.Particles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

/** 1.7 {@code TileEntityMachineGasFlare}'s burning pilot: one noclip vanilla smoke a tick, 50 ticks, within 32 blocks. */
public final class FlarePilotSmoke {

    private FlarePilotSmoke() {}

    public static void tick(Level level, BlockPos pos) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null || me.distanceToSqr(pos.getX(), pos.getY() + 10, pos.getZ()) > 1024) return;
        boolean even = level.getGameTime() % 2 == 0;
        Particle smoke = Particles.spawn(ParticleTypes.SMOKE, pos.getX() + (even ? 1.5 : 1.125),
                pos.getY() + (even ? 10.75 : 11.75), pos.getZ() + (even ? 1.5 : -0.5), 0, 0, 0);
        if (smoke == null) return;
        Particles.noPhysics(smoke);
        smoke.setLifetime(50);
    }
}
