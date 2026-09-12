package com.hbm.blocks.network;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

/**
 * Rotation and mirroring for a block whose shape is six per-direction connection flags.
 *
 * <p>Vanilla rotates a {@code Direction} property for free, because it knows the value is a
 * direction. A ring of six booleans carries the same information and vanilla cannot see it, so
 * {@code rotate} and {@code mirror} default to returning the state unchanged — and a block that
 * says nothing keeps its connections pointing the way they pointed in the file.
 *
 * <p>That is not cosmetic for a cable or a duct. A structure template placed at a rotation lays
 * down a run whose arms face the authored direction while its neighbours face the placed one, so
 * the run is visibly broken and the network never forms.
 */
public final class ConnectionRing {

    private ConnectionRing() {}

    /** Moves each direction's flag to the direction the rotation sends it to. */
    public static BlockState rotate(BlockState state, Rotation rotation,
                                    Map<Direction, BooleanProperty> ring) {
        BlockState rotated = state;
        for (Map.Entry<Direction, BooleanProperty> arm : ring.entrySet()) {
            rotated = rotated.setValue(ring.get(rotation.rotate(arm.getKey())), state.getValue(arm.getValue()));
        }
        return rotated;
    }

    /** The same, across a mirror plane; the vertical arms are unaffected by either. */
    public static BlockState mirror(BlockState state, Mirror mirror,
                                    Map<Direction, BooleanProperty> ring) {
        BlockState mirrored = state;
        for (Map.Entry<Direction, BooleanProperty> arm : ring.entrySet()) {
            mirrored = mirrored.setValue(ring.get(mirror.mirror(arm.getKey())), state.getValue(arm.getValue()));
        }
        return mirrored;
    }
}
