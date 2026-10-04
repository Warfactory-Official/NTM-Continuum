package com.hbm.world.feature;

import com.hbm.blocks.ModTerrainBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code hbm:geyser_chlorine}: 1.7 {@code world/feature/Geyser} (a converted schematic) as placed by
 * {@code HbmWorldGen}: in plains, one chunk in {@code geyserChlorine} (3000), at the surface of a random column, and only
 * where that surface is grass. The rarity, the column and {@code y = getHeightValue} are the placed feature's
 * {@code rarity_filter} + {@code in_square} + {@code heightmap}; the grass check stays here.
 *
 * <p>The schematic is a 5 x 6 x 5 plug reaching 6 blocks down: stone walls, a chamber of water and yellowcake, two air
 * layers and a grass/gravel cap with {@code geysir_chlorine} in the middle. Its corners and two cells of the second
 * air layer are never written, exactly as in 1.7.
 */
public class Geyser extends Feature<NoneFeatureConfiguration> {

    /**
     * Layers {@code y+5 .. y+10} of 1.7 {@code generate_r0}, rows z = 0..4, columns x = 0..4.
     * {@code s} stone, {@code w} water, {@code y} yellowcake, {@code a} air, {@code G} grass, {@code g} gravel,
     * {@code c} the chlorine geyser, space: not written.
     */
    private static final String[][] LAYERS = {
            {" sss ", "sssss", "sssss", "sssss", " sss "},
            {" sss ", "sssss", "sssss", "sssss", " sss "},
            {" sss ", "swyws", "sywws", "swyys", " sss "},
            {" sss ", "saaas", "saaas", "saaas", " sss "},
            {" sss ", "ss ss", "saaas", "ss ss", " sss "},
            {" GGG ", "GgsGs", "sGcGg", "GsGgG", " GGG "},
    };

    public Geyser(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        // 1.7 HbmWorldGen: the chlorine geyser sits inside the enableDungeons block (History §19ej-6)
        if (!DungeonBlockFeature.dungeonsEnabled(level)) return false;
        BlockPos top = context.origin();
        if (!level.getBlockState(top.below()).is(Blocks.GRASS_BLOCK)) return false;
        generate(level, top.getX(), top.getY(), top.getZ());
        return true;
    }

    /** 1.7 {@code generate_r0}: the plug with its cap at {@code y - 1}, centred on x, z. */
    public static void generate(WorldGenLevel level, int x, int y, int z) {
        x -= 2;
        y -= 11;
        z -= 2;
        for (int layer = 0; layer < LAYERS.length; layer++) {
            for (int dz = 0; dz < 5; dz++) {
                String row = LAYERS[layer][dz];
                for (int dx = 0; dx < 5; dx++) {
                    BlockState state = switch (row.charAt(dx)) {
                        case 's' -> Blocks.STONE.defaultBlockState();
                        case 'w' -> Blocks.WATER.defaultBlockState();
                        case 'y' -> ModTerrainBlocks.block_yellowcake.get().defaultBlockState();
                        case 'a' -> Blocks.AIR.defaultBlockState();
                        case 'G' -> Blocks.GRASS_BLOCK.defaultBlockState();
                        case 'g' -> Blocks.GRAVEL.defaultBlockState();
                        case 'c' -> ModTerrainBlocks.geysir_chlorine.get().defaultBlockState();
                        default -> null;
                    };
                    if (state != null) level.setBlock(new BlockPos(x + dx, y + 5 + layer, z + dz), state, 2); // 1.7 flag 3; worldgen has no neighbours to notify
                }
            }
        }
    }
}
