package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.AssemblyFactoryArmLayout;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** Animated OBJ renderer for the four-lane Assembly Factory. */
public final class AssemblyFactoryRenderer
        extends GemBER<TileEntityMachineAssemblyFactory, AssemblyFactoryRenderState> {

    private static final int FULLBRIGHT = 0xF000F0;
    private static final float BASE_YAW = 90F;
    private static final int FRAME_HEIGHT = 3;
    private static final double DISPLAY_DISTANCE = 35D * 35D;

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/assembly_factory.obj"));
    private final Surface body = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/assembly_factory.png");
    private final RenderType sparksType = CompatRenderTypes.entityTranslucentEmissive(
            dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/assembly_factory_sparks.png"));

    /**
     * Four sliders on one gantry, each carrying an arm of the assembly machine's shape; the two outer
     * ones carry a saw blade as well. The sliders on a lane move together and opposite, which is the
     * sign in front of each one's travel.
     */
    @Override
    protected void setup(Gem.Model<AssemblyFactoryRenderState> declaration) {
        Gem.Part<AssemblyFactoryRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw);

        machine.child(model.part("Base"), body);
        machine.child(model.part("Frame"), body)
                .visibleIf(state -> state.showFrame);

        slider(machine, 1, 0, 0.5F, -1F, 0);
        slider(machine, 2, 0, -0.5F, 1F, 1);
        slider(machine, 3, 1, -0.5F, 1F, 2);
        slider(machine, 4, 1, 0.5F, -1F, 3);
    }

    private void slider(Gem.Part<AssemblyFactoryRenderState> machine, int index, int lane,
                        float rest, float sign, int arm) {
        Gem.Part<AssemblyFactoryRenderState> carriage = machine.joint()
                .translate(rest, 0F, 0F)
                .slide(Gem.Axes.X, state -> sign * (float) state.sliders[lane]);
        carriage.child(model.part("Slider" + index), body);
        arm(carriage, AssemblyFactoryArmLayout.forArm(index), state -> state.arms[arm]);
    }

    private void arm(Gem.Part<AssemblyFactoryRenderState> carriage, AssemblyFactoryArmLayout layout,
                     Function<AssemblyFactoryRenderState, double[]> angles) {
        String part = Integer.toString(layout.part());
        Gem.Part<AssemblyFactoryRenderState> lower = carriage.joint()
                .translate(0F, 1.625F, layout.pivotZ())
                .angle(Gem.Axes.X, state -> layout.rotationSign() * (float) angles.apply(state)[0])
                .translate(0F, -1.625F, -layout.pivotZ());
        lower.child(model.part("ArmLower" + part), body);

        Gem.Part<AssemblyFactoryRenderState> upper = lower.joint()
                .translate(0F, 2.375F, layout.pivotZ())
                .angle(Gem.Axes.X, state -> layout.rotationSign() * (float) angles.apply(state)[1])
                .translate(0F, -2.375F, -layout.pivotZ());
        upper.child(model.part("ArmUpper" + part), body);

        Gem.Part<AssemblyFactoryRenderState> head = upper.joint()
                .translate(0F, 2.375F, layout.headZ())
                .angle(Gem.Axes.X, state -> layout.rotationSign() * (float) angles.apply(state)[2])
                .translate(0F, -2.375F, -layout.headZ());
        head.child(model.part("Head" + part), body);

        Gem.Part<AssemblyFactoryRenderState> striker = head.joint()
                .slide(Gem.Axes.Y, state -> (float) angles.apply(state)[3]);
        striker.child(model.part("Striker" + part), body);

        if (layout.saw()) {
            striker.joint()
                    .translate(0F, 1.625F, layout.bladeZ())
                    .angle(Gem.Axes.X, state -> layout.bladeSign() * (float) angles.apply(state)[4])
                    .translate(0F, -1.625F, -layout.bladeZ())
                    .child(model.part("Blade" + part), body);
        }
    }

    @Override public AssemblyFactoryRenderState createRenderState() { return new AssemblyFactoryRenderState(); }

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

    /** Recipe-chosen item models and clock-scrolled spark quads; neither is a mesh a backend can hold. */
    @Override
    protected void submitUndeclared(AssemblyFactoryRenderState state, PoseStack pose, SubmitSink collector, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.translate(0.5F, 0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(BASE_YAW + state.yaw));

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

}
