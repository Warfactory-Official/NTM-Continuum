package com.hbm.client.render;

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CameraInfo;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Heat boiler renderer — port of the 1.12.2 {@code RenderHeatBoiler}: the static {@code boiler.obj},
 * swapped for {@code boiler_burst.obj} once {@code hasExploded}; while the output tank is over 90%
 * full the body pulses (±1% squash), the old about-to-burst warning wobble. Old yaw map
 * (S0/E90/N180/W270) = base 180 + the standard facing map.
 */
public class HeatBoilerRenderer implements CompatBER<TileEntityHeatBoiler, HeatBoilerRenderState> {

    private static final float BASE_YAW = 180F;

    private final WavefrontObj model;
    private final WavefrontObj burstModel;
    private final RenderType renderType;

    public HeatBoilerRenderer() {
        this.model = WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/boiler.obj"));
        this.burstModel = WavefrontObj.load(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/boiler_burst.obj"));
        this.renderType = CompatRenderTypes.entityCutout(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/boiler.png"));
    }

    @Override public HeatBoilerRenderState createRenderState() { return new HeatBoilerRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityHeatBoiler be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityHeatBoiler be, HeatBoilerRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;   // WEST
            case 3 -> 180F;  // SOUTH
            case 5 -> 270F;  // EAST
            default -> 0F;   // NORTH
        };
        state.hasExploded = be.hasExploded;
        state.sine = !be.hasExploded && be.tanks[1].getFill() > be.tanks[1].getMaxFill() * 0.9
                ? (float) (Math.sin(System.currentTimeMillis() / 50D % (Math.PI * 2)) * 0.01D) : 0F;
    }

    @Override
    public void submit(HeatBoilerRenderState s, PoseStack ps, SubmitSink col, CameraInfo camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));
        if (s.sine != 0F) ps.scale(1F - s.sine, 1F + s.sine, 1F - s.sine);

        WavefrontObj obj = s.hasExploded ? burstModel : model;
        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> obj.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));

        ps.popPose();
    }
}
