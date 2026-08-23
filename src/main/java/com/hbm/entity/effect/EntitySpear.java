package com.hbm.entity.effect;

import com.hbm.capability.HbmLivingProps;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ContaminationUtil;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** DRX's descending digamma spear. Its server effects deliberately do not depend on a renderer. */
public class EntitySpear extends CompatEntity {
    public int ticksInGround;

    public EntitySpear(EntityType<EntitySpear> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(dev.golem.core.data.SynchedData data) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {
        xOld = getX();
        yOld = getY();
        zOld = getZ();
        int x = Mth.floor(getX()), y = Mth.floor(getY()), z = Mth.floor(getZ());
        if (level().getBlockState(new BlockPos(x, y - 1, z)).isAir()) {
            setPos(getX(), getY() - 0.2D, getZ());
            if (level() instanceof ServerLevel server) {
                double blastX = getX() + random.nextGaussian() * 25D;
                double blastZ = getZ() + random.nextGaussian() * 25D;
                double blastY = server.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(blastX), Mth.floor(blastZ)) + 2D;
                server.explode(this, blastX, blastY, blastZ, 7.5F, Level.ExplosionInteraction.NONE);
                for (net.minecraft.server.level.ServerPlayer player : server.players()) {
                    ContaminationUtil.contaminate(player, ContaminationUtil.HazardType.DIGAMMA,
                            ContaminationUtil.ContaminationType.DIGAMMA, 0.05D);
                }
            }
            if (level().getBlockState(new BlockPos(x, y - 3, z)).isAir()) ticksInGround = 0;
        } else {
            ticksInGround++;
            if (level() instanceof ServerLevel server && ticksInGround > 100) {
                java.util.List<Entity> entities = new java.util.ArrayList<>();
                server.getAllEntities().forEach(entities::add);
                for (Entity entity : entities) if (entity instanceof LivingEntity living) {
                    ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.DIGAMMA,
                            ContaminationUtil.ContaminationType.DIGAMMA2, 10D);
                    HbmLivingProps.setDigamma(living, 10D);
                    DamageSource source = ModDamageSources.digamma(server);
                    Compat.hurt(living, server, source, living.getMaxHealth());
                }
                discard();
            }
        }
    }

    @Override
    protected boolean onHurt(DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void loadData(dev.golem.core.data.DataIn input) {
        ticksInGround = input.getIntOr("ticksInGround", 0);
    }

    @Override
    protected void saveData(dev.golem.core.data.DataOut output) {
        output.putInt("ticksInGround", ticksInGround);
    }
}
