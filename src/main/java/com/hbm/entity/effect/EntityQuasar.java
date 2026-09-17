package com.hbm.entity.effect;

import com.hbm.entity.ModNtmEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code EntityQuasar}: a black hole spawned by dropped digamma items (1.7 ItemDigamma, 5F size).
 */
public class EntityQuasar extends EntityBlackHole {

    public EntityQuasar(EntityType<? extends EntityQuasar> type, Level level) {
        super(type, level);
        // 1.7: ignoreFrustumCheck = true - frustum culling is the renderer's call in this port
    }

    public EntityQuasar(Level level, float size) {
        this(ModNtmEntities.DIGAMMA_QUASAR.get(), level);
        setSize(size);
    }

    // 1.7: isImmuneToFire = true
    @Override
    public boolean fireImmune() {
        return true;
    }

}
