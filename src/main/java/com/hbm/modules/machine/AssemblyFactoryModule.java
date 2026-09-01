package com.hbm.modules.machine;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;

/** Assembly module variant that owns exactly one recipe input and output tank pair. */
public final class AssemblyFactoryModule extends ModuleMachineAssembler {

    public AssemblyFactoryModule(int index, TileEntityMachineAssemblyFactory factory,
                                 FluidTankNTM inputTank, FluidTankNTM outputTank) {
        super(index, factory, factory.inventory);
        this.inputTanks = new FluidTankNTM[]{inputTank};
        this.outputTanks = new FluidTankNTM[]{outputTank};
    }

    @Override
    public AssemblyFactoryModule itemInput(int from) {
        super.itemInput(from);
        return this;
    }

    @Override
    public AssemblyFactoryModule itemOutput(int slot) {
        super.itemOutput(slot);
        return this;
    }

    @Override
    public void setupTanks(GenericRecipe recipe) {
        super.setupTanks(recipe);
        if (recipe == null) return;

        if (recipe.inputFluid != null && recipe.inputFluid.length > 0) {
            inputTanks[0].changeTankSize(Math.max(Math.max(inputTanks[0].getFill(), recipe.inputFluid[0].fill * 2), 4_000));
        }
        if (recipe.outputFluid != null && recipe.outputFluid.length > 0) {
            outputTanks[0].changeTankSize(Math.max(Math.max(outputTanks[0].getFill(), recipe.outputFluid[0].fill * 2), 4_000));
        }
    }
}
