package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RBMKCraneConsoleRenderer implements CompatBER<BlockEntityCraneConsole, RBMKCraneConsoleRenderer.State> {
    private static final WavefrontObj DESK = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/crane_console.obj"));
    private static final WavefrontObj CRANE = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/rbmk/crane.obj"));
    private static final Identifier DESK_TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/machines/rbmk_crane_console.png");
    private static final Identifier CRANE_TEXTURE = Ids.of(Constants.MOD_ID, "textures/models/machines/rbmk_crane.png");
    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int viewDistance() { return 256; }
    /** Folded cells own block entities of the same type; only the core draws the desk and crane. */
    @Override public boolean shouldRender(BlockEntityCraneConsole be, Vec3 camera) {
        return !(be.getBlockState().getBlock() instanceof BlockMultiblockCore core && core.isCellOf(be.getBlockState()))
                && CompatBER.super.shouldRender(be, camera);
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
    @Override public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose(); pose.translate(.5D, 0D, .5D); pose.mulPose(Axis.YP.rotationDegrees(state.yaw)); pose.translate(.5D, 0D, 0D);
        part(sink, pose, DESK, DESK_TEXTURE, "Console_Coonsole", state.lightCoords);
        pose.pushPose(); pose.translate(.75D, 1D, 0D); pose.mulPose(Axis.ZP.rotationDegrees(state.tiltFront)); pose.mulPose(Axis.XP.rotationDegrees(state.tiltLeft)); pose.translate(-.75D, -1.015D, 0D);
        part(sink, pose, DESK, DESK_TEXTURE, "Joystick", state.lightCoords); pose.popPose();
        meter(state, pose, sink, .75D, state.jitter + 135D - 270D * state.heat, "Meter1"); meter(state, pose, sink, .25D, state.jitter + 135D - 270D * state.enrichment, "Meter2"); pose.popPose();
        if (!state.active) return;
        pose.pushPose(); pose.translate(.5D, -1D, .5D); pose.translate(state.x, state.y, state.z); pose.mulPose(Axis.YP.rotationDegrees(state.yaw)); pose.translate(-state.front, 0D, state.left); pose.mulPose(Axis.YP.rotationDegrees(state.rotation));
        pose.pushPose(); pose.mulPose(Axis.YP.rotationDegrees(-state.rotation));
        int span;
        switch (state.rotation) { case 90 -> { span = state.spanL + state.spanR + 1; pose.translate(0D, 0D, -state.left - state.spanR); } case 180 -> { span = state.spanF + state.spanB + 1; pose.translate(state.front - state.spanF, 0D, 0D); } case 270 -> { span = state.spanL + state.spanR + 1; pose.translate(0D, 0D, -state.left + state.spanL); } default -> { span = state.spanF + state.spanB + 1; pose.translate(state.front + state.spanB, 0D, 0D); } }
        pose.mulPose(Axis.YP.rotationDegrees(state.rotation)); for (int i = 0; i < span; i++) { part(sink, pose, CRANE, CRANE_TEXTURE, "Girder", state.lightCoords); pose.translate(-1D, 0D, 0D); } pose.popPose();
        part(sink, pose, CRANE, CRANE_TEXTURE, "Main", state.lightCoords);
        pose.pushPose(); for (int i = 0; i < state.height; i++) { part(sink, pose, CRANE, CRANE_TEXTURE, "Tube", state.lightCoords); pose.translate(0D, 1D, 0D); } pose.translate(0D, -1D, 0D); part(sink, pose, CRANE, CRANE_TEXTURE, "Carriage", state.lightCoords); pose.popPose();
        pose.translate(0D, -3.25D * (1D - state.progress), 0D); part(sink, pose, CRANE, CRANE_TEXTURE, "Lift", state.lightCoords); pose.popPose();
    }
    private static void part(SubmitSink sink, PoseStack pose, WavefrontObj model, Identifier texture, String name, int light) { sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutout(texture), (last, buffer) -> model.renderPart(name, last, buffer, light, OverlayTexture.NO_OVERLAY)); }
    private static void meter(State state, PoseStack pose, SubmitSink sink, double z, double angle, String name) { pose.pushPose(); pose.translate(0D, 1.25D, z); pose.mulPose(Axis.XP.rotationDegrees((float) angle)); pose.translate(0D, -1.25D, -z); part(sink, pose, DESK, DESK_TEXTURE, name, state.lightCoords); pose.popPose(); }
    private static float yaw(net.minecraft.core.Direction direction) { return switch (direction) { case NORTH -> 90F; case WEST -> 180F; case SOUTH -> 270F; default -> 0F; }; }
    public static final class State extends CompatRenderState { float yaw, tiltFront, tiltLeft; double heat, enrichment, jitter; boolean active; int height, rotation, spanF, spanB, spanL, spanR; double x, y, z, front, left, progress; }
}
