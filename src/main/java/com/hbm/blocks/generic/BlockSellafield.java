package com.hbm.blocks.generic;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.ModTerrainBlocks;

/**
 * 1.7 {@code BlockSellafield} ({@code sellafield}, metas 0..5: Sellafite, Hot, Boiling, Blazing, Infernal,
 * Sellafite-Corium): the still-hot vitrified ground under a fresh crater, flattened to
 * {@code sellafield_<level>}. Each random tick pumps {@code 0.5 * (level + 1)} rads into the chunk and,
 * one time in 25 (level 0) or 15 (otherwise), cools a level; level 0 becomes slaked sellafite. Walking
 * on it gives the radiation effect (amplifier {@code level} below 5, {@code 2 * level} at 5) for 30 s.
 */
public class BlockSellafield extends CompatBlock {

    public static final int LEVELS = 6;

    public final int level;

    public BlockSellafield(Properties properties, int level) {
        super(properties);
        this.level = level;
    }

    private void irradiate(Level world, Entity entity) {
        if (!world.isClientSide() && entity instanceof LivingEntity living) {
            Effects.addRegistered(living, ModEffects.RADIATION.get(), 30 * 20, level < 5 ? level : level * 2);
        }
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(world, pos, state, entity);
        irradiate(world, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        ChunkRadiationManager.proxy.incrementRad(world, pos, 0.5F * (level + 1));

        if (rand.nextInt(level == 0 ? 25 : 15) == 0) {
            if (level > 0) {
                world.setBlock(pos, ModTerrainBlocks.sellafield(level - 1).defaultBlockState(), 2);
            } else {
                world.setBlockAndUpdate(pos, ModFalloutBlocks.sellafield_slaked.get().defaultBlockState());
            }
        }
    }
}
