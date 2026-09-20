package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The original B92 beam: a fast, short-lived explosive projectile rather than a Sedna bullet. */
public final class EntityB92Beam extends CompatEntity {

    private int xTile = -1;
    private int yTile = -1;
    private int zTile = -1;
    private int ticksInAir;
    private double damage = 2.0D;
    public Entity shootingEntity;

    public EntityB92Beam(EntityType<? extends EntityB92Beam> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public EntityB92Beam(Level level, LivingEntity shooter, float velocity) {
        this(com.hbm.entity.ModEntities.BEAM_BOMB.get(), level);
        this.shootingEntity = shooter;
        setPos(shooter.getX(), shooter.getY() + shooter.getEyeHeight(), shooter.getZ());
        setPos(getX() - net.minecraft.util.Mth.cos(shooter.getYRot() / 180F * (float) Math.PI) * 0.16F,
                getY() - 0.1D,
                getZ() - net.minecraft.util.Mth.sin(shooter.getYRot() / 180F * (float) Math.PI) * 0.16F);

        double motionX = -net.minecraft.util.Mth.sin(shooter.getYRot() / 180F * (float) Math.PI)
                * net.minecraft.util.Mth.cos(shooter.getXRot() / 180F * (float) Math.PI);
        double motionZ = net.minecraft.util.Mth.cos(shooter.getYRot() / 180F * (float) Math.PI)
                * net.minecraft.util.Mth.cos(shooter.getXRot() / 180F * (float) Math.PI);
        double motionY = -net.minecraft.util.Mth.sin(shooter.getXRot() / 180F * (float) Math.PI);
        setThrowableHeading(motionX, motionY, motionZ, velocity * 1.5F, 1.0F);
    }

    @Override
    protected void defineData(SynchedData data) {}

    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        x /= length;
        y /= length;
        z /= length;
        x += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
        y += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
        z += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.002499999832361937D * inaccuracy;
        setDeltaMovement(x * velocity, y * velocity, z * velocity);
        Vec3 motion = getDeltaMovement();
        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        setYRot((float) (Math.atan2(motion.x, motion.z) * 180D / Math.PI));
        setXRot((float) (Math.atan2(motion.y, horizontal) * 180D / Math.PI));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100) {
            discard();
            return;
        }

        if (xTile != -1 || yTile != -1 || zTile != -1) {
            BlockState state = level().getBlockState(new BlockPos(xTile, yTile, zTile));
            if (!state.isAir()) {
                discard();
                explode();
                return;
            }
        }

        ++ticksInAir;
        Vec3 from = position();
        Vec3 motion = getDeltaMovement();
        Vec3 to = from.add(motion);
        HitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.MISS) hit = null;
        if (hit != null) to = hit.getLocation();

        Entity entity = null;
        double nearest = 0D;
        AABB search = getBoundingBox().expandTowards(motion.x, motion.y, motion.z).inflate(1D);
        List<Entity> entities = level().getEntities(this, search);
        for (Entity candidate : entities) {
            if (!candidate.isPickable() || (candidate != shootingEntity && ticksInAir < 5)) continue;
            var intercept = candidate.getBoundingBox().inflate(0.3D).clip(from, to);
            if (intercept.isPresent()) {
                double distance = from.distanceTo(intercept.get());
                if (entity == null || distance < nearest) {
                    entity = candidate;
                    nearest = distance;
                }
            }
        }
        if (entity != null) hit = new EntityHitResult(entity, to);

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Player player
                && (player.getAbilities().invulnerable || shootingEntity instanceof Player shooter
                && !shooter.canHarmPlayer(player))) {
            hit = null;
        }

        if (hit != null) {
            if (hit.getType() == HitResult.Type.ENTITY) {
                explode();
                discard();
                return;
            }
            if (hit instanceof BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();
                xTile = pos.getX();
                yTile = pos.getY();
                zTile = pos.getZ();
            }
        }

        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setYRot((float) (Math.atan2(motion.x, motion.z) * 180D / Math.PI));
        if (isInWater()) {
            discard();
            explode();
        } else if (isInWaterOrRain()) {
            extinguishFire();
        }
    }

    private void explode() {
        if (level().isClientSide()) return;
        EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFac(level(), 10, getX(), getY(), getZ());
        if (level().addFreshEntity(blast)) blast.startChunkLoading();
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 100.0F, level().getRandom().nextFloat() * 0.1F + 0.9F);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("xTile", xTile);
        output.putInt("yTile", yTile);
        output.putInt("zTile", zTile);
        output.putDouble("damage", damage);
    }

    @Override
    protected void loadData(DataIn input) {
        xTile = input.getIntOr("xTile", -1);
        yTile = input.getIntOr("yTile", -1);
        zTile = input.getIntOr("zTile", -1);
        damage = input.getDoubleOr("damage", 2.0D);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = getBoundingBox().getSize() * 10D * 64D;
        return distance < range * range;
    }
}
