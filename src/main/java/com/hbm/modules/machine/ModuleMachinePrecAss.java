package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import dev.golem.core.machine.ItemHandler;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;

/** Precision assembler processing module: nine item inputs/outputs and one fluid tank pair. */
public final class ModuleMachinePrecAss extends ModuleMachineBase {

    public ModuleMachinePrecAss(int index, IEnergyHandlerMK2 battery, ItemHandler inventory) {
        super(index, battery, inventory);
        inputSlots = new int[9];
        outputSlots = new int[9];
        inputTanks = new FluidTankNTM[1];
        outputTanks = new FluidTankNTM[1];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return PrecAssRecipes.INSTANCE;
    }

    @Override
    public void setupTanks(GenericRecipe recipe) {
        super.setupTanks(recipe);
        if (recipe == null) return;
        if (recipe.inputFluid != null && recipe.inputFluid.length > 0)
            inputTanks[0].changeTankSize(Math.max(Math.max(inputTanks[0].getFill(), recipe.inputFluid[0].fill * 2), 4_000));
        if (recipe.outputFluid != null && recipe.outputFluid.length > 0)
            outputTanks[0].changeTankSize(Math.max(Math.max(outputTanks[0].getFill(), recipe.outputFluid[0].fill * 2), 4_000));
    }

    public ModuleMachinePrecAss itemInput(int from) {
        for (int i = 0; i < inputSlots.length; i++) inputSlots[i] = from + i;
        return this;
    }

    public ModuleMachinePrecAss itemOutput(int from) {
        for (int i = 0; i < outputSlots.length; i++) outputSlots[i] = from + i;
        return this;
    }

    public ModuleMachinePrecAss fluidInput(FluidTankNTM tank) {
        inputTanks[0] = tank;
        return this;
    }

    public ModuleMachinePrecAss fluidOutput(FluidTankNTM tank) {
        outputTanks[0] = tank;
        return this;
    }
}
