package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.rail.RailNarrowCurve;
import com.hbm.blocks.rail.RailNarrowStraight;
import com.hbm.blocks.rail.RailStandardBuffer;
import com.hbm.blocks.rail.RailStandardCurveBase;
import com.hbm.blocks.rail.RailStandardCurveWide7;
import com.hbm.blocks.rail.RailStandardCurveWide9;
import com.hbm.blocks.rail.RailStandardRamp;
import com.hbm.blocks.rail.RailStandardStraight;
import com.hbm.blocks.rail.RailStandardStraightShort;
import com.hbm.blocks.rail.RailStandardSwitch;
import com.hbm.blocks.rail.RailStandardSwitchFlipped;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.BlockEntityRail;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import java.util.EnumMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Source-faithful rail OBJ renderer.  This is the Gem form of ntm-next's {@code RenderRail}: the
 * standard family is one OBJ anchored at the folded core, while narrow straight is five one-block
 * meshes.  The two switch signs follow {@link BlockEntityRail#isSwitched} and retain their separate
 * textures from the original renderer.
 */
public final class RailRenderer extends GemBER<BlockEntityRail, RailRenderer.State> {

    private static final Surface STANDARD = Surface.cutout(Constants.MOD_ID + ":textures/blocks/rail_standard_straight.png");
    private static final Surface BUFFER = Surface.cutout(Constants.MOD_ID + ":textures/blocks/rail_standard_buffer.png");
    private static final Surface NARROW = Surface.cutout(Constants.MOD_ID + ":textures/blocks/rail_narrow_neo.png");
    private static final Surface SIGN = Surface.cutout(Constants.MOD_ID + ":textures/blocks/rail_switch_sign.png");
    private static final Surface SIGN_FLIPPED = Surface.cutout(Constants.MOD_ID + ":textures/blocks/rail_switch_sign_flipped.png");

    private record Mesh(Geometry whole, Geometry rail, Geometry signStraight, Geometry signTurn) {}

    private enum Kind {
        STRAIGHT, STRAIGHT_SHORT, CURVE, CURVE_7, CURVE_9, RAMP, BUFFER,
        SWITCH, SWITCH_FLIPPED, NARROW_STRAIGHT, NARROW_CURVE
    }

    private final EnumMap<Kind, Mesh> meshes = new EnumMap<>(Kind.class);

    @Override
    protected void setup(Gem.Model<State> declaration) {
        load(Kind.STRAIGHT, "rail_standard.obj");
        load(Kind.STRAIGHT_SHORT, "rail_standard_short.obj");
        load(Kind.CURVE, "rail_standard_bend.obj");
        load(Kind.CURVE_7, "rail_standard_bend_wide.obj");
        load(Kind.CURVE_9, "rail_standard_bend_wide9.obj");
        load(Kind.RAMP, "rail_standard_ramp.obj");
        load(Kind.BUFFER, "rail_standard_buffer.obj");
        load(Kind.SWITCH, "rail_standard_switch.obj");
        load(Kind.SWITCH_FLIPPED, "rail_standard_switch_flipped.obj");
        load(Kind.NARROW_STRAIGHT, "rail_narrow.obj");
        load(Kind.NARROW_CURVE, "rail_narrow_bend.obj");

        Gem.Part<State> root = declaration.offScreen().viewDistance(256).joint();
        for (Direction facing : HORIZONTAL) {
            whole(root, Kind.STRAIGHT, STANDARD, facing, yawFlat(facing), shiftX(facing), shiftZ(facing));
            whole(root, Kind.STRAIGHT_SHORT, STANDARD, facing, yawFlat(facing), shiftX(facing), shiftZ(facing));
            whole(root, Kind.CURVE, STANDARD, facing, yawTurned(facing), 0, 0);
            whole(root, Kind.CURVE_7, STANDARD, facing, yawTurned(facing), 0, 0);
            whole(root, Kind.CURVE_9, STANDARD, facing, yawTurned(facing), 0, 0);
            whole(root, Kind.RAMP, STANDARD, facing, yawTurned(facing), shiftX(facing), shiftZ(facing));
            whole(root, Kind.BUFFER, BUFFER, facing, yawTurned(facing), shiftX(facing), shiftZ(facing));
            switchParts(root, Kind.SWITCH, SIGN, facing, yawTurned(facing), shiftX(facing), shiftZ(facing));
            switchParts(root, Kind.SWITCH_FLIPPED, SIGN_FLIPPED, facing, yawTurned(facing), shiftX(facing), shiftZ(facing));
            narrowCurve(root, facing);

            float yaw = alongZ(facing) ? 90F : 0F;
            for (int i = -2; i <= 2; i++) {
                tile(root, facing, i, yaw);
            }
        }
    }

    private void load(Kind kind, String file) {
        WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/blocks/" + file));
        boolean switched = kind == Kind.SWITCH || kind == Kind.SWITCH_FLIPPED;
        meshes.put(kind, new Mesh(model.all(), switched ? model.part("Rail") : model.all(),
                switched ? model.part("SignStraight") : model.all(),
                switched ? model.part("SignTurn") : model.all()));
    }

    private void whole(Gem.Part<State> root, Kind kind, Surface surface, Direction facing, float yaw,
                       float x, float z) {
        Mesh mesh = meshes.get(kind);
        root.child(mesh.whole(), surface)
                .translate(0.5F + x, 0F, 0.5F + z)
                .angle(Gem.Axes.Y, ignored -> yaw)
                .visibleIf(state -> state.kind == kind && state.facing == facing);
    }

    private void switchParts(Gem.Part<State> root, Kind kind, Surface sign, Direction facing, float yaw,
                             float x, float z) {
        Mesh mesh = meshes.get(kind);
        Gem.Part<State> rail = root.child(mesh.rail(), STANDARD)
                .translate(0.5F + x, 0F, 0.5F + z)
                .angle(Gem.Axes.Y, ignored -> yaw)
                .visibleIf(state -> state.kind == kind && state.facing == facing);
        rail.child(mesh.signTurn(), sign)
                .visibleIf(state -> state.kind == kind && state.facing == facing && state.switched);
        rail.child(mesh.signStraight(), sign)
                .visibleIf(state -> state.kind == kind && state.facing == facing && !state.switched);
    }

    private void narrowCurve(Gem.Part<State> root, Direction facing) {
        Mesh mesh = meshes.get(Kind.NARROW_CURVE);
        root.child(mesh.whole(), NARROW)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, ignored -> yawNarrowCurve(facing))
                .visibleIf(state -> state.kind == Kind.NARROW_CURVE && state.facing == facing);
    }

    private void tile(Gem.Part<State> root, Direction facing, int distance, float yaw) {
        Mesh mesh = meshes.get(Kind.NARROW_STRAIGHT);
        root.child(mesh.whole(), NARROW)
                .translate(0.5F + facing.getStepX() * distance, 0F,
                        0.5F + facing.getStepZ() * distance)
                .angle(Gem.Axes.Y, ignored -> yaw)
                .visibleIf(state -> state.kind == Kind.NARROW_STRAIGHT && state.facing == facing);
    }

    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private static boolean alongZ(Direction facing) {
        return facing == Direction.NORTH || facing == Direction.SOUTH;
    }

    private static float yawFlat(Direction facing) {
        return alongZ(facing) ? 0F : 90F;
    }

    private static float yawTurned(Direction facing) {
        return switch (facing) {
            case NORTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F;
        };
    }

    private static float yawNarrowCurve(Direction facing) {
        return switch (facing) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
    }

    private static float shiftX(Direction facing) {
        return facing.getClockWise().getStepX() * 0.5F;
    }

    private static float shiftZ(Direction facing) {
        return facing.getClockWise().getStepZ() * 0.5F;
    }

    private static Kind kindOf(Block block) {
        // The narrow curve inherits the standard curve's movement math, not its mesh or skin.
        if (block instanceof RailNarrowCurve) return Kind.NARROW_CURVE;
        if (block instanceof RailStandardStraightShort) return Kind.STRAIGHT_SHORT;
        if (block instanceof RailStandardCurveWide9) return Kind.CURVE_9;
        if (block instanceof RailStandardCurveWide7) return Kind.CURVE_7;
        if (block instanceof RailStandardCurveBase) return Kind.CURVE;
        if (block instanceof RailStandardRamp) return Kind.RAMP;
        if (block instanceof RailStandardBuffer) return Kind.BUFFER;
        if (block instanceof RailStandardSwitchFlipped) return Kind.SWITCH_FLIPPED;
        if (block instanceof RailStandardSwitch) return Kind.SWITCH;
        if (block instanceof RailNarrowStraight) return Kind.NARROW_STRAIGHT;
        return Kind.STRAIGHT;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(BlockEntityRail blockEntity, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = blockEntity.getBlockState();
        state.kind = kindOf(blockState.getBlock());
        state.facing = blockState.hasProperty(BlockMultiblockCore.FACING)
                ? blockState.getValue(BlockMultiblockCore.FACING) : Direction.NORTH;
        state.switched = blockEntity.isSwitched;
    }

    public static final class State extends CompatRenderState {
        private Kind kind = Kind.STRAIGHT;
        private Direction facing = Direction.NORTH;
        private boolean switched;
    }
}
