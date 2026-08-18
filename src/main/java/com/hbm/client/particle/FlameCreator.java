package com.hbm.client.particle;

import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * The flamethrower's burning cone. One particle per call — the emitter spams it.
 *
 * <p>Ported from 1.12.2 {@code FlameCreator}. The {@code meta} selects the fuel, which only changes
 * the flame's colour band (see {@link ParticleFlamethrower}).
 */
public class FlameCreator implements IParticleCreator {

    public static final int META_NORMAL = 0;
    public static final int META_BALEFIRE = 1;
    public static final int META_DIGAMMA = 2;

    /**
     * Client-side: play it locally without a round trip. Upstream keeps this separate because the
     * flamethrower's own holder already knows it fired and does not need to be told by the server.
     */
    public static void composeEffectClient(ClientLevel level, double x, double y, double z, int meta) {
        CompoundTag data = new CompoundTag();
        data.putInt("meta", meta);
        HbmEffectNT.Flamethrower.summonParticle(level, x, y, z, data);
    }

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
            double x, double y, double z, CompoundTag data) {
        Particles.add(new ParticleFlamethrower(level, x, y, z, Tags.getIntOr(data, "meta", META_NORMAL)));
    }
}
