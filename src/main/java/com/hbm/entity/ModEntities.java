package com.hbm.entity;

import com.hbm.Constants;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.logic.EntityEMP;
import com.hbm.entity.projectile.EntityZirnoxDebris;
import com.hbm.entity.projectile.EntitySawblade;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registry for HBM entity types. Entity types are built purely in common: {@code EntityType.Builder}
 * and {@code build(ResourceKey)} are vanilla/public, so no platform service is needed for registration
 * (only the client-side renderer registration is per-loader — see {@code HbmEntityRenderers}).
 *
 * <p>The {@link DeferredRegister#register(String, java.util.function.Function)} form hands the factory
 * the entry's {@link ResourceKey}, which {@code build} requires.
 */
public final class ModEntities {

    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Constants.MOD_ID);

    /** The invisible logic entity driving a Fat Man / mk5 nuclear blast. */
    public static final RegistryObject<EntityType<EntityNukeExplosionMK5>> NUKE_EXPLOSION_MK5 =
            ENTITY_TYPES.register("entity_nuke_mk5", key -> dev.golem.core.registry.Entities.build(
                    EntityType.Builder.of(EntityNukeExplosionMK5::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(8)
                            .updateInterval(Integer.MAX_VALUE), key));

    /** Invisible six-minute EMP field; its HBM-energy drain runs server-side. */
    public static final RegistryObject<EntityType<EntityEMP>> ENTITY_EMP =
            ENTITY_TYPES.register("entity_emp", key -> dev.golem.core.registry.Entities.build(
                    EntityType.Builder.<EntityEMP>of(EntityEMP::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(64)
                            .updateInterval(20), key));

    /** The invisible fallout sweep spawned when the crater completes (old {@code entity_fallout_rain}). */
    public static final RegistryObject<EntityType<com.hbm.entity.effect.EntityFalloutRain>> FALLOUT_RAIN =
            ENTITY_TYPES.register("entity_fallout_rain", key -> dev.golem.core.registry.Entities.build(
                    EntityType.Builder.of(com.hbm.entity.effect.EntityFalloutRain::new, MobCategory.MISC)
                            .sized(4.0F, 20.0F)
                            .clientTrackingRange(8)
                            .updateInterval(Integer.MAX_VALUE), key));

    /**
     * The toroidal-convection mushroom cloud spawned alongside a nuke blast (old {@code entity_effect_torex}).
     * A 20×40 fire-immune effect entity whose simulation runs client-side. {@code clientTrackingRange(64)}
     * (≈1000 blocks in the original, clamped to the server view distance); {@code updateInterval(20)} — the
     * client simulates cloudlets locally, so it only needs the SCALE/TYPE synched data occasionally.
     */
    public static final RegistryObject<EntityType<com.hbm.entity.effect.EntityNukeTorex>> NUKE_TOREX =
            ENTITY_TYPES.register("entity_effect_torex", key -> dev.golem.core.registry.Entities.build(
                    EntityType.Builder.of(com.hbm.entity.effect.EntityNukeTorex::new, MobCategory.MISC)
                            .sized(20.0F, 40.0F)
                            .fireImmune()
                            .clientTrackingRange(64)
                            .updateInterval(20), key));

    /** Collectable debris thrown from a ZIRNOX reactor meltdown. */
    public static final RegistryObject<EntityType<EntityZirnoxDebris>> ZIRNOX_DEBRIS =
            ENTITY_TYPES.register("entity_zirnox_debris", key -> dev.golem.core.registry.Entities.build(
                    EntityType.Builder.<EntityZirnoxDebris>of(EntityZirnoxDebris::new, MobCategory.MISC)
                            .sized(1.0F, 0.5F)
                            .clientTrackingRange(64)
                            .updateInterval(1), key));

    public static final RegistryObject<EntityType<EntitySawblade>> SAWBLADE =
            ENTITY_TYPES.register("entity_sawblade", key -> dev.golem.core.registry.Entities.build(
                    EntityType.Builder.<EntitySawblade>of(EntitySawblade::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1), key));

    /**
     * A bullet in flight. {@code updateInterval(1)} because a round covers its whole velocity in one
     * tick and often lives only a handful of them — anything slower and the client never sees it move.
     */
    public static final RegistryObject<EntityType<com.hbm.entity.projectile.EntityBulletBaseMK4>> BULLET_MK4 =
            ENTITY_TYPES.register("entity_bullet_mk4", key -> dev.golem.core.registry.Entities.build(
                    EntityType.Builder.<com.hbm.entity.projectile.EntityBulletBaseMK4>of(
                                    com.hbm.entity.projectile.EntityBulletBaseMK4::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .fireImmune()
                            .clientTrackingRange(16)
                            .updateInterval(1), key));

    public static void init() {}
}
