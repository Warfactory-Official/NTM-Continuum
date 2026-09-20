package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import com.hbm.entity.ModRBMKEntities;
import com.hbm.util.ContaminationUtil;
import com.hbm.items.ModItems;
import com.hbm.items.ModRBMKItems;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Flying RBMK column fragments. This is intentionally separate from ZIRNOX debris: its ascent destroys a 3x3x3 path. */
public class EntityRBMKDebris extends CompatEntity {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityRBMKDebris.class, EntityDataSerializers.INT);

    public float rot;
    public float lastRot;

    public EntityRBMKDebris(EntityType<EntityRBMKDebris> type, Level level) {
        super(type, level);
        rot = lastRot = random.nextFloat() * 360.0F;
    }

    public EntityRBMKDebris(Level level, double x, double y, double z, DebrisType type) {
        this(ModRBMKEntities.RBMK_DEBRIS.get(), level);
        setPos(x, y, z);
        setDebrisType(type);
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(TYPE, DebrisType.BLANK.ordinal());
    }

    @Override
    protected void loadData(DataIn input) {
        setDebrisType(DebrisType.byId(input.getIntOr("debtype", 0)));
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("debtype", entityData.get(TYPE));
    }

    @Override
    protected boolean onHurt(DamageSource source, float damage) {
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
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (player.getInventory().add(new ItemStack(getDebrisType().item.get()))) discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (!level.isClientSide()) {
            Vec3 movement = getDeltaMovement();
            if (movement.y > 0.0D) {
                BlockHitResult hit = level.clip(new ClipContext(position(), position().add(movement.scale(2.0D)),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos impact = hit.getBlockPos();
                    for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                        int distance = Math.abs(x) + Math.abs(y) + Math.abs(z);
                        if (distance <= 1 || random.nextInt(distance) == 0) level.removeBlock(impact.offset(x, y, z), false);
                    }
                    discard();
                    return;
                }
            }
            if (getDebrisType() == DebrisType.FUEL || getDebrisType() == DebrisType.GRAPHITE) {
                double dose = getDebrisType() == DebrisType.FUEL ? 150.0D : 50.0D;
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.5D))) {
                    ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION,
                            ContaminationUtil.ContaminationType.CREATIVE, dose);
                }
            }
            if (!RBMKDials.getPermaScrap(level) && tickCount > getDebrisType().lifetime + getId() % 50) {
                discard();
                return;
            }
        }

        lastRot = rot;
        Vec3 before = getDeltaMovement().add(0.0D, -0.04D, 0.0D);
        setDeltaMovement(before);
        move(MoverType.SELF, before);
        Vec3 after = getDeltaMovement();
        double x = before.x != after.x ? -before.x * 0.75D : after.x;
        double y = before.y != after.y ? 0.0D : after.y;
        double z = before.z != after.z ? -before.z * 0.75D : after.z;
        if (onGround()) {
            x *= 0.85D;
            z *= 0.85D;
            y *= -0.5D;
        } else {
            rot = (rot + 10.0F) % 360.0F;
            if (rot < lastRot) lastRot -= 360.0F;
        }
        setDeltaMovement(x, y, z);
    }

    public DebrisType getDebrisType() {
        return DebrisType.byId(entityData.get(TYPE));
    }

    public void setDebrisType(DebrisType type) {
        entityData.set(TYPE, type.ordinal());
        refreshDimensions();
    }

    public enum DebrisType {
        BLANK(ModItems.debris_metal, 30 * 60 * 20, 0.5F, 0.5F),
        ELEMENT(ModItems.debris_metal, 30 * 60 * 20, 1.0F, 1.0F),
        FUEL(ModItems.debris_fuel, 100 * 60 * 20, 0.25F, 0.25F),
        ROD(ModItems.debris_metal, 600 * 20, 0.75F, 0.5F),
        GRAPHITE(ModItems.debris_graphite, 150 * 60 * 20, 0.25F, 0.25F),
        LID(ModRBMKItems.rbmk_lid, 300 * 20, 1.0F, 0.5F);

        private static final DebrisType[] VALUES = values();
        private final dev.golem.core.registry.RegistryObject<? extends Item> item;
        private final int lifetime;
        private final EntityDimensions dimensions;

        DebrisType(dev.golem.core.registry.RegistryObject<? extends Item> item, int lifetime, float width, float height) {
            this.item = item;
            this.lifetime = lifetime;
            this.dimensions = EntityDimensions.scalable(width, height);
        }

        private static DebrisType byId(int id) {
            return VALUES[Math.floorMod(id, VALUES.length)];
        }
    }
}
