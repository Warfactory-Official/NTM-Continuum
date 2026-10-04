package com.hbm.explosion;

import com.hbm.config.BombConfig;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.main.MainRegistry;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** The parameterised small-nuke helper used by the legacy micro missile. */
@Deprecated
public final class ExplosionNukeSmall {

    private ExplosionNukeSmall() {}

    /** Keeps the old effect/sound/damage/entity ordering used by all small-nuke presets. */
    public static void explode(Level world, double x, double y, double z, MukeParams params) {
        if (params.particle != null) {
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            if (params.particle == HbmEffectNT.Muke
                    && (MainRegistry.polaroidID == 11 || world.getRandom().nextInt(100) == 0)) {
                data.putBoolean("balefire", true);
            }
            params.particle.send(world, x, y + 0.5D, z, data);
        }

        world.playSound(null, BlockPos.containing(x, y, z), ModSounds.MUKE_EXPLOSION.get(),
                SoundSource.BLOCKS, 15.0F, 1.0F);

        if (params.shrapnelCount > 0) {
            ExplosionLarge.spawnShrapnels(world, x, y, z, params.shrapnelCount);
        }

        if (params.miniNuke && !params.safe) {
            new ExplosionVNT(world, x, y, z, params.blastRadius)
                    .setBlockAllocator(new BlockAllocatorStandard(params.resolution))
                    .setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(new BlockMutatorFire()))
                    .explode();
        }

        if (params.killRadius > 0) {
            List<Entity> entities = world.getEntities(null,
                    new AABB(x - params.killRadius, y - params.killRadius, z - params.killRadius,
                            x + params.killRadius, y + params.killRadius, z + params.killRadius));
            ExplosionNukeGeneric.dealDamage(world, entities, x, y, z, params.killRadius);
        }

        if (!params.miniNuke && world instanceof ServerLevel server) {
            EntityNukeExplosionMK5 blast = EntityNukeExplosionMK5.statFac(
                    server, (int) params.blastRadius, x, y, z);
            if (server.addFreshEntity(blast)) blast.startChunkLoading();
        }

        if (params.miniNuke) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    if (Math.abs(i) + Math.abs(j) < 4) {
                        ChunkRadiationManager.proxy.incrementRad(world,
                                pos.set(Mth.floor(x + i * 16D), Mth.floor(y), Mth.floor(z + j * 16D)),
                                radiationAt(i, j, params.radiationLevel));
                    }
                }
            }
        }
    }

    /**
     * 1.7 :45, {@code 50 / (|i| + |j| + 1) * radMod}: the 50 divides as an INT (50, 25, 16, 12, ...)
     * before the float {@code radiationLevel / 3} multiplies it.
     */
    public static float radiationAt(int i, int j, float radiationLevel) {
        float radMod = radiationLevel / 3F;
        return 50 / (Math.abs(i) + Math.abs(j) + 1) * radMod;
    }

    public static final MukeParams PARAMS_SAFE = new MukeParams();
    public static final MukeParams PARAMS_TOTS = new MukeParams();
    public static final MukeParams PARAMS_LOW = new MukeParams();
    public static final MukeParams PARAMS_MEDIUM = new MukeParams();
    public static final MukeParams PARAMS_HIGH = new MukeParams();

    static {
        PARAMS_SAFE.safe = true;
        PARAMS_SAFE.killRadius = 45F;
        PARAMS_SAFE.radiationLevel = 2F;

        PARAMS_TOTS.blastRadius = 10F;
        PARAMS_TOTS.killRadius = 30F;
        PARAMS_TOTS.particle = HbmEffectNT.TinyTot;
        PARAMS_TOTS.shrapnelCount = 0;
        PARAMS_TOTS.resolution = 32;
        PARAMS_TOTS.radiationLevel = 1F;

        PARAMS_LOW.blastRadius = 15F;
        PARAMS_LOW.killRadius = 45F;
        PARAMS_LOW.radiationLevel = 2F; // 1.7 ExplosionNukeSmall:54
        PARAMS_MEDIUM.blastRadius = 20F;
        PARAMS_MEDIUM.killRadius = 55F;
        PARAMS_MEDIUM.radiationLevel = 3F; // 1.7 ExplosionNukeSmall:55

        PARAMS_HIGH.miniNuke = false;
        PARAMS_HIGH.blastRadius = BombConfig.fatmanRadius;
        PARAMS_HIGH.shrapnelCount = 0;
    }

    public enum ExAttrib { FIRE, NOPARTICLE, NOSOUND, NODROP, NOHURT }

    public static final class MukeParams {
        public boolean miniNuke = true;
        public boolean safe = false;
        public float blastRadius;
        public float killRadius;
        public float radiationLevel = 1F;
        public HbmEffectNT particle = HbmEffectNT.Muke;
        public int shrapnelCount = 25;
        public int resolution = 64;
        public ExAttrib[] explosionAttribs = ExAttrib.values();
    }
}
