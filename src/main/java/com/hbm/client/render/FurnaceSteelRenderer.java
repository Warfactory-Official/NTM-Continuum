package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityFurnaceSteel;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Steel furnace renderer, including the animated additive heat sheet from the old TESR. */
public final class FurnaceSteelRenderer extends GemBER<TileEntityFurnaceSteel, FurnaceRenderState> {
    private static final Identifier OBJ = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/furnace_steel.obj");
    private static final Identifier TEXTURE = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/furnace_steel.png");
    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final Surface surface = Surface.cutout(TEXTURE.toString());
    private final RenderType heatType = CompatRenderTypes.lightning();

    @Override
    protected void setup(Gem.Model<FurnaceRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.all(), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
    }

    @Override public FurnaceRenderState createRenderState() { return new FurnaceRenderState(); }

    @Override public void extract(TileEntityFurnaceSteel furnace, FurnaceRenderState state, float partialTicks, Vec3 cameraPosition) {
        int facing = furnace.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        state.yaw = switch (facing) {
            case 3 -> -90F;
            case 5 -> 0F;
            case 2 -> 90F;
            case 4 -> 180F;
            default -> 0F;
        };
        state.burning = furnace.wasOn;
    }

    /**
     * The heat shimmer over the vents is four hand-built quads whose colour flickers with the clock,
     * not a mesh either backend could hold.
     */
    @Override protected void submitUndeclared(FurnaceRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        if (state.burning) {
            float flicker = (float) Math.sin(GameTime.now() * 0.001D);
            int color = ((int) (128F * 0.875F) << 24)
                    | ((int) ((0.875F + flicker * 0.125F) * 255F) << 16)
                    | ((int) ((0.625F + flicker * 0.375F) * 255F) << 8);
            collector.order(1).submitCustomGeometry(poseStack, heatType, (pose, buffer) -> {
                for (int i = 0; i < 4; i++) {
                    float x = 1F + i * 0.0625F;
                    Vertexes.color(buffer, pose, x, 1F, -1F, color);
                    Vertexes.color(buffer, pose, x, 1F, 1F, color);
                    Vertexes.color(buffer, pose, x, 0.5F, 1F, color);
                    Vertexes.color(buffer, pose, x, 0.5F, -1F, color);
                }
            });
        }
        poseStack.popPose();
    }
}
