package com.hbm.entity.projectile;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class EntityChopperMine extends CompatEntity {

    public int timer = 0;
    // 1.7: plain field - neither synced nor persisted
    public Entity shooter;

    public EntityChopperMine(EntityType<? extends EntityChopperMine> type, Level level) {
        super(type, level);
    }

    public EntityChopperMine(Level level, double x, double y, double z, double moX, double moY, double moZ,
                             Entity shooter) {
        this(ModNtmEntities.CHOPPER_MINE.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(moX, moY, moZ);
        this.shooter = shooter;
    }

    @Override
    protected void defineData(SynchedData builder) {
        // 1.7: entityInit empty - no tracked data
    }

    // 26.2: hurt() final; not damageable in flight
    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        // 26.2: baseTick no longer rolls prev-position, manual move below needs it
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        if (!level().isClientSide()) {

            Vec3 from = position();
            Vec3 to = from.add(getDeltaMovement());

            // 1.7: world.rayTraceBlocks(from, to, stopOnLiquid=false, ignoreBB=true, returnLastUncollidable=false)
            BlockHitResult blockHit = level().clip(
                    new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            HitResult mop = blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
            if (mop != null) to = mop.getLocation();

            // 1.7: sweep the motion segment against nearby collidable boxes, nearest intercept wins
            Entity hit = null;
            List<Entity> list = level().getEntities(this,
                    this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D, 1.0D, 1.0D));
            double nearest = 0.0D;

            for (int i = 0; i < list.size(); ++i) {
                Entity candidate = list.get(i);

                // 26.2: 1.7's no-arg canBeCollidedWith(), which EntityLivingBase answered true, is isPickable();
                // the surviving canBeCollidedWith(Entity) is a collision-push test no LivingEntity opts into.
                if (candidate.isPickable() && candidate != this.shooter) {
                    float slack = 0.3F;
                    AABB box = candidate.getBoundingBox().inflate(slack, slack, slack);
                    Vec3 hitVec = box.clip(from, to).orElse(null);

                    if (hitVec != null) {
                        double dist = from.distanceTo(hitVec);

                        if (dist < nearest || nearest == 0.0D) {
                            hit = candidate;
                            nearest = dist;
                        }
                    }
                }
            }

            if (hit != null) mop = new EntityHitResult(hit);

            // 1.7: only an EntityPlayer impact triggers the payload early
            if (mop instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Player) {
                this.detonate();
            }

            level().playSound(null, getX(), getY(), getZ(), ModSounds.NULL_MINE.get(),
                    SoundSource.PLAYERS, 10.0F, 1F);

            if (this.timer >= 100 || !level().getBlockState(
                    new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
                this.detonate();
            }
        }

        Vec3 motion = getDeltaMovement();
        if (motion.y > -0.85D) motion = new Vec3(motion.x, motion.y - 0.05D, motion.z);
        motion = new Vec3(motion.x * 0.9D, motion.y, motion.z * 0.9D);
        this.setDeltaMovement(motion);

        // 1.7:135-137 assigns posX/Y/Z directly and never calls setPosition, so the AABB stays where the
        // ctor left it - at the world origin - and the proximity sweep at 1.7:80-103 never finds a player.
        // Deliberate divergence: setPos keeps the box on the mine, so proximity detonation works.
        // 1.7: raw integration, no collision resolution - flies through geometry until the air check fires
        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        this.timer++;
    }

    // 1.7: world.createExplosion(shooter, x, y, z, 5F, false) - isSmoking=false, no block destruction
    private void detonate() {
        level().explode(this.shooter, null, null, getX(), getY(), getZ(), 5F, false,
                Level.ExplosionInteraction.NONE);
        this.discard();
    }

    // 1.7: writeEntityToNBT/readEntityFromNBT empty - persists carrying no data
    @Override
    protected void loadData(DataIn input) {
    }

    @Override
    protected void saveData(DataOut output) {
    }
}
