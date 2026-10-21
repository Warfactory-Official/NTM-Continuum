package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockFissure;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.world.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A TNT-family block whose primed entity goes off as a medium mini-nuke ({@link ExplosionNukeSmall#PARAMS_MEDIUM}).
 * It then turns bedrock ore within 5 blocks into {@code ore_volcano} (the crater variant inside a crater biome) and
 * {@code ore_bedrock_oil} into bedrock.
 */
public class BlockFissureBomb extends BlockTNTBase {

    public static final int KIND = 4;
    private static final int RANGE = 5;

    public BlockFissureBomb(Properties properties) {
        super(properties);
    }

    @Override public int kindId() { return KIND; }
    @Override public String textureName() { return "fissure_bomb"; }
    @Override public float blastStrength() { return 0F; }

    @Override
    public void detonate(Level level, double x, double y, double z, EntityPrimedExplosive primed) {
        ExplosionNukeSmall.explode(level, x, y, z, ExplosionNukeSmall.PARAMS_MEDIUM);
        convertBedrockOres(level, x, y, z);
    }

    public static void convertBedrockOres(Level level, double x, double y, double z) {
        Holder<Biome> biome = level.getBiome(BlockPos.containing(x, y, z));
        boolean crater = biome.is(ModBiomes.crater) || biome.is(ModBiomes.crater_inner) || biome.is(ModBiomes.crater_outer);
        BlockState volcano = ModTerrainBlocks.ore_volcano.get().defaultBlockState().setValue(BlockFissure.CRATER, crater);
        for (int i = -RANGE; i <= RANGE; i++) {
            for (int j = -RANGE; j <= RANGE; j++) {
                for (int k = -RANGE; k <= RANGE; k++) {
                    BlockPos pos = BlockPos.containing(x + i, y + j, z + k);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModOreBlocks.ore_bedrock_block.get())) {
                        level.setBlock(pos, volcano, 3);
                    } else if (state.is(ModBlocks.ore_bedrock_oil.get())) {
                        level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}
