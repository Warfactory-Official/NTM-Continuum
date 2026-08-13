package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.machine.BlockRefueler;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityRefueler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Refueler renderer — port of the 1.12.2 {@code RenderRefueler}. The refueler is a single
 * {@code FACING} block (not a {@link com.hbm.blocks.BlockDummyable}), so it needs its own renderer:
 * the generic {@code MachineObjRenderer} reads {@code BlockDummyable.META} and would leave it stuck
 * facing north. Old transform: {@code rotate(90)} then the meta map W90/S180/E270/N0 — i.e. base 90 +
 * the standard facing map. Draws the static {@code Fueler} part, then the {@code Fluid} part offset by
 * {@code (1 − fillLevel) · −0.625} and tinted with the tank fluid's colour.
 *
 * <p>Deviation (§20): the old fluid part used a GL clip plane (cutting below y=0.125) + additive
 * blending; both are dropped here (no clean modern equivalent) — the fluid renders as plain tinted
 * translucent geometry.
 */
public class RefuelerRenderer implements CompatBER<TileEntityRefueler, RefuelerRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public RefuelerRenderer() {
        this.model = WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/refueler.obj"));
        this.renderType = CompatRenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/refueler.png"));
    }

    @Override public RefuelerRenderState createRenderState() { return new RefuelerRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityRefueler be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityRefueler be, RefuelerRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        Direction facing = bs.hasProperty(BlockRefueler.FACING) ? bs.getValue(BlockRefueler.FACING) : Direction.NORTH;
        // Old: a base rotate(90) then the meta map W(4)=90, S(3)=180, E(5)=270, N(2)=0.
        state.yaw = 90F + switch (facing) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F; // NORTH
        };
        state.fillLevel = (float) (be.prevFillLevel + (be.fillLevel - be.prevFillLevel) * partialTicks);
        state.fluidColor = be.tank.getTankType().getColor();
    }

    @Override
    public void submit(RefuelerRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart("Fueler", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        // Fluid level: old translate(0, (1 - fillLevel) * -0.625, 0), tinted by the tank fluid.
        ps.translate(0F, (1F - s.fillLevel) * -0.625F, 0F);
        int argb = 0xBF000000 | (s.fluidColor & 0xFFFFFF); // 0.75 alpha, as the old color(..., 0.75F)
        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart("Fluid", pose, buffer, light, OverlayTexture.NO_OVERLAY, argb));

        ps.popPose();
    }
}
