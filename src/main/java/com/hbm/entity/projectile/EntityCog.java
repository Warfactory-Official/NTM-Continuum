package com.hbm.entity.projectile;

import com.hbm.entity.ModNtmEntities;
import com.hbm.items.ModGearItems;
import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import com.hbm.items.EnumGearType;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The gear a broken Stirling engine throws: it bounces and blasts while fast, then LANDS - the orientation
 * gains 6, which is what stops the renderer spinning it and what {@code onUpdate} takes back off if it is ever
 * airborne again.
 */
public class EntityCog extends EntityThrowableInterp {

    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(EntityCog.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> META =
            SynchedEntityData.defineId(EntityCog.class, EntityDataSerializers.INT);

    /**
     * 1.7: below this the gear stops bouncing and settles instead.
     */
    private static final double SETTLE_SPEED = 0.75D;
    private static final int LANDED = 6;
    /**
     * 1.7: anything softer than this is punched out by the bounce.
     */
    private static final float BREAKABLE_RESISTANCE = 50F;

    public EntityCog(EntityType<? extends EntityCog> type, Level level) {
        super(type, level);
    }

    public EntityCog(Level level, double x, double y, double z) {
        this(ModNtmEntities.COG.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(ORIENTATION, 0);
        builder.define(META, 0);
    }

    public EntityCog setOrientation(int rot) {
        entityData.set(ORIENTATION, rot);
        return this;
    }

    public EntityCog setMeta(int meta) {
        entityData.set(META, meta);
        return this;
    }

    public int getOrientation() {
        return entityData.get(ORIENTATION);
    }

    public int getMeta() {
        return entityData.get(META);
    }

    // 1.7 interactFirst: the gear goes back in the pocket
    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            player.getInventory().placeItemBackInInventory(ModGearItems.gear_large.get().of(getMeta()));
            discard();
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (level() instanceof ServerLevel server && mop instanceof EntityHitResult hit
                && hit.getEntity().isAlive()) {
            Entity e = hit.getEntity();
            Compat.hurt(e, server, ModDamageSources.source(level(), ModDamageSources.RUBBLE), 1000F);
            if (!e.isAlive() && e instanceof LivingEntity living) {
                ParticleCreators.giblets(server, living, ParticleGiblet.TYPE_MEAT, 5);
                server.playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F,
                        0.95F + random.nextFloat() * 0.2F);
            }
        }

        if (tickCount > 1 && level() instanceof ServerLevel server && mop instanceof BlockHitResult hit) {

            int orientation = entityData.get(ORIENTATION);

            if (orientation < LANDED) {

                if (getDeltaMovement().length() < SETTLE_SPEED) {
                    entityData.set(ORIENTATION, orientation + LANDED);
                    orientation += LANDED;
                } else {
                    Direction side = hit.getDirection();
                    Vec3 motion = getDeltaMovement();
                    setDeltaMovement(
                            motion.x * (1 - Math.abs(side.getStepX()) * 2),
                            motion.y * (1 - Math.abs(side.getStepY()) * 2),
                            motion.z * (1 - Math.abs(side.getStepZ()) * 2));
                    server.explode(this, null, null, getX(), getY(), getZ(), 3F, false,
                            Level.ExplosionInteraction.NONE);

                    if (server.getBlockState(hit.getBlockPos()).getBlock().getExplosionResistance()
                            < BREAKABLE_RESISTANCE) {
                        server.destroyBlock(hit.getBlockPos(), false);
                    }
                }
            }

            // 1.7 sets the flag itself rather than calling getStuck, so no stuck block is recorded and the
            // next tick un-grounds it again; the pair is what makes a settled gear jitter in place.
            if (orientation >= LANDED) {
                setDeltaMovement(Vec3.ZERO);
                inGround = true;
            }
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            int orientation = entityData.get(ORIENTATION);
            if (orientation >= LANDED && !inGround) entityData.set(ORIENTATION, orientation - LANDED);
        }
        super.tick();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public double getGravityVelocity() {
        return inGround ? 0 : 0.03D;
    }

    // 1.7: a settled gear waits forever
    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("rot", getOrientation());
        output.putInt("meta", getMeta());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        setOrientation(input.getIntOr("rot", 0));
        setMeta(input.getIntOr("meta", 0));
    }
}
