package com.hbm.particle;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/** Common particle-type declarations. Client providers are installed separately. */
public final class HbmParticles {
    private HbmParticles() {}

    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Constants.MOD_ID);
    // The original effect is deliberately uncapped: its 250-block broadcast outlives vanilla's normal
    // particle limiter and the cloud is the visible indication of an RBMK melt.
    public static final RegistryObject<SimpleParticleType> RBMK_MUSH =
            PARTICLES.register("rbmk_mush", key -> new SimpleParticleType(true) {});

    /** Class-load hook for the parent bootstrap, alongside the other deferred registrars. */
    public static void init() {}
}
