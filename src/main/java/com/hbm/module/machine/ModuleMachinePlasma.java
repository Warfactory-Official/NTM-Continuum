package com.hbm.module.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;

/** Plasma forge processing module: twelve item inputs, one output, and one dynamic fluid input. */
public class ModuleMachinePlasma extends ModuleMachineBase {

    public ModuleMachinePlasma(int index, IEnergyHandlerMK2 battery, NTMItemHandler inventory) {
        super(index, battery, inventory);
        this.inputSlots = new int[12];
        this.outputSlots = new int[1];
        this.inputTanks = new FluidTankNTM[1];
        this.outputTanks = new FluidTankNTM[0];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return PlasmaForgeRecipes.INSTANCE;
    }

    @Override
    public void setupTanks(GenericRecipe recipe) {
        super.setupTanks(recipe);
        if (recipe != null && recipe.inputFluid != null && recipe.inputFluid.length > 0) {
            inputTanks[0].changeTankSize(Math.max(inputTanks[0].getFill(),
                    Math.max(recipe.inputFluid[0].fill * 2, 16_000)));
        }
    }

    public ModuleMachinePlasma itemInput(int from) {
        for (int i = 0; i < inputSlots.length; i++) inputSlots[i] = from + i;
        return this;
    }

    public ModuleMachinePlasma itemOutput(int slot) {
        outputSlots[0] = slot;
        return this;
    }

    public ModuleMachinePlasma fluidInput(FluidTankNTM tank) {
        inputTanks[0] = tank;
        return this;
    }
}
