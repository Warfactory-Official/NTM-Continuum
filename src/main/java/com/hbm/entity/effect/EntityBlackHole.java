package com.hbm.entity.effect;

import dev.golem.core.world.Explosions;
import dev.golem.core.world.SubLevels;
import com.hbm.config.CompatibilityConfig;
import com.hbm.entity.ModEntities;
import com.hbm.items.ModItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.lib.ModDamageSources;
import com.hbm.entity.projectile.EntityRubble;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The legacy black-hole field: block extraction, entity pull, absolute damage, and antimatter detonation. */
public class EntityBlackHole extends CompatEntity {
    public static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(EntityBlackHole.class, EntityDataSerializers.FLOAT);

    protected boolean breaksBlocks = true;

    public EntityBlackHole(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public EntityBlackHole(EntityType<?> type, Level level, float size) {
        this(type, level);
        setSize(size);
    }

    public EntityBlackHole noBreak() {
        breaksBlocks = false;
        return this;
    }

    public float getSize() {
        return entityData.get(SIZE);
    }

    public void setSize(float size) {
        entityData.set(SIZE, size);
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(SIZE, 0.5F);
    }

    @Override
    protected void loadData(DataIn input) {
        setSize(input.getFloatOr("size", 0.5F));
        breaksBlocks = input.getBooleanOr("breaksBlocks", true);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putFloat("size", getSize());
        output.putBoolean("breaksBlocks", breaksBlocks);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25_000D;
    }

    @Override
    protected boolean shouldIgnoreExplosion() {
        return true;
    }

    @Override
    public void tick() {
        baseTick();
        if (!CompatibilityConfig.isWarDim(level())) {
            discard();
            return;
        }

        float size = getSize();
        double range = size * 15D;
        if (!level().isClientSide()) {
            if (breaksBlocks) pullBlocks(size, getX(), getY(), getZ());
            pullSubLevels(size, range);
        }

        AABB bounds = new AABB(getX() - range, getY() - range, getZ() - range,
                getX() + range, getY() + range, getZ() + range);
        List<Entity> entities = level().getEntities(this, bounds, ignored -> true);
        for (Entity entity : entities) {
            if (entity instanceof Player player && player.isCreative()) continue;

            if (entity instanceof FallingBlockEntity falling && !level().isClientSide() && falling.tickCount > 1) {
                BlockState state = falling.getBlockState();
                Vec3 position = falling.position();
                Vec3 motion = falling.getDeltaMovement();
                falling.discard();

                EntityRubble rubble = new EntityRubble(ModEntities.RUBBLE.get(), level());
                rubble.setPos(position);
                rubble.setBlockState(state);
                rubble.setDeltaMovement(motion);
                level().addFreshEntity(rubble);
            }

            Vec3 pull = new Vec3(getX() - entity.getX(), getY() - entity.getY(), getZ() - entity.getZ());
            double distance = pull.length();
            if (distance > range) continue;

            pull = pull.normalize();
            if (!(entity instanceof ItemEntity)) {
                pull = rotatePull(pull);
            }

            entity.setDeltaMovement(entity.getDeltaMovement().add(pull.x * 0.1D, pull.y * 0.2D, pull.z * 0.1D));
            if (entity instanceof EntityBlackHole) continue;

            if (distance < size * 1.5F && level() instanceof ServerLevel server) {
                Compat.hurt(entity, server, ModDamageSources.blackhole(server), 1000F);
                if (!(entity instanceof LivingEntity)) entity.discard();
                if (entity instanceof ItemEntity item && (item.getItem().is(ModItems.flame_pony.get())
                        || item.getItem().is(ModSingularityItems.pellet_antimatter.get()))) {
                    discard();
                    Explosions.explode(server, null, getX(), getY(), getZ(), 5F, true,
                            Level.ExplosionInteraction.TNT);
                    return;
                }
            }
        }

        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y,
                getZ() + getDeltaMovement().z);
        setDeltaMovement(getDeltaMovement().scale(0.99D));
    }

    /** port: moving sub-levels (Create Aeronautics) in range fall in like entities and lose blocks to the same rays. */
    private void pullSubLevels(float size, double range) {
        Vec3 centre = position();
        for (SubLevels.Local local : SubLevels.intoSubLevels(level(), centre, range)) {
            if (breaksBlocks) pullBlocks(size, local.point().x, local.point().y, local.point().z);
            Vec3 pull = centre.subtract(SubLevels.toWorld(level(), local.anchor()));
            if (pull.length() > range) continue;
            pull = rotatePull(pull.normalize());
            SubLevels.accelerate(level(), local.anchor(), new Vec3(pull.x * 0.1D, pull.y * 0.2D, pull.z * 0.1D));
        }
    }

    private void pullBlocks(float size, double centreX, double centreY, double centreZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int k = 0; k < size * 2F; k++) {
            double phi = level().getRandom().nextDouble() * (Math.PI * 2D);
            double cosTheta = level().getRandom().nextDouble() * 2D - 1D;
            double theta = Math.acos(cosTheta);
            double x = Math.sin(theta) * Math.cos(phi);
            double y = Math.sin(theta) * Math.sin(phi);
            double z = Math.cos(theta);
            int length = (int) Math.ceil(size * 15D);

            for (int i = 0; i < length; i++) {
                int x0 = (int) (centreX + x * i);
                int y0 = (int) (centreY + y * i);
                int z0 = (int) (centreZ + z * i);
                pos.set(x0, y0, z0);
                BlockState state = level().getBlockState(pos);
                if (!state.getFluidState().isEmpty()) {
                    level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    state = level().getBlockState(pos);
                }
                if (!state.isAir()) {
                    EntityRubble rubble = new EntityRubble(ModEntities.RUBBLE.get(), level());
                    rubble.setPos(x0 + 0.5F, y0, z0 + 0.5F);
                    rubble.setBlockState(state);
                    level().addFreshEntity(rubble);
                    level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
            }
        }
    }

    /** 1.12 Vec3NT.rotateYawSelf(+15): x'=x*c+z*s, z'=z*c-x*s. */
    public static Vec3 rotatePull(Vec3 pull) {
        double angle = Math.toRadians(15D);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(pull.x * cos + pull.z * sin, pull.y,
                pull.z * cos - pull.x * sin);
    }
}
