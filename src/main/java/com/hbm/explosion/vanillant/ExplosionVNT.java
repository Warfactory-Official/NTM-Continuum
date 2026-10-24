package com.hbm.explosion.vanillant;

import com.hbm.explosion.vanillant.interfaces.IBlockAllocator;
import com.hbm.api.event.HbmEvents;
import com.hbm.explosion.SubLevelBlasts;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.explosion.vanillant.interfaces.IPlayerProcessor;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.CustomDamageHandlerAmat;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm.explosion.vanillant.standard.ExplosionEffectAmat;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import dev.golem.core.world.SubLevels;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A vanilla-shaped explosion assembled from swappable parts, 1.12.2's {@code ExplosionVNT}, whose own
 * comment ("time to over-engineer this into oblivion so that I never have to write a vanilla-esque
 * explosion class ever again") is the design brief. An explosion picks blocks
 * ({@link IBlockAllocator}), does something to them ({@link IBlockProcessor}), hurts entities
 * ({@link IEntityProcessor} + {@link IPlayerProcessor}) and looks like something
 * ({@link IExplosionSFX}); any of the four can be left out.
 *
 * On modern targets the blast implements vanilla's explosion context directly, preserving its identity across
 * block callbacks. Older targets retain the separate VNT callback interface.
 */
public class ExplosionVNT
        //? if >=26.1 {
        implements net.minecraft.world.level.Explosion
        //?}
{

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

    public float blockResistance(Level level, BlockPos pos, BlockState state) {
        var resistance = Math.max(state.getBlock().getExplosionResistance(), level.getFluidState(pos).getExplosionResistance());
        //? if >=26.1 {
        return exploder == null ? resistance : exploder.getBlockExplosionResistance(this, level, pos, state, state.getFluidState(), resistance);
        //?} else {
        /*return resistance;
        *///?}
    }

    public boolean blockExplodes(Level level, BlockPos pos,
            BlockState state, float power) {
        //? if >=26.1 {
        return exploder == null || exploder.shouldBlockExplode(this, level, pos, state, power);
        //?} else {
        /*return true;
        *///?}
    }

    //? if >=26.1 {
    @Override public net.minecraft.server.level.ServerLevel level() { return (net.minecraft.server.level.ServerLevel) world; }
    @Override public BlockInteraction getBlockInteraction() {
        return blockProcessor == null ? BlockInteraction.DESTROY_WITH_DECAY : blockProcessor.interaction();
    }
    @Override public net.minecraft.world.entity.@Nullable LivingEntity getIndirectSourceEntity() {
        return net.minecraft.world.level.Explosion.getIndirectSourceEntity(exploder);
    }
    @Override public @Nullable Entity getDirectSourceEntity() { return exploder; }
    @Override public float radius() { return size; }
    @Override public net.minecraft.world.phys.Vec3 center() { return new net.minecraft.world.phys.Vec3(posX, posY, posZ); }
    @Override public boolean canTriggerBlocks() { return false; }
    @Override public boolean shouldAffectBlocklikeEntities() { return true; }
    //?}

    public void explode() {

        boolean processBlocks = blockAllocator != null && blockProcessor != null;
        boolean processEntities = entityProcessor != null && playerProcessor != null;

        // port: a centre on a moving sub-level is in its plot; entities, effects and the world are where it is seen
        Vec3 centre = new Vec3(posX, posY, posZ);
        boolean carried = SubLevels.isOnSubLevel(world, centre);
        Vec3 seen = SubLevels.toWorld(world, centre);

        ObjectOpenHashSet<BlockPos> affectedBlocks = null;
        if (processBlocks) {
            // Exposure must be calculated against the pre-blast world, as in 1.12.2.
            affectedBlocks = blockAllocator.allocate(this, world, posX, posY, posZ, size);
        }

        HashMap<Player, Vec3> affectedPlayers = null;
        if (processEntities) {
            affectedPlayers = entityProcessor.process(this, world, seen.x, seen.y, seen.z, size);
        }

        if (processBlocks) {
            List<BlockPos> solid = carried ? solid(affectedBlocks) : List.of();
            blockProcessor.process(this, world, posX, posY, posZ, affectedBlocks);
            if (carried) {
                SubLevelBlasts.push(world, centre, solid);
                blastAt(seen, false);
            }
            for (SubLevels.Local local : SubLevels.intoSubLevels(world, seen, size * 2D)) {
                if (local.point().distanceToSqr(centre) > 1.0E-6D) blastAt(local.point(), true);
            }
        }

        if (processEntities) {
            playerProcessor.process(this, world, seen.x, seen.y, seen.z, affectedPlayers);
        }

        if (sfx != null) {
            for (IExplosionSFX fx : sfx) {
                fx.doEffect(this, world, seen.x, seen.y, seen.z, size);
            }
        }

        HbmEvents.explosion(world, seen.x, seen.y, seen.z, size, exploder, "vnt");
    }

    /** port: the block half of the blast around {@code at}, the centre as the world or one sub-level sees it. */
    private void blastAt(Vec3 at, boolean push) {
        ObjectOpenHashSet<BlockPos> blocks = blockAllocator.allocate(this, world, at.x, at.y, at.z, size);
        List<BlockPos> solid = push ? solid(blocks) : List.of();
        blockProcessor.process(this, world, at.x, at.y, at.z, blocks);
        if (push) SubLevelBlasts.push(world, at, solid);
    }

    private List<BlockPos> solid(ObjectOpenHashSet<BlockPos> blocks) {
        return blocks.stream().filter(pos -> !world.getBlockState(pos).isAir()).map(BlockPos::immutable).toList();
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

    /**
     * 1.7 {@code makeAmat}: 16 rays per edge below size 15 (32 above), no block drops, twice the entity range,
     * 50 RAD per unit of size on top of the blast damage, and the antimatter flash.
     */
    public ExplosionVNT makeAmat() {
        this.setBlockAllocator(new BlockAllocatorStandard(this.size < 15 ? 16 : 32));
        this.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
        this.setEntityProcessor(new EntityProcessorStandard()
                .withRangeMod(2F)
                .withDamageMod(new CustomDamageHandlerAmat(50F)));
        this.setPlayerProcessor(new PlayerProcessorStandard());
        this.setSFX(new ExplosionEffectAmat());
        return this;
    }
}
