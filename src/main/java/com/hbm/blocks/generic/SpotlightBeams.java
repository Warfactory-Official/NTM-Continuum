package com.hbm.blocks.generic;

import com.hbm.blocks.machine.ISpotlight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.machine.Spotlight;
import com.hbm.blocks.machine.SpotlightBeam;

/** The static beam walkers of the 1.7 {@code Spotlight} class, shared by the spotlights and the tritium lamps. */
public final class SpotlightBeams {

    public static final int META_YELLOW = 0;
    public static final int META_GREEN = 1;
    public static final int META_BLUE = 2;

    private SpotlightBeams() {}

    private static boolean isAirOrBeam(BlockState state) {
        return state.isAir() || state.getBlock() instanceof SpotlightBeam;
    }

    // Recursively add beam blocks, updating any that already exist with new incoming light directions
    public static void propagateBeam(Level level, BlockPos from, Direction dir, int distance, int meta) {
        distance--;
        if (distance <= 0) return;
        BlockPos pos = from.relative(dir);
        BlockState state = level.getBlockState(pos);
        if (!isAirOrBeam(state)) return;
        if (!(state.getBlock() instanceof SpotlightBeam)) {
            level.setBlock(pos, ModDecoContent.spotlight_beam.get().defaultBlockState().setValue(SpotlightBeam.COLOR, meta), 3);
        }
        // If we encounter an existing beam, add a new INCOMING direction to the
        // metadata, and cancel propagation if something goes wrong
        if (SpotlightBeam.setDirection(level, pos, dir, true) == 0) return;
        propagateBeam(level, pos, dir, distance, meta);
    }

    // Recursively delete beam blocks, if they aren't still illuminated from a different direction
    public static void unpropagateBeam(Level level, BlockPos from, Direction dir) {
        BlockPos pos = from.relative(dir);
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SpotlightBeam)) return;
        // Remove the metadata associated with this direction
        // If all directions are set to zero, delete the beam
        if (SpotlightBeam.setDirection(level, pos, dir, false) == 0) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        unpropagateBeam(level, pos, dir);
    }

    // Travels back through a beam to the source, and if found, repropagates the beam
    public static void backPropagate(Level level, BlockPos from, Direction dir, int meta) {
        BlockPos pos = from.relative(dir.getOpposite());
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ISpotlight spot) {
            propagateBeam(level, pos, dir, spot.getBeamLength(), meta);
        } else if (!(state.getBlock() instanceof SpotlightBeam)) {
            return;
        }
        backPropagate(level, pos, dir, meta);
    }
}
