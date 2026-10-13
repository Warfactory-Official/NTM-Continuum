package com.hbm.handler;

import java.util.List;

import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.saveddata.TomSaveData;
import dev.golem.core.world.Fires;
import dev.golem.core.world.Vegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1.7.10's {@code ImpactWorldHandler}: what Tom's dust and fire do to the loaded overworld, three random loaded chunks
 * a tick (driven by {@link com.hbm.main.ModEventHandlerImpact}), plus the client's copy of the synced impact values.
 *
 * <ul>
 *   <li>{@link #die}: with dust, plants without light die - grass to dirt, bushes, leaves and vines removed - where
 *       {@code max(blockLight, light * (1 - dust))} above the block is below 4;</li>
 *   <li>{@link #burn}: with fire, a flammable block under open sky gets fire on top (leaves and bushes burn away),
 *       grassy soil becomes {@code burning_earth}, frozen dirt thaws.</li>
 * </ul>
 */
public final class ImpactWorldHandler {

    private ImpactWorldHandler() {}

    public static void impactEffects(ServerLevel world, List<LevelChunk> loaded) {

        if (world.dimension() != Level.OVERWORLD) return;

        TomSaveData data = TomSaveData.get(world);

        if (data.dust <= 0 && data.fire <= 0) return;

        int listSize = loaded.size();
        RandomSource rand = world.getRandom();

        if (listSize > 0) {
            for (int i = 0; i < 3; i++) {

                LevelChunk chunk = loaded.get(rand.nextInt(listSize));
                int chunkX = chunk.getPos().getMinBlockX();
                int chunkZ = chunk.getPos().getMinBlockZ();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {

                        if (rand.nextBoolean()) continue;

                        int X = chunkX + x;
                        int Z = chunkZ + z;
                        int height = world.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z);
                        // 1.7: getHeightValue - rand(max(1, getHeightValue)), a y between 1 and the surface measured
                        // from 1.7's floor at 0; kept absolute so a surface block is hit as often as in 1.7.
                        int Y = height - rand.nextInt(Math.max(1, height));
                        BlockPos pos = new BlockPos(X, Y, Z);

                        if (data.dust > 0) {
                            die(world, pos, data);
                        }
                        if (data.fire > 0) {
                            burn(world, pos);
                        }
                    }
                }
            }
        }
    }

    /** Plants die without sufficient light. */
    public static void die(ServerLevel world, BlockPos pos, TomSaveData data) {
        BlockPos above = pos.above();
        int light = Math.max(world.getBrightness(LightLayer.BLOCK, above),
                (int) (world.getMaxLocalRawBrightness(above) * (1 - data.dust)));

        if (light < 4) {
            BlockState state = world.getBlockState(pos);
            if (state.is(Blocks.GRASS_BLOCK)) {
                world.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            } else if (Vegetation.isPlant(state)) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else if (Vegetation.isLeaves(state)) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else if (state.getBlock() instanceof VineBlock) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Burn the world. */
    public static void burn(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        BlockPos above = pos.above();
        int sky = world.getBrightness(LightLayer.SKY, above);

        if (Fires.isFlammable(world, pos, state, Direction.UP) && world.getBlockState(above).isAir() && sky >= 7) {
            if (Vegetation.isLeaves(state) || Vegetation.isPlant(state)) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            world.setBlock(above, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);

        } else if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(ModFalloutBlocks.waste_earth.get())
                || state.is(ModBombs2Blocks.frozen_grass.get()) || state.is(ModFalloutBlocks.waste_mycelium.get()))
                // 1.7 !canLightningStrikeAt(x, y, z), asked of the soil block itself: it never sees the sky, so this
                // is always true and rain does not stop it. isRainingAt answers the same for a block under the surface.
                && !world.isRainingAt(pos) && sky >= 7) {
            world.setBlock(pos, ModTerrainBlocks.burning_earth.get().defaultBlockState(), Block.UPDATE_ALL);

        } else if (state.is(ModBombs2Blocks.frozen_dirt.get()) && sky >= 7) {
            world.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // The client's copy, written by the impact sync payload (1.7 PermaSyncHandler)

    public static Level lastSyncWorld = null;
    public static float fire = 0F;
    public static float dust = 0F;
    public static boolean impact = false;

    public static float getFireForClient(Level world) {
        if (world != lastSyncWorld) return 0F;
        return fire;
    }

    public static float getDustForClient(Level world) {
        if (world != lastSyncWorld) return 0F;
        return dust;
    }

    public static boolean getImpactForClient(Level world) {
        if (world != lastSyncWorld) return false;
        return impact;
    }
}
