package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Physical debris ejected by a ZIRNOX reactor meltdown. */
public class EntityZirnoxDebris extends Entity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityZirnoxDebris.class, EntityDataSerializers.INT);
    public float rot;
    public float lastRot;

    public EntityZirnoxDebris(EntityType<EntityZirnoxDebris> type, Level level) {
        super(type, level);
        this.rot = this.lastRot = this.random.nextFloat() * 360.0F;
    }

    public EntityZirnoxDebris(Level level, double x, double y, double z, DebrisType type) {
        this(ModEntities.ZIRNOX_DEBRIS.get(), level);
        setPos(x, y, z);
        setDebrisType(type);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, DebrisType.BLANK.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setDebrisType(DebrisType.byId(input.getIntOr("type", DebrisType.BLANK.ordinal())));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("type", entityData.get(TYPE));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return getDebrisType().dimensions;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (TYPE.equals(key)) refreshDimensions();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPosition) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = new ItemStack(getDebrisType().item.get());
        if (player.getInventory().add(stack)) discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            if (getDebrisType() == DebrisType.ELEMENT) {
                ChunkRadiationManager.proxy.incrementRad(level(), blockPosition(), 20.0D, 150.0D);
            }
            if (tickCount > getDebrisType().lifetime + getId() % 50) {
                discard();
                return;
            }
        }

        Vec3 velocity = getDeltaMovement().add(0.0D, -0.04D, 0.0D);
        move(MoverType.SELF, velocity);
        lastRot = rot;
        if (onGround()) {
            velocity = new Vec3(velocity.x * 0.85D, velocity.y * -0.5D, velocity.z * 0.85D);
        } else {
            rot += 10.0F;
            if (rot >= 360.0F) {
                rot -= 360.0F;
                lastRot -= 360.0F;
            }
        }
        setDeltaMovement(velocity);
    }

    public DebrisType getDebrisType() {
        return DebrisType.byId(entityData.get(TYPE));
    }

    public void setDebrisType(DebrisType type) {
        entityData.set(TYPE, type.ordinal());
        refreshDimensions();
    }

    public enum DebrisType {
        BLANK(ModItems.debris_metal, 3 * 60 * 20, 0.5F, 0.5F),
        ELEMENT(ModItems.debris_element, 10 * 60 * 20, 0.75F, 0.5F),
        SHRAPNEL(ModItems.debris_shrapnel, 15 * 60 * 20, 0.5F, 0.5F),
        GRAPHITE(ModItems.debris_graphite, 15 * 60 * 20, 0.25F, 0.25F),
        CONCRETE(ModItems.debris_concrete, 60 * 20, 0.75F, 0.5F),
        EXCHANGER(ModItems.debris_exchanger, 60 * 20, 1.0F, 0.5F);

        private static final DebrisType[] VALUES = values();

        private final com.hbm.registry.RegistryObject<Item> item;
        private final int lifetime;
        private final EntityDimensions dimensions;

        DebrisType(com.hbm.registry.RegistryObject<Item> item, int lifetime, float width, float height) {
            this.item = item;
            this.lifetime = lifetime;
            this.dimensions = EntityDimensions.scalable(width, height);
        }

        private static DebrisType byId(int id) {
            return VALUES[Math.floorMod(id, VALUES.length)];
        }
    }
}
