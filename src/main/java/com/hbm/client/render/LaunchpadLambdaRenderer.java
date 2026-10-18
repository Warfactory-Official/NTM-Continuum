package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.client.render.entity.RenderLambdaRocket;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityLaunchpadLambda;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.7 {@code RenderLaunchpadLambda}: the silo, its sliding doors cut off at the frame, and the erector with rotor,
 * pivots and clamps cut off below the deck (1.7's GL clip planes, here {@link ClippedObjPart}). The rocket rides the
 * erector until it stands on the doors.
 */
public final class LaunchpadLambdaRenderer extends GemBER<TileEntityLaunchpadLambda, LaunchpadLambdaRenderer.State> {

    private static final WavefrontObj MODEL =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/machines/launchpad_lambda.obj"));
    private static final String TEXTURE = Constants.MOD_ID + ":textures/models/machines/launchpad_lambda.png";
    private static final Surface SKIN = Surface.cutoutCull(TEXTURE);
    private static final Surface ROCKET_SKIN = Surface.cutoutCull(Constants.MOD_ID + ":textures/models/lambda_rocket.png");
    private static final RenderType TYPE = CompatRenderTypes.entityCutoutCull(Ids.parse(TEXTURE));

    private static final double[] DOOR_LEFT = {0, 0, -1, 6};
    private static final double[] DOOR_RIGHT = {0, 0, 1, 6};
    private static final float[] DECK = {0, 1, 0, -0.25F};

    private final Map<String, ClippedObjPart> parts = new HashMap<>();
    private ClippedObjPart rocket;

    private ClippedObjPart part(String name) {
        return parts.computeIfAbsent(name, key -> new ClippedObjPart(MODEL, key));
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> pad = declaration.offScreen().viewDistance(256).joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.rotation);
        pad.child(MODEL.part("Silo"), SKIN);
        pad.child(RenderLambdaRocket.MODEL.all(), ROCKET_SKIN)
                .translate(0F, 2F, 0F)
                .angle(Gem.Axes.Y, state -> -state.rotation)
                .visibleIf(state -> state.erected);
    }

    @Override
    protected void submitUndeclared(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        int overlay = OverlayTexture.NO_OVERLAY;

        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation));

        drawClipped(pose, sink, "DoorLeft", new double[][] {DOOR_LEFT}, 0, 0, state.doors, light, overlay);
        drawClipped(pose, sink, "DoorRight", new double[][] {DOOR_RIGHT}, 0, 0, -state.doors, light, overlay);

        // everything below is drawn in its own frame; the deck plane is carried into it
        Matrix4f local = new Matrix4f().translate(0F, state.erector, 0F);
        pose.translate(0D, state.erector, 0D);
        drawDecked(pose, sink, local, part("Erector"), light, overlay);

        rotate(pose, local, 0F, 13.25F, 0F, Axis.XP, state.rotor);
        drawDecked(pose, sink, local, part("Rotor"), light, overlay);

        drawClamp(pose, sink, local, 3.5F, 19.75F, -state.clamps, state.pistons, "PivotUpper1", "ClampUpper1", light, overlay);
        drawClamp(pose, sink, local, 3.5F, 6.75F, state.clamps, state.pistons, "PivotLower1", "ClampLower1", light, overlay);
        drawClamp(pose, sink, local, -3.5F, 19.75F, state.clamps, -state.pistons, "PivotUpper2", "ClampUpper2", light, overlay);
        drawClamp(pose, sink, local, -3.5F, 6.75F, -state.clamps, -state.pistons, "PivotLower2", "ClampLower2", light, overlay);

        if (state.erecting && !state.erected) {
            pose.translate(0D, 2D, 0D);
            local.translate(0F, 2F, 0F);
            pose.mulPose(Axis.YP.rotationDegrees(-state.rotation));
            local.rotateY((float) Math.toRadians(-state.rotation));
            if (rocket == null) rocket = new ClippedObjPart(RenderLambdaRocket.MODEL, "Circle");
            ClippedObjPart body = rocket;
            double[][] planes = {deckIn(local)};
            sink.submitCustomGeometry(pose, RenderLambdaRocket.TYPE, (p, buffer) -> body.draw(p, buffer, light, overlay, planes, 0, 0, 0));
        }

        pose.popPose();
    }

    private void drawClamp(PoseStack pose, SubmitSink sink, Matrix4f local, float px, float py, float degrees, float piston,
                           String pivot, String clamp, int light, int overlay) {
        pose.pushPose();
        Matrix4f own = new Matrix4f(local);
        rotate(pose, own, px, py, 0F, Axis.ZP, degrees);
        drawDecked(pose, sink, own, part(pivot), light, overlay);
        pose.translate(piston, 0D, 0D);
        own.translate(piston, 0F, 0F);
        drawDecked(pose, sink, own, part(clamp), light, overlay);
        pose.popPose();
    }

    private static void rotate(PoseStack pose, Matrix4f local, float px, float py, float pz, Axis axis, float degrees) {
        pose.translate(px, py, pz);
        pose.mulPose(axis.rotationDegrees(degrees));
        pose.translate(-px, -py, -pz);
        local.translate(px, py, pz);
        if (axis == Axis.XP) local.rotateX((float) Math.toRadians(degrees));
        else local.rotateZ((float) Math.toRadians(degrees));
        local.translate(-px, -py, -pz);
    }

    /** The deck plane {@code y >= 0.25} of the pad frame, expressed in the frame {@code local} maps from. */
    private static double[] deckIn(Matrix4f local) {
        Vector4f plane = new Vector4f(DECK[0], DECK[1], DECK[2], DECK[3]);
        local.transformTranspose(plane);
        return new double[] {plane.x, plane.y, plane.z, plane.w};
    }

    private void drawDecked(PoseStack pose, SubmitSink sink, Matrix4f local, ClippedObjPart part, int light, int overlay) {
        double[][] planes = {deckIn(local)};
        sink.submitCustomGeometry(pose, TYPE, (p, buffer) -> part.draw(p, buffer, light, overlay, planes, 0, 0, 0));
    }

    private void drawClipped(PoseStack pose, SubmitSink sink, String name, double[][] planes, double ox, double oy,
                             double oz, int light, int overlay) {
        ClippedObjPart part = part(name);
        pose.pushPose();
        pose.translate(ox, oy, oz);
        sink.submitCustomGeometry(pose, TYPE, (p, buffer) -> part.draw(p, buffer, light, overlay, planes, ox, oy, oz));
        pose.popPose();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityLaunchpadLambda pad, State state, float interp, Vec3 camera) {
        state.rotation = switch (MachineCore.facingOrdinal(pad.getBlockState(), 5)) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.doors = pad.getInterpPos(TileEntityLaunchpadLambda.INDEX_DOORS, interp);
        state.erector = pad.getInterpPos(TileEntityLaunchpadLambda.INDEX_ERECTOR, interp) - 25F;
        state.rotor = pad.getInterpPos(TileEntityLaunchpadLambda.INDEX_ROTOR, interp);
        state.clamps = pad.getInterpPos(TileEntityLaunchpadLambda.INDEX_CLAMPS, interp);
        state.pistons = pad.getInterpPos(TileEntityLaunchpadLambda.INDEX_PISTONS, interp);
        state.erecting = pad.erecting;
        state.erected = pad.erected;
    }

    public static final class State extends CompatRenderState {
        float rotation;
        float doors;
        float erector;
        float rotor;
        float clamps;
        float pistons;
        boolean erecting;
        boolean erected;
    }
}
