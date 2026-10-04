package com.hbm.blocks.generic;

import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.handler.radiation.ChunkRadiationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code YellowBarrel}: the radioactive waste drums. Every second ({@code tickRate 20}) a
 * {@code yellow_barrel} adds 5 rads to its chunk and a {@code vitrified_barrel} 0.5; both shed the
 * town-aura mote above the lid. Only the yellow barrel pops from an explosion; when it goes off it has
 * a one-in-three chance to leave a puddle of ooze instead of a 12-strength blast, wastes the land
 * within 35 blocks, fills a fifth of the air within ±5 with dense radon and dumps 35 rads.
 */
public class YellowBarrel extends BlockBarrelDeco {

    private final boolean vitrified;

    public YellowBarrel(Properties properties, String texture, int kindId, boolean vitrified) {
        super(properties, texture, kindId, false);
        this.vitrified = vitrified;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 20);
    }

    /** 1.7 updateTick: the chunk gets its dose and the block re-schedules itself every 20 ticks. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChunkRadiationManager.proxy.incrementRad(level, pos, vitrified ? 0.5F : 5.0F);
        level.scheduleTick(pos, this, 20);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        level.addParticle(ParticleTypes.MYCELIUM, pos.getX() + rand.nextFloat() * 0.5F + 0.25F, pos.getY() + 1.1F,
                pos.getZ() + rand.nextFloat() * 0.5F + 0.25F, 0.0D, 0.0D, 0.0D);
    }

    /** Only the yellow barrel is set off by an explosion; the vitrified drum is inert. */
    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos,
                                  net.minecraft.world.level.Explosion explosion,
                                  java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, BlockPos> onHit) {
        if (vitrified) return false;
        return super.onExplosion(state, level, pos, explosion, onHit);
    }

    @Override
    public void detonate(Level level, double x, double y, double z, @Nullable EntityPrimedExplosive primed) {
        int ix = Mth.floor(x), iy = Mth.floor(y), iz = Mth.floor(z);
        BlockPos center = new BlockPos(ix, iy, iz);
        RandomSource rand = level.getRandom();
        if (rand.nextInt(3) == 0) {
            level.setBlock(center, ModFluidNetBlocks.toxic_block.get().defaultBlockState(), 3);
        } else {
            level.explode(primed, x, y, z, 12.0F, Level.ExplosionInteraction.TNT);
        }
        ExplosionNukeGeneric.waste(level, center, 35); // 1.7 YellowBarrel:103
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                for (int k = -5; k <= 5; k++) {
                    BlockPos at = center.offset(i, j, k);
                    if (rand.nextInt(5) == 0 && level.getBlockState(at).isAir()) {
                        level.setBlock(at, ModNuclearBlocks.gas_radon_dense.get().defaultBlockState(), 3);
                    }
                }
            }
        }
        ChunkRadiationManager.proxy.incrementRad(level, center, 35);
    }
}
