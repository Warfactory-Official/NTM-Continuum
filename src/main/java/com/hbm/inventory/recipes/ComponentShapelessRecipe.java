package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidTank;
import dev.golem.core.world.ComponentIngredient;
import dev.golem.core.world.ItemStackSpec;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * HBM's output policy for component shapeless crafting: a crafted fluid container (1.7's
 * {@code new ItemStack(ModItems.canister_full, 2, Fluids.LUBRICANT.getID())}) gets its fluid (now the {@code hbm:fluid} registry name) written the way
 * every other filled container does, tint included, so it looks and stacks like a machine-filled one.
 */
public final class ComponentShapelessRecipe extends dev.golem.core.world.ComponentShapelessRecipe {

    public static final RecipeSerializer<ComponentShapelessRecipe> SERIALIZER =
            dev.golem.core.world.CustomRecipes.componentShapeless(ComponentShapelessRecipe::new);

    private ComponentShapelessRecipe(String namespace, String path, List<ComponentIngredient> ingredients,
                                     ItemStackSpec result) {
        super(namespace, path, ingredients, result);
    }

    /** Test and hand-built recipe entry point; datapack loading uses {@link #SERIALIZER}. */
    public static ComponentShapelessRecipe of(List<ComponentIngredient> ingredients, ItemStack result) {
        return new ComponentShapelessRecipe("hbm", "component_shapeless", ingredients, ItemStackSpec.of(result));
    }

    @Override
    protected ItemStack finishResult(ItemStack assembled) {
        if (assembled.getItem() instanceof ItemFluidTank tank) {
            FluidType type = ItemFluidTank.getType(assembled);
            if (type != Fluids.NONE) tank.setFluid(assembled, type, tank.getFill(assembled));
        }
        return assembled;
    }

    @Override
    protected RecipeSerializer<?> serializer() {
        return SERIALIZER;
    }
}
