package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import com.hbm.entity.ModEntities;
import com.hbm.items.ModItems;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The sawblade thrown when a sawmill runs above its safe heat limit. */
public class EntitySawblade extends CompatEntity {

    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(EntitySawblade.class, EntityDataSerializers.INT);
    private boolean stuck;
    public float rotation;
    public float lastRotation;

    public EntitySawblade(EntityType<EntitySawblade> type, Level level) {
        super(type, level);
    }

    public EntitySawblade(Level level, double x, double y, double z, int orientation) {
        this(ModEntities.SAWBLADE.get(), level);
        setPos(x, y, z);
        setOrientation(orientation);
    }

    @Override
    protected void defineData(dev.golem.core.data.SynchedData data) {
        data.define(ORIENTATION, 0);
    }

    public int getOrientation() {
        return entityData.get(ORIENTATION);
    }

    public void setOrientation(int orientation) {
        entityData.set(ORIENTATION, orientation);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        lastRotation = rotation;
        if (stuck) return;

        Vec3 velocity = getDeltaMovement().add(0D, -0.03D, 0D);
        move(MoverType.SELF, velocity);
        setDeltaMovement(velocity);
        rotation += 18F;
        if (rotation >= 360F) rotation -= 360F;

        if (!level().isClientSide()) {
            for (Entity entity : level().getEntities(this, getBoundingBox().inflate(0.25D), Entity::isPickable)) {
                if (level() instanceof ServerLevel server
                        && Compat.hurt(entity, server, server.damageSources().generic(), 1000F)) discard();
            }
            if (horizontalCollision || verticalCollision) {
                stuck = true;
                setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    @Override
    protected InteractionResult onInteract(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.sawblade.get()))) discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean onHurt(DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void loadData(DataIn input) {
        setOrientation(input.getIntOr("orientation", 0));
        stuck = input.getBooleanOr("stuck", false);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("orientation", getOrientation());
        output.putBoolean("stuck", stuck);
    }
}
