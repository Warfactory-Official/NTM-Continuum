package com.hbm.handler.radiation;

import com.hbm.api.data.WorldDataMaps;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.config.RadiationConfig;
import com.hbm.util.SavedIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The environmental-change half of the radiation system, ported from the fork's
 * {@code RadiationWorldHandler}: pockets holding ≥5 RAD are occasionally queued during the decay pass
 * (see {@code SectionRad.decay}); each server tick ONE queued pocket per level is drained here. Its
 * section's 4096 blocks are sampled at 1-in-3, and any sampled surface block (top of the heightmap
 * column ± 1) belonging to that pocket decays: grass turns to dead grass, plants pop, leaves wither into
 * dead leaves.
 *
 * <p>Surface note: 1.12 keyed on the light/motion heightmap ({@code getHeightValue}); {@code MOTION_BLOCKING}
 * is the closest analog, and it ignores non-motion-blocking plants (tall grass, ferns, flowers), so every
 * surface grass block lands at {@code topY} and converts uniformly. ({@code WORLD_SURFACE} is wrong here: it
 * counts those plants and leaves grass under 2-tall foliage permanently out-of-band, giving patchy, slow decay.)
 */
final class RadiationWorldHandler {

    private RadiationWorldHandler() {}

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /**
     * Drains up to {@link com.hbm.config.RadiationConfig#worldRad} queued pockets per level per tick and
     * applies their sampled surface decay. Server thread only. {@code worldRad} ("block operations per tick",
     * default 10) governs the count; the fork drained one pocket per tick, which crawls over a nuke-sized
     * irradiated area.
     */
    static void handleWorldDestruction(RadiationSystemNT.WorldRadiationData data) {
        int budget = Math.max(1, RadiationConfig.worldRad);
        for (int n = 0; n < budget; n++) {
            if (!drainOnePocket(data)) return; // queue empty
        }
    }

    /** Drains a single queued pocket; returns false if the queue was empty. */
    private static boolean drainOnePocket(RadiationSystemNT.WorldRadiationData data) {
        int[] key = data.pollDestruction();
        if (key == null) return false;
        int cx = key[0], sy = key[1], cz = key[2], pocketIndex = key[3];

        // Re-resolve the section. Stale keys (chunk unloaded / section rebuilt since the roll) drop silently
        // but still count as a consumed poll (return true so the caller keeps draining the queue).
        RadiationSystemNT.ChunkRad cr = data.chunks.get(ChunkPos.pack(cx, cz));
        if (cr == null) return true;
        int idx = sy - data.minSectionY;
        if (idx < 0 || idx >= cr.sections.length) return true;
        RadiationSystemNT.SectionRad sr = cr.sections[idx];
        if (sr == null || !sr.built || pocketIndex >= sr.pocketCount) return true;

        ServerLevel level = data.level;
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) return true;

        int baseX = cx << 4;
        int baseY = sy << 4;
        int baseZ = cz << 4;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < RadiationSystemNT.SectionRad.SECTION_BLOCKS; i++) {
            if (level.getRandom().nextInt(3) != 0) continue;             // fork: 1-in-3 block sample
            if (sr.pocketOf[i] != pocketIndex) continue;            // only this pocket's blocks
            int lx = i & 15;
            int lz = (i >> 4) & 15;
            int ly = i >> 8;                                        // RadiationSystemNT.idx packing
            int myY = baseY + ly;
            // MOTION_BLOCKING is the faithful analog of 1.12's getHeightValue: it ignores non-motion-blocking
            // plants (tall grass, ferns, flowers) so a surface grass block is always at topY. WORLD_SURFACE
            // counted those plants, pushing topY above the ground and leaving grass under 2-tall plants
            // permanently out-of-band, giving patchy, throttled decay.
            int topY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
            if (myY < topY - 1 || myY > topY) continue;             // surface band only (top block ± 1)

            pos.set(baseX + lx, myY, baseZ + lz);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            decayBlock(level, pos, state);
        }
        return true;
    }

    /** The fork's block-decay transforms (flags 2 = send to clients, no neighbour updates). */
    static void decayBlock(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        // A block named in the hbm:radiation_decay data map follows its value; the 1.7 rules below key on block
        // classes and tags, and stay the default for everything else.
        WorldDataMaps.Decay decay = WorldDataMaps.RADIATION_DECAY.get(block);
        if (decay != null) {
            if (decay.to().isEmpty()) return;
            BlockState to = decay.chance() >= 1F || level.getRandom().nextFloat() < decay.chance()
                    ? SavedIds.stateFromName(decay.to(), AIR) : AIR;
            level.setBlock(pos, to, Block.UPDATE_CLIENTS);
            return;
        }

        if (block instanceof DoublePlantBlock) {
            if (!state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) return;
            DoubleBlockHalf half = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos lowerPos = (half == DoubleBlockHalf.LOWER) ? pos : pos.below();
            BlockPos upperPos = (half == DoubleBlockHalf.LOWER) ? pos.above() : pos;
            level.setBlock(upperPos, AIR, Block.UPDATE_CLIENTS);
            level.setBlock(lowerPos, AIR, Block.UPDATE_CLIENTS);
            return;
        }
        if (block instanceof GrassBlock) {
            level.setBlock(pos, ModFalloutBlocks.waste_earth.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            return;
        }
        if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN)) { // old TALLGRASS metas (grass + fern)
            level.setBlock(pos, AIR, Block.UPDATE_CLIENTS);
            return;
        }
        if (state.is(BlockTags.LEAVES) && block != ModFalloutBlocks.waste_leaves.get()) {
            if (level.getRandom().nextInt(7) <= 5) {
                level.setBlock(pos, ModFalloutBlocks.waste_leaves.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(pos, AIR, Block.UPDATE_CLIENTS);
            }
        }
    }
}
