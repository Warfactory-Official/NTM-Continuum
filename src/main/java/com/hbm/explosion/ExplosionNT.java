package com.hbm.explosion;

import com.hbm.api.data.WorldDataMaps;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * The part of 1.7's (deprecated) {@code ExplosionNT} that is not already a {@link ExplosionVNT} part.
 *
 * <p>This port models {@code ExplosionNT} and its {@code ExAttrib} set as VNT parts: LAVA_V/LAVA_R is
 * {@code BlockMutatorLava}, NODROP is {@code setNoDrop}, NOSOUND/NOPARTICLE is "no SFX". {@code ERRODE} is
 * {@link BlockAllocatorStandard#withErosion} (concrete the ray could not break is still affected) plus
 * {@link BlockProcessorStandard#withErosion} (60% per block, nothing drops, the block becomes
 * {@link #errosion()}'s target). This class owns the table and the one caller's recipe, the drill missile.
 */
public final class ExplosionNT {

    private ExplosionNT() {}

    /**
     * 1.7 {@code ExplosionNT.errosion}: what an eroding blast leaves of a block. The table is the {@code hbm:erosion}
     * block data map ({@link com.hbm.api.data.WorldDataMaps#EROSION}); this is a live view of it, read per lookup.
     */
    public static Map<Block, Block> errosion() {
        return EROSION_VIEW;
    }

    private static final Map<Block, Block> EROSION_VIEW = new AbstractMap<>() {
        @Override
        public Block get(Object key) {
            if (!(key instanceof Block block)) return null;
            String to = WorldDataMaps.EROSION.get(block);
            return to == null ? null : WorldDataMaps.blockOf(to);
        }

        @Override
        public boolean containsKey(Object key) {
            return key instanceof Block block && WorldDataMaps.EROSION.get(block) != null;
        }

        @Override
        public Set<Map.Entry<Block, Block>> entrySet() {
            Set<Map.Entry<Block, Block>> set = new LinkedHashSet<>();
            WorldDataMaps.EROSION.byId().forEach((from, to) -> set.add(Map.entry(
                    WorldDataMaps.blockOf(from), WorldDataMaps.blockOf(to))));
            return set;
        }
    };

    /**
     * {@code new ExplosionNT(world, exploder, x, y, z, size)} with {@code addAllAttrib(ExAttrib.ERRODE)} and
     * nothing else, then {@code explode()}: 16-ray vanilla fan, drops at {@code 1/size}, entities hurt, and the
     * vanilla explosion sound only. {@code doExplosionB} also called {@code worldObj.spawnParticle} (the huge
     * explosion, and explode/smoke per block), but the drill runs it on the server, where a 1.7
     * {@code WorldServer}'s {@code spawnParticle} reaches only {@code WorldManager}'s empty method; only
     * {@code playSoundEffect} was broadcast. So the drill's 30 stacked blasts were heard and never seen.
     */
    public static void explodeErode(Level level, @Nullable Entity exploder, double x, double y, double z, float size) {
        new ExplosionVNT(level, x, y, z, size, exploder)
                .setBlockAllocator(new BlockAllocatorStandard().withErosion(errosion()))
                .setBlockProcessor(new BlockProcessorStandard().withErosion(errosion()))
                .setEntityProcessor(new EntityProcessorStandard())
                .setPlayerProcessor(new PlayerProcessorStandard())
                .setSFX(ExplosionNT::soundOnly)
                .explode();
    }

    /** {@code doExplosionB}'s {@code playSoundEffect("random.explode", 4, (1 + (r - r) * 0.2) * 0.7)}, nothing else. */
    private static void soundOnly(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {
        if (level.isClientSide()) return;
        RandomSource rand = level.getRandom();
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 4.0F, (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
    }
}
