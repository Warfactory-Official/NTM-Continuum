package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Builds the particles for one effect from the tag that came off the wire.
 *
 * <p>Client only. The 1.12.2 signature also took a {@code TextureManager} so particles could bind their own
 * texture; sprites come off a stitched atlas ({@link dev.golem.core.render.Particles#sprite}), so that parameter
 * is dropped.
 *
 * <p>{@code player} is the local player. Several effects are positioned or scaled relative to the
 * viewer (first-person casings, screen-space flashes) and upstream passes it for exactly that.
 */
@FunctionalInterface
public interface IParticleCreator {

    void makeParticle(ClientLevel level, Player player, RandomSource rand,
            double x, double y, double z, CompoundTag data);
}
