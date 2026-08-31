package com.hbm.client.render;

import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Animated OBJ renderer for PrecAss, porting RenderPrecAss's ring and four striker arms. */
public final class PrecisionAssemblerRenderer implements CompatBER<TileEntityMachinePrecAss, PrecisionAssemblerRenderState> {

    private final WavefrontObj model;
    private final RenderType renderType;

    public PrecisionAssemblerRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/assembly_machine.obj"));
        renderType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/precass.png"));
    }

    @Override public PrecisionAssemblerRenderState createRenderState() { return new PrecisionAssemblerRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachinePrecAss be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachinePrecAss be, PrecisionAssemblerRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META) ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = be.frame;
        state.ring = be.prevRing + (be.ring - be.prevRing) * partialTicks;
        for (int i = 0; i < 3; i++) state.arm[i] = be.prevArmAngles[i]
                + (be.armAngles[i] - be.prevArmAngles[i]) * partialTicks;
        for (int i = 0; i < 4; i++) state.strikers[i] = be.prevStrikers[i]
                + (be.strikers[i] - be.prevStrikers[i]) * partialTicks;

        state.showItem = false;
        GenericRecipe recipe = PrecAssRecipes.INSTANCE.getRecipe(be.assemblerModule.recipe);
        if (recipe != null && be.getLevel() != null) {
            double dx = cameraPosition.x - (be.getBlockPos().getX() + 0.5);
            double dy = cameraPosition.y - (be.getBlockPos().getY() + 1.0);
            double dz = cameraPosition.z - (be.getBlockPos().getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz < 35 * 35) {
                ItemStack icon = recipe.getIcon();
                if (!icon.isEmpty()) {
                    ItemStack one = icon.copy();
                    one.setCount(1);
                    state.outputItem.extract(be.getLevel(), one, ItemDisplayContext.FIXED);
                    state.itemIsBlock = one.getItem() instanceof BlockItem;
                    state.itemIs3d = one.getItem() instanceof BlockItem;
                    state.showItem = !state.outputItem.isEmpty();
                }
            }
        }
        if (!state.showItem) state.outputItem.clear();
    }

    @Override
    public void submit(PrecisionAssemblerRenderState state, PoseStack pose, SubmitSink collector, CameraInfo camera) {
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(90F + state.yaw));
        part(collector, pose, "Base", state.lightCoords);
        if (state.showFrame) part(collector, pose, "Frame", state.lightCoords);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees((float) state.ring));
        part(collector, pose, "Ring", state.lightCoords);
        part(collector, pose, "Ring2", state.lightCoords);
        for (int i = 0; i < 4; i++) {
            renderArm(collector, pose, state.arm, (float) state.strikers[i], state.lightCoords);
            pose.mulPose(Axis.YP.rotationDegrees(-90F));
        }
        pose.popPose();

        if (state.showItem) {
            pose.pushPose();
            pose.translate(0F, 1.0625F, 0F);
            if (state.itemIsBlock) {
                if (state.itemIs3d) pose.translate(0F, -0.0625F, 0F);
                else {
                    pose.translate(0F, -0.125F, 0F);
                    pose.scale(0.5F, 0.5F, 0.5F);
                }
            } else {
                pose.mulPose(Axis.XP.rotationDegrees(-90F));
                pose.translate(0F, -0.25F, 0F);
            }
            pose.scale(1.25F, 1.25F, 1.25F);
            collector.submitItem(pose, state.outputItem, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    private void renderArm(SubmitSink collector, PoseStack pose, double[] arm, float striker, int light) {
        pose.pushPose();
        pose.translate(0F, 1.625F, 0.9375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) arm[0]));
        pose.translate(0F, -1.625F, -0.9375F);
        part(collector, pose, "ArmLower1", light);
        pose.translate(0F, 2.375F, 0.9375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) arm[1]));
        pose.translate(0F, -2.375F, -0.9375F);
        part(collector, pose, "ArmUpper1", light);
        pose.translate(0F, 2.375F, 0.4375F);
        pose.mulPose(Axis.XP.rotationDegrees((float) arm[2]));
        pose.translate(0F, -2.375F, -0.4375F);
        part(collector, pose, "Head1", light);
        pose.translate(0F, striker, 0F);
        part(collector, pose, "Spike1", light);
        pose.popPose();
    }

    private void part(SubmitSink collector, PoseStack pose, String name, int light) {
        collector.submitCustomGeometry(pose, renderType,
                (snapshot, buffer) -> model.renderPart(name, snapshot, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
