package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemBlueprints;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Item-only processing core, ported from HBM's {@code ModuleMachineBase}. Drives one recipe at a time:
 * checks power + inputs + output space ({@link #canProcess}), drains power and advances
 * {@link #progress} ({@link #process}), then on completion consumes inputs and produces outputs. The
 * fluid I/O of the original is deferred (see IMPLEMENTATION.md §18); this version handles items only.
 *
 * <p><b>Auto-switch:</b> recipes can share an auto-switch group; if the recipe has one and the first
 * input slot holds an item matching another group recipe, the module switches to it. Combined with
 * {@link #autoSelect} (pick an initial recipe from the blueprint pool) this lets a machine run from
 * just "insert template + ingredient", with no in-GUI recipe picker yet.
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

    public String recipe = "null";
    public double progress;
    public boolean didProcess = false;
    public boolean markDirty = false;

    public ModuleMachineBase(int index, IEnergyHandlerMK2 battery, NTMItemHandler inventory) {
        this.index = index;
        this.battery = battery;
        this.inventory = inventory;
    }

    public abstract GenericRecipes getRecipeSet();

    public GenericRecipe getRecipe() {
        return getRecipeSet().getRecipe(this.recipe);
    }

    public boolean canProcess(GenericRecipe recipe, double speed, double power) {
        if (recipe == null) return false;

        // Within-group auto-switch (ported from HBM): if the selected recipe has an auto-switch group and
        // the input buffer holds the first ingredient of another recipe in that group, hop to it. The
        // original keys off input slot 0; in this buffer model any input slot counts. Cancel this tick on
        // a switch so the new recipe is fully re-checked next tick.
        if (recipe.autoSwitchGroup != null) {
            List<GenericRecipe> group = getRecipeSet().autoSwitchGroups.get(recipe.autoSwitchGroup);
            if (group != null) for (GenericRecipe next : group) {
                if (next.getInternalName().equals(this.recipe)) continue;
                if (next.inputItem == null || next.inputItem.length == 0) continue;
                for (int s : inputSlots) {
                    if (next.inputItem[0].matchesRecipe(inventory.getStackInSlot(s), true)) {
                        this.recipe = next.getInternalName();
                        return false;
                    }
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

    /** Whether the input tanks hold enough of each of the recipe's input fluids (true for item-only machines). */
    private boolean hasInputFluid(GenericRecipe recipe) {
        if (recipe.inputFluid == null || inputTanks == null) return true;
        for (int i = 0; i < Math.min(recipe.inputFluid.length, inputTanks.length); i++) {
            if (inputTanks[i].getFill() < recipe.inputFluid[i].fill) return false;
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
     * Assigns each recipe ingredient to a distinct input slot that satisfies it (the input area is a
     * shared buffer — items can sit in any slot, so a hopper can fill it and multiple stacks are usable).
     * @return one slot index per ingredient, or {@code null} if not all ingredients are present.
     */
    private int[] matchInputs(GenericRecipe recipe) {
        if (recipe.inputItem == null) return new int[0];
        int[] assigned = new int[recipe.inputItem.length];
        boolean[] used = new boolean[inputSlots.length];
        for (int i = 0; i < recipe.inputItem.length; i++) {
            int found = -1;
            for (int s = 0; s < inputSlots.length; s++) {
                if (used[s]) continue;
                if (recipe.inputItem[i].matchesRecipe(inventory.getStackInSlot(inputSlots[s]), false)) { found = s; break; }
            }
            if (found < 0) return null;
            assigned[i] = found;
            used[found] = true;
        }
        return assigned;
    }

    protected boolean canFitOutput(GenericRecipe recipe) {
        if (recipe.outputItem != null) {
            for (int i = 0; i < Math.min(recipe.outputItem.length, outputSlots.length); i++) {
                ItemStack slot = inventory.getStackInSlot(outputSlots[i]);
                if (slot.isEmpty()) continue;
                ItemStack out = recipe.outputItem[i];
                if (!ItemStack.isSameItemSameComponents(slot, out)) return false;
                if (slot.getCount() + out.getCount() > slot.getMaxStackSize()) return false;
            }
        }
        if (recipe.outputFluid != null && outputTanks != null) {
            for (int i = 0; i < Math.min(recipe.outputFluid.length, outputTanks.length); i++) {
                if (recipe.outputFluid[i].fill + outputTanks[i].getFill() > outputTanks[i].getMaxFill()) return false;
            }
        }
        return true;
    }

    public void process(GenericRecipe recipe, double speed, double power) {
        battery.setPower(battery.getPower() - (long) (recipe.power * power));
        double step = Math.min(speed / recipe.duration, 1D);
        this.progress += step;

        if (this.progress >= 1D) {
            consumeInput(recipe);
            produceItem(recipe);
            if (canProcess(recipe, speed, power)) this.progress -= 1D;
            else this.progress = 0D;
        }
    }

    protected void consumeInput(GenericRecipe recipe) {
        int[] assigned = matchInputs(recipe);
        if (assigned == null) return;
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

    protected void produceItem(GenericRecipe recipe) {
        if (recipe.outputItem != null) {
            for (int i = 0; i < Math.min(recipe.outputItem.length, outputSlots.length); i++) {
                ItemStack out = recipe.outputItem[i].copy();
                int idx = outputSlots[i];
                ItemStack slot = inventory.getStackInSlot(idx);
                if (slot.isEmpty()) {
                    inventory.setStackInSlot(idx, out);
                } else {
                    ItemStack grown = slot.copy();
                    grown.grow(out.getCount());
                    inventory.setStackInSlot(idx, grown);
                }
            }
        }
        if (recipe.outputFluid != null && outputTanks != null) {
            for (int i = 0; i < Math.min(recipe.outputFluid.length, outputTanks.length); i++) {
                outputTanks[i].setFill(outputTanks[i].getFill() + recipe.outputFluid[i].fill);
            }
        }
        this.markDirty = true;
    }

    public void update(double speed, double power, boolean extraCondition, ItemStack blueprint) {
        GenericRecipe recipe = getRecipe();

        // recipe no longer permitted by the inserted template → clear it
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

    /**
     * If no recipe is selected, pick the first recipe in {@code blueprint}'s pool whose first input
     * matches the ingredient in input slot 0. Lets the machine start without a GUI recipe picker.
     */
    public void autoSelect(ItemStack blueprint) {
        String pool = ItemBlueprints.grabPool(blueprint);
        if (pool == null) return;

        // Keep the current recipe while its ingredients are still in the buffer…
        GenericRecipe current = getRecipe();
        if (current != null && matchInputs(current) != null) return;

        // …otherwise switch to the first recipe in the template's pool that the buffer can make now.
        String chosen = "null";
        List<String> names = GenericRecipes.blueprintPools.get(pool);
        if (names != null) for (String name : names) {
            GenericRecipe rec = getRecipeSet().getRecipe(name);
            if (rec != null && matchInputs(rec) != null) { chosen = name; break; }
        }
        if (!chosen.equals(this.recipe)) {
            this.recipe = chosen;
            this.progress = 0D;
        }
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

    /**
     * Whether {@code stack} may go in {@code slot} given the inserted {@code blueprint}. Accepts the
     * current recipe's inputs, the first ingredient of any recipe in its auto-switch group (so inserting
     * a sibling-group ingredient triggers the in-process {@link #canProcess auto-switch}), and the inputs
     * of any recipe in the inserted template's pool (so an ingredient can be inserted before a recipe is
     * selected). Without this, no recipe ⇒ no valid input ⇒ nothing could ever start.
     */
    public boolean isItemValidForPool(int slot, ItemStack stack, ItemStack blueprint) {
        // Buffer model: any input slot accepts any ingredient of the current recipe…
        GenericRecipe recipe = getRecipe();
        if (recipe != null && recipe.inputItem != null) {
            for (AStack in : recipe.inputItem) if (in.matchesRecipe(stack, true)) return true;
        }
        // …or the first ingredient of any recipe in the current recipe's auto-switch group…
        if (recipe != null && recipe.autoSwitchGroup != null) {
            List<GenericRecipe> group = getRecipeSet().autoSwitchGroups.get(recipe.autoSwitchGroup);
            if (group != null) for (GenericRecipe next : group) {
                if (next.inputItem != null && next.inputItem.length > 0 && next.inputItem[0].matchesRecipe(stack, true)) return true;
            }
        }
        // …or of any recipe in the inserted template's pool.
        String pool = ItemBlueprints.grabPool(blueprint);
        if (pool == null) return false;
        List<String> names = GenericRecipes.blueprintPools.get(pool);
        if (names == null) return false;
        for (String name : names) {
            GenericRecipe rec = getRecipeSet().getRecipe(name);
            if (rec == null || rec.inputItem == null) continue;
            for (AStack in : rec.inputItem) if (in.matchesRecipe(stack, true)) return true;
        }
        return false;
    }

    // --- sync + persistence ---

    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(progress);
        buf.writeUtf(recipe);
    }

    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.progress = buf.readDouble();
        this.recipe = buf.readUtf();
    }

    public void save(ValueOutput out) {
        out.putDouble("progress" + index, progress);
        out.putString("recipe" + index, recipe);
    }

    public void load(ValueInput in) {
        this.progress = in.getDoubleOr("progress" + index, 0D);
        this.recipe = in.getStringOr("recipe" + index, "null");
    }
}
