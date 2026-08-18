package com.hbm.client.particle;

import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A small blast: a knot of fireballs plus a spray of debris torn from whatever solid block is
 * adjacent to the blast point.
 *
 * <p>Ported from 1.12.2 {@code ExplosionSmallCreator}. The debris texture is picked by probing the
 * six neighbours for the first non-air block, exactly as upstream does — so a blast in open air
 * throws no debris at all, which is deliberate rather than a bug.
 *
 * <p><b>Not ported: the delayed report.</b> Upstream plays {@code explosionSmallNear}/{@code Far}
 * scheduled by {@code distance / speedOfSound}, so distant blasts arrive late — a genuinely nice
 * touch. Both sound events are unported, so the hook is left out rather than substituted with a
 * vanilla boom that would sound wrong and then need un-picking later.
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
}
