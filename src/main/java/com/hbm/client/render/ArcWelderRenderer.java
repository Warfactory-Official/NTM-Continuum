package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Arc welder: the static machine, and the recipe output floating in its cradle. */
public class ArcWelderRenderer extends GemBER<TileEntityMachineArcWelder, ArcWelderRenderState> {

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/arc_welder.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/arc_welder.png");

    @Override
    protected void setup(Gem.Model<ArcWelderRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.all(), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> 90F + state.yaw)
                .translate(-0.5F, 0F, 0F);
    }

    @Override
    public ArcWelderRenderState createRenderState() {
        return new ArcWelderRenderState();
    }

    @Override
    public void extract(TileEntityMachineArcWelder be, ArcWelderRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };

        state.showItem = false;
        ItemStack display = be.display;
        if (display != null && !display.isEmpty()) {
            ItemStack one = display.copy();
            one.setCount(1);
            state.outputItem.extract(be.getLevel(), one, ItemDisplayContext.NONE);
            state.showItem = !state.outputItem.isEmpty();
        }
        if (!state.showItem) state.outputItem.clear();
    }

    /**
     * The displayed output is whatever item the recipe makes, baked by the item pipeline into a model
     * that changes with the recipe, so it has no mesh either backend could hold. Do not change these
     * transforms: they are the 1.12.2 RenderArcWelder workaround.
     */
    @Override
    protected void submitUndeclared(ArcWelderRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!state.showItem) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F + state.yaw));
        poseStack.translate(-0.5F, 0F, 0F);
        poseStack.translate(0D, 1.125D, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
        poseStack.scale(0.76923075F, 0.76923075F, 0.76923075F);
        sink.submitItem(poseStack, state.outputItem, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
