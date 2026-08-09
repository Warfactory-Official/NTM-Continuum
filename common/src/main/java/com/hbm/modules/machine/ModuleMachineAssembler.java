package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;

/** Assembler processing module: 12 item input slots, 1 output slot. */
public class ModuleMachineAssembler extends ModuleMachineBase {

    public ModuleMachineAssembler(int index, IEnergyHandlerMK2 battery, NTMItemHandler inventory) {
        super(index, battery, inventory);
        this.inputSlots = new int[12];
        this.outputSlots = new int[1];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return AssemblyMachineRecipes.INSTANCE;
    }

    public ModuleMachineAssembler itemInput(int from) {
        for (int i = 0; i < inputSlots.length; i++) inputSlots[i] = from + i;
        return this;
    }

    public ModuleMachineAssembler itemOutput(int slot) {
        outputSlots[0] = slot;
        return this;
    }
}
