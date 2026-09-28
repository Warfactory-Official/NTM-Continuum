package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code hbm:fallen_meteorite}: 1.7 {@code HbmWorldGen}'s fallen meteorite, one chunk in {@code meteoriteSpawn}. The
 * gate, the column and {@code getHeightValue} are the placed feature's {@code rarity_filter} + {@code in_square} +
 * {@code heightmap}; the meteorite is then sunk {@code rand(10)} blocks and built (no specials, no impact damage, not
 * safe) unless the block two below that point is air or liquid.
 *
 * <p>1.7 read that block at {@code y - 2} but asked {@code isAir} at {@code y}: the block type decides, which is what
 * is checked here.
 */
public class FallenMeteoriteFeature extends Feature<NoneFeatureConfiguration> {

    public FallenMeteoriteFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos top = context.origin();
        int y = top.getY() - rand.nextInt(10);
        BlockState b = level.getBlockState(new BlockPos(top.getX(), y - 2, top.getZ()));
        if (b.isAir() || !b.getFluidState().isEmpty() || y <= level.getMinY() + 1) return false;
        new Meteorite().generate(level, rand, new BlockPos(top.getX(), y, top.getZ()), false, false, false);
        return true;
    }
}
