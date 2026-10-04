package com.hbm.world.feature;

import com.hbm.blocks.ModDecoContent;
import com.hbm.saveddata.TomSaveData;
import com.mojang.serialization.Codec;
import dev.golem.core.world.Vegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The worldgen half of 1.7.10's {@code ModEventHandlerImpact}, for chunks generated after Tom's impact, run once per
 * overworld chunk at {@code top_layer_modification} (after every decoration):
 *
 * <ul>
 *   <li>{@code postImpactGeneration}: while there is dust or fire, grassy biomes' top block is {@code impact_dirt}
 *       - here every grass block of the chunk;</li>
 *   <li>{@code postImpactDecoration}: while there is dust or fire, the TREE, BIG_SHROOM, GRASS, REED, FLOWERS,
 *       DEAD_BUSH, CACTUS, PUMPKIN and LILYPAD decorations are denied; once both are gone, trees, huge mushrooms and
 *       cacti each come back in one chunk of nine;</li>
 *   <li>{@code populateChunkPost}: with dust above 0.25 or any fire, every grass block becomes impact dirt and every
 *       log, leaf, plant and bush is removed.</li>
 * </ul>
 *
 * <p>Deviation: 1.7 vetoed the decoration events before they placed anything. Modern decorations are placed
 * features with no runtime veto, so this removes what a denied decoration placed in the chunk; it removes them in the
 * chunk being generated only, so a tree a neighbour grew across the border loses its part inside this chunk, as it
 * did in 1.7's own post-populate wipe. The 1.7 decoration rolls were one per chunk, so is the 1-in-9 recovery roll.
 */
public class ImpactAftermathFeature extends Feature<NoneFeatureConfiguration> {

    public ImpactAftermathFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** One chunk's verdict, computed once (public for the GameTests). */
    public record Verdict(boolean dusty, boolean wipe, boolean trees, boolean shrooms, boolean cacti) {
        public static Verdict of(float dust, float fire, RandomSource rand) {
            boolean dusty = dust > 0 || fire > 0;
            boolean wipe = dust > 0.25F || fire > 0;
            if (dusty) return new Verdict(true, wipe, false, false, false);
            // 1.7: TREE / BIG_SHROOM / CACTUS each DEFAULT on world.rand.nextInt(9) == 0, otherwise DENY
            return new Verdict(false, false, rand.nextInt(9) == 0, rand.nextInt(9) == 0, rand.nextInt(9) == 0);
        }

        /** What this block becomes, or null to leave it. */
        public BlockState apply(BlockState state, BlockState impactDirt) {
            if (dusty) {
                if (state.is(Blocks.GRASS_BLOCK)) return impactDirt;
                if (isTree(state) || isShroom(state) || state.is(Blocks.CACTUS) || isDecorationPlant(state)) return AIR;
                if (wipe && (state.is(BlockTags.LOGS) || Vegetation.isLeaves(state) || Vegetation.isPlant(state))) return AIR;
                return null;
            }
            if (!trees && isTree(state)) return AIR;
            if (!shrooms && isShroom(state)) return AIR;
            if (!cacti && state.is(Blocks.CACTUS)) return AIR;
            return null;
        }
    }

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    static boolean isTree(BlockState state) {
        return state.is(BlockTags.LOGS) || Vegetation.isLeaves(state);
    }

    static boolean isShroom(BlockState state) {
        return state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.MUSHROOM_STEM);
    }

    /** GRASS, REED, FLOWERS, DEAD_BUSH, PUMPKIN, LILYPAD: the plants 1.7's decorator scattered. */
    static boolean isDecorationPlant(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN) || state.is(Blocks.SUGAR_CANE) || state.is(BlockTags.FLOWERS)
                || state.is(Blocks.DEAD_BUSH) || state.is(Blocks.PUMPKIN) || state.is(Blocks.LILY_PAD);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        TomSaveData data = TomSaveData.published(level.getLevel());
        if (data == null || !data.impact) return false;

        Verdict verdict = Verdict.of(data.dust, data.fire, context.random());
        BlockState impactDirt = ModDecoContent.impact_dirt.get().defaultBlockState();
        BlockPos origin = context.origin();
        ChunkAccess chunk = level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        boolean changed = false;

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;
            int baseY = (chunk.getMinSectionY() + i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.isAir()) continue;
                        BlockState to = verdict.apply(state, impactDirt);
                        if (to == null) continue;
                        level.setBlock(pos.set(baseX + x, baseY + y, baseZ + z), to, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }
}
