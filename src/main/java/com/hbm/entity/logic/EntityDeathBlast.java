package com.hbm.entity.logic;

import com.hbm.particle.helper.EntityEffects;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 EntityDeathBlast - server-side timer entity placed by SatelliteDeathRay's beam impact;
 * after 60 ticks it detonates a fallout-free MK5 blast of radius 40 with the muke flash and sound.
 */
public class EntityDeathBlast extends CompatEntity {

    public static final int maxAge = 60;
    private static final long NONE = Long.MIN_VALUE;
    private long anchorChunk = NONE;

    public EntityDeathBlast(EntityType<? extends EntityDeathBlast> type, Level level) {
        super(type, level);
        // 1.7: ignoreFrustumCheck is renderer-side in 26.1 (see RenderMeteor)
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    @Override
    protected void loadData(DataIn input) {
    }

    @Override
    protected void saveData(DataOut output) {
    }

    // 26.1: hurt() final; 1.7 leaves Entity's default (immune)
    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        ensureAnchor();
        super.tick();
        if (this.tickCount >= maxAge && !level().isClientSide()) {
            this.discard();

            level().addFreshEntity(EntityNukeExplosionMK5.statFacNoRad(level(), 40, getX(), getY(), getZ()));

            // 1.7:42-54 ring of 100 MASKMAN_BOLT bolts; sedna's ctor normalizes the direction, per-shot speed
            // lives in the config (XFactoryNPC class doc)
            int count = 100;
            for (int i = 0; i < count; i++) {
                Vec3 vec = new Vec3(0.2D, 0D, 0D).yRot((float) (2 * Math.PI * (i / (float) count)));
                level().addFreshEntity(new EntityBulletBaseMK4(level(), null, XFactoryNPC.maskman_bolt,
                        XFactoryNPC.MASKMAN_BOLT_DAMAGE, 0F, getX(), getY() + 2, getZ(), vec.x, -0.01D, vec.z));
            }

            if (level() instanceof ServerLevel server) {
                EntityEffects.muke(server, getX(), getY() + 0.5D, getZ(), false, false, 250);
            }
            // 1.7: playSoundEffect "hbm:weapon.mukeExplosion" vol 25 pitch 0.9
            level().playSound(null, getX(), getY(), getZ(), ModSounds.MUKE_EXPLOSION.get(),
                    SoundSource.BLOCKS, 25.0F, 0.9F);
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

    // 1.7: getBrightnessForRender/getBrightness fullbright - no per-entity hook in 26.1

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }
}
