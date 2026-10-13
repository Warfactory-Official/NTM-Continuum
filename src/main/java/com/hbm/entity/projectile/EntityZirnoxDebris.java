package com.hbm.entity.projectile;

import com.hbm.potion.ModEffects;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.util.SavedIds;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.Tags;

import dev.golem.core.data.CompatEntity;
import com.hbm.entity.ModEntities;
import com.hbm.items.ModItems;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Effects;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Physical debris ejected by a ZIRNOX reactor meltdown. */
public class EntityZirnoxDebris extends CompatEntity {

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
    protected void defineData(SynchedData data) {
        data.define(TYPE, DebrisType.BLANK.ordinal());
    }

    @Override
    protected void loadData(DataIn input) {
        setDebrisType(SavedIds.getEnum(input, "type", DebrisType.BLANK));
    }

    @Override
    protected void saveData(DataOut output) {
        SavedIds.putEnum(output, "type", getDebrisType()); // by name; the synched int is session-only
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
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    protected InteractionResult onInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = new ItemStack(getDebrisType().item.get());
        if (player.getInventory().add(stack)) discard();
        return InteractionResult.SUCCESS;
    }

    /**
     * 1.7 {@code onUpdate}: move first, then the server logic. 1.7 adds no chunk radiation here (1.12.2's
     * +20 RAD per tick for ELEMENT debris is not ported). Instead ELEMENT and GRAPHITE
     * debris give every living entity within 2.5 blocks the radiation effect, and rising CONCRETE/EXCHANGER debris
     * smashes a rough 3x3x3 hole where it hits a block.
     */
    @Override
    public void tick() {
        super.tick();

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

        if (level().isClientSide()) return;
        DebrisType type = getDebrisType();

        if ((type == DebrisType.CONCRETE || type == DebrisType.EXCHANGER) && velocity.y > 0) {
            Vec3 pos = position();
            Vec3 next = pos.add(velocity.scale(2D));
            BlockHitResult hit = level().clip(new ClipContext(pos, next,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos centre = hit.getBlockPos();
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        for (int k = -1; k <= 1; k++) {
                            int rn = Math.abs(i) + Math.abs(j) + Math.abs(k);
                            if (rn <= 1 || random.nextInt(rn) == 0) {
                                level().setBlock(centre.offset(i, j, k), Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }
                }
                discard();
                return;
            }
        }

        if (type == DebrisType.ELEMENT || type == DebrisType.GRAPHITE) {
            int amplifier = type == DebrisType.ELEMENT ? 7 : 4;
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                    getBoundingBox().inflate(2.5D))) {
                Effects.addRegistered(e, ModEffects.RADIATION.get(), 60 * 20, amplifier);
            }
        }

        if (!RBMKDials.getPermaScrap(level())
                && tickCount > type.lifetime + getId() % 50) {
            discard();
        }
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

        private final RegistryObject<Item> item;
        private final int lifetime;
        private final EntityDimensions dimensions;

        DebrisType(RegistryObject<Item> item, int lifetime, float width, float height) {
            this.item = item;
            this.lifetime = lifetime;
            this.dimensions = EntityDimensions.scalable(width, height);
        }

        private static DebrisType byId(int id) {
            return VALUES[Math.floorMod(id, VALUES.length)];
        }
    }
}
