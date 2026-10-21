package com.hbm.blocks.bomb;

import com.hbm.api.block.IFuckingExplode;
import com.hbm.config.BombConfig;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.IBlockExplodedVNT;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IBomb;
import com.hbm.particle.helper.ExplosionCreator;
import dev.golem.core.world.CompatBlock;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ExplosiveCharge}: the redstone-fired {@code det_charge} and {@code det_nuke} blocks (the
 * class also served {@code det_cord}, which has its own class here for its cable model). Caught in an
 * explosion it becomes a zero-fuse primed entity (1.7 {@code BlockDetonatable}, popFuse 0) and never
 * drops.
 */
public class ExplosiveCharge extends CompatBlock implements IBomb, IDetConnectible, IFuckingExplode, IBlockExplodedVNT {

    public enum Kind { CHARGE, NUKE }

    public final Kind kind;

    public ExplosiveCharge(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
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
        level.removeBlock(pos, false);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;

        switch (kind) {
            case CHARGE -> {
                // 1.7: ExplosionNT(15).overrideResolution(64) - the vanilla algorithm at 64 rays, with
                // drops and damage. ExplosionNT is not ported; this is the same blast on the VNT seam.
                ExplosionVNT vnt = new ExplosionVNT(level, x, y, z, 15F);
                vnt.setBlockAllocator(new BlockAllocatorStandard(64));
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorStandard());
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();
                ExplosionCreator.composeEffectStandard(level, x, y + 0.5, z);
            }
            case NUKE -> {
                EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.statFac(level, BombConfig.missileRadius, x, y, z);
                if (detonator != null) mk5.detonator = detonator.getUUID();
                if (level.addFreshEntity(mk5)) mk5.startChunkLoading();
                if (BombConfig.enableNukeClouds) {
                    EntityNukeTorex torex = EntityNukeTorex.statFac(level, x, y, z, BombConfig.missileRadius);
                    if (level.addFreshEntity(torex)) torex.startChunkLoading();
                }
            }
        }
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
