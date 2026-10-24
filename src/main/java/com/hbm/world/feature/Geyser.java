package com.hbm.world.feature;

import com.hbm.blocks.ModTerrainBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * {@code hbm:geyser_chlorine}: 1.7 {@code world/feature/Geyser} (a converted schematic) as placed by
 * {@code HbmWorldGen}: in plains, one chunk in {@code geyserChlorine} (3000), at the surface of a random column, and only
 * where the block under that surface matches {@code surface}. The rarity, the column and {@code y = getHeightValue}
 * are the placed feature's {@code rarity_filter} + {@code in_square} + {@code heightmap}.
 *
 * <p>The schematic is a 5 x 6 x 5 plug reaching 6 blocks down: walls, a chamber of fluid and ore, two air layers and
 * a cap with the vent in the middle. Its corners and two cells of the second air layer are never written, as in 1.7.
 */
public class Geyser extends Feature<Geyser.Config> {

    public record Config(RuleTest surface, BlockState wall, BlockState fluid, BlockState ore, BlockState cap,
                         BlockState capAlt, BlockState vent) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                FeatureCodecs.rule("surface", () -> FeatureCodecs.block(Blocks.GRASS_BLOCK)).forGetter(Config::surface),
                FeatureCodecs.state("wall", () -> Blocks.STONE).forGetter(Config::wall),
                FeatureCodecs.state("fluid", () -> Blocks.WATER).forGetter(Config::fluid),
                FeatureCodecs.state("ore", ModTerrainBlocks.block_yellowcake).forGetter(Config::ore),
                FeatureCodecs.state("cap", () -> Blocks.GRASS_BLOCK).forGetter(Config::cap),
                FeatureCodecs.state("cap_alt", () -> Blocks.GRAVEL).forGetter(Config::capAlt),
                FeatureCodecs.state("vent", ModTerrainBlocks.geysir_chlorine).forGetter(Config::vent)
        ).apply(i, Config::new));
    }

    /**
     * Layers {@code y+5 .. y+10} of 1.7 {@code generate_r0}, rows z = 0..4, columns x = 0..4.
     * {@code s} wall, {@code w} fluid, {@code y} ore, {@code a} air, {@code G} cap, {@code g} cap_alt,
     * {@code c} the vent, space: not written.
     */
    private static final String[][] LAYERS = {
            {" sss ", "sssss", "sssss", "sssss", " sss "},
            {" sss ", "sssss", "sssss", "sssss", " sss "},
            {" sss ", "swyws", "sywws", "swyys", " sss "},
            {" sss ", "saaas", "saaas", "saaas", " sss "},
            {" sss ", "ss ss", "saaas", "ss ss", " sss "},
            {" GGG ", "GgsGs", "sGcGg", "GsGgG", " GGG "},
    };

    public Geyser(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        // 1.7 HbmWorldGen: the chlorine geyser sits inside the enableDungeons block
        if (!DungeonBlockFeature.dungeonsEnabled(level)) return false;
        Config c = context.config();
        BlockPos top = context.origin();
        if (!c.surface().test(level.getBlockState(top.below()), context.random())) return false;
        generate(level, top.getX(), top.getY(), top.getZ(), c);
        return true;
    }

    /** 1.7 {@code generate_r0}: the plug with its cap at {@code y - 1}, centred on x, z. */
    public static void generate(WorldGenLevel level, int x, int y, int z, Config c) {
        x -= 2;
        y -= 11;
        z -= 2;
        for (int layer = 0; layer < LAYERS.length; layer++) {
            for (int dz = 0; dz < 5; dz++) {
                String row = LAYERS[layer][dz];
                for (int dx = 0; dx < 5; dx++) {
                    BlockState state = switch (row.charAt(dx)) {
                        case 's' -> c.wall();
                        case 'w' -> c.fluid();
                        case 'y' -> c.ore();
                        case 'a' -> Blocks.AIR.defaultBlockState();
                        case 'G' -> c.cap();
                        case 'g' -> c.capAlt();
                        case 'c' -> c.vent();
                        default -> null;
                    };
                    // 1.7 flag 3; worldgen has no neighbours to notify
                    if (state != null) level.setBlock(new BlockPos(x + dx, y + 5 + layer, z + dz), state, 2);
                }
            }
        }
    }
}
