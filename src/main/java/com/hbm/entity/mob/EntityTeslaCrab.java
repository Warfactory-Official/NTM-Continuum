package com.hbm.entity.mob;

import com.hbm.particle.helper.EntityEffects;
import com.hbm.util.TeslaZap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class EntityTeslaCrab extends EntityCyberCrab {

    /** Arc endpoints, computed server-side and pushed to watchers by {@code EntityEffects.teslaArc}. */
    public List<Vec3> targets = new ArrayList<>();

    public EntityTeslaCrab(EntityType<? extends EntityTeslaCrab> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCyberCrab.createAttributes()
                             .add(Attributes.MAX_HEALTH, 10.0D)
                             .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.75F, 1.25F);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server) {
            List<Vec3> zapped = TeslaZap.zap(server, getX(), getY() + 1D, getZ(), 3D, this);
            // zap runs every tick; only a changed arc set is worth a packet
            if (!zapped.equals(targets)) {
                targets = zapped;
                EntityEffects.teslaArc(this, targets, 64);
            }
        }

        super.tick();
    }

}
