package com.hbm.entity.projectile;

import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.config.CompatibilityConfig;
import com.hbm.entity.ModEntities;
import com.hbm.tileentity.TileEntityVolcanoCore;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The old EntityThrowable shrapnel projectile used by large and small explosion helpers, the geysers and
 * the volcano cores. The synced {@code TRAIL} byte is 1.7's dataWatcher 16 and selects the impact arm:
 * 0 a lava puff, {@link #TRAIL_FLAME} the same puff with a flame trail, {@link #TRAIL_VOLCANO} /
 * {@link #TRAIL_RAD_VOLCANO} a volcano blob (falling: pools lava on the block it hit and fills the
 * 3x3x3 above it with carbon monoxide; rising: a strength-7 lava blast into the roof it struck), and
 * {@link #TRAIL_WATZ} a mud block. Every arm waits out the 5-tick grace and kills the shard.
 */
public final class EntityShrapnel extends CompatEntity {
    /** 1.7 setTrail(true). */
    public static final int TRAIL_FLAME = 1;
    /** 1.7 setVolcano(true). */
    public static final int TRAIL_VOLCANO = 2;
    /** 1.7 setWatz(true). */
    public static final int TRAIL_WATZ = 3;
    /** 1.7 setRadVolcano(true). */
    public static final int TRAIL_RAD_VOLCANO = 4;
    /** 1.7 onImpact: {@code ticksExisted > 5} before any arm runs. */
    public static final int GRACE_TICKS = 5;

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
            boolean hitEntity = false;
            for (Entity entity : Raycasts.sweepEntities(level(), this, from, entityEnd, 0.3D,
                    ignored -> true)) {
                Compat.hurt(entity, server, ModDamageSources.shrapnel(server), 15F);
                hitEntity = true;
            }
            // 1.7 onImpact is one body: past the grace window any hit, entity or block, ends the shard
            if (tickCount > GRACE_TICKS && (hitEntity || blockHit.getType() != HitResult.Type.MISS)) {
                onImpact(server, blockHit.getType() == HitResult.Type.MISS ? null : blockHit.getBlockPos());
                return;
            }
        }

        // EntityThrowable moves the complete segment after impact; only the next tick's gravity is applied
        // here. This preserves the legacy early-impact behavior while avoiding endpoint tunnelling.
        setPos(to);
        setDeltaMovement(motion.scale(0.99D).subtract(0D, 0.03D, 0D));
    }

    /**
     * 1.7 {@code onImpact} past the grace window. {@code hit} is the block struck, or null for an entity
     * hit (1.7 read {@code mop.block*} there too, which is (0,0,0); the block arms are skipped instead).
     */
    private void onImpact(ServerLevel server, @Nullable BlockPos hit) {
        discard();
        // old-hbm: a shard in a peace dimension dies on impact with no arm at all
        if (!CompatibilityConfig.isWarDim(server)) return;

        int trail = getTrail();
        if (trail == TRAIL_VOLCANO || trail == TRAIL_RAD_VOLCANO) {
            if (hit != null) erupt(server, hit, trail == TRAIL_RAD_VOLCANO);
        } else if (trail == TRAIL_WATZ) {
            if (hit != null && server.getBlockState(hit.above()).canBeReplaced()) {
                server.setBlockAndUpdate(hit.above(), ModHazardBlocks.mud_block.get().defaultBlockState());
            }
        } else {
            server.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 5, 0D, 0D, 0D, 0D);
        }
        // 1.7: "random.fizz"
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 1F, 1F);
    }

    /**
     * The volcano blob (1.7 dataWatcher 2 / 4): a blob falling faster than 0.2/t pools lava on top of the
     * block it hit (if that cell is replaceable) and fills the 3x3x3 from the hit block up two with carbon
     * monoxide; a blob still rising blasts a strength-7 lava pocket into the roof it struck — the same
     * {@code ExplosionNT} attribute set as the core's own channel blasts.
     */
    private void erupt(ServerLevel server, BlockPos hit, boolean radioactive) {
        Block lava = radioactive ? ModFluidNetBlocks.rad_lava_block.get() : ModFluidNetBlocks.volcanic_lava_block.get();
        double motionY = getDeltaMovement().y;

        if (motionY < -0.2D) {
            if (server.getBlockState(hit.above()).canBeReplaced()) {
                server.setBlockAndUpdate(hit.above(), lava.defaultBlockState());
            }
            BlockState monoxide = ModNuclearBlocks.gas_monoxide.get().defaultBlockState();
            for (int x = hit.getX() - 1; x <= hit.getX() + 1; x++) {
                for (int y = hit.getY(); y <= hit.getY() + 2; y++) {
                    for (int z = hit.getZ() - 1; z <= hit.getZ() + 1; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (server.getBlockState(pos).isAir()) server.setBlockAndUpdate(pos, monoxide);
                    }
                }
            }
        }

        if (motionY > 0) {
            TileEntityVolcanoCore.lavaBlast(server, lava, hit.getX() + 0.5, hit.getY() + 0.5, hit.getZ() + 0.5, 7);
        }
    }

    public void setTrail(boolean enabled) {
        entityData.set(TRAIL, (byte) (enabled ? TRAIL_FLAME : 0));
    }

    public void setVolcano(boolean enabled) {
        entityData.set(TRAIL, (byte) (enabled ? TRAIL_VOLCANO : 0));
    }

    public void setWatz(boolean enabled) {
        entityData.set(TRAIL, (byte) (enabled ? TRAIL_WATZ : 0));
    }

    public void setRadVolcano(boolean enabled) {
        entityData.set(TRAIL, (byte) (enabled ? TRAIL_RAD_VOLCANO : 0));
    }

    public byte getTrail() {
        return entityData.get(TRAIL);
    }
}
