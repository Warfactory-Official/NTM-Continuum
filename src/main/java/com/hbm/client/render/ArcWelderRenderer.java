package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/** The old static OBJ pass and exact floating-item transforms; Hadron/Tau particles are not fabricated. */
public class ArcWelderRenderer implements CompatBER<TileEntityMachineArcWelder, ArcWelderRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public ArcWelderRenderer() {
        Identifier obj = dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/arc_welder.obj");
        Identifier texture = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/arc_welder.png");
        this.model = WavefrontObj.load(obj);
        this.renderType = CompatRenderTypes.entityCutout(texture);
    }

    @Override public ArcWelderRenderState createRenderState() { return new ArcWelderRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineArcWelder be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineArcWelder be, ArcWelderRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
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

    @Override
    public void submit(ArcWelderRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F + state.yaw));
        poseStack.translate(-0.5F, 0F, 0F);
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderExcept(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, Set.of()));

        if (state.showItem) {
            poseStack.pushPose();
            // Do not change these transforms: they are the 1.12.2 RenderArcWelder workaround.
            poseStack.translate(0D, 1.125D, 0D);
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
            poseStack.scale(0.76923075F, 0.76923075F, 0.76923075F);
            sink.submitItem(poseStack, state.outputItem, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
