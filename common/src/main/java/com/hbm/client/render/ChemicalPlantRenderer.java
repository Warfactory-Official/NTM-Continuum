package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ChemicalPlantRenderer implements BlockEntityRenderer<TileEntityMachineChemicalPlant, ChemPlantRenderState> {

    private static final float BASE_YAW = 90F;
    private static final int FRAME_HEIGHT = 3;

    private final WavefrontObj model;
    private final RenderType baseType;
    private final RenderType fluidType;

    public ChemicalPlantRenderer() {
        Identifier obj = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "models/machines/chemical_plant.obj");
        Identifier tex = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/chemical_plant.png");
        Identifier fluidTex = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/models/machines/chemical_plant_fluid.png");
        this.model = WavefrontObj.load(obj);
        this.baseType = RenderTypes.entityCutout(tex);
        this.fluidType = RenderTypes.entityTranslucent(fluidTex, false);
    }

    @Override public ChemPlantRenderState createRenderState() { return new ChemPlantRenderState(); }
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(TileEntityMachineChemicalPlant be, Vec3 cameraPosition) { return true; }
    @Override public int getViewDistance() { return 256; }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public void extractRenderState(TileEntityMachineChemicalPlant be, ChemPlantRenderState state, float partialTicks,
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
        state.anim = be.prevAnim + (be.anim - be.prevAnim) * partialTicks;
        state.processing = be.didProcess;
        state.fluidColor = recipeFluidColor(be);
    }

    private static int recipeFluidColor(TileEntityMachineChemicalPlant be) {
        GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.getRecipe(be.chemplantModule.recipe);
        if (recipe == null) return 0xFFFFFF;
        FluidStack[] fluids = recipe.outputFluid != null ? recipe.outputFluid : recipe.inputFluid;
        if (fluids == null || fluids.length == 0) return 0xFFFFFF;
        int r = 0, g = 0, b = 0;
        for (FluidStack fs : fluids) {
            int c = fs.type.getColor();
            r += (c >> 16) & 0xFF; g += (c >> 8) & 0xFF; b += c & 0xFF;
        }
        int n = fluids.length;
        return ((r / n) << 16) | ((g / n) << 8) | (b / n);
    }

    @Override
    public void submit(ChemPlantRenderState s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(BASE_YAW + s.yaw));

        part(col, ps, "Base", light);
        if (s.showFrame) part(col, ps, "Frame", light);

        ps.pushPose();
        ps.translate((float) (sps(s.anim * 0.125) * 0.375), 0F, 0F);
        part(col, ps, "Slider", light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.5F, 0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees((float) ((s.anim * 15) % 360D)));
        ps.translate(-0.5F, 0F, -0.5F);
        part(col, ps, "Spinner", light);
        ps.popPose();

        if (s.processing) {
            int color = 0xB0000000 | (s.fluidColor & 0xFFFFFF);
            col.submitCustomGeometry(ps, fluidType,
                    (pose, buffer) -> model.renderPart("Fluid", pose, buffer, light, OverlayTexture.NO_OVERLAY, color));
        }

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, String name, int light) {
        col.submitCustomGeometry(ps, baseType,
                (pose, buffer) -> model.renderPart(name, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }
}
