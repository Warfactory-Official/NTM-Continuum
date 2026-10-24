package com.hbm.world.feature;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.lib.HbmBlockTags;
import com.hbm.world.gen.ConfigInt;
import com.hbm.world.gen.WorldgenKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;

import static com.hbm.world.feature.FeatureCodecs.field;
import static com.hbm.world.feature.FeatureCodecs.lazy;

/**
 * {@code hbm:ore_layers}: 1.7's {@code OreLayer3D} layers, in list order (hematite, bauxite, malachite). A cell between
 * {@code min_y} and {@code max_y} becomes the layer's {@code state} when {@code nX · nY · nZ > threshold} and it is a
 * solid host block. The thresholds are in the hundreds because the product is of unnormalised noise.
 * <pre>
 *   nX = noiseX(y · scale_v, z · scale_h)
 *   nY = noiseY(x · scale_h, z · scale_h)
 *   nZ = noiseX(y · scale_v, (chunkZ + 8 + xOff) · scale_h)     // sic
 * </pre>
 * The 1.7 sampling bug is ported: 1.7 reads {@code nz = cacheX[ox][y]}, noiseX again at the z the x offset indexes, so
 * noiseZ is never read. The fields are seeded {@code seed + 101 + seed_offset} and {@code seed + 102 + seed_offset}
 * (1.7's static registration counter, pinned per layer).
 */
public class OreLayer3DFeature extends Feature<OreLayer3DFeature.Config> {

    public record Layer(BlockState state, double scaleH, double scaleV, double threshold, long seedOffset,
                        ConfigInt enabled) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(Layer::state),
                Codec.DOUBLE.fieldOf("scale_h").forGetter(Layer::scaleH),
                Codec.DOUBLE.fieldOf("scale_v").forGetter(Layer::scaleV),
                Codec.DOUBLE.fieldOf("threshold").forGetter(Layer::threshold),
                field(Codec.LONG, "seed_offset", 0L).forGetter(Layer::seedOffset),
                field(ConfigInt.CODEC, "enabled", ConfigInt.of(1)).forGetter(Layer::enabled)
        ).apply(i, Layer::new));
    }

    public record Config(List<Layer> layers, RuleTest host, int minY, int maxY) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                lazy(Layer.CODEC.listOf(), "layers", Config::layers17).forGetter(Config::layers),
                FeatureCodecs.rule("host", () -> FeatureCodecs.tag(HbmBlockTags.LEGACY_STONE)).forGetter(Config::host),
                field(Codec.INT, "min_y", 6).forGetter(Config::minY),
                field(Codec.INT, "max_y", 64).forGetter(Config::maxY)
        ).apply(i, Config::new));

        static List<Layer> layers17() {
            return List.of(
                    new Layer(ModOreBlocks.stone_resource_hematite.get().defaultBlockState(), 0.04D, 0.25D, 230D, 0,
                            ConfigInt.key(WorldgenKeys.HEMATITE)),
                    new Layer(ModOreBlocks.stone_resource_bauxite.get().defaultBlockState(), 0.03D, 0.15D, 300D, 1,
                            ConfigInt.key(WorldgenKeys.BAUXITE)),
                    new Layer(ModOreBlocks.stone_resource_malachite.get().defaultBlockState(), 0.10D, 0.15D, 275D, 2,
                            ConfigInt.key(WorldgenKeys.MALACHITE)));
        }
    }

    public OreLayer3DFeature(Codec<Config> codec) {
        super(codec);
    }

    private static LegacyPerlinNoise noiseX(long seed, Layer layer) {
        return NoiseCache.get(seed + 101L + layer.seedOffset(), 4);
    }

    private static LegacyPerlinNoise noiseY(long seed, Layer layer) {
        return NoiseCache.get(seed + 102L + layer.seedOffset(), 4);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config c = context.config();
        WorldGenLevel level = context.level();
        int cx = context.origin().getX();
        int cz = context.origin().getZ();
        for (Layer layer : c.layers()) {
            if (layer.enabled().get() == 0) continue;
            generate(level, context.random(), cx, cz, c, layer);
        }
        return true;
    }

    /**
     * Test seam: the noise product 1.7 compares against the threshold for one cell of the chunk at block
     * {@code (cx, cz)}, offsets {@code xOff, zOff}, default layer {@code 0..2} (hematite, bauxite, malachite).
     */
    public static double cellProduct17(long seed, int layerId, int cx, int cz, int xOff, int zOff, int y) {
        Layer layer = Config.layers17().get(layerId);
        int x = cx + 8 + xOff;
        int z = cz + 8 + zOff;
        double nY = noiseY(seed, layer).getValue(x * layer.scaleH(), z * layer.scaleH());
        double nX = noiseX(seed, layer).getValue(y * layer.scaleV(), z * layer.scaleH());
        double nZ = noiseX(seed, layer).getValue(y * layer.scaleV(), (cz + 8 + xOff) * layer.scaleH());
        return nX * nY * nZ;
    }

    private static void generate(WorldGenLevel level, RandomSource rand, int cx, int cz, Config c, Layer layer) {
        LegacyPerlinNoise noiseX = noiseX(level.getSeed(), layer);
        LegacyPerlinNoise noiseY = noiseY(level.getSeed(), layer);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startX = cx + 8;   // the 1.7 +8 decoration offset
        int startZ = cz + 8;
        int floor = level.getMinY();
        int ceiling = level.getMaxY();

        for (int xOff = 0; xOff < 16; xOff++) {
            int x = startX + xOff;
            for (int zOff = 0; zOff < 16; zOff++) {
                int z = startZ + zOff;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                double nY = noiseY.getValue(x * layer.scaleH(), z * layer.scaleH());

                for (int y = c.maxY(); y >= c.minY(); y--) {
                    if (y < floor || y > ceiling) continue;
                    double nX = noiseX.getValue(y * layer.scaleV(), z * layer.scaleH());
                    double nZ = noiseX.getValue(y * layer.scaleV(), (startZ + xOff) * layer.scaleH());
                    if (nX * nY * nZ <= layer.threshold()) continue;

                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (Compat.isSolidRender(state) && c.host().test(state, rand)) {
                        level.setBlock(pos, layer.state(), 2 | 16);
                    }
                }
            }
        }
    }
}
