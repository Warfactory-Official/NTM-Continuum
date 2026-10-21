package com.hbm.blocks.bomb;

import com.hbm.api.block.IFuckingExplode;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.IBlockExplodedVNT;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.interfaces.IBomb;
import dev.golem.core.world.CompatBlock;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code DetMiner}: the redstone-fired mining charge, a pillar-textured block. Upstream fires
 * {@code ExplosionNT(4)} with ALLDROP and NOHURT and {@code doExplosionB(false)} (sound, no particles),
 * then the 30-count smoke cloud. {@code ExplosionNT} is not ported; the VNT with the standard 16-ray
 * allocator, an all-drop processor and no entity processor is the same blast.
 */
public class DetMiner extends CompatBlock implements IBomb, IFuckingExplode, IBlockExplodedVNT {

    public DetMiner(Properties properties) {
        super(properties);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;
        level.destroyBlock(pos, false);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;

        ExplosionVNT explosion = new ExplosionVNT(level, x, y, z, 4F);
        explosion.setBlockAllocator(new BlockAllocatorStandard());
        explosion.setBlockProcessor(new BlockProcessorStandard().setAllDrop());
        explosion.explode();
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F);

        ExplosionLarge.spawnParticles(level, x, y, z, 30);
        return BombReturnCode.DETONATED;
    }

    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        LivingEntity igniter = explosion.getIndirectSourceEntity();
        level.removeBlock(pos, false);
        EntityTntNtm primed = new EntityTntNtm(ModNtmEntities.TNT_NTM.get(), level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, igniter, state);
        primed.setFuse(0);
        level.addFreshEntity(primed);
        return true;
    }

    @Override
    public boolean onVNTExploded(ServerLevel level, BlockPos pos, BlockState state, ExplosionVNT explosion) {
        level.removeBlock(pos, false);
        EntityTntNtm primed = new EntityTntNtm(ModNtmEntities.TNT_NTM.get(), level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, IBlockExplodedVNT.igniter(explosion), state);
        primed.setFuse(0);
        level.addFreshEntity(primed);
        return true;
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, EntityTntNtm entity) {
        explode(level, BlockPos.containing(x, y, z), entity);
    }
}
