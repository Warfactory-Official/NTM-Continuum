package com.hbm.client.particle;

import com.hbm.sound.ModSounds;
import dev.golem.core.client.Sounds;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A small blast: a knot of fireballs plus a spray of debris torn from whatever solid block is
 * adjacent to the blast point.
 *
 * <p>Ported from 1.12.2 {@code ExplosionSmallCreator}. The debris texture is picked by probing the
 * six neighbours for the first non-air block, as upstream does, so a blast in open air
 * throws no debris.
 *
 * <p>The near/far report is scheduled by {@code distance / speedOfSound}, keeping the delayed
 * sound arrival of the original.
 */
public class ExplosionSmallCreator implements IParticleCreator {

    /** Metres per tick, from upstream: 343 m/s over 20 ticks, halved to taste. */
    public static final double speedOfSound = 17.15D * 0.5;

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
            double x, double y, double z, CompoundTag data) {

        int cloudCount = Tags.getIntOr(data, "cloudCount", 0);
        float cloudScale = Tags.getFloatOr(data, "cloudScale", 1F);
        float cloudSpeedMult = Tags.getFloatOr(data, "cloudSpeedMult", 0F);
        int debris = Tags.getIntOr(data, "debris", 0);

        if (player != null) {
            float distance = (float) Math.sqrt(player.distanceToSqr(x, y, z));
            if (distance < 200F) {
                var sound = distance < 80F ? ModSounds.EXPLOSION_SMALL_NEAR.get()
                        : ModSounds.EXPLOSION_SMALL_FAR.get();
                Sounds.playDelayed(
                        new PositionalSound(sound, x, y, z), (int) (distance / speedOfSound));
            }
        }

        for (int i = 0; i < cloudCount; i++) {
            Particles.add(new ParticleExplosionSmall(level, x, y, z, cloudScale, cloudSpeedMult));
        }

        BlockState state = null;
        for (Direction dir : Direction.values()) {
            BlockPos pos = BlockPos.containing(x, y, z).relative(dir);
            BlockState candidate = level.getBlockState(pos);
            if (!candidate.isAir()) {
                state = candidate;
                break;
            }
        }

        if (state == null) return;

        for (int i = 0; i < debris; i++) {
            ParticleLargeBlockDebris fx = new ParticleLargeBlockDebris(level,
                    x, y + 0.1, z,
                    rand.nextGaussian() * 0.2,
                    0.5F + rand.nextDouble() * 0.7,
                    rand.nextGaussian() * 0.2,
                    state,
                    50 + rand.nextInt(20));
            fx.scale(2);
            Particles.add(fx);
        }
    }

    /** Shared with {@link ExplosionLargeCreator}: a fixed point in the world, played on arrival. */
    static final class PositionalSound extends AbstractSoundInstance {
        PositionalSound(SoundEvent sound, double x, double y, double z) {
            super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.x = x;
            this.y = y;
            this.z = z;
            this.volume = 100F;
            this.pitch = 1F;
        }
    }
}
