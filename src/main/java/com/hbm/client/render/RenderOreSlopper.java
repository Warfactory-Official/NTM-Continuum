package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.special.BedrockOreType;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.tileentity.machine.TileEntityMachineOreSlopper;
import com.hbm.tileentity.machine.TileEntityMachineOreSlopper.SlopperAnimation;
import com.hbm.tileentity.MachineCore;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderOreSlopper}: the {@code Base}, the {@code Slider} carrying the {@code Hydraulics} and
 * {@code Bucket} down and back up, a bedrock ore chunk riding the bucket while it lifts, the two
 * counter-rotating shredder blade drums and the fan.
 */
public final class RenderOreSlopper extends GemBER<TileEntityMachineOreSlopper, OreSlopperRenderState> {

    private final WavefrontObj model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/ore_slopper.obj"));
    private final Surface surface = Surface.cutout(Constants.MOD_ID + ":textures/models/machines/ore_slopper.png");

    @Override
    protected void setup(Gem.Model<OreSlopperRenderState> declaration) {
        Gem.Part<OreSlopperRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Base"), surface);

        Gem.Part<OreSlopperRenderState> slider = machine.joint()
                .slide(Gem.Axes.Z, state -> state.slide * -3F);
        slider.child(model.part("Slider"), surface);
        Gem.Part<OreSlopperRenderState> hydraulics = slider.joint()
                .slide(Gem.Axes.Y, state -> -Mth.clamp(state.extend - 0.25F, 0F, 1.25F));
        hydraulics.child(model.part("Hydraulics"), surface);
        hydraulics.joint()
                .slide(Gem.Axes.Y, state -> -Mth.clamp(state.extend, 0F, 1.25F))
                .child(model.part("Bucket"), surface);

        machine.child(model.part("BladesLeft"), surface)
                .spin(Gem.Axes.Z, 0.375F, 2.75F, 0F, state -> state.blades);
        machine.child(model.part("BladesRight"), surface)
                .spin(Gem.Axes.Z, -0.375F, 2.75F, 0F, state -> -state.blades);
        machine.child(model.part("Fan"), surface)
                .spin(Gem.Axes.X, 0F, 1.875F, -1F, state -> -state.fan);
    }

    @Override
    public OreSlopperRenderState createRenderState() {
        return new OreSlopperRenderState();
    }

    /** Old meta-offset yaw map: S(3)=180, E(5)=270, N(2)=0, W(4)=90. */
    @Override
    public void extract(TileEntityMachineOreSlopper be, OreSlopperRenderState state, float partialTicks, Vec3 cameraPosition) {
        int meta = MachineCore.facingOrdinal(be.getBlockState(), 2);
        state.yaw = switch (meta) {
            case 3 -> 180F;
            case 5 -> 270F;
            case 4 -> 90F;
            default -> 0F;
        };
        state.slide = be.prevSlider + (be.slider - be.prevSlider) * partialTicks;
        state.extend = (be.prevBucket + (be.bucket - be.prevBucket) * partialTicks) * 1.5F;
        state.blades = be.prevBlades + (be.blades - be.prevBlades) * partialTicks;
        state.fan = be.prevFan + (be.fan - be.prevFan) * partialTicks;
        state.lifting = be.animation == SlopperAnimation.LIFTING;
        if (state.lifting && be.getLevel() != null) {
            // 1.7: new ItemStack(ModItems.bedrock_ore, 1, 0) - BASE grade, LIGHT_METAL type.
            state.ore.extract(be.getLevel(), new ItemStack(ModProduction2Blocks.bedrockOreBase(BedrockOreType.LIGHT_METAL)), ItemDisplayContext.FIXED);
        } else {
            state.ore.clear();
        }
    }

    /** The lifted ore chunk is an item model, so it is drawn through the item pipeline in the bucket's frame. */
    @Override
    protected void submitUndeclared(OreSlopperRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        if (!state.lifting || state.ore.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.translate(0F, 0F, state.slide * -3F);
        poseStack.translate(0F, -Mth.clamp(state.extend - 0.25F, 0F, 1.25F), 0F);
        poseStack.translate(0F, -Mth.clamp(state.extend, 0F, 1.25F), 0F);
        poseStack.translate(0.0625F, 4.3125F, 2F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
        poseStack.scale(1.75F, 1.75F, 1.75F);
        sink.submitItem(poseStack, state.ore, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
