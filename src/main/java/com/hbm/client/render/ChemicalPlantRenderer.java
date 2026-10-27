package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Animated renderer for the chemical plant, a port of the 1.12.2 {@code RenderChemicalPlant} TESR to the
 * 26.1 render-state pipeline. It draws the static {@code Base} (+ conditional {@code Frame}), the sliding
 * {@code Slider} (a soft-peak-sine X slide), the rotating {@code Spinner}, and, only while processing,
 * the {@code Fluid} mixer overlay tinted by the recipe's averaged fluid colour. The animation counter
 * lives on the {@link TileEntityMachineChemicalPlant} (advanced by its client ticker, gated by
 * {@code didProcess}) and is interpolated here by partial ticks.
 */
public class ChemicalPlantRenderer extends GemBER<TileEntityMachineChemicalPlant, ChemPlantRenderState> {

    private static final float BASE_YAW = 90F;
    private static final int FRAME_HEIGHT = 3;

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/chemical_plant.obj"));
    private final Surface body = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/chemical_plant.png");
    private final Surface fluid = Surface.translucent(
            Constants.MOD_ID + ":textures/models/machines/chemical_plant_fluid.png");

    @Override
    protected void setup(Gem.Model<ChemPlantRenderState> declaration) {
        Gem.Part<ChemPlantRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw);

        machine.child(model.part("Base"), body);
        machine.child(model.part("Frame"), body)
                .visibleIf(state -> state.showFrame);
        machine.child(model.part("Slider"), body)
                .slide(Gem.Axes.X, state -> (float) (sps(state.anim * 0.125) * 0.375));
        machine.child(model.part("Spinner"), body)
                .spin(Gem.Axes.Y, 0.5F, 0F, 0.5F, state -> (float) ((state.anim * 15) % 360D));
        machine.child(model.part("Fluid"), fluid)
                .visibleIf(state -> state.processing)
                .tint(state -> 0xB0000000 | (state.fluidColor & 0xFFFFFF));
    }

    @Override
    public ChemPlantRenderState createRenderState() {
        return new ChemPlantRenderState();
    }

    /** Soft-peak sine (1.12.2 {@code BobMathUtil.sps}). */
    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    @Override
    public void extract(TileEntityMachineChemicalPlant be, ChemPlantRenderState state, float partialTicks,
                                   Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        int meta = MachineCore.facingOrdinal(bs, 2);
        state.yaw = switch (meta) {
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

    /** Average of the selected recipe's output (else input) fluid colours, or white. */
    private static int recipeFluidColor(TileEntityMachineChemicalPlant be) {
        GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.getRecipe(be.chemplantModule.recipe);
        if (recipe == null) return 0xFFFFFF;
        FluidStack[] fluids = recipe.outputFluid != null ? recipe.outputFluid : recipe.inputFluid;
        if (fluids == null || fluids.length == 0) return 0xFFFFFF;
        int r = 0, g = 0, b = 0;
        for (FluidStack fs : fluids) {
            int c = fs.type.clientMetadata().getColor();
            r += (c >> 16) & 0xFF; g += (c >> 8) & 0xFF; b += c & 0xFF;
        }
        int n = fluids.length;
        return ((r / n) << 16) | ((g / n) << 8) | (b / n);
    }

}
