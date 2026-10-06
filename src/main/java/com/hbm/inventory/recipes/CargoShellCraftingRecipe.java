package com.hbm.inventory.recipes;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code CargoShellCraftingHandler}: an empty cargo artillery shell ({@code ammo_arty_cargo}, 1.7 {@code ammo_arty} meta 8, with no
 * cargo yet - 1.7 tested {@code !hasTagCompound()}) and exactly one other item, anywhere in the grid, give the
 * shell carrying one of that item ({@link ModDataComponents#ARTY_CARGO}, 1.7's {@code cargo} tag). Exactly two
 * occupied slots; an item with a crafting remainder (a bucket, a filled tank) refuses the grid, as 1.7's
 * {@code hasContainerItem} check did.
 */
public final class CargoShellCraftingRecipe extends CompatCustomRecipe {
    public static final CargoShellCraftingRecipe INSTANCE = new CargoShellCraftingRecipe();
    public static final RecipeSerializer<CargoShellCraftingRecipe> SERIALIZER = CustomRecipes.serializer(() -> INSTANCE);

    private CargoShellCraftingRecipe() {
        super("hbm", "ammo_arty_cargo");
    }

    private static boolean emptyShell(ItemStack stack) {
        return ModAmmoItems.ammo_arty.is(stack, com.hbm.items.weapon.ItemAmmoArty.ArtilleryShellType.CARGO)
                && !ModDataComponents.ARTY_CARGO.has(stack);
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        int items = 0;
        int shells = 0;
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            if (!Compat.craftingRemainder(stack).isEmpty()) return false;
            items++;
            if (emptyShell(stack)) shells++;
        }
        return items == 2 && shells == 1;
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        ItemStack shell = ItemStack.EMPTY;
        ItemStack cargo = ItemStack.EMPTY;
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            // 1.7 walks the grid and takes the last match of each role; with two items exactly one is the shell
            if (emptyShell(stack)) shell = stack.copyWithCount(1);
            else cargo = stack.copyWithCount(1);
        }
        if (shell.isEmpty() || cargo.isEmpty()) return ItemStack.EMPTY;
        ModDataComponents.ARTY_CARGO.set(shell, cargo);
        return shell;
    }

    @Override
    protected RecipeSerializer<CargoShellCraftingRecipe> serializer() {
        return SERIALIZER;
    }
}
