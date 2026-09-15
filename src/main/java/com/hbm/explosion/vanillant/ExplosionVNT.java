package com.hbm.explosion.vanillant;

import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.explosion.vanillant.interfaces.IPlayerProcessor;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A vanilla-shaped explosion assembled from swappable parts — 1.12.2's {@code ExplosionVNT}, whose own
 * comment ("time to over-engineer this into oblivion so that I never have to write a vanilla-esque
 * explosion class ever again") is the design brief. An explosion picks blocks
 * ({@link IBlockAllocator}), does something to them ({@link IBlockProcessor}), hurts entities
 * ({@link IEntityProcessor} + {@link IPlayerProcessor}) and looks like something
 * ({@link IExplosionSFX}); any of the four can be left out.
 *
 * <p><b>One deviation, and why.</b> Upstream carried a dummy vanilla {@code Explosion} object
 * (<i>"compat"</i>) whose only job was to be handed to vanilla's block and entity hooks —
 * {@code getExplosionResistance}, {@code canDropFromExplosion}, {@code onBlockExploded},
 * {@code ForgeEventFactory.onExplosionDetonate} — and to carry its own player-knockback map. That
 * object cannot be reproduced portably: {@code Explosion} is an <i>interface</i> at 26.1 (implemented
 * by {@code ServerExplosion}) and a concrete class below it, so any instance HBM built would be a
 * version-specific type in a shared source tree. The parts therefore call the underlying vanilla
 * operations directly (resistance from the block, drops through {@code Block.dropResources}), which is
 * what those hooks do anyway. Everything observable — which blocks go, what they drop, how hard
 * entities are hit — follows the original formulas unchanged.
 */
public class ExplosionVNT {

    private @Nullable IBlockAllocator blockAllocator;
    private @Nullable IEntityProcessor entityProcessor;
    private @Nullable IBlockProcessor blockProcessor;
    private @Nullable IPlayerProcessor playerProcessor;
    /** Sound and particles are per-effect and usually stacked, so this one is an array. */
    private IExplosionSFX @Nullable [] sfx;

    public final Level world;
    public final double posX;
    public final double posY;
    public final double posZ;
    public final float size;
    public final @Nullable Entity exploder;

    public ExplosionVNT(Level world, double x, double y, double z, float size) {
        this(world, x, y, z, size, null);
    }

    public ExplosionVNT(Level world, BlockPos pos, float size) {
        this(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, size, null);
    }

    public ExplosionVNT(Level world, double x, double y, double z, float size, @Nullable Entity exploder) {
        this.world = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.size = size;
        this.exploder = exploder;
    }

    public float blockResistance(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return Math.max(state.getBlock().getExplosionResistance(), level.getFluidState(pos).getExplosionResistance());
    }

    public boolean blockExplodes(Level level, BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, float power) {
        return true;
    }

    public void explode() {

        boolean processBlocks = blockAllocator != null && blockProcessor != null;
        boolean processEntities = entityProcessor != null && playerProcessor != null;

        ObjectOpenHashSet<BlockPos> affectedBlocks = null;
        if (processBlocks) {
            // Exposure must be calculated against the pre-blast world, as in old-hbm.
            affectedBlocks = blockAllocator.allocate(this, world, posX, posY, posZ, size);
        }

        HashMap<Player, Vec3> affectedPlayers = null;
        if (processEntities) {
            affectedPlayers = entityProcessor.process(this, world, posX, posY, posZ, size);
        }

        if (processBlocks) {
            blockProcessor.process(this, world, posX, posY, posZ, affectedBlocks);
        }

        if (processEntities) {
            playerProcessor.process(this, world, posX, posY, posZ, affectedPlayers);
        }

        if (sfx != null) {
            for (IExplosionSFX fx : sfx) {
                fx.doEffect(this, world, posX, posY, posZ, size);
            }
        }
    }

    public ExplosionVNT setBlockAllocator(IBlockAllocator blockAllocator) {
        this.blockAllocator = blockAllocator;
        return this;
    }

    public ExplosionVNT setEntityProcessor(IEntityProcessor entityProcessor) {
        this.entityProcessor = entityProcessor;
        return this;
    }

    public ExplosionVNT setBlockProcessor(IBlockProcessor blockProcessor) {
        this.blockProcessor = blockProcessor;
        return this;
    }

    public ExplosionVNT setPlayerProcessor(IPlayerProcessor playerProcessor) {
        this.playerProcessor = playerProcessor;
        return this;
    }

    public ExplosionVNT setSFX(IExplosionSFX... sfx) {
        this.sfx = sfx;
        return this;
    }

    /**
     * Upstream's {@code makeStandard} minus its SFX: {@code ExplosionEffectStandard} belongs to the
     * unported particle families, so a caller that wants a look supplies it with {@link #setSFX}.
     * Also uses {@link EntityProcessorCross} rather than upstream's {@code EntityProcessorStandard},
     * which is the processor the missiles all use anyway.
     */
    public ExplosionVNT makeStandard() {
        this.setBlockAllocator(new BlockAllocatorStandard());
        this.setBlockProcessor(new BlockProcessorStandard());
        this.setEntityProcessor(new EntityProcessorCross());
        this.setPlayerProcessor(new PlayerProcessorStandard());
        return this;
    }
}
