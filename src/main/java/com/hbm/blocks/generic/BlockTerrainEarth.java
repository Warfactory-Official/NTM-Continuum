package com.hbm.blocks.generic;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModBombs2Blocks;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import dev.golem.core.world.Fires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * 1.7 {@code WasteEarth} in its {@code burning_earth} branch: sets whatever walks on it on fire for 5 s,
 * one random tick in five spreads to the grass-like blocks around it (not in the rain), clears leaves and
 * bushes, thaws frozen dirt and lights flammable neighbours; every random tick it burns out, and liquid or
 * a solid block above smothers it. Drops dirt. The frozen ground it interacts with is the bombs2 family's.
 *
 * <p>Deviation: upstream's burnt-out ground was {@code impact_dirt}, which is not in this tree; plain
 * dirt stands in for it.
 */
public class BlockTerrainEarth extends dev.golem.core.world.CompatBlock {

    public BlockTerrainEarth(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // 1.7 WasteEarth: one dirt; 1.7 harvestBlock checks silk touch before getDrops, and this is a plain cube without a tile entity, so silk touch keeps it.
        if (OreDrops.silkTouch(params)) return List.of(new ItemStack(this));
        return List.of(new ItemStack(Blocks.DIRT));
    }

    /** 1.7 {@code onEntityWalking}: 5 s of fire. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (entity instanceof LivingEntity living) Effects.ignite(living, 5);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        level.addParticle(ParticleTypes.FLAME, pos.getX() + rand.nextFloat(), pos.getY() + 1.1F, pos.getZ() + rand.nextFloat(), 0, 0, 0);
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + 1.1F, pos.getZ() + rand.nextFloat(), 0, 0, 0);
    }

    private static boolean isGrassLike(Block b) {
        return b == Blocks.GRASS_BLOCK || b == Blocks.MYCELIUM || b == ModFalloutBlocks.waste_earth.get()
                || b == ModBombs2Blocks.frozen_grass.get() || b == ModFalloutBlocks.waste_mycelium.get();
    }

    private static boolean isLeavesOrBush(BlockState state) {
        return state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LEAVES) || state.getBlock() instanceof VegetationBlock;
    }

    /** 1.7 {@code WasteEarth.updateTick}, burning branch: one in five ticks it spreads; every tick it burns out. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(5) == 0) {
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    for (int k = -1; k < 2; k++) {
                        BlockPos at = pos.offset(i, j, k);
                        if (!level.isLoaded(at)) continue;

                        BlockState b0 = level.getBlockState(at);
                        BlockState b1 = level.getBlockState(at.above());
                        boolean coveredAbove = Compat.isSolidRender(b1, level, at.above());

                        if (!coveredAbove && isGrassLike(b0.getBlock()) && !level.isRainingAt(pos)) {
                            level.setBlockAndUpdate(at, defaultBlockState());
                        }
                        if (isLeavesOrBush(b0)) {
                            level.removeBlock(at, false);
                        }
                        if (b0.is(ModBombs2Blocks.frozen_dirt.get())) {
                            level.setBlockAndUpdate(at, Blocks.DIRT.defaultBlockState());
                        }
                        // 1.7 tests the neighbour's flammability but lights the fire over the core block
                        if (Fires.isFlammable(level, at.above(), b1, Direction.UP) && !isLeavesOrBush(b1)
                                && level.getBlockState(pos.above()).isAir()) {
                            level.setBlockAndUpdate(pos.above(), Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }
        }
        // 1.7: impact_dirt (unported) -> dirt
        level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
    }

    /** 1.7 {@code onNeighborBlockChange}: burning ground smothered by liquid or a solid block turns to dirt. */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        BlockPos above = pos.above();
        BlockState b = level.getBlockState(above);
        if (!b.getFluidState().isEmpty() || Compat.isSolidRender(b, level, above)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }
}
