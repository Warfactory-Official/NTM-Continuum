package com.hbm.module.machine;

import com.hbm.inventory.fluid.FluidStack;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.event.HbmEvents;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemBlueprints;
import dev.golem.core.world.ItemStacks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

import java.util.List;

/**
 * Processing core, ported from HBM's {@code ModuleMachineBase}. Drives one recipe at a time: checks power
 * + inputs + output space ({@link #canProcess}), drains power and advances {@link #progress}
 * ({@link #process}), then on completion consumes inputs and produces outputs. Item and fluid inputs and
 * outputs are validated before a completed operation mutates either buffer.
 *
 * <p>Input slot {@code i} accepts ingredient {@code i} of the selected recipe and nothing else, as in 1.7. Recipes can
 * share an auto-switch group: if the first input slot holds the first ingredient of another recipe in the group, the
 * module switches to it.
 */
public abstract class ModuleMachineBase {

    public final int index;
    public final IEnergyHandlerMK2 battery;
    public final NTMItemHandler inventory;
    public int[] inputSlots;
    public int[] outputSlots;
    /** Optional fluid tanks (null for item-only machines like the assembler). Set by fluid machines. */
    public FluidTankNTM[] inputTanks;
    public FluidTankNTM[] outputTanks;

    /** Server-side only: rolls chance outputs (see {@link #rollOutput}). */
    protected final RandomSource random = RandomSource.create();

    public String recipe = "null";
    public double progress;
    public boolean didProcess = false;
    public boolean markDirty = false;
    /** Redstone-over-Radio recipe selection uses the old quarter-speed restricted mode. */
    public boolean restrictedMode = false;

    public ModuleMachineBase(int index, IEnergyHandlerMK2 battery, NTMItemHandler inventory) {
        this.index = index;
        this.battery = battery;
        this.inventory = inventory;
    }

    /**
     * The machine this module works in, for {@link com.hbm.api.event.HbmEvents#MACHINE_RECIPE_COMPLETED} (level and
     * position). Every HBM machine passes itself as the module's battery, which is the default; a machine whose battery
     * is another object sets itself here.
     */
    private @Nullable BlockEntity owner;

    /** Sets the machine this module works in (only needed when the battery is not the machine itself). */
    public ModuleMachineBase owner(BlockEntity owner) {
        this.owner = owner;
        return this;
    }

    /** The machine this module works in, or null if it cannot tell. */
    public @Nullable BlockEntity owner() {
        if (owner != null) return owner;
        return battery instanceof BlockEntity machine ? machine : null;
    }

    /** Tells {@link com.hbm.api.event.HbmEvents#MACHINE_RECIPE_COMPLETED} that one operation of {@code recipe} finished. */
    protected void completed(GenericRecipe recipe) {
        if (!HbmEvents.machineRecipeObserved()) return;
        GenericRecipes set = getRecipeSet();
        String table = set instanceof DataGenericRecipes<?> data
                ? data.table.id() : set.getClass().getName();
        HbmEvents.machineRecipeCompleted(owner(), table, recipe.getInternalName());
    }

    public abstract GenericRecipes getRecipeSet();

    public GenericRecipe getRecipe() {
        return getRecipeSet().getRecipe(this.recipe);
    }

    public boolean canProcess(GenericRecipe recipe, double speed, double power) {
        if (recipe == null) return false;

        if (recipe.autoSwitchGroup != null && inputSlots.length > 0) {
            ItemStack switchBy = inventory.getStackInSlot(inputSlots[0]);
            List<GenericRecipe> group = getRecipeSet().autoSwitchGroups.get(recipe.autoSwitchGroup);
            if (!switchBy.isEmpty() && group != null) for (GenericRecipe next : group) {
                if (next.getInternalName().equals(this.recipe)) continue;
                if (next.inputItem == null || next.inputItem.length == 0) continue;
                if (next.inputItem[0].matchesRecipe(switchBy, true)) {
                    this.recipe = next.getInternalName();
                    return false; // re-check everything for the new recipe next tick
                }
            }
        }

        if (battery.getPower() < (long) (recipe.power * power)) return false;
        if (!hasInput(recipe)) return false;
        return canFitOutput(recipe);
    }

    protected boolean hasInput(GenericRecipe recipe) {
        return matchInputs(recipe) != null && hasInputFluid(recipe);
    }

    /** Whether the input tanks hold enough of each requested fluid, in the requested type/pressure slot. */
    private boolean hasInputFluid(GenericRecipe recipe) {
        if (recipe.inputFluid == null || recipe.inputFluid.length == 0) return true;
        if (inputTanks == null || inputTanks.length < recipe.inputFluid.length) return false;
        for (int i = 0; i < recipe.inputFluid.length; i++) {
            FluidStack required = recipe.inputFluid[i];
            FluidTankNTM tank = inputTanks[i];
            if (required == null || tank == null || tank.getTankType() != required.type
                    || tank.getPressure() != required.pressure || tank.getFill() < required.fill) return false;
        }
        return true;
    }

    /**
     * Conforms the input/output tanks to the selected recipe's fluids (so they accept/emit the right types
     * and the network fills/drains them), resetting any unused tank. No-op for item-only machines. Ported
     * from HBM's {@code ModuleMachineBase.setupTanks}.
     */
    public void setupTanks(GenericRecipe recipe) {
        if (recipe == null) return;
        if (inputTanks != null) for (int i = 0; i < inputTanks.length; i++) {
            if (recipe.inputFluid != null && recipe.inputFluid.length > i) inputTanks[i].conform(recipe.inputFluid[i]);
            else inputTanks[i].resetTank();
        }
        if (outputTanks != null) for (int i = 0; i < outputTanks.length; i++) {
            if (recipe.outputFluid != null && recipe.outputFluid.length > i) outputTanks[i].conform(recipe.outputFluid[i]);
            else outputTanks[i].resetTank();
        }
    }

    /**
     * The input slot index of each ingredient, or null if one is missing. 1.7 reads ingredient i from input slot i and
     * ignores ingredients beyond the input slots. port: failing that, each ingredient may come from any unused input
     * slot, so stacks a save put in another ingredient's slot are still used up.
     */
    public @Nullable int[] matchInputs(GenericRecipe recipe) {
        if (recipe.inputItem == null) return new int[0];
        int[] assigned = new int[Math.min(recipe.inputItem.length, inputSlots.length)];
        boolean inPlace = true;
        for (int i = 0; i < assigned.length && inPlace; i++) {
            inPlace = recipe.inputItem[i].matchesRecipe(inventory.getStackInSlot(inputSlots[i]), false);
            assigned[i] = i;
        }
        if (inPlace) return assigned;

        boolean[] used = new boolean[inputSlots.length];
        for (int i = 0; i < assigned.length; i++) {
            int found = -1;
            for (int s = 0; s < inputSlots.length && found < 0; s++) {
                if (!used[s] && recipe.inputItem[i].matchesRecipe(inventory.getStackInSlot(inputSlots[s]), false)) found = s;
            }
            if (found < 0) return null;
            assigned[i] = found;
            used[found] = true;
        }
        return assigned;
    }

    protected boolean canFitOutput(GenericRecipe recipe) {
        if (recipe.outputChoices != null) {
            for (GenericRecipe.OutputChoice choice : recipe.outputChoices) {
                if (!canFitOutput(recipe, new ItemStack[]{choice.stack()})) return false;
            }
            return true;
        }
        return canFitOutput(recipe, recipe.outputItem);
    }

    /** Validates a concrete item result before completion consumes any inputs. */
    private boolean canFitOutput(GenericRecipe recipe, ItemStack[] itemOutputs) {
        if (recipe.outputItem != null) {
            if (outputSlots == null || outputSlots.length < recipe.outputItem.length
                    || itemOutputs == null || itemOutputs.length < recipe.outputItem.length) return false;
            for (int i = 0; i < recipe.outputItem.length; i++) {
                ItemStack out = itemOutputs[i];
                if (out == null || out.isEmpty()) continue;
                int idx = outputSlots[i];
                ItemStack slot = inventory.getStackInSlot(idx);
                if (!GenericRecipe.outputFits(slot, out, inventory.getSlotLimit(idx))) return false;
            }
        }
        if (recipe.outputFluid != null) {
            if (outputTanks == null || outputTanks.length < recipe.outputFluid.length) return false;
            for (int i = 0; i < recipe.outputFluid.length; i++) {
                FluidStack fluid = recipe.outputFluid[i];
                FluidTankNTM tank = outputTanks[i];
                if (fluid == null || tank == null || tank.getTankType() != fluid.type
                        || tank.getPressure() != fluid.pressure
                        || fluid.fill > tank.getMaxFill() - tank.getFill()) return false;
            }
        }
        return true;
    }

    public void process(GenericRecipe recipe, double speed, double power) {
        if (restrictedMode) speed *= 0.25D;
        battery.setPower(battery.getPower() - (long) (recipe.power * power));
        double step = Math.min(speed / recipe.duration, 1D);
        this.progress += step;

        if (this.progress >= 1D) {
            ItemStack[] outputs = rollOutputs(recipe);
            if (!complete(recipe, outputs)) {
                // The preflight above normally makes this unreachable. Keep the operation atomic if a
                // caller changes a buffer between canProcess() and completion.
                this.progress = 0D;
                return;
            }
            if (canProcess(recipe, speed, power)) this.progress -= 1D;
            else this.progress = 0D;
        }
    }

    /** Completes one operation only after all item/fluid inputs and concrete outputs pass preflight. */
    private boolean complete(GenericRecipe recipe, ItemStack[] outputs) {
        int[] assigned = matchInputs(recipe);
        if (assigned == null || !hasInputFluid(recipe) || !canFitOutput(recipe, outputs)) return false;
        consumeInput(recipe, assigned);
        produceItem(recipe, outputs);
        completed(recipe);
        return true;
    }

    private void consumeInput(GenericRecipe recipe, int[] assigned) {
        for (int i = 0; i < assigned.length; i++) {
            int idx = inputSlots[assigned[i]];
            ItemStack in = inventory.getStackInSlot(idx).copy();
            if (in.isEmpty()) continue;
            in.shrink(recipe.inputItem[i].stacksize);
            inventory.setStackInSlot(idx, in.getCount() <= 0 ? ItemStack.EMPTY : in);
        }
        if (recipe.inputFluid != null && inputTanks != null) {
            for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
                inputTanks[i].setFill(inputTanks[i].getFill() - recipe.inputFluid[i].fill);
            }
        }
    }

    /** Compatibility entry point for the fusion module's deliberate non-rolled, continuous output path. */
    protected void produceItem(GenericRecipe recipe) {
        produceItem(recipe, recipe.outputItem);
    }

    private void produceItem(GenericRecipe recipe, ItemStack[] outputs) {
        if (recipe.outputItem != null) {
            for (int i = 0; i < recipe.outputItem.length; i++) {
                ItemStack out = outputs[i];
                if (out.isEmpty()) continue;
                int idx = outputSlots[i];
                ItemStack slot = inventory.getStackInSlot(idx);
                if (slot.isEmpty()) {
                    // Never place the recipe's shared ItemStack instance into a machine inventory.
                    inventory.setStackInSlot(idx, out.copy());
                } else {
                    ItemStack grown = slot.copy();
                    grown.grow(out.getCount());
                    inventory.setStackInSlot(idx, grown);
                }
            }
        }
        if (recipe.outputFluid != null) {
            for (int i = 0; i < recipe.outputFluid.length; i++) {
                outputTanks[i].setFill(outputTanks[i].getFill() + recipe.outputFluid[i].fill);
            }
        }
        this.markDirty = true;
    }

    /**
     * The actual stack output {@code index} yields this cycle. Guaranteed outputs pass through unchanged;
     * a chance output rolls once <b>per item</b> in the stack, so a 25% ×4 output averages one item (the
     * original {@code ChanceOutput.collapse}). May return an empty stack when every roll fails.
     */
    private ItemStack[] rollOutputs(GenericRecipe recipe) {
        if (recipe.outputItem == null) return new ItemStack[0];
        ItemStack[] outputs = new ItemStack[recipe.outputItem.length];
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = recipe.rollOutput(i, random);
        }
        return outputs;
    }

    public void update(double speed, double power, boolean extraCondition, ItemStack blueprint) {
        GenericRecipe recipe = getRecipe();

        // recipe not permitted by the inserted template: clear it
        if (recipe != null && recipe.isPooled() && !recipe.isPartOfPool(ItemBlueprints.grabPool(blueprint))) {
            this.didProcess = false;
            this.progress = 0D;
            this.recipe = "null";
            return;
        }

        this.setupTanks(recipe);

        this.didProcess = false;
        this.markDirty = false;

        if (extraCondition && canProcess(recipe, speed, power)) {
            process(recipe, speed, power);
            this.didProcess = true;
        } else {
            this.progress = 0D;
        }
    }

    public void setRecipe(String name, boolean ror) {
        this.recipe = name;
        this.restrictedMode = ror;
    }

    /** An input slot holding something its ingredient slot does not accept; automation may pull it out. */
    public boolean isSlotClogged(int slot) {
        boolean isInput = false;
        for (int s : inputSlots) if (s == slot) { isInput = true; break; }
        if (!isInput) return false;
        ItemStack stack = inventory.getStackInSlot(slot);
        return !stack.isEmpty() && !isItemValid(slot, stack);
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        GenericRecipe recipe = getRecipe();
        if (recipe == null || recipe.inputItem == null) return false;
        for (int i = 0; i < Math.min(inputSlots.length, recipe.inputItem.length); i++) {
            if (inputSlots[i] == slot && recipe.inputItem[i].matchesRecipe(stack, true)) return true;
        }
        if (recipe.autoSwitchGroup != null) {
            List<GenericRecipe> group = getRecipeSet().autoSwitchGroups.get(recipe.autoSwitchGroup);
            if (group != null) for (GenericRecipe next : group) {
                if (next.inputItem != null && inputSlots[0] == slot && next.inputItem[0].matchesRecipe(stack, true)) return true;
            }
        }
        return false;
    }

    // Sync + persistence

    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(progress);
        buf.writeBoolean(restrictedMode);
        buf.writeUtf(recipe);
    }

    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.progress = buf.readDouble();
        this.restrictedMode = buf.readBoolean();
        this.recipe = buf.readUtf();
    }

    public void save(DataOut out) {
        out.putDouble("progress" + index, progress);
        out.putString("recipe" + index, recipe);
        out.putBoolean("restrictedMode" + index, restrictedMode);
    }

    public void load(DataIn in) {
        this.progress = in.getDoubleOr("progress" + index, 0D);
        this.recipe = in.getStringOr("recipe" + index, "null");
        this.restrictedMode = in.getBooleanOr("restrictedMode" + index, false);
    }
}
