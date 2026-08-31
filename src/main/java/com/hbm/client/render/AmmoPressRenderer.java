package com.hbm.client.render;

import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.machine.TileEntityMachineAmmoPress;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** OBJ renderer ported from the old RenderAmmoPress, including the separate moving parts. */
public class AmmoPressRenderer implements CompatBER<TileEntityMachineAmmoPress, AmmoPressRenderState> {

    private final WavefrontObj model;
    private final RenderType type;

    public AmmoPressRenderer() {
        Identifier obj = dev.golem.core.Ids.of("hbm", "models/machines/ammo_press.obj");
        Identifier texture = dev.golem.core.Ids.of("hbm", "textures/models/machines/ammo_press.png");
        model = WavefrontObj.load(obj);
        // The legacy TESR explicitly disabled face culling for this model.
        type = CompatRenderTypes.entityCutoutNoCull(texture);
    }

    @Override public AmmoPressRenderState createRenderState() { return new AmmoPressRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineAmmoPress be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineAmmoPress be, AmmoPressRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.lift = be.prevLift + (be.lift - be.prevLift) * partialTicks;
        state.press = be.prevPress + (be.press - be.prevPress) * partialTicks;
        state.showBullets = be.animState == TileEntityMachineAmmoPress.AnimationState.RETRACTING
                || be.animState == TileEntityMachineAmmoPress.AnimationState.LOWERING;
    }

    @Override
    public void submit(AmmoPressRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        part(poseStack, collector, "Frame", state.lightCoords);

        poseStack.pushPose();
        poseStack.translate(0F, -state.press * 0.25F, 0F);
        part(poseStack, collector, "Press", state.lightCoords);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0F, state.lift * 0.5F - 0.5F, 0F);
        part(poseStack, collector, "Shells", state.lightCoords);
        if (state.showBullets) part(poseStack, collector, "Bullets", state.lightCoords);
        poseStack.popPose();
        poseStack.popPose();
    }

    private void part(PoseStack poseStack, SubmitSink collector, String name, int light) {
        collector.submitCustomGeometry(poseStack, type,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
