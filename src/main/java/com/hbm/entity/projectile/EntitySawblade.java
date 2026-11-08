package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Protection;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The blade an overspeeding sawmill throws: it bounces and blasts while fast, then lands. Landing adds 6 to the
 * orientation, which stops the renderer spinning it; once airborne again it loses the 6.
 */
public class EntitySawblade extends EntityThrowableInterp {

    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(EntitySawblade.class, EntityDataSerializers.INT);

    /** 1.7: below this the blade stops bouncing and settles instead. */
    private static final double SETTLE_SPEED = 0.75D;
    public static final int LANDED = 6;
    /** 1.7: anything softer than this is punched out by the bounce. */
    private static final float BREAKABLE_RESISTANCE = 50F;

    public EntitySawblade(EntityType<? extends EntitySawblade> type, Level level) {
        super(type, level);
    }

    public EntitySawblade(Level level, double x, double y, double z) {
        this(ModEntities.SAWBLADE.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(ORIENTATION, 0);
    }

    public EntitySawblade setOrientation(int rot) {
        entityData.set(ORIENTATION, rot);
        return this;
    }

    public int getOrientation() {
        return entityData.get(ORIENTATION);
    }

    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide() && player.getInventory().add(new ItemStack(ModItems.sawblade.get()))) discard();
        return InteractionResult.TRY_WITH_EMPTY_HAND;
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
                            < BREAKABLE_RESISTANCE && Protection.mayAlter(server, hit.getBlockPos(),
                            Protection.Source.of(this), Protection.Cause.EXPLOSION)) {
                        server.destroyBlock(hit.getBlockPos(), false);
                    }
                }
            }

            // 1.7 sets the flag rather than calling getStuck, so the next tick un-grounds it again (see EntityCog)
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

    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("rot", getOrientation());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        // older saves name the orientation "orientation"
        setOrientation(input.getIntOr("rot", input.getIntOr("orientation", 0)));
    }
}
