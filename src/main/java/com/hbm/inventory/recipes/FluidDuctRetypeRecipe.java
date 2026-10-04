package com.hbm.inventory.recipes;

import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFluidItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidDuct;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code CraftingManager}:643-661, the per-fluid duct loop folded into one recipe: for every fluid {@code i >= 1},
 * one or eight plain {@code fluid_duct_neo} - or one or eight typed {@code fluid_duct} of any fluid - plus a fluid
 * identifier set to {@code i} give as many {@code fluid_duct} of fluid {@code i}. 1.7 registered these as
 * 4 x (fluid count) shapeless recipes; the arithmetic is the same here, decided in Java.
 *
 * <p>The identifier stays in the grid, as 1.7's {@code ItemFluidIDMulti.getContainerItem} (a copy of itself) kept
 * it. 1.7 matched the multi identifier by its metadata, which {@code updateMeta} kept equal to the primary fluid;
 * here the primary fluid is read directly, and the port's single-fluid {@code fluid_identifier} is accepted too.
 */
public final class FluidDuctRetypeRecipe extends CompatCustomRecipe {
    public static final FluidDuctRetypeRecipe INSTANCE = new FluidDuctRetypeRecipe();
    public static final RecipeSerializer<FluidDuctRetypeRecipe> SERIALIZER = CustomRecipes.serializer(() -> INSTANCE);

    private FluidDuctRetypeRecipe() {
        super("hbm", "fluid_duct_retype");
    }

    private record Grid(int ducts, FluidType type) {}

    private static Grid read(List<ItemStack> input) {
        int neo = 0;
        int typed = 0;
        FluidType type = null;
        for (ItemStack stack : input) {
            if (stack.isEmpty()) continue;
            if (stack.is(ModFluidNetBlocks.fluid_duct_neo.get().asItem())) neo++;
            else if (stack.is(ModFluidItems.fluid_duct.get())) typed++;
            else if (stack.getItem() instanceof IItemFluidId id) {
                if (type != null) return null;
                type = id.getType(null, 0, 0, 0, stack);
            } else return null;
        }
        if (type == null || type == Fluids.NONE) return null;
        if (neo > 0 && typed > 0) return null;
        int ducts = neo + typed;
        return ducts == 1 || ducts == 8 ? new Grid(ducts, type) : null;
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        return read(input) != null;
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        Grid grid = read(input);
        return grid == null ? ItemStack.EMPTY : ItemFluidDuct.make(ModFluidItems.fluid_duct.get(), grid.type(), grid.ducts());
    }

    @Override
    protected ItemStack remainder(List<ItemStack> input, int slot, ItemStack consumed) {
        if (consumed.getItem() instanceof IItemFluidId) return consumed.copyWithCount(1);
        return super.remainder(input, slot, consumed);
    }

    @Override
    protected RecipeSerializer<FluidDuctRetypeRecipe> serializer() {
        return SERIALIZER;
    }
}
