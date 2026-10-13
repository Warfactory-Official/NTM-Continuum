package com.hbm.blocks.generic;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.config.TerrainConfig;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Effects;
import dev.golem.core.world.LightLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The contaminated ground blocks from 1.7's {@code WasteEarth}: {@code waste_earth} (dead grass) and
 * {@code waste_mycelium} (glowing mycelium - radiation on contact + spore particles). Only the mycelium had an
 * {@code onEntityWalking} effect (1.7: radiation amplifier 3 for 30 s, i.e. 0.2 RAD/tick; not the 1.12.2
 * amplifier 29). The random tick is 1.7's {@code updateTick}: mycelium spread, the regrowth to
 * dirt ({@code RADWORLD_03_regrow}, or always in the dark under an opaque block) and the mushroom swap. The
 * frozen/burning variants are not ported.
 */
public class BlockWasteEarth extends CompatBlock {

    private final boolean mycelium;

    public BlockWasteEarth(Properties props, boolean mycelium) {
        super(props);
        this.mycelium = mycelium;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        super.stepOn(level, pos, onState, entity);
        irradiate(level, entity);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        irradiate(level, entity);
    }

    private void irradiate(Level level, Entity entity) {
        if (mycelium && !level.isClientSide() && entity instanceof LivingEntity living) {
            Effects.addRegistered(living, ModEffects.RADIATION.get(), 30 * 20, 3);
        }
    }

    /** 1.7 {@code updateTick}, the waste_earth / waste_mycelium branches. */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (mycelium && TerrainConfig.enableMycelium) {
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    for (int k = -1; k < 2; k++) {
                        at.set(pos.getX() + i, pos.getY() + j, pos.getZ() + k);
                        BlockState b0 = level.getBlockState(at);
                        BlockPos above = at.above();
                        BlockState b1 = level.getBlockState(above);
                        if (!Compat.isSolidRender(b1, level, above) && spreadsOnto(b0)) {
                            level.setBlockAndUpdate(at, ModFalloutBlocks.waste_mycelium.get().defaultBlockState());
                        }
                    }
                }
            }
        }

        BlockPos above = pos.above();
        if (TerrainConfig.cleanupDeadDirt || (LightLevels.combined(level, above) < 4
                && LightLevels.opacity(level, above) > 2)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }

        // 1.7: a BlockMushroom on top turns into the glowing mushroom
        if (level.getBlockState(above).getBlock() instanceof MushroomBlock) {
            level.setBlockAndUpdate(above, ModTerrainBlocks.mush.get().defaultBlockState());
        }
    }

    /** 1.7: {@code dirt} (every meta: plain, coarse, podzol), {@code grass}, {@code mycelium}, {@code waste_earth}. */
    private static boolean spreadsOnto(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM) || state.is(ModFalloutBlocks.waste_earth.get());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (mycelium) {
            level.addParticle(ParticleTypes.MYCELIUM,
                    pos.getX() + random.nextFloat(), pos.getY() + 1.1F, pos.getZ() + random.nextFloat(), 0, 0, 0);
        }
    }
}
