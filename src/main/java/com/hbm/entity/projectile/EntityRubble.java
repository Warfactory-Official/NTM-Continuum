package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import com.hbm.entity.ModEntities;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.CompatFallingEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Raycasts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A tumbling block fragment emitted by the black-hole pull field. */
public final class EntityRubble extends CompatFallingEntity {
    private static final EntityDataAccessor<Integer> BLOCK_STATE =
            SynchedEntityData.defineId(EntityRubble.class, EntityDataSerializers.INT);

    public EntityRubble(EntityType<? extends EntityRubble> type, Level level) {
        super(type, level);
    }

    /** ntm-next convenience: rubble at a position, before {@link #shoot}. */
    public EntityRubble(Level level, double x, double y, double z) {
        this(ModEntities.RUBBLE.get(), level);
        setPos(x, y, z);
    }

    /** Vanilla {@code Projectile.shoot} semantics (direction, velocity, inaccuracy) for a falling-block rubble. */
    public void shoot(double dx, double dy, double dz, float velocity, float inaccuracy) {
        Vec3 dir = new Vec3(dx, dy, dz).normalize()
                .add(random.triangle(0.0D, 0.0172275D * inaccuracy), random.triangle(0.0D, 0.0172275D * inaccuracy),
                        random.triangle(0.0D, 0.0172275D * inaccuracy)).scale(velocity);
        setDeltaMovement(dir);
    }

    public BlockState getBlockState() {
        return Block.stateById(entityData.get(BLOCK_STATE));
    }

    public void setBlockState(BlockState state) {
        entityData.set(BLOCK_STATE, Block.getId(state));
    }

    @Override
    protected void defineData(dev.golem.core.data.SynchedData data) {
        data.define(BLOCK_STATE, Block.getId(Blocks.STONE.defaultBlockState()));
    }

    @Override
    protected void loadData(DataIn input) {
        entityData.set(BLOCK_STATE, input.getIntOr("blockState", Block.getId(Blocks.STONE.defaultBlockState())));
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("blockState", entityData.get(BLOCK_STATE));
    }

    @Override
    public void tick() {
        baseTick();
        Vec3 motion = getDeltaMovement();
        Vec3 from = position();
        Vec3 to = from.add(motion);
        net.minecraft.world.phys.BlockHitResult blockHit = Compat.clip(level(), from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE);
        Vec3 entityEnd = blockHit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                ? to : blockHit.getLocation();

        if (!level().isClientSide() && level() instanceof ServerLevel server) {
            Entity entity = Raycasts.sweepEntities(level(), this, from, entityEnd, 0.3D, ignored -> true)
                    .stream().findFirst().orElse(null);
            boolean hitEntity = entity != null;
            if (hitEntity) Compat.hurt(entity, server, ModDamageSources.rubble(server), 15F);
            if ((hitEntity || blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) && tickCount > 2) {
                discard();
                server.playSound(null, blockPosition(), ModSounds.DEBRIS.get(), SoundSource.BLOCKS, 1.5F, 1F);
                net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                data.putInt("block", Block.getId(getBlockState()));
                com.hbm.particle.helper.HbmEffectNT.BlockBurst.send(server,
                        getX() - 1D, getY(), getZ() - 1D, data);
                return;
            }
        }

        // EntityThrowableNT applies drag before gravity. Rubble's legacy drag is exactly 1.0.
        setPos(to);
        setDeltaMovement(motion.add(0D, -0.03D, 0D));
    }
}
