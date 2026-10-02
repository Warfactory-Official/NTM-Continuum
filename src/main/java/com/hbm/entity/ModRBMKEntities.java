package com.hbm.entity;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.entity.effect.EntitySpear;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity registrations owned by the RBMK wave. */
public final class ModRBMKEntities {

    private ModRBMKEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.createEntityTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ENTITY_TYPES);

    public static final RegistryObject<EntityType<EntityRBMKDebris>> RBMK_DEBRIS =
            REG.entity("rbmk_debris", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntityRBMKDebris>of(EntityRBMKDebris::new, MobCategory.MISC))
                            .fireImmune()
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(8)
                             .updateInterval(2), key));

    public static final RegistryObject<EntityType<EntitySpear>> SPEAR =
            REG.entity("entity_spear", key -> Entities.build(
                    Entities.noLoot(EntityType.Builder.<EntitySpear>of(EntitySpear::new, MobCategory.MISC))
                            .fireImmune()
                            .sized(2.0F, 10.0F)
                            .clientTrackingRange(64)
                            .updateInterval(2), key));

    public static void init() {}
}
