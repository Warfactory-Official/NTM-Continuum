package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import dev.golem.core.render.SubmitSink;

/**
 * Draws the lift: guide rails repeated per course, and a platform on a piston stack that grows as it
 * rises. Ported from 1.12.2's {@code RenderCargoElevator}.
 */
public final class CargoElevatorRenderer
        implements CompatBER<TileEntityCargoElevator, CargoElevatorRenderer.State> {

    private static final WavefrontObj MODEL =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/elevator.obj"));
    private static final Identifier TEXTURE =
            Ids.of(Constants.MOD_ID, "textures/models/machines/elevator.png");

    public CargoElevatorRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityCargoElevator lift, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public void extract(TileEntityCargoElevator lift, State state, float partial, Vec3 camera) {
        state.height = lift.height;
        state.extension = lift.prevExtension + (lift.extension - lift.prevExtension) * partial;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);

        part(state, pose, sink, "Base");

        pose.pushPose();
        pose.translate(0D, state.extension, 0D);
        part(state, pose, sink, "Platform");
        for (int i = 0; i < state.extension + 1; i++) {
            part(state, pose, sink, "Piston");
            pose.translate(0D, -1D, 0D);
        }
        pose.popPose();

        pose.pushPose();
        for (int i = 0; i <= state.height; i++) {
            part(state, pose, sink, "Guides");
            pose.translate(0D, 1D, 0D);
        }
        pose.popPose();

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
