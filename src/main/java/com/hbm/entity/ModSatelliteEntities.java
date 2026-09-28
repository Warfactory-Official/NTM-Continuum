package com.hbm.entity;

import com.hbm.Constants;
import com.hbm.entity.logic.EntityOrbitalLaser;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity registrations owned by the satellite family; 1.7 {@code EntityMappings}. */
public final class ModSatelliteEntities {

    private ModSatelliteEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Constants.MOD_ID);

    /** 1.7 {@code addEntity(EntityOrbitalLaser.class, "entity_orbital_laser", 1000)}. */
    public static final RegistryObject<EntityType<EntityOrbitalLaser>> ORBITAL_LASER =
            ENTITY_TYPES.register("entity_orbital_laser", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityOrbitalLaser>of(EntityOrbitalLaser::new, MobCategory.MISC))
                            .fireImmune().sized(0.6F, 1.8F).clientTrackingRange(63).updateInterval(1), key));

    public static void init() {}
}
