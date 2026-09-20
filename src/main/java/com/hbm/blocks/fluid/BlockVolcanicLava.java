package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code VolcanicBlock} ({@code volcanic_lava_block}): lava that turns water to stone, logs to
 * waste logs, planks to waste planks, leaves to fire and diamond ore to basalt ore on every neighbour
 * change; on a random tick, with few lava neighbours (fewer than 2, or one chance in five below 5) and
 * no lava below, it solidifies — mostly to basalt, rarely to a basalt ore, and to a basalt gem where it
 * is fully surrounded and ten blocks under more basalt or lava. Vanilla-style lava pops and drips.
 *
 * <p>{@code ore_basalt} is not ported, so every basalt-ore outcome is plain basalt here (marked below).
 */
public class BlockVolcanicLava extends BlockStaticFluid {

    public BlockVolcanicLava(Properties properties) {
        super(properties);
    }

    /**
     * 1.7 {@code Material.lava}: anything standing in the block is set alight for 15 seconds and takes
     * 4 lava damage per tick ({@code Entity.setOnFireFromLava}), unless it is immune to fire.
     */
    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || entity.fireImmune()) return;
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 15 * 20));
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
        if (b.getFluidState().is(Fluids.WATER) || b.getFluidState().is(Fluids.FLOWING_WATER)) return Blocks.STONE.defaultBlockState();
        if (b.is(BlockTags.LOGS)) return ModFalloutBlocks.waste_log.get().defaultBlockState();
        if (b.is(BlockTags.PLANKS)) return ModBlocks.waste_planks.get().defaultBlockState();
        if (b.is(BlockTags.LEAVES)) return Blocks.FIRE.defaultBlockState();
        if (b.is(Blocks.DIAMOND_ORE)) return ModBlocks.basalt.get().defaultBlockState(); // 1.7: ore_basalt (unported)
        return null;
    }

    protected Block getBasaltForCheck() {
        return ModBlocks.basalt.get();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int lavaCount = 0;
        int basaltCount = 0;
        for (Direction dir : Direction.values()) {
            BlockState b = level.getBlockState(pos.relative(dir));
            if (b.is(this)) lavaCount++;
            if (b.is(getBasaltForCheck())) basaltCount++;
        }
        // 1.7: (!source && lavaCount < 2) || (rand 1/5 && lavaCount < 5); a static block is always a source.
        if ((rand.nextInt(5) == 0 && lavaCount < 5) && !level.getBlockState(pos.below()).is(this)) {
            onSolidify(level, pos, lavaCount, basaltCount, rand);
        }
    }

    protected void onSolidify(ServerLevel level, BlockPos pos, int lavaCount, int basaltCount, RandomSource rand) {
        int r = rand.nextInt(200);
        BlockState above = level.getBlockState(pos.above(10));
        boolean canMakeGem = lavaCount + basaltCount == 6 && lavaCount < 3 && (above.is(ModBlocks.basalt.get()) || above.is(this));
        BlockState basalt = ModBlocks.basalt.get().defaultBlockState();
        // 1.7: r < 2 -> ore_basalt:0, 2 -> :1, 3 -> :2, 4 -> :4, r < 15 && canMakeGem -> :3 (gem); all unported -> basalt.
        if (r < 5 || (r < 15 && canMakeGem)) level.setBlock(pos, basalt, 3);
        else level.setBlock(pos, basalt, 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        BlockState above = level.getBlockState(pos.above());
        if (above.isAir()) {
            if (rand.nextInt(100) == 0) {
                double dx = pos.getX() + rand.nextFloat();
                double dy = pos.getY() + 0.875D;
                double dz = pos.getZ() + rand.nextFloat();
                level.addParticle(ParticleTypes.LAVA, dx, dy, dz, 0.0D, 0.0D, 0.0D);
                level.playLocalSound(dx, dy, dz, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
            if (rand.nextInt(200) == 0) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        }
        if (rand.nextInt(10) == 0 && dev.golem.core.world.Compat.isSolidRender(level.getBlockState(pos.below())) && level.getBlockState(pos.below(2)).getCollisionShape(level, pos.below(2)).isEmpty()) {
            double dx = pos.getX() + rand.nextFloat();
            double dy = pos.getY() - 1.05D;
            double dz = pos.getZ() + rand.nextFloat();
            level.addParticle(ParticleTypes.DRIPPING_LAVA, dx, dy, dz, 0.0D, 0.0D, 0.0D);
        }
    }
}
