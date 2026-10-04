package com.hbm.inventory.recipes;

import com.hbm.items.weapon.grenade.ItemGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code GrenadeCraftingHandler}: one shell, one filling, one fuze and at most one extra, anywhere in the grid,
 * assemble into {@code ItemGrenadeUniversal.make(shell, filling, fuze, extra)}. Anything that is not a grenade part,
 * more than four parts, two parts of one kind, or a filling the shell cannot take refuses the grid.
 *
 * <p>1.7's {@code getFirst} returns null for a second part of the same kind, and the extra is only implicitly
 * limited (four parts total, three of them mandatory) - both kept. The port flattened 1.7's metadata parts into one
 * item per enum constant, so a part's kind is its item class and its value the item's {@code type}.
 */
public final class GrenadeCraftingRecipe extends CompatCustomRecipe {
    public static final GrenadeCraftingRecipe INSTANCE = new GrenadeCraftingRecipe();
    public static final RecipeSerializer<GrenadeCraftingRecipe> SERIALIZER = CustomRecipes.serializer(() -> INSTANCE);

    private GrenadeCraftingRecipe() {
        super("hbm", "grenade_universal");
    }

    /** 1.7 hasForeignObject: a non-part item, or more than four parts. */
    private static boolean hasForeignObject(List<ItemStack> input) {
        int count = 0;
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            Object item = stack.getItem();
            if (!(item instanceof ItemGrenadeShell) && !(item instanceof ItemGrenadeFilling)
                    && !(item instanceof ItemGrenadeFuze) && !(item instanceof ItemGrenadeExtra)) return true;
            if (++count > 4) return true;
        }
        return false;
    }

    /** 1.7 getFirst: the one part of this kind, or null when there is none or more than one. */
    private static <I, T> @Nullable T first(List<ItemStack> input, Class<I> kind, Function<I, T> type) {
        T found = null;
        for (ItemStack stack : input) {
            if (stack.isEmpty() || !kind.isInstance(stack.getItem())) continue;
            if (found != null) return null;
            found = type.apply(kind.cast(stack.getItem()));
        }
        return found;
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        if (hasForeignObject(input)) return false;
        ItemGrenadeShell.EnumGrenadeShell shell = first(input, ItemGrenadeShell.class, i -> i.type);
        ItemGrenadeFilling.EnumGrenadeFilling filling = first(input, ItemGrenadeFilling.class, i -> i.type);
        if (filling != null && shell != null && !filling.compatibleShells.contains(shell)) return false;
        ItemGrenadeFuze.EnumGrenadeFuze fuze = first(input, ItemGrenadeFuze.class, i -> i.type);
        return shell != null && filling != null && fuze != null;
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        if (!matchesItems(input, null)) return ItemStack.EMPTY;
        return ItemGrenadeUniversal.make(first(input, ItemGrenadeShell.class, i -> i.type),
                first(input, ItemGrenadeFilling.class, i -> i.type),
                first(input, ItemGrenadeFuze.class, i -> i.type),
                first(input, ItemGrenadeExtra.class, i -> i.type));
    }

    @Override
    protected RecipeSerializer<GrenadeCraftingRecipe> serializer() {
        return SERIALIZER;
    }
}
