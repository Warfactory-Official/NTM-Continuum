package com.hbm.entity;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import com.hbm.entity.logic.EntityOrbitalLaser;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity registrations owned by the satellite family; 1.7 {@code EntityMappings}. */
public final class ModSatelliteEntities {

    private ModSatelliteEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.createEntityTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ENTITY_TYPES);

    /** 1.7 {@code addEntity(EntityOrbitalLaser.class, "entity_orbital_laser", 1000)}. */
    public static final RegistryObject<EntityType<EntityOrbitalLaser>> ORBITAL_LASER =
            REG.entity("entity_orbital_laser", () -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityOrbitalLaser>of(EntityOrbitalLaser::new, MobCategory.MISC))
                            .fireImmune().sized(0.6F, 1.8F).clientTrackingRange(63).updateInterval(1), key("entity_orbital_laser")));

    /** The entry's registry key, as {@code DeferredRegister#register(name, key -> ...)} handed it to the factory. */
    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Ids.of(Constants.MOD_ID, name));
    }

    public static void init() {}
}
