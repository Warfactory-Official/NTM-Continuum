package com.hbm.client;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Client-only side effects of the {@link EntityNukeTorex} simulation, kept out of the entity class so a
 * dedicated server never resolves {@code net.minecraft.client} symbols when it class-loads the entity.
 * Every method here is called only from the {@code level().isClientSide()} branch of the entity's tick.
 */
public final class TorexClientEffects {

    private TorexClientEffects() {}

    /** The vanilla lightning sky flash, as in the old {@code world.setLastLightningBolt(2)}. */
    public static void skyFlash(Level level) {
        if (level instanceof ClientLevel client) {
            client.setSkyFlashTime(2);
        }
    }

    /**
     * Plays the nuclear-explosion sound once the shockwave reaches the local player (old condition:
     * distance {@code < (ticksExisted * 1.5 + 1) * 1.5}). Volume 10,000 gives the huge audible range.
     * Returns true when it played, so the caller can latch {@code didPlaySound}.
     */
    public static boolean tryPlayDetonationSound(EntityNukeTorex torex) {
        Player me = Minecraft.getInstance().player;
        if (me == null) return false;
        if (me.distanceTo(torex) < (torex.tickCount * 1.5 + 1) * 1.5) {
            torex.level().playLocalSound(torex.getX(), torex.getY(), torex.getZ(),
                    ModSounds.NUCLEAR_EXPLOSION.get(), SoundSource.HOSTILE, 10_000F, 1F, false);
            return true;
        }
        return false;
    }

    /**
     * Stands in for the old {@code Biome.getRainfall()} that gated the Wilson-condensation rings. 26.1
     * hides the biome's {@code downfall} float behind a private nested record (no public getter, and a
     * Mixin accessor can't name the type / reflection isn't Fabric-production-safe), so we approximate
     * from the public {@link Biome#hasPrecipitation()}: humid (raining/snowing) biomes get the rings,
     * dry ones don't. A known divergence: the original scaled ring density by the exact 0..1 value.
     */
    public static float humidity(EntityNukeTorex torex) {
        BlockPos pos = torex.blockPosition();
        Biome biome = torex.level().getBiome(pos).value();
        return biome.hasPrecipitation() ? 0.5F : 0.0F;
    }
}
