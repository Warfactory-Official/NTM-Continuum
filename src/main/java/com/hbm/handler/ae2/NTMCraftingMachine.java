package com.hbm.handler.ae2;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.module.machine.ModuleMachineBase;
import dev.golem.core.autocraft.CraftingMachine;
import dev.golem.core.autocraft.CraftingPlan;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * 1.12.2's {@code NTMCraftingMachineHelper}: a pushed job selects the recipe it was encoded from and places its
 * ingredients, as the recipe arrows and a player would. Fluids come with the job or from a separately stocked tank.
 * port: a job is refused unless every ingredient finds a place (1.12.2 dropped leftovers) and the inserted template
 * permits the recipe (otherwise the next tick would clear it with the ingredients inside).
 */
public record NTMCraftingMachine(BlockEntity machine, ModuleMachineBase module, int blueprintSlot)
        implements CraftingMachine {

    @Override
    public boolean acceptsPlans() {
        if (module.progress > 0D) return false;
        for (int slot : module.inputSlots) {
            if (!module.inventory.getStackInSlot(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(machine.getBlockState().getBlock());
    }

    @Override
    public boolean push(CraftingPlan plan) {
        if (!acceptsPlans()) return false;
        if (module.outputSlots != null) {
            for (int slot : module.outputSlots) {
                if (!module.inventory.getStackInSlot(slot).isEmpty()) return false;
            }
        }
        String pool = ItemBlueprints.grabPool(module.inventory.getStackInSlot(blueprintSlot));
        for (GenericRecipe recipe : module.getRecipeSet().recipeOrderedList) {
            if (recipe.isPooled() && !recipe.isPartOfPool(pool)) continue;
            if (!makes(recipe, plan)) continue;
            Placement placement = place(recipe, plan);
            if (placement == null) continue;
            apply(recipe, placement);
            return true;
        }
        return false;
    }

    private record Placement(ItemStack[] items, int[] fluids) {
    }

    private static boolean makes(GenericRecipe recipe, CraftingPlan plan) {
        if (plan.primaryItem() != null) {
            if (recipe.outputItem != null) {
                for (ItemStack out : recipe.outputItem) {
                    if (out != null && ItemStack.isSameItem(out, plan.primaryItem())) return true;
                }
            }
            if (recipe.outputChoices != null) {
                for (GenericRecipe.OutputChoice choice : recipe.outputChoices) {
                    if (ItemStack.isSameItem(choice.stack(), plan.primaryItem())) return true;
                }
            }
        }
        if (plan.primaryFluid() != null && recipe.outputFluid != null) {
            for (FluidStack out : recipe.outputFluid) {
                if (out != null && out.type == Fluids.fromVanilla(plan.primaryFluid())) return true;
            }
        }
        return false;
    }

    private @Nullable Placement place(GenericRecipe recipe, CraftingPlan plan) {
        AStack[] wantItems = recipe.inputItem == null ? new AStack[0] : recipe.inputItem;
        FluidStack[] wantFluids = recipe.inputFluid == null ? new FluidStack[0] : recipe.inputFluid;
        if (wantItems.length == 0 && wantFluids.length == 0) return null;
        if (wantItems.length > module.inputSlots.length) return null;
        if (wantFluids.length > 0 && (module.inputTanks == null || wantFluids.length > module.inputTanks.length)) return null;

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : plan.items()) items.add(stack.copy());
        ItemStack[] placed = new ItemStack[wantItems.length];
        for (int i = 0; i < wantItems.length; i++) {
            placed[i] = take(items, wantItems[i]);
            if (placed[i] == null) return null;
        }
        for (ItemStack left : items) {
            if (!left.isEmpty()) return null;
        }

        List<CraftingPlan.FluidAmount> fluids = new ArrayList<>(plan.fluids());
        int[] fills = new int[wantFluids.length];
        for (int i = 0; i < wantFluids.length; i++) {
            FluidStack want = wantFluids[i];
            for (int j = 0; j < fluids.size(); j++) {
                if (Fluids.fromVanilla(fluids.get(j).fluid()) != want.type) continue;
                fills[i] = fluids.remove(j).mB();
                break;
            }
            FluidTankNTM tank = module.inputTanks[i];
            boolean same = tank.getTankType() == want.type && tank.getPressure() == want.pressure;
            if ((same ? tank.getFill() : 0) + fills[i] > tank.getMaxFill()) return null;
        }
        return fluids.isEmpty() ? new Placement(placed, fills) : null;
    }

    private static @Nullable ItemStack take(List<ItemStack> items, AStack want) {
        for (ItemStack candidate : items) {
            if (want.matchesRecipe(candidate, false)) return candidate.split(want.stacksize);
        }
        return null;
    }

    private void apply(GenericRecipe recipe, Placement placement) {
        module.setRecipe(recipe.getInternalName(), false);
        module.setupTanks(recipe);
        for (int i = 0; i < placement.items().length; i++) {
            module.inventory.setStackInSlot(module.inputSlots[i], placement.items()[i]);
        }
        for (int i = 0; i < placement.fluids().length; i++) {
            if (placement.fluids()[i] <= 0) continue;
            FluidTankNTM tank = module.inputTanks[i];
            tank.setFill(tank.getFill() + placement.fluids()[i]);
        }
        module.markDirty = true;
        machine.setChanged();
    }
}
