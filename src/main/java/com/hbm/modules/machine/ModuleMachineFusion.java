package com.hbm.modules.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * The fusion torus's processing module. It differs from every other machine module in two ways, both
 * upstream's:
 *
 * <ul>
 *   <li><b>It throttles by a continuous factor</b> rather than running or not. {@link #processSpeed}
 *       is the reactor's fuel-and-power fraction, and it scales both the fuel drawn and the progress
 *       made — so a starved torus burns less and produces less rather than stalling.</li>
 *   <li><b>It consumes fuel before the output is finished.</b> Every other machine takes its inputs on
 *       completion; the torus is burning plasma continuously, so the fluid goes every tick.</li>
 * </ul>
 *
 * <p>{@link #bonusSpeed} is the collector bonus: each connected fusion collector adds half a
 * progress-worth of extra output per tick, banked in {@link #bonus} and cashed as a whole extra item.
 */
public class ModuleMachineFusion extends ModuleMachineBase {

    public double processSpeed = 1D;
    public double bonusSpeed = 0D;
    public double bonus;

    public ModuleMachineFusion(int index, IEnergyHandlerMK2 battery, NTMItemHandler inventory) {
        super(index, battery, inventory);
        this.inputSlots = new int[0];
        this.outputSlots = new int[1];
        this.inputTanks = new FluidTankNTM[3];
        this.outputTanks = new FluidTankNTM[1];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return FusionRecipes.INSTANCE;
    }

    public ModuleMachineFusion itemOutput(int slot) { outputSlots[0] = slot; return this; }

    public ModuleMachineFusion fluidInput(FluidTankNTM a, FluidTankNTM b, FluidTankNTM c) {
        inputTanks[0] = a;
        inputTanks[1] = b;
        inputTanks[2] = c;
        return this;
    }

    public ModuleMachineFusion fluidOutput(FluidTankNTM a) { outputTanks[0] = a; return this; }

    /** Must run before {@link #update}: carries the per-tick throttle the base module knows nothing about. */
    public void preUpdate(double processSpeed, double bonusSpeed) {
        this.processSpeed = processSpeed;
        this.bonusSpeed = bonusSpeed;
    }

    @Override
    protected boolean hasInput(GenericRecipe recipe) {
        if (processSpeed <= 0) return false;

        if (recipe.inputFluid != null) {
            for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
                // A tank that is empty is fine (the reactor idles); one that is part-full but short of
                // this tick's throttled draw is not, or the burn would run at the wrong rate.
                if (inputTanks[i].getFill() > 0
                        && inputTanks[i].getFill() < (int) Math.ceil(recipe.inputFluid[i].fill * processSpeed)) return false;
            }
        }
        return true;
    }

    @Override
    public void process(GenericRecipe recipe, double speed, double power) {
        battery.setPower(battery.getPower()
                - (long) Math.ceil((power == 1 ? recipe.power : (long) (recipe.power * power)) * processSpeed));

        // Never more than one recipe per tick, however fast the reactor is running.
        double step = Math.min(speed / recipe.duration * processSpeed, 1D);
        this.progress += step;
        this.bonus += step * this.bonusSpeed;
        // The bonus may not be cashed the instant it completes, so allow a 50% buffer.
        this.bonus = Math.min(this.bonus, 1.5D);

        if (recipe.inputFluid != null) {
            for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
                inputTanks[i].setFill(Math.max(0,
                        inputTanks[i].getFill() - (int) Math.ceil(recipe.inputFluid[i].fill * processSpeed)));
            }
        }

        if (this.progress >= 1D) {
            produceItem(recipe);
            if (canProcess(recipe, speed, power)) this.progress -= 1D;
            else this.progress = 0D;
        }

        if (this.bonus >= 1D && canFitOutput(recipe)) {
            produceItem(recipe);
            this.bonus -= 1D;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(bonus);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.bonus = buf.readDouble();
    }

    @Override
    public void save(DataOut out) {
        super.save(out);
        out.putDouble("bonus" + index, bonus);
    }

    @Override
    public void load(DataIn in) {
        super.load(in);
        this.bonus = in.getDoubleOr("bonus" + index, 0D);
    }
}
