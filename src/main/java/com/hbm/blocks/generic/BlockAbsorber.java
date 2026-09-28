package com.hbm.blocks.generic;

import com.hbm.handler.radiation.ChunkRadiationManager;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockAbsorber}, flattened by tier: every ten ticks it takes its tier's amount out of the chunk's
 * radiation (base 2.5, red 10, green 100, pink 10000 RAD) and schedules itself again. 1.7 also ticked randomly,
 * which only added extra rescheduling on top of the fixed ten-tick loop.
 */
public class BlockAbsorber extends CompatBlock {

    // Enum for tiers they are in order of meta data, 0, 1, 2, 3 for Base, Red, Green, Pink
    public enum EnumAbsorberTier {

        BASE(2.5F, "absorber"), RED(10F, "absorber_red"), GREEN(100F, "absorber_green"), PINK(10000F, "absorber_pink");

        public final float absorbAmount;
        public final String textureName;

        EnumAbsorberTier(float absorb, String texture) {
            this.absorbAmount = absorb;
            this.textureName = texture;
        }
    }

    public final EnumAbsorberTier tier;

    public BlockAbsorber(Properties properties, EnumAbsorberTier tier) {
        super(properties);
        this.tier = tier;
    }

    public int tickRate() {
        return 10;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, tickRate());
    }

    // All that rad math shit that was on there already, did not touch this
    // -Wolf
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChunkRadiationManager.proxy.decrementRad(level, pos, tier.absorbAmount);
        level.scheduleTick(pos, this, tickRate());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tick(state, level, pos, random);
    }
}
