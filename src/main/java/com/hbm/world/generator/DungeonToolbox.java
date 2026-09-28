package com.hbm.world.generator;

import com.hbm.world.gen.LegacyDungeonWorld;
import com.hbm.world.gen.component.LegacyBlock;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * 1.7 {@code com.hbm.world.generator.DungeonToolbox}: the box and random-pick helpers of the legacy dungeons, on
 * {@link LegacyDungeonWorld}. 1.7's ore and flower helpers are world features in this port ({@code ModBiomeModifiers}).
 */
public class DungeonToolbox {

    /** 1.7 {@code RecipesCommon.MetaBlock}: a block and its metadata. */
    public record MetaBlock(LegacyBlock block, int meta) {
        public MetaBlock(LegacyBlock block) {
            this(block, 0);
        }
    }

    public static void generateBox(LegacyDungeonWorld world, int x, int y, int z, int sx, int sy, int sz, List<MetaBlock> blocks) {

        if(blocks.isEmpty())
            return;

        for(int i = x; i < x + sx; i++) {
            for(int j = y; j < y + sy; j++) {
                for(int k = z; k < z + sz; k++) {
                    MetaBlock b = getRandom(blocks, world.rand);
                    world.setBlock(i, j, k, b.block, b.meta, 2);
                }
            }
        }
    }

    public static void generateBox(LegacyDungeonWorld world, int x, int y, int z, int sx, int sy, int sz, LegacyBlock block) {
        generateBox(world, x, y, z, sx, sy, sz, new MetaBlock(block));
    }

    //i know it's copy paste, but it's a better strat than using a wrapper and generating single-entry lists for no good reason
    public static void generateBox(LegacyDungeonWorld world, int x, int y, int z, int sx, int sy, int sz, MetaBlock block) {

        for(int i = x; i < x + sx; i++) {

            for(int j = y; j < y + sy; j++) {

                for(int k = z; k < z + sz; k++) {

                    world.setBlock(i, j, k, block.block, block.meta, 2);
                }
            }
        }
    }

    public static <T> T getRandom(List<T> list, RandomSource rand) {

        if(list.isEmpty())
            return null;

        return list.get(rand.nextInt(list.size()));
    }
}
