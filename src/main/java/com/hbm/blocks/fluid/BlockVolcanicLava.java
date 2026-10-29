package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockOreBasalt;
import dev.golem.core.fluid.FluidBlock;
import dev.golem.core.fluid.WorldFluids;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code VolcanicBlock} ({@code volcanic_lava_block}): a flowing {@code BlockFluidClassic} of quanta 4 on a
 * 15-tick flow ({@code Material.lava}: lava physics), ticking randomly as well. It flows into (and replaces) anything
 * flammable or replaceable, water included; on every neighbour change it turns water to stone, logs to waste logs,
 * planks to waste planks, leaves to fire and diamond ore to basalt ore. After every flow update with no lava below,
 * a flowing block with fewer than two lava neighbours, or any block one time in five with fewer than five, solidifies:
 * mostly to basalt, rarely to a basalt ore, and to a basalt gem where it is fully surrounded and ten blocks under more
 * basalt or lava. Vanilla-style lava pops and drips. Port: deepslate diamond ore reacts like diamond ore.
 */
public class BlockVolcanicLava extends FluidBlock {

    public BlockVolcanicLava(Properties properties) {
        this(properties, ModWorldFluids.VOLCANIC_LAVA);
    }

    protected BlockVolcanicLava(Properties properties, WorldFluids.Handle fluid) {
        super(properties, fluid);
    }

    /**
     * 1.7 {@code Material.lava}: anything standing in the block is set alight for 15 seconds and takes
     * 4 lava damage per tick ({@code Entity.setOnFireFromLava}), unless it is immune to fire.
     */
    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || entity.fireImmune()) return;
        Effects.ignite(entity, 15); // 1.7 lava setFire(15), Fire Protection applies
        entity.hurt(level.damageSources().lava(), 4F);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level.isClientSide()) return;
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            BlockState reaction = getReaction(level, at);
            if (reaction != null) level.setBlock(at, reaction, 3);
        }
    }

    protected @Nullable BlockState getReaction(Level level, BlockPos pos) {
        BlockState b = level.getBlockState(pos);
        if (b.liquid() && b.getFluidState().is(FluidTags.WATER)) return Blocks.STONE.defaultBlockState(); // 1.7 Material.water
        if (b.is(BlockTags.LOGS)) return ModFalloutBlocks.waste_log.get().defaultBlockState();
        if (b.is(BlockTags.PLANKS)) return ModBlocks.waste_planks.get().defaultBlockState();
        if (b.is(BlockTags.LEAVES)) return Blocks.FIRE.defaultBlockState();
        if (b.is(BlockTags.DIAMOND_ORES)) return ModTerrainBlocks.ore_basalt(BlockOreBasalt.Type.GEM).defaultBlockState();
        return null;
    }

    protected Block getBasaltForCheck() {
        return ModBlocks.basalt.get();
    }

    /**
     * 1.7: anything fire can catch, or anything replaceable, gives way to the lava; else the fluid default. Lava is
     * replaceable itself, so this answers true for the lava's own blocks too: a lava block above more lava re-sets the
     * one below to a falling (meta 1) block on each flow instead of spreading sideways, turning a buried source into
     * flowing lava. Odd, but it is what 1.7 did and it shapes how volcano lava sets, so it is kept.
     */
    @Override
    protected boolean canDisplace(BlockGetter level, BlockPos pos) {
        BlockState b = level.getBlockState(pos);
        if (b.ignitedByLava()) return true; // 1.7 Blocks.fire.getFlammability(b) > 0
        if (b.canBeReplaced()) return true;
        return super.canDisplace(level, pos);
    }

    @Override
    protected boolean displaceIfPossible(Level level, BlockPos pos) {
        return super.displaceIfPossible(level, pos) || canDisplace(level, pos);
    }

    /** 1.7 {@code updateTick} after {@code super.updateTick}: count lava and basalt around, maybe solidify. */
    @Override
    protected void afterFlow(ServerLevel level, BlockPos pos, RandomSource rand) {
        int lavaCount = 0;
        int basaltCount = 0;
        for (Direction dir : Direction.values()) {
            BlockState b = level.getBlockState(pos.relative(dir));
            if (b.is(this)) lavaCount++;
            if (b.is(getBasaltForCheck())) basaltCount++;
        }
        // 1.7's expression as written: (flowing && lavaCount < 2) || (1/5 && lavaCount < 5), and no lava below; the
        // roll is only drawn when the first clause fails
        if (((!isSourceBlock(level, pos) && lavaCount < 2) || (rand.nextInt(5) == 0) && lavaCount < 5)
                && !level.getBlockState(pos.below()).is(this)) {
            onSolidify(level, pos, lavaCount, basaltCount, rand);
        }
    }

    protected void onSolidify(ServerLevel level, BlockPos pos, int lavaCount, int basaltCount, RandomSource rand) {
        int r = rand.nextInt(200);
        BlockState above = level.getBlockState(pos.above(10));
        boolean canMakeGem = lavaCount + basaltCount == 6 && lavaCount < 3 && (above.is(ModBlocks.basalt.get()) || above.is(this));
        Block block;
        if (r < 2) block = ModTerrainBlocks.ore_basalt(BlockOreBasalt.Type.SULFUR);
        else if (r == 2) block = ModTerrainBlocks.ore_basalt(BlockOreBasalt.Type.FLUORITE);
        else if (r == 3) block = ModTerrainBlocks.ore_basalt(BlockOreBasalt.Type.ASBESTOS);
        else if (r == 4) block = ModTerrainBlocks.ore_basalt(BlockOreBasalt.Type.MOLYSITE);
        else if (r < 15 && canMakeGem) block = ModTerrainBlocks.ore_basalt(BlockOreBasalt.Type.GEM);
        else block = ModBlocks.basalt.get();
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        BlockState above = level.getBlockState(pos.above());
        if (above.isAir()) {
            if (rand.nextInt(100) == 0) {
                double dx = pos.getX() + rand.nextFloat();
                double dy = pos.getY() + state.getFluidState().getHeight(level, pos); // 1.7 maxY
                double dz = pos.getZ() + rand.nextFloat();
                level.addParticle(ParticleTypes.LAVA, dx, dy, dz, 0.0D, 0.0D, 0.0D);
                level.playLocalSound(dx, dy, dz, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
            if (rand.nextInt(200) == 0) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        }
        if (rand.nextInt(10) == 0 && Compat.isSolidRender(level.getBlockState(pos.below())) && level.getBlockState(pos.below(2)).getCollisionShape(level, pos.below(2)).isEmpty()) {
            double dx = pos.getX() + rand.nextFloat();
            double dy = pos.getY() - 1.05D;
            double dz = pos.getZ() + rand.nextFloat();
            level.addParticle(ParticleTypes.DRIPPING_LAVA, dx, dy, dz, 0.0D, 0.0D, 0.0D);
        }
    }
}
