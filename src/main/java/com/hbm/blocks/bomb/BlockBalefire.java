package com.hbm.blocks.bomb;

import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import dev.golem.core.world.CompatFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Balefire — the 1.12.2 {@code Balefire} block: the green-white flame left behind by balefire
 * ordnance, and by a GAMMA-wavelength FEL beam burning through a block.
 *
 * <p>It is a fire block with the spread rules turned up hard. Where vanilla fire tries to catch six
 * neighbours and spreads into a 3×3×(−1..4) box, balefire uses the <b>same box but a flat 500/300
 * catch chance</b> and, critically, a {@code h = 3} radius — a 7×7×6 volume per tick — so it engulfs an
 * area rather than creeping along fuel. It also seeds neighbouring balefire with a lower age, which
 * keeps the fire alive far longer than vanilla's.
 *
 * <p>Standing in it sets you on fire for 10 seconds <i>and</i> applies radiation X (5 s), which is what
 * makes it lethal rather than merely damaging.
 */
public class BlockBalefire extends CompatFireBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;

    public BlockBalefire(Properties properties) {
        super(properties, 1.0F, BlockBalefire::new);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    /** Balefire itself is not "burnable" in the vanilla sense; it spreads by its own rules below. */
    @Override
    protected boolean canBurn(BlockState state) {
        return true;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true; // upstream had no placement restriction; the tick decides whether it lives
    }

    /** The old {@code tickRate(world) + rand.nextInt(10)}; vanilla's own delay helper is private. */
    private static int fireTickDelay(RandomSource random) {
        return 30 + random.nextInt(10);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!dev.golem.core.world.Fires.canSpreadAround(level, pos)) return;

        int age = state.getValue(AGE);
        if (age < 15) {
            level.scheduleTick(pos, this, fireTickDelay(random));
        }

        BlockPos below = pos.below();
        boolean downSolid = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        if (!downSolid && !hasFlammableNeighbour(level, pos)) {
            level.removeBlock(pos, false);
            return;
        }
        if (age >= 15) return;

        // The six direct neighbours: 500 for the horizontals, 300 for up/down (bigger number = less
        // likely; the roll is `rand.nextInt(chance) < flammability`).
        tryCatchFire(level, pos.east(), 500, random, age);
        tryCatchFire(level, pos.west(), 500, random, age);
        tryCatchFire(level, pos.north(), 500, random, age);
        tryCatchFire(level, pos.south(), 500, random, age);
        tryCatchFire(level, pos.above(), 300, random, age);
        tryCatchFire(level, pos.below(), 300, random, age);

        spread(level, pos, random, age);
    }

    /** The 7×7×6 area sweep — {@code h = 3} instead of vanilla's 1. */
    private void spread(ServerLevel level, BlockPos pos, RandomSource random, int age) {
        final int h = 3;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int dx = -h; dx <= h; dx++) {
            for (int dz = -h; dz <= h; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    mpos.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    BlockState s = level.getBlockState(mpos);

                    // Existing balefire gets its age pulled DOWN toward ours, refreshing the fire.
                    if (s.is(this)) {
                        int theirAge = s.getValue(AGE);
                        if (theirAge > age + 1) {
                            level.setBlock(mpos, s.setValue(AGE, Math.min(age + 1, 15)), 3);
                        }
                        continue;
                    }
                    if (!s.isAir()) continue;

                    int neighbourSpread = 0;
                    for (Direction f : Direction.values()) {
                        BlockState ns = level.getBlockState(mpos.relative(f));
                        neighbourSpread = Math.max(fireSpreadSpeed(ns), neighbourSpread);
                    }
                    if (neighbourSpread == 0) continue;

                    int fireLimit = 100;
                    if (dy > 1) fireLimit += (dy - 1) * 100;
                    int adjusted = (neighbourSpread + 40 + level.getDifficulty().getId() * 7) / (age + 30);
                    if (adjusted > 0 && random.nextInt(fireLimit) <= adjusted) {
                        level.setBlock(mpos, defaultBlockState().setValue(AGE, Math.min(age + 1, 15)), 3);
                    }
                }
            }
        }
    }

    private void tryCatchFire(ServerLevel level, BlockPos pos, int chance, RandomSource random, int age) {
        BlockState target = level.getBlockState(pos);
        int flammability = flammability(target);
        if (flammability <= 0) return;
        if (random.nextInt(chance) >= flammability) return;

        level.setBlock(pos, defaultBlockState().setValue(AGE, Math.min(age + 1, 15)), 3);
        if (target.is(Blocks.TNT)) {
            // Upstream called TNT's own "destroyed while lit" path; 26.1's equivalent is to replace the
            // block with a primed entity, which is what that path ultimately did.
            level.removeBlock(pos, false);
            level.addFreshEntity(new net.minecraft.world.entity.item.PrimedTnt(
                    level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, null));
        }
    }

    private static boolean hasFlammableNeighbour(Level level, BlockPos pos) {
        for (Direction f : Direction.values()) {
            if (flammability(level.getBlockState(pos.relative(f))) > 0) return true;
        }
        return false;
    }

    /**
     * 26.1 keeps the burn/spread tables inside the vanilla {@code FireBlock} instance rather than on
     * each block (1.12's {@code Block.getFlammability}/{@code getFireSpreadSpeed}), and both lookups
     * are private — hence the {@link com.hbm.mixin.FireBlockAccessor} invokers, which the fallout
     * sweep already relies on. Reading the real table keeps balefire's spread numerically identical
     * to the original instead of approximating it.
     */
    private static int flammability(BlockState state) {
        return ((com.hbm.mixin.FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(state);
    }

    private static int fireSpreadSpeed(BlockState state) {
        return ((com.hbm.mixin.FireBlockAccessor) Blocks.FIRE).hbm$getIgniteOdds(state);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        Effects.ignite(entity, 10);
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            Effects.addRegistered(living, ModEffects.RADIATION.get(), 5 * 20, 9);
        }
    }
}
