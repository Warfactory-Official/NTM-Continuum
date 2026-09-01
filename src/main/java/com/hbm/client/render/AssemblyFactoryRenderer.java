package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import com.hbm.tileentity.machine.AssemblyFactoryArmLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Animated OBJ renderer for the four-lane Assembly Factory. */
public final class AssemblyFactoryRenderer
        implements CompatBER<TileEntityMachineAssemblyFactory, AssemblyFactoryRenderState> {

    private static final int FULLBRIGHT = 0xF000F0;
    private static final float BASE_YAW = 90F;
    private static final int FRAME_HEIGHT = 3;
    private static final double DISPLAY_DISTANCE = 35D * 35D;

    private final WavefrontObj model;
    private final RenderType bodyType;
    private final RenderType sparksType;

    public AssemblyFactoryRenderer() {
        model = WavefrontObj.load(dev.golem.core.Ids.of(Constants.MOD_ID,
                "models/machines/assembly_factory.obj"));
        bodyType = CompatRenderTypes.entityCutout(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/assembly_factory.png"));
        sparksType = CompatRenderTypes.entityTranslucentEmissive(dev.golem.core.Ids.of(Constants.MOD_ID,
                "textures/models/machines/assembly_factory_sparks.png"));
    }

    @Override public AssemblyFactoryRenderState createRenderState() { return new AssemblyFactoryRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineAssemblyFactory be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    @Override
    public void extract(TileEntityMachineAssemblyFactory be, AssemblyFactoryRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = blockState.hasProperty(BlockDummyable.META)
                ? blockState.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = be.frame;
        state.sliders[0] = be.animations[0].getSlider(partialTicks);
        state.sliders[1] = be.animations[1].getSlider(partialTicks);
        state.arms[0] = be.animations[0].striker.getPositions(partialTicks);
        state.arms[1] = be.animations[0].saw.getPositions(partialTicks);
        state.arms[2] = be.animations[1].striker.getPositions(partialTicks);
        state.arms[3] = be.animations[1].saw.getPositions(partialTicks);
        long gameTime = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        state.sparksU = (float) ((gameTime / 10D + partialTicks) % 10D);
        double cameraX = cameraPosition.x - (be.getBlockPos().getX() + 0.5D);
        double cameraY = cameraPosition.y - (be.getBlockPos().getY() + 1D);
        double cameraZ = cameraPosition.z - (be.getBlockPos().getZ() + 0.5D);
        state.nearby = cameraX * cameraX + cameraY * cameraY + cameraZ * cameraZ < DISPLAY_DISTANCE;

        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            state.showItems[lane] = false;
            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(be.assemblerModule[lane].recipe);
            if (recipe == null || be.getLevel() == null || !state.nearby) {
                state.outputItems[lane].clear();
                continue;
            }
            ItemStack icon = recipe.getIcon();
            if (icon.isEmpty()) {
                state.outputItems[lane].clear();
                continue;
            }
            ItemStack one = icon.copy();
            one.setCount(1);
            state.outputItems[lane].extract(be.getLevel(), one, ItemDisplayContext.FIXED);
            state.showItems[lane] = !state.outputItems[lane].isEmpty();
        }
    }

    @Override
    public void submit(AssemblyFactoryRenderState state, PoseStack pose, SubmitSink collector, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(BASE_YAW + state.yaw));

        part(collector, pose, "Base", light);
        if (state.showFrame) part(collector, pose, "Frame", light);

        pose.pushPose();
        pose.translate(0.5F - (float) state.sliders[0], 0F, 0F);
        part(collector, pose, "Slider1", light);
        renderArm(collector, pose, state.arms[0], AssemblyFactoryArmLayout.forArm(1), light);
        pose.popPose();

        pose.pushPose();
        pose.translate(-0.5F + (float) state.sliders[0], 0F, 0F);
        part(collector, pose, "Slider2", light);
        renderArm(collector, pose, state.arms[1], AssemblyFactoryArmLayout.forArm(2), light);
        pose.popPose();

        pose.pushPose();
        pose.translate(-0.5F + (float) state.sliders[1], 0F, 0F);
        part(collector, pose, "Slider3", light);
        renderArm(collector, pose, state.arms[2], AssemblyFactoryArmLayout.forArm(3), light);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5F - (float) state.sliders[1], 0F, 0F);
        part(collector, pose, "Slider4", light);
        renderArm(collector, pose, state.arms[3], AssemblyFactoryArmLayout.forArm(4), light);
        pose.popPose();

        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            if (!state.showItems[lane]) continue;
            pose.pushPose();
            pose.translate(1.5F - lane, 0F, 0F);
            pose.mulPose(Axis.YP.rotationDegrees(90F));
            pose.translate(0F, 1.0625F, 0F);
            if (state.outputItems[lane].isGui3d()) {
                pose.translate(0F, 0.125F, 0F);
                pose.scale(1.25F, 1.25F, 1.25F);
            } else {
                pose.translate(0F, 0.015F, 0F);
                pose.mulPose(Axis.XP.rotationDegrees(-90F));
                pose.mulPose(Axis.ZP.rotationDegrees(-90F));
                pose.scale(0.85F, 0.85F, 0.85F);
            }
            collector.submitItem(pose, state.outputItems[lane], light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }

        if (state.nearby) renderSparks(state, pose, collector);
        pose.popPose();
    }

    private void renderArm(SubmitSink collector, PoseStack pose, double[] arm,
                           AssemblyFactoryArmLayout layout, int light) {
        String part = Integer.toString(layout.part());
        pose.pushPose();
        pose.translate(0F, 1.625F, layout.pivotZ());
        pose.mulPose(Axis.XP.rotationDegrees(layout.rotationSign() * (float) arm[0]));
        pose.translate(0F, -1.625F, -layout.pivotZ());
        part(collector, pose, "ArmLower" + part, light);

        pose.translate(0F, 2.375F, layout.pivotZ());
        pose.mulPose(Axis.XP.rotationDegrees(layout.rotationSign() * (float) arm[1]));
        pose.translate(0F, -2.375F, -layout.pivotZ());
        part(collector, pose, "ArmUpper" + part, light);

        pose.translate(0F, 2.375F, layout.headZ());
        pose.mulPose(Axis.XP.rotationDegrees(layout.rotationSign() * (float) arm[2]));
        pose.translate(0F, -2.375F, -layout.headZ());
        part(collector, pose, "Head" + part, light);
        pose.translate(0F, (float) arm[3], 0F);
        part(collector, pose, "Striker" + part, light);

        if (layout.saw()) {
            pose.translate(0F, 1.625F, layout.bladeZ());
            pose.mulPose(Axis.XP.rotationDegrees(layout.bladeSign() * (float) arm[4]));
            pose.translate(0F, -1.625F, -layout.bladeZ());
            part(collector, pose, "Blade" + part, light);
        }
        pose.popPose();
    }

    private void renderSparks(AssemblyFactoryRenderState state, PoseStack pose, SubmitSink collector) {
        SubmitSink sparks = collector.order(1);
        double arm2 = state.arms[1][2];
        if (state.arms[1][3] <= -0.375D) {
            pose.pushPose();
            pose.translate(0.5D + state.sliders[0], 1.0625D, -arm2 / 45D);
            sparkQuad(sparks, pose, false, state.sparksU);
            pose.popPose();
        }

        double arm4 = state.arms[3][2];
        if (state.arms[3][3] <= -0.375D) {
            pose.pushPose();
            pose.translate(-0.5D - state.sliders[1], 1.0625D, arm4 / 45D);
            sparkQuad(sparks, pose, true, state.sparksU);
            pose.popPose();
        }
    }

    private void sparkQuad(SubmitSink collector, PoseStack pose, boolean reverse, float u) {
        double wide = 0.1875D;
        double length = 1.25D;
        double epsilon = 0.01D;
        float near = u + 0.5F;
        float far = u + 1.5F;
        collector.submitCustomGeometry(pose, sparksType, (last, buffer) -> {
            if (!reverse) {
                sparkVertex(buffer, last, -epsilon, -wide, length, near, 0F, 0x00FFFFFF);
                sparkVertex(buffer, last, -epsilon, wide, length, near, 1F, 0x00FFFFFF);
                sparkVertex(buffer, last, -epsilon, 0F, 0F, far, 1F, 0xFFFFFFFF);
                sparkVertex(buffer, last, -epsilon, 0F, 0F, far, 0F, 0xFFFFFFFF);
                sparkVertex(buffer, last, epsilon, -wide, length, u, 1F, 0x00FFFFFF);
                sparkVertex(buffer, last, epsilon, wide, length, u, 0F, 0x00FFFFFF);
                sparkVertex(buffer, last, epsilon, 0F, 0F, u + 1F, 0F, 0xFFFFFFFF);
                sparkVertex(buffer, last, epsilon, 0F, 0F, u + 1F, 1F, 0xFFFFFFFF);
            } else {
                sparkVertex(buffer, last, -epsilon, -wide, -length, near, 0F, 0x00FFFFFF);
                sparkVertex(buffer, last, -epsilon, wide, -length, near, 1F, 0x00FFFFFF);
                sparkVertex(buffer, last, -epsilon, 0F, 0F, far, 1F, 0xFFFFFFFF);
                sparkVertex(buffer, last, -epsilon, 0F, 0F, far, 0F, 0xFFFFFFFF);
                sparkVertex(buffer, last, epsilon, -wide, -length, u, 1F, 0x00FFFFFF);
                sparkVertex(buffer, last, epsilon, wide, -length, u, 0F, 0x00FFFFFF);
                sparkVertex(buffer, last, epsilon, 0F, 0F, u + 1F, 0F, 0xFFFFFFFF);
                sparkVertex(buffer, last, epsilon, 0F, 0F, u + 1F, 1F, 0xFFFFFFFF);
            }
        });
    }

    private static void sparkVertex(VertexConsumer buffer, PoseStack.Pose pose,
                                     double x, double y, double z, float u, float v, int color) {
        Vertexes.vertex(buffer, pose, (float) x, (float) y, (float) z, color,
                u, v, OverlayTexture.NO_OVERLAY, FULLBRIGHT, 0F, 1F, 0F);
    }

    private void part(SubmitSink collector, PoseStack pose, String name, int light) {
        collector.submitCustomGeometry(pose, bodyType,
                (last, buffer) -> model.renderPart(name, last, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
