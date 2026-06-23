package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AssemblyMachineRenderer implements BlockEntityRenderer<TileEntityMachineAssemblyMachine, AssemblyRenderState> {

    private static final float BASE_YAW = 90F;
    private static final int FRAME_HEIGHT = 3;

    private final WavefrontObj model;
    private final RenderType renderType;

    public AssemblyMachineRenderer() {
        Identifier ns = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/assembly_machine.obj");
        Identifier tex = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/assembly_machine.png");
        this.model = WavefrontObj.load(ns);
        this.renderType = RenderTypes.entityCutout(tex);
    }

    @Override
    public AssemblyRenderState createRenderState() {
        return new AssemblyRenderState();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(TileEntityMachineAssemblyMachine blockEntity, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(TileEntityMachineAssemblyMachine be, AssemblyRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = be.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = be.getLevel() != null
                && !be.getLevel().getBlockState(be.getBlockPos().above(FRAME_HEIGHT)).isAir();
        state.ring = be.prevRing + (be.ring - be.prevRing) * partialTicks;
        state.arm1 = be.arms[0].getPositions(partialTicks);
        state.arm2 = be.arms[1].getPositions(partialTicks);

        state.showItem = false;
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(be.assemblerModule.recipe);
        if (recipe != null && be.getLevel() != null) {
            double dx = cameraPosition.x - (be.getBlockPos().getX() + 0.5);
            double dy = cameraPosition.y - (be.getBlockPos().getY() + 1.0);
            double dz = cameraPosition.z - (be.getBlockPos().getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz < 35 * 35) {
                ItemStack icon = recipe.getIcon();
                if (!icon.isEmpty()) {
                    ItemStack one = icon.copy();
                    one.setCount(1);
                    Minecraft.getInstance().getItemModelResolver().updateForTopItem(
                            state.outputItem, one, ItemDisplayContext.FIXED, be.getLevel(), null, 0);
                    state.itemIs3d = one.getItem() instanceof BlockItem;
                    state.showItem = !state.outputItem.isEmpty();
                }
            }
        }
        if (!state.showItem) state.outputItem.clear();
    }

    @Override
    public void submit(AssemblyRenderState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));

        part(col, ps, "Base", light);
        if (s.showFrame) part(col, ps, "Frame", light);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees((float) s.ring));
        part(col, ps, "Ring", light);

        ps.pushPose();
        ps.translate(0F, 1.625F, 0.9375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) s.arm1[0]));
        ps.translate(0F, -1.625F, -0.9375F);
        part(col, ps, "ArmLower1", light);
        ps.translate(0F, 2.375F, 0.9375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) s.arm1[1]));
        ps.translate(0F, -2.375F, -0.9375F);
        part(col, ps, "ArmUpper1", light);
        ps.translate(0F, 2.375F, 0.4375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) s.arm1[2]));
        ps.translate(0F, -2.375F, -0.4375F);
        part(col, ps, "Head1", light);
        ps.translate(0F, (float) s.arm1[3], 0F);
        part(col, ps, "Spike1", light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0F, 1.625F, -0.9375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm2[0]));
        ps.translate(0F, -1.625F, 0.9375F);
        part(col, ps, "ArmLower2", light);
        ps.translate(0F, 2.375F, -0.9375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm2[1]));
        ps.translate(0F, -2.375F, 0.9375F);
        part(col, ps, "ArmUpper2", light);
        ps.translate(0F, 2.375F, -0.4375F);
        ps.mulPose(Axis.XP.rotationDegrees((float) -s.arm2[2]));
        ps.translate(0F, -2.375F, 0.4375F);
        part(col, ps, "Head2", light);
        ps.translate(0F, (float) s.arm2[3], 0F);
        part(col, ps, "Spike2", light);
        ps.popPose();

        ps.popPose();

        if (s.showItem) {
            ps.pushPose();
            ps.translate(0F, 1.0625F, 0F);
            if (s.itemIs3d) {
                ps.translate(0F, 0.125F, 0F);
                ps.scale(1.25F, 1.25F, 1.25F);
            } else {
                ps.translate(0F, 0.015F, 0F);
                ps.mulPose(Axis.XP.rotationDegrees(-90F));
                ps.mulPose(Axis.ZP.rotationDegrees(-90F));
                ps.scale(0.85F, 0.85F, 0.85F);
            }
            s.outputItem.submit(ps, col, light, OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, String name, int light) {
        col.submitCustomGeometry(ps, renderType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
