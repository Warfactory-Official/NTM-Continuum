package com.hbm.world.feature;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.generic.BlockGlyphid;
import com.hbm.util.LootGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code world/feature/GlyphidHive}: the small hive, an 11 x 5 x 11 dome of {@code glyphid_base} with a
 * spawner-studded core and a 3 x 3 floor of skulls and loot piles. Used by worldgen ({@code GlyphidHiveFeature}) and
 * by a glyphid scout that has found a spot ({@code EntityGlyphidScout}), exactly as in 1.7.
 */
public final class GlyphidHive {

    private GlyphidHive() {}

    /** 1.7 {@code schematicSmall}, indexed {@code [4 - j][i][k]}: 1 hive block, 2 hive or (1 in 3) spawner, 3 floor. */
    public static final int[][][] SCHEMATIC_SMALL = new int[][][] {
            {
                    {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,1,1,1,0,0,0,0}, {0,0,0,0,1,1,1,0,0,0,0}, {0,0,0,0,1,1,1,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0},
            },
            {
                    {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,1,1,1,0,0,0,0}, {0,0,0,1,1,1,1,1,0,0,0},
                    {0,0,1,1,1,1,1,1,1,0,0}, {0,0,1,1,1,1,1,1,1,0,0}, {0,0,1,1,1,1,1,1,1,0,0}, {0,0,0,1,1,1,1,1,0,0,0},
                    {0,0,0,0,1,1,1,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0},
            },
            {
                    {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,1,1,1,0,0,0,0}, {0,0,1,1,1,1,1,1,1,0,0}, {0,0,1,1,1,1,1,1,1,0,0},
                    {0,1,1,1,3,3,3,1,1,1,0}, {0,1,1,1,3,3,3,1,1,1,0}, {0,1,1,1,3,3,3,1,1,1,0}, {0,0,1,1,1,1,1,1,1,0,0},
                    {0,0,1,1,1,1,1,1,1,0,0}, {0,0,0,0,1,1,1,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0},
            },
            {
                    {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,1,1,1,0,0,0,0}, {0,0,1,1,1,1,1,1,1,0,0}, {0,0,1,1,2,2,2,1,1,0,0},
                    {0,1,1,2,2,2,2,2,1,1,0}, {0,1,1,2,2,2,2,2,1,1,0}, {0,1,1,2,2,2,2,2,1,1,0}, {0,0,1,1,2,2,2,1,1,0,0},
                    {0,0,1,1,1,1,1,1,1,0,0}, {0,0,0,0,1,1,1,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0},
            },
            {
                    {0,0,0,0,0,0,0,0,0,0,0}, {0,0,0,0,1,1,1,0,0,0,0}, {0,0,1,1,1,1,1,1,1,0,0}, {0,0,1,1,1,1,1,1,1,0,0},
                    {0,1,1,1,1,1,1,1,1,1,0}, {0,1,1,1,1,1,1,1,1,1,0}, {0,1,1,1,1,1,1,1,1,1,0}, {0,0,1,1,1,1,1,1,1,0,0},
                    {0,0,1,1,1,1,1,1,1,0,0}, {0,0,0,0,1,1,1,0,0,0,0}, {0,0,0,0,0,0,0,0,0,0,0},
            }
    };

    /**
     * 1.7 {@code generateSmall}. {@code infected} is the metadata override (1 = the infested hive); {@code loot}
     * turns the third kind of floor block into a hive loot pile (worldgen) instead of a hive block (scouts).
     */
    public static void generateSmall(LevelAccessor level, int x, int y, int z, RandomSource rand, boolean infected, boolean loot) {
        BlockGlyphid.HiveType type = infected ? BlockGlyphid.HiveType.INFESTED : BlockGlyphid.HiveType.NORMAL;
        BlockState base = ModDecoCrateBlocks.glyphid_base.get().defaultBlockState().setValue(BlockGlyphid.TYPE, type);
        BlockState spawner = ModDecoCrateBlocks.glyphid_spawner.get().defaultBlockState().setValue(BlockGlyphid.TYPE, type);
        BlockState pile = ModDungeonBlocks.deco_loot.get().defaultBlockState();

        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 5; j++) {
                for (int k = 0; k < 11; k++) {
                    int block = SCHEMATIC_SMALL[4 - j][i][k];
                    BlockPos pos = new BlockPos(x + i - 5, y + j - 2, z + k - 5);
                    switch (block) {
                        case 1 -> level.setBlock(pos, base, 2);
                        case 2 -> level.setBlock(pos, rand.nextInt(3) == 0 ? spawner : base, 2);
                        case 3 -> {
                            int r = rand.nextInt(3);
                            if (r == 0) {
                                // 1.7: Blocks.skull meta 1 (on the floor), the skull tile's rotation rand(16), type 0 = skeleton
                                level.setBlock(pos, Blocks.SKELETON_SKULL.defaultBlockState()
                                        .setValue(SkullBlock.ROTATION, rand.nextInt(16)), 3);
                            } else if (r == 1) {
                                level.setBlock(pos, pile, 2);
                                LootGenerator.lootBones(level, pos);
                            } else if (loot) {
                                level.setBlock(pos, pile, 2);
                                LootGenerator.lootGlyphidHive(level, pos);
                            } else {
                                level.setBlock(pos, base, 2);
                            }
                        }
                        default -> { }
                    }
                }
            }
        }
    }
}
