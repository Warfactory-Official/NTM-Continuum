package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the lift: guide rails repeated per course, and a platform on a piston stack that grows as it
 * rises. Ported from 1.12.2's {@code RenderCargoElevator}.
 *
 * <p>A lift splices onto the one above it, so its height is whatever the shaft was built to and has no
 * bound short of the world's. {@link #DECLARED} courses of rail and piston are therefore declared and
 * shown up to the count the lift actually reached; a shaft taller than that draws its remaining courses
 * through the vanilla pass, which is the only part of this model whose count is not known in advance.
 */
public final class CargoElevatorRenderer
        extends GemBER<TileEntityCargoElevator, CargoElevatorRenderer.State> {

    /** Courses of rail and piston held as declared parts. A taller shaft than this is not a built one. */
    private static final int DECLARED = 32;

    private static final WavefrontObj MODEL =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/elevator.obj"));
    private static final Surface SURFACE =
            Surface.solid(Constants.MOD_ID + ":textures/models/machines/elevator.png");
    private static final Identifier TEXTURE =
            Ids.of(Constants.MOD_ID, "textures/models/machines/elevator.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> lift = declaration.offScreen().joint().translate(0.5F, 0F, 0.5F);
        lift.child(MODEL.part("Base"), SURFACE);

        Gem.Part<State> carriage = lift.joint().slide(Gem.Axes.Y, state -> (float) state.extension);
        carriage.child(MODEL.part("Platform"), SURFACE);

        Geometry piston = MODEL.part("Piston");
        Geometry guides = MODEL.part("Guides");
        for (int course = 0; course < DECLARED; course++) {
            int at = course;
            carriage.child(piston, SURFACE)
                    .translate(0F, -at, 0F)
                    .visibleIf(state -> at < state.extension + 1);
            lift.child(guides, SURFACE)
                    .translate(0F, at, 0F)
                    .visibleIf(state -> at <= state.height);
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityCargoElevator lift, State state, float partial, Vec3 camera) {
        state.height = lift.height;
        state.extension = lift.prevExtension + (lift.extension - lift.prevExtension) * partial;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.height < DECLARED) return;

        pose.pushPose();
        pose.translate(.5D, 0D, .5D);

        pose.pushPose();
        pose.translate(0D, state.extension - DECLARED, 0D);
        for (int course = DECLARED; course < state.extension + 1; course++) {
            part(state, pose, sink, "Piston");
            pose.translate(0D, -1D, 0D);
        }
        pose.popPose();

        pose.translate(0D, DECLARED, 0D);
        for (int course = DECLARED; course <= state.height; course++) {
            part(state, pose, sink, "Guides");
            pose.translate(0D, 1D, 0D);
        }

        pose.popPose();
    }

    private static void part(State state, PoseStack pose, SubmitSink sink, String name) {
        sink.submitCustomGeometry(pose, CompatRenderTypes.entitySolid(TEXTURE),
                (last, buffer) -> MODEL.renderPart(name, last, buffer, state.lightCoords,
                        OverlayTexture.NO_OVERLAY));
    }

    public static final class State extends CompatRenderState {
        int height;
        double extension;
    }
}
