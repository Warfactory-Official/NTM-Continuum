package com.hbm.entity.explosive;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity types owned by the conventional-explosive feature. */
public final class ModExplosiveEntities {

    private ModExplosiveEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<EntityType<EntityPrimedExplosive>> PRIMED_EXPLOSIVE =
            ENTITY_TYPES.register("entity_ntm_tnt_primed", key -> dev.golem.core.registry.Entities.build(
                    dev.golem.core.registry.Entities.trackDeltas(
                            EntityType.Builder.<EntityPrimedExplosive>of(EntityPrimedExplosive::new, MobCategory.MISC)
                                    .fireImmune()
                                    .sized(0.98F, 0.98F)
                                    .clientTrackingRange(10)
                                    .updateInterval(10)), key));

    public static void init() {}
}
