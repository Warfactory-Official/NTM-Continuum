package com.hbm.inventory.recipes;

import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.inventory.RecipesCommon.AStack;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The controllers' crafting recipes. 1.7 {@code CustomMachineConfigJSON.readConfig} handed each machine's
 * {@code recipeShape}/{@code recipeParts} to {@code CraftingManager.addRecipeAuto}, a mirrorable shaped ore recipe
 * made at start-up from a player-edited file. A data-pack recipe cannot come from that file, so this one special
 * recipe ({@code hbm:custom_machine}) matches every configured shape at crafting time, mirrored or not, and yields
 * that machine's controller.
 */
public final class CustomMachineCraftingRecipe extends CompatCustomRecipe {

    public static final CustomMachineCraftingRecipe INSTANCE = new CustomMachineCraftingRecipe();
    public static final RecipeSerializer<CustomMachineCraftingRecipe> SERIALIZER = CustomRecipes.serializer(() -> INSTANCE);

    private CustomMachineCraftingRecipe() {
        super("hbm", "custom_machine");
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        int size = (int) Math.round(Math.sqrt(input.size()));
        return find(size, size, input) != null;
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        int size = (int) Math.round(Math.sqrt(input.size()));
        return assembleGrid(size, size, input);
    }

    @Override
    protected boolean matchesGrid(int width, int height, List<ItemStack> input, Level level) {
        return find(width, height, input) != null;
    }

    @Override
    protected ItemStack assembleGrid(int width, int height, List<ItemStack> input) {
        Integer index = find(width, height, input);
        if (index == null) return ItemStack.EMPTY;
        return ModCustomMachineBlocks.custom_machine_item.get().of(index);
    }

    /** The index of the first configured machine whose shape the grid holds, or null. */
    public static @Nullable Integer find(int width, int height, List<ItemStack> input) {
        for (int i = 0; i < CustomMachineConfigJSON.niceList.size(); i++) {
            MachineConfiguration config = CustomMachineConfigJSON.niceList.get(i);
            if (config.recipeShape == null || config.recipeParts() == null) continue;
            if (matches(config.recipeShape, config.recipeParts(), width, height, input)) return i;
        }
        return null;
    }

    /** Vanilla shaped matching over the grid: every offset, each also mirrored left to right. */
    static boolean matches(String[] shape, Map<Character, AStack> parts, int width, int height, List<ItemStack> input) {
        int rows = shape.length;
        int cols = 0;
        for (String row : shape) cols = Math.max(cols, row.length());
        if (rows == 0 || cols == 0 || rows > height || cols > width) return false;

        for (int ox = 0; ox <= width - cols; ox++) {
            for (int oy = 0; oy <= height - rows; oy++) {
                if (matchesAt(shape, parts, cols, width, height, input, ox, oy, false)) return true;
                if (matchesAt(shape, parts, cols, width, height, input, ox, oy, true)) return true;
            }
        }
        return false;
    }

    private static boolean matchesAt(String[] shape, Map<Character, AStack> parts, int cols, int width, int height,
                                     List<ItemStack> input, int ox, int oy, boolean mirror) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int sx = x - ox;
                int sy = y - oy;
                AStack wanted = null;
                if (sx >= 0 && sy >= 0 && sx < cols && sy < shape.length) {
                    String row = shape[sy];
                    int column = mirror ? cols - 1 - sx : sx;
                    char key = column < row.length() ? row.charAt(column) : ' ';
                    if (key != ' ') {
                        wanted = parts.get(key);
                        if (wanted == null) return false;
                    }
                }
                ItemStack stack = input.get(x + y * width);
                if (wanted == null) {
                    if (!stack.isEmpty()) return false;
                } else if (!wanted.matchesRecipe(stack, true)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected RecipeSerializer<CustomMachineCraftingRecipe> serializer() {
        return SERIALIZER;
    }
}
