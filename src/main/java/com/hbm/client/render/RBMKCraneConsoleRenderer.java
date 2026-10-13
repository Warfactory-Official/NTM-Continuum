package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import dev.golem.core.Ids;
import dev.golem.core.render.*;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RBMKCraneConsoleRenderer extends GemBER<BlockEntityCraneConsole, RBMKCraneConsoleRenderer.State> {
    private static final WavefrontObj DESK = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/crane_console.obj"));
    private static final WavefrontObj CRANE = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/crane.obj"));
    private static final Identifier DESK_TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/machines/rbmk_crane_console.png");
    private static final Identifier CRANE_TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/machines/rbmk_crane.png");
    /** {@code findRoomExtent} caps each half-span at 16, so the rail cannot be longer than this. */
    private static final int MAX_SPAN = 33;
    private static final int MAX_TUBES = 16;
    @Override protected void setup(Gem.Model<State> declaration) {
        Surface desk = Surface.cutout(DESK_TEXTURE.toString());
        Gem.Part<State> console = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(.5F, 0F, .5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(.5F, 0F, 0F);

        console.child(DESK.part("Console_Coonsole"), desk);
        console.child(DESK.part("Joystick"), desk)
                .translate(.75F, 1F, 0F)
                .angle(Gem.Axes.Z, state -> state.tiltFront)
                .angle(Gem.Axes.X, state -> state.tiltLeft)
                .translate(-.75F, -1.015F, 0F);
        meter(console, desk, "Meter1", .75F, state -> (float) (state.jitter + 135D - 270D * state.heat));
        meter(console, desk, "Meter2", .25F, state -> (float) (state.jitter + 135D - 270D * state.enrichment));

        crane(declaration);
    }

    /**
     * The crane, drawn at the reactor's centre rather than at the console's own block. Its rail is as
     * many girders as the room is wide and its mast as many tubes as it is tall, so every one the room
     * could hold is declared and the surplus hidden.
     */
    private static void crane(Gem.Model<State> declaration) {
        Surface skin = Surface.cutout(CRANE_TEXTURE.toString());
        Gem.Part<State> crane = declaration.joint()
                .visibleIf(state -> state.active)
                .translate(.5F, -1F, .5F)
                .slide(Gem.Axes.X, state -> (float) state.x)
                .slide(Gem.Axes.Y, state -> (float) state.y)
                .slide(Gem.Axes.Z, state -> (float) state.z)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .slide(Gem.Axes.X, state -> (float) -state.front)
                .slide(Gem.Axes.Z, state -> (float) state.left)
                .angle(Gem.Axes.Y, state -> state.rotation);

        Gem.Part<State> rail = crane.joint()
                .angle(Gem.Axes.Y, state -> -state.rotation)
                .slide(Gem.Axes.X, RBMKCraneConsoleRenderer::railX)
                .slide(Gem.Axes.Z, RBMKCraneConsoleRenderer::railZ)
                .angle(Gem.Axes.Y, state -> state.rotation);
        for (int i = 0; i < MAX_SPAN; i++) {
            final int girder = i;
            rail.child(CRANE.part("Girder"), skin)
                    .visibleIf(state -> girder < span(state))
                    .translate(-girder, 0F, 0F);
        }

        crane.child(CRANE.part("Main"), skin);

        Gem.Part<State> mast = crane.joint();
        for (int i = 0; i < MAX_TUBES; i++) {
            final int tube = i;
            mast.child(CRANE.part("Tube"), skin)
                    .visibleIf(state -> tube < state.height)
                    .translate(0F, tube, 0F);
        }
        mast.child(CRANE.part("Carriage"), skin)
                .slide(Gem.Axes.Y, state -> state.height - 1F);

        crane.child(CRANE.part("Lift"), skin)
                .slide(Gem.Axes.Y, state -> (float) (-3.25D * (1D - state.progress)));
    }

    private static int span(State state) {
        return state.rotation == 90 || state.rotation == 270
                ? state.spanL + state.spanR + 1 : state.spanF + state.spanB + 1;
    }

    private static float railX(State state) {
        return switch (state.rotation) {
            case 90, 270 -> 0F;
            case 180 -> (float) (state.front - state.spanF);
            default -> (float) (state.front + state.spanB);
        };
    }

    private static float railZ(State state) {
        return switch (state.rotation) {
            case 90 -> (float) (-state.left - state.spanR);
            case 270 -> (float) (-state.left + state.spanL);
            default -> 0F;
        };
    }

    private static void meter(Gem.Part<State> console, Surface desk, String name, float z,
                              Gem.FloatOf<State> angle) {
        console.child(DESK.part(name), desk)
                .translate(0F, 1.25F, z)
                .angle(Gem.Axes.X, angle)
                .translate(0F, -1.25F, -z);
    }

    @Override public State createRenderState() { return new State(); }
    /** Folded cells own block entities of the same type; only the core draws the desk and crane. */
    @Override public boolean shouldRender(BlockEntityCraneConsole be, Vec3 camera) {
        return !(be.getBlockState().getBlock() instanceof BlockMultiblockCore core && core.isCellOf(be.getBlockState()))
                && super.shouldRender(be, camera);
    }
    @Override public void extract(BlockEntityCraneConsole be, State state, float partial, Vec3 camera) {
        state.yaw = yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING));
        state.tiltFront = (float) Mth.lerp(partial, be.lastTiltFront, be.tiltFront);
        state.tiltLeft = (float) Mth.lerp(partial, be.lastTiltLeft, be.tiltLeft);
        state.heat = be.loadedHeat; state.enrichment = be.loadedEnrichment; state.active = be.setUpCrane;
        long millis = be.getLevel() == null ? 0L : be.getLevel().getGameTime() * 50L + (long) (partial * 50F);
        state.jitter = Math.sin(millis * .01D % 360D) * 180D / Math.PI * .05D;
        if (!state.active) return;
        var position = be.getBlockPos();
        state.height = be.height - 6; state.x = be.centerX - position.getX(); state.y = be.centerY - position.getY() + 1; state.z = be.centerZ - position.getZ();
        state.front = Mth.lerp(partial, be.lastPosFront, be.posFront); state.left = Mth.lerp(partial, be.lastPosLeft, be.posLeft);
        state.rotation = be.craneRotationOffset; state.spanF = be.spanF; state.spanB = be.spanB; state.spanL = be.spanL; state.spanR = be.spanR; state.progress = Mth.lerp(partial, be.lastProgress, be.progress);
    }
    private static float yaw(Direction direction) { return switch (direction) { case NORTH -> 90F; case WEST -> 180F; case SOUTH -> 270F; default -> 0F; }; }
    public static final class State extends CompatRenderState { float yaw, tiltFront, tiltLeft; double heat, enrichment, jitter; boolean active; int height, rotation, spanF, spanB, spanL, spanR; double x, y, z, front, left, progress; }
}
