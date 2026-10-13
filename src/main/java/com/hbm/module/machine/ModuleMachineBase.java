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
     * Assigns each recipe ingredient to a distinct input slot that satisfies it (the input area is a
     * shared buffer; items can sit in any slot, so a hopper can fill it and multiple stacks are usable).
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
                int limit = Math.min(inventory.getSlotLimit(idx), out.getMaxStackSize());
                if (out.getCount() > limit) return false;
                if (slot.isEmpty()) continue;
                if (!ItemStacks.sameItemAndData(slot, out)
                        || slot.getCount() + out.getCount() > limit) return false;
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
            ItemStack out = recipe.outputItem[i].copy();
            if (recipe.outputChoices != null && i == 0) {
                float total = 0F;
                for (GenericRecipe.OutputChoice choice : recipe.outputChoices) total += Math.max(0F, choice.weight());
                float pick = random.nextFloat() * total;
                for (GenericRecipe.OutputChoice choice : recipe.outputChoices) {
                    pick -= Math.max(0F, choice.weight());
                    if (pick <= 0F) {
                        out = choice.stack().copy();
                        break;
                    }
                }
            }
            float chance = recipe.getChance(i);
            if (chance <= 0F) {
                out = ItemStack.EMPTY;
            } else if (chance < 1F) {
                int kept = 0;
                for (int j = 0; j < out.getCount(); j++) if (random.nextFloat() <= chance) kept++;
                if (kept <= 0) out = ItemStack.EMPTY;
                else out.setCount(kept);
            }
            outputs[i] = out;
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

    public void setRecipe(String name, boolean ror) {
        this.recipe = name;
        this.restrictedMode = ror;
    }

    /**
     * Whether {@code slot} is an input slot holding something the current recipe cannot use. Such a stack
     * must be extractable again so automation can unblock the machine (the original
     * {@code ModuleMachineBase.isSlotClogged}, used by {@code canExtractItem}).
     */
    public boolean isSlotClogged(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty()) return false;
        boolean isInput = false;
        for (int s : inputSlots) if (s == slot) { isInput = true; break; }
        if (!isInput) return false;

        // Buffer model: usable in ANY input slot, so "clogged" means it matches no ingredient of the
        // current recipe (nor the trigger ingredient of a sibling in its auto-switch group).
        GenericRecipe recipe = getRecipe();
        if (recipe == null) return true;
        if (recipe.inputItem != null) for (AStack in : recipe.inputItem) if (in.matchesRecipe(stack, true)) return false;
        if (recipe.autoSwitchGroup != null) {
            List<GenericRecipe> group = getRecipeSet().autoSwitchGroups.get(recipe.autoSwitchGroup);
            if (group != null) for (GenericRecipe next : group) {
                if (next.inputItem != null && next.inputItem.length > 0 && next.inputItem[0].matchesRecipe(stack, true)) return false;
            }
        }
        return true;
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
