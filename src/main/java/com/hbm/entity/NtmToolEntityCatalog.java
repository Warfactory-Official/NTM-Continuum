package com.hbm.entity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.entity.projectile.EntityCoin;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.Entities;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Entity registration owned by the specialized Sedna tool wave. */
public final class NtmToolEntityCatalog {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.createEntityTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ENTITY_TYPES);

    public static final RegistryObject<EntityType<EntityCoin>> COIN = REG.entity("entity_coin", key ->
            Entities.build(EntityType.Builder.<EntityCoin>of(EntityCoin::new, MobCategory.MISC)
                    .sized(1F, 1F).clientTrackingRange(1000).updateInterval(1), key));

    private NtmToolEntityCatalog() {}
    public static void init() {}
}
