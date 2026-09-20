package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineExcavator;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderExcavator}: the {@code Main} body, two counter-rotating crushers, the drill bit
 * descending on its extension with as many {@code Shaft} segments as the extension needs, and the
 * scrolling cobblestone/gravel chute rubble while the chute timer runs.
 */
public final class RenderExcavator extends GemBER<TileEntityMachineExcavator, ExcavatorRenderState> {

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/mining_drill.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/mining_drill.png");
    private final RenderType shaftType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/mining_drill.png"));
    private final RenderType cobbleType = CompatRenderTypes.entitySolid(dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/cobblestone.png"));
    private final RenderType gravelType = CompatRenderTypes.entitySolid(dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/gravel.png"));

    @Override
    protected void setup(Gem.Model<ExcavatorRenderState> declaration) {
        Gem.Part<ExcavatorRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw)
                .translate(0F, -3F, 0F); // getHeightOffset()

        machine.child(model.part("Main"), surface);
        machine.child(model.part("Crusher1"), surface)
                .spin(Gem.Axes.X, 0F, 2F, 2.8125F, state -> -state.crusher);
        machine.child(model.part("Crusher2"), surface)
                .spin(Gem.Axes.X, 0F, 2F, 2.1875F, state -> state.crusher);

        // glRotatef(rot, 0, -1, 0) is a rotation of -rot about +Y.
        machine.joint()
                .angle(Gem.Axes.Y, state -> -state.drillRotation)
                .slide(Gem.Axes.Y, state -> -state.extension)
                .child(model.part("Drillbit"), surface);
    }

    @Override
    public ExcavatorRenderState createRenderState() {
        return new ExcavatorRenderState();
    }

    /** Old meta-offset yaw map: S(3)=0, E(5)=90, N(2)=180, W(4)=270. */
    @Override
    public void extract(TileEntityMachineExcavator be, ExcavatorRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 3);
        state.yaw = switch (meta) {
            case 5 -> 90F;
            case 2 -> 180F;
            case 4 -> 270F;
            default -> 0F;
        };
        state.crusher = be.prevCrusherRotation + (be.crusherRotation - be.prevCrusherRotation) * partialTicks;
        state.drillRotation = be.prevDrillRotation + (be.drillRotation - be.prevDrillRotation) * partialTicks;
        state.extension = be.prevDrillExtension + (be.drillExtension - be.prevDrillExtension) * partialTicks;
        state.chute = be.chuteTimer > 0;
        state.crusherOn = be.enableCrusher;
    }

    /** The shaft count and the rubble both vary per frame, so neither is a declared part. */
    @Override
    protected void submitUndeclared(ExcavatorRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0F, -3F, 0F);

        // Shaft segments, stacked upward from the drill bit every two blocks while ext >= -1.5.
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.drillRotation));
        poseStack.translate(0F, -state.extension, 0F);
        float ext = state.extension;
        int light = state.lightCoords;
        while (ext >= -1.5F) {
            sink.submitCustomGeometry(poseStack, shaftType,
                    (pose, buffer) -> model.renderPart("Shaft", pose, buffer, light, OverlayTexture.NO_OVERLAY));
            poseStack.translate(0F, 2F, 0F);
            ext -= 2;
        }
        poseStack.popPose();

        if (state.chute) {
            double speed = 250D;
            float dropU = (float) (-(System.currentTimeMillis() % speed) / speed);
            float dropL = dropU + 4;

            float widthX = 0.125F, widthZ = 0.125F;
            sink.submitCustomGeometry(poseStack, cobbleType,
                    (pose, buffer) -> rubble(pose, buffer, light, widthX, widthZ, 3F, 2F, 2.5F, 1F, 1F, dropU, dropL));

            boolean smoosh = state.crusherOn;
            float lowerX = smoosh ? 0.5F : 0.25F, lowerZ = 0.0625F;
            float uU = smoosh ? 4 : 2, uL = 0.5F;
            sink.submitCustomGeometry(poseStack, smoosh ? gravelType : cobbleType,
                    (pose, buffer) -> rubble(pose, buffer, light, lowerX, lowerZ, 2F, 1F, 2.5F, uU, uL, dropU, dropL));
        }

        poseStack.popPose();
    }

    /** The four vertical faces of a rubble column between {@code yTop} and {@code yBottom}, scrolling downward. */
    private static void rubble(PoseStack.Pose pose, VertexConsumer buffer, int light, float widthX, float widthZ,
                               float yTop, float yBottom, float zc, float uWide, float uNarrow, float dropU, float dropL) {
        quad(pose, buffer, light, 0, 0, 1,
                widthX, yTop, zc + widthZ, 0, dropU,
                -widthX, yTop, zc + widthZ, uWide, dropU,
                -widthX, yBottom, zc + widthZ, uWide, dropL,
                widthX, yBottom, zc + widthZ, 0, dropL);
        quad(pose, buffer, light, 0, 0, -1,
                -widthX, yTop, zc - widthZ, uWide, dropU,
                widthX, yTop, zc - widthZ, 0, dropU,
                widthX, yBottom, zc - widthZ, 0, dropL,
                -widthX, yBottom, zc - widthZ, uWide, dropL);
        quad(pose, buffer, light, -1, 0, 0,
                -widthX, yTop, zc + widthZ, 0, dropU,
                -widthX, yTop, zc - widthZ, uNarrow, dropU,
                -widthX, yBottom, zc - widthZ, uNarrow, dropL,
                -widthX, yBottom, zc + widthZ, 0, dropL);
        quad(pose, buffer, light, 1, 0, 0,
                widthX, yTop, zc - widthZ, uNarrow, dropU,
                widthX, yTop, zc + widthZ, 0, dropU,
                widthX, yBottom, zc + widthZ, 0, dropL,
                widthX, yBottom, zc - widthZ, uNarrow, dropL);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int light, float nx, float ny, float nz,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
        vertex(pose, buffer, light, x0, y0, z0, u0, v0, nx, ny, nz);
        vertex(pose, buffer, light, x1, y1, z1, u1, v1, nx, ny, nz);
        vertex(pose, buffer, light, x2, y2, z2, u2, v2, nx, ny, nz);
        vertex(pose, buffer, light, x3, y3, z3, u3, v3, nx, ny, nz);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, int light,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
    }
}
