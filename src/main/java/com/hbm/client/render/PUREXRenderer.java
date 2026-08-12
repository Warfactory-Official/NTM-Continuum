package com.hbm.client.render;

import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Animated renderer for the PUREX plant — a port of the 1.12.2 {@code RenderPUREX} TESR. Draws the static
 * {@code Base}, the {@code Frame} only when a block sits 5 above the core (the original polled that once a
 * second), the {@code Fan} spinning 45° per processed tick about its local Z axis at pivot (1.5, 1.25, 0),
 * and the {@code Pump} sliding along local X on a soft-peak sine. The animation counter lives on the
 * {@link TileEntityMachinePUREX} (advanced by its client ticker while {@code didProcess}) and is
 * interpolated here by partial ticks.
 */
public class PUREXRenderer implements CompatBER<TileEntityMachinePUREX, PurexRenderState> {

    private static final float BASE_YAW = 90F;

    private final WavefrontObj model;
    private final RenderType baseType;

    public PUREXRenderer() {
        Identifier obj = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/purex.obj");
        Identifier tex = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/purex.png");
        this.model = WavefrontObj.load(obj);
        this.baseType = RenderTypes.entityCutout(tex);
    }

    @Override public PurexRenderState createRenderState() { return new PurexRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachinePUREX be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    /** Soft-peak sine (1.12.2 {@code BobMathUtil.sps}), the pump's travel curve. */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public void extract(TileEntityMachinePUREX be, PurexRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH
        };
        state.showFrame = be.frame;
        state.anim = be.prevAnim + (be.anim - be.prevAnim) * partialTicks;
    }

    @Override
    public void submit(PurexRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));

        part(col, ps, "Base", light);
        if (s.showFrame) part(col, ps, "Frame", light);

        // Fan: 45° per processed tick about local Z, around the fan hub.
        ps.pushPose();
        ps.translate(1.5F, 1.25F, 0F);
        ps.mulPose(Axis.ZP.rotationDegrees((float) ((s.anim * 45) % 360D)));
        ps.translate(-1.5F, -1.25F, 0F);
        part(col, ps, "Fan", light);
        ps.popPose();

        // Pump: soft-peak-sine travel of ±0.5 along local X.
        ps.pushPose();
        ps.translate((float) (sps(s.anim * 0.25) * 0.5), 0F, 0F);
        part(col, ps, "Pump", light);
        ps.popPose();

        ps.popPose();
    }

    private void part(SubmitSink col, PoseStack ps, String name, int light) {
        col.submitCustomGeometry(ps, baseType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
