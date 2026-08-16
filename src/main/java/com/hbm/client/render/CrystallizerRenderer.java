package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Animated renderer for the ore acidizer — a port of the 1.12.2 {@code RenderCrystallizer} TESR to
 * the 26.1 render-state pipeline. Draws the static {@code Body}, the {@code Spinner} rotating with
 * the tile's interpolated angle, and — only while processing — the {@code Fluid} overlay tinted by
 * the tank fluid's colour (the old renderer bound the fluid's own texture; fluid textures aren't
 * wired in the port, so this uses the chem plant's tinted-overlay substitute).
 */
public class CrystallizerRenderer implements CompatBER<TileEntityMachineCrystallizer, CrystallizerRenderState> {

    private final WavefrontObj model;
    private final RenderType baseType;
    private final RenderType fluidType;

    public CrystallizerRenderer() {
        Identifier obj = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/crystallizer.obj");
        Identifier tex = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/crystallizer.png");
        Identifier fluidTex = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/chemical_plant_fluid.png");
        this.model = WavefrontObj.load(obj);
        this.baseType = CompatRenderTypes.entityCutout(tex);
        this.fluidType = CompatRenderTypes.entityTranslucent(fluidTex, false);
    }

    @Override public CrystallizerRenderState createRenderState() { return new CrystallizerRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineCrystallizer be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineCrystallizer be, CrystallizerRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        // The 1.12.2 RenderCrystallizer facing map (differs from the chem plant's — no base yaw).
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.angle = be.prevAngle + (be.angle - be.prevAngle) * partialTicks;
        state.processing = be.isOn;
        state.fluidColor = be.tank.getTankType().getColor();
    }

    @Override
    public void submit(CrystallizerRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        part(col, ps, "Body", light);

        // Spinner: continuous Y rotation about the machine centre, interpolated by partial ticks.
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.angle));
        part(col, ps, "Spinner", light);
        ps.popPose();

        // Acid overlay: only while processing, tinted (~69% alpha) by the tank fluid's colour.
        if (s.processing) {
            int color = 0xB0000000 | (s.fluidColor & 0xFFFFFF);
            col.submitCustomGeometry(ps, fluidType,
                    (pose, buffer) -> model.renderPart("Fluid", pose, buffer, light, OverlayTexture.NO_OVERLAY, color));
        }

        ps.popPose();
    }

    private void part(SubmitSink col, PoseStack ps, String name, int light) {
        col.submitCustomGeometry(ps, baseType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
