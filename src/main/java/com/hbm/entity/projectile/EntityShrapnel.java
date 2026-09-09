package com.hbm.entity.projectile;

import com.hbm.config.CompatibilityConfig;
import com.hbm.entity.ModEntities;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Raycasts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The old EntityThrowable shrapnel projectile used by large and small explosion helpers. */
public final class EntityShrapnel extends CompatEntity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Byte> TRAIL =
            net.minecraft.network.syncher.SynchedEntityData.defineId(EntityShrapnel.class,
                    net.minecraft.network.syncher.EntityDataSerializers.BYTE);

    public EntityShrapnel(EntityType<?> type, Level level) {
        super(type, level);
    }

    public EntityShrapnel(Level level, double x, double y, double z) {
        this(ModEntities.SHRAPNEL.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineData(dev.golem.core.data.SynchedData data) {
        data.define(TRAIL, (byte) 0);
    }

    @Override
    protected void loadData(DataIn input) {
        entityData.set(TRAIL, input.getByteOr("trail", (byte) 0));
    }

    @Override
    protected void saveData(DataOut output) {
        output.putByte("trail", entityData.get(TRAIL));
    }

    @Override
    public void tick() {
        baseTick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0D, 0D, 0D);
        }

        Vec3 from = position();
        Vec3 motion = getDeltaMovement();
        Vec3 to = from.add(motion);
        BlockHitResult blockHit = Compat.clip(level(), from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE);
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();

        if (!level().isClientSide() && level() instanceof ServerLevel server) {
            if (!CompatibilityConfig.isWarDim(level())) {
                discard();
                return;
            }
            boolean hitEntity = false;
            for (Entity entity : Raycasts.sweepEntities(level(), this, from, entityEnd, 0.3D,
                    ignored -> true)) {
                Compat.hurt(entity, server, ModDamageSources.shrapnel(server), 15F);
                hitEntity = true;
            }
            if (blockHit.getType() != HitResult.Type.MISS && tickCount > 5) {
                discard();
                server.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 5,
                        0D, 0D, 0D, 0D);
                server.playSound(null, blockPosition(), SoundEvents.LAVA_EXTINGUISH,
                        SoundSource.HOSTILE, 1F, 1F);
                return;
            }
            if (hitEntity && isRemoved()) return;
        }

        // EntityThrowable moves the complete segment after impact; only the next tick's gravity is applied
        // here. This preserves the legacy early-impact behavior while avoiding endpoint tunnelling.
        setPos(to);
        setDeltaMovement(motion.scale(0.99D).subtract(0D, 0.03D, 0D));
    }

    public void setTrail(boolean enabled) {
        entityData.set(TRAIL, (byte) (enabled ? 1 : 0));
    }

    public void setVolcano(boolean enabled) {
        entityData.set(TRAIL, (byte) (enabled ? 2 : 0));
    }

    public void setWatz(boolean enabled) {
        entityData.set(TRAIL, (byte) (enabled ? 3 : 0));
    }

    public void setRadVolcano(boolean enabled) {
        entityData.set(TRAIL, (byte) (enabled ? 4 : 0));
    }

    public byte getTrail() {
        return entityData.get(TRAIL);
    }
}
