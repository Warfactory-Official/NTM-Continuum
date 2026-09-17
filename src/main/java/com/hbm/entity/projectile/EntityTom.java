package com.hbm.entity.projectile;

import com.hbm.entity.ModNtmEntities;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import net.minecraft.sounds.SoundSource;
import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.entity.logic.EntityTomBlast;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityTom extends EntityThrowableNT {

    private static final long NONE = Long.MIN_VALUE;
    private long anchorChunk = NONE;

    public EntityTom(EntityType<? extends EntityTom> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        ensureAnchor();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (this.tickCount % 100 == 0) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.ALARM_CHIME.get(), SoundSource.RECORDS,
                    10000.0F, 1.0F);
        }

        setDeltaMovement(motion.x, -0.5D, motion.z);

        // 1.7: getBlock((int) posX, ...) != Blocks.air - int truncation, not floor
        if (!level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir() || getY() < 10) {
            if (!level().isClientSide()) {

                EntityTomBlast tom = new EntityTomBlast(ModNtmEntities.TOM_BUST.get(), level());
                tom.setPos(getX(), getY(), getZ());
                tom.destructionRange = 600;
                level().addFreshEntity(tom);

                level().addFreshEntity(EntityCloudTom.statFac(level(), 500, getX(), getY(), getZ()));
            }
            this.discard();
        }
    }

    public void anchorAndLoad(ServerLevel level, int chunkX, int chunkZ) {
        long chunk = ChunkPos.pack(chunkX, chunkZ);
        moveAnchor(level, chunk);
        ChunkUtil.loadEntityTickingChunk(level, chunk);
    }

    private void ensureAnchor() {
        if (level() instanceof ServerLevel server) {
            moveAnchor(server, ChunkPos.pack(getBlockX() >> 4, getBlockZ() >> 4));
        }
    }

    private void moveAnchor(ServerLevel level, long chunk) {
        if (anchorChunk == chunk) return;
        ChunkUtil.addAnchor(level, chunk);
        if (anchorChunk != NONE) ChunkUtil.releaseAnchor(level, anchorChunk);
        anchorChunk = chunk;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel server && anchorChunk != NONE) {
            ChunkUtil.releaseAnchor(server, anchorChunk);
        }
        anchorChunk = NONE;
        super.remove(reason);
    }

    @Override
    protected void onImpact(HitResult mop) {
        // 1.7: empty override - detonation comes from the solid-block probe above
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D;
    }

    // 26.2: 1.7's fullbright getBrightnessForRender/getBrightness have no common-code override left - RenderCloudTom
    // emits the packed light.
}
