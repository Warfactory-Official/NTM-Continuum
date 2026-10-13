package com.hbm.module.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;

/**
 * PUREX processing module with the original's limits: 3 item inputs, 6 item outputs, 3 reagent tanks and 1
 * product tank ({@code PUREXRecipes.inputItemLimit/outputItemLimit/inputFluidLimit/outputFluidLimit}).
 */
public final class ModuleMachinePUREX extends ModuleMachineBase {
    public ModuleMachinePUREX(int index, IEnergyHandlerMK2 battery, NTMItemHandler inventory) {
        super(index, battery, inventory);
        inputSlots = new int[3];
        outputSlots = new int[6];
        inputTanks = new FluidTankNTM[3];
        outputTanks = new FluidTankNTM[1];
    }

    @Override public GenericRecipes getRecipeSet() { return PUREXRecipes.INSTANCE; }

    /** The three item input slots, starting at {@code first} (4, 5, 6). */
    public ModuleMachinePUREX itemInput(int first) {
        inputSlots = new int[]{first, first + 1, first + 2};
        return this;
    }

    /** The six item output slots, starting at {@code first} (7 … 12). */
    public ModuleMachinePUREX itemOutput(int first) {
        outputSlots = new int[]{first, first + 1, first + 2, first + 3, first + 4, first + 5};
        return this;
    }

    public ModuleMachinePUREX fluidInput(FluidTankNTM a, FluidTankNTM b, FluidTankNTM c) {
        inputTanks = new FluidTankNTM[]{a, b, c};
        return this;
    }

    public ModuleMachinePUREX fluidOutput(FluidTankNTM tank) {
        outputTanks = new FluidTankNTM[]{tank};
        return this;
    }
}
