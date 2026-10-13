package com.hbm.inventory.recipes;

import com.hbm.items.special.ItemBookLore;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatCustomRecipe;
import dev.golem.core.world.CustomRecipes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code MKUCraftingHandler}: the secret MKU syringe recipe. Six ingredients and three gaps are shuffled into a
 * 3x3 grid by {@code Collections.shuffle(list, new Random(worldSeed))} - the same {@link java.util.Random} and the
 * same shuffle, so a world seed yields 1.7's arrangement - and only that arrangement crafts
 * {@code syringe_mkunicorn}. The meteor-dungeon books ({@link #generateBook}) give away one ingredient's slot each.
 *
 * <p>1.7 compared the nine slots of the 3x3 table absolutely; the port's crafting input is trimmed to the occupied
 * rows and columns (as every shaped recipe is), so the arrangement may sit anywhere in the grid. 1.7's
 * {@code ingot_mercury} is the port's {@code nugget_mercury} (the 125 mB droplet).
 */
public final class MKUCraftingRecipe extends CompatCustomRecipe {
    public static final MKUCraftingRecipe INSTANCE = new MKUCraftingRecipe();
    public static final RecipeSerializer<MKUCraftingRecipe> SERIALIZER = CustomRecipes.serializer(() -> INSTANCE);

    /** 1.7's list order, which the seeded shuffle permutes; null is an empty slot. */
    static final String[] INGREDIENTS = {"powder_iodine", "powder_fire", "dust", "nugget_mercury", "morning_glory",
            "syringe_metal_empty", null, null, null};
    /** 1.7 generateBook: lore key and page count per ingredient, in {@link #INGREDIENTS} order. */
    private static final String[][] BOOKS = {{"book_iodine", "3"}, {"book_phosphorous", "2"}, {"book_dust", "3"},
            {"book_mercury", "2"}, {"book_flower", "2"}, {"book_syringe", "2"}};

    private static String @Nullable [] recipe;
    private static long lastSeed;

    private MKUCraftingRecipe() {
        super("hbm", "syringe_mkunicorn");
    }

    /** 1.7 generateRecipe: the nine-slot arrangement (row-major, null = empty) for this world seed. */
    public static synchronized String[] arrangement(long seed) {
        if (recipe == null || seed != lastSeed) {
            List<String> list = new ArrayList<>(Arrays.asList(INGREDIENTS));
            Collections.shuffle(list, new Random(seed));
            recipe = list.toArray(new String[9]);
            lastSeed = seed;
        }
        return recipe;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", id)).orElse(Items.AIR);
    }

    @Override
    protected boolean matchesGrid(int width, int height, List<ItemStack> input, Level level) {
        if (!(level instanceof ServerLevel server)) return false; // the seed is the server's secret
        String[] want = arrangement(server.getSeed());
        int[] box = box(3, 3, Arrays.asList(want).stream().map(s -> s != null).toList());
        int[] have = box(width, height, input.stream().map(s -> !s.isEmpty()).toList());
        if (box == null || have == null || box[2] != have[2] || box[3] != have[3]) return false;
        for (int row = 0; row < box[3]; row++) {
            for (int col = 0; col < box[2]; col++) {
                String id = want[(box[1] + row) * 3 + box[0] + col];
                ItemStack stack = input.get((have[1] + row) * width + have[0] + col);
                if (id == null ? !stack.isEmpty() : !stack.is(item(id))) return false;
            }
        }
        return true;
    }

    /** {minColumn, minRow, columns, rows} of the occupied cells, or null when none is occupied. */
    private static int @Nullable [] box(int width, int height, List<Boolean> occupied) {
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int i = 0; i < occupied.size(); i++) {
            if (!occupied.get(i)) continue;
            int x = i % width, y = i / width;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        return maxX < 0 ? null : new int[] {minX, minY, maxX - minX + 1, maxY - minY + 1};
    }

    @Override
    protected boolean matchesItems(List<ItemStack> input, Level level) {
        int side = input.size() == 4 ? 2 : 3;
        return matchesGrid(side, side, input, level);
    }

    @Override
    protected ItemStack assembleItems(List<ItemStack> input) {
        return new ItemStack(item("syringe_mkunicorn"));
    }

    /** 1.7 getMKUItem: one of the six ingredients at random. */
    public static String randomIngredient(RandomSource rand) {
        return INGREDIENTS[rand.nextInt(6)];
    }

    /** 1.7 generateBook: the lore book for {@code ingredient}, whose last page names that ingredient's slot (1..9). */
    public static ItemStack generateBook(long seed, String ingredient) {
        String[] arrangement = arrangement(seed);
        int index = Arrays.asList(INGREDIENTS).indexOf(ingredient);
        if (index < 0 || index >= BOOKS.length) return new ItemStack(item("flame_pony"));
        int slot = 1;
        for (int i = 0; i < 9; i++) {
            if (ingredient.equals(arrangement[i])) {
                slot = i + 1;
                break;
            }
        }
        int pages = Integer.parseInt(BOOKS[index][1]);
        ItemStack book = ItemBookLore.createBook(BOOKS[index][0], pages, 0x271E44, 0xFBFFF4);
        ItemBookLore.addArgs(book, pages - 1, String.valueOf(slot));
        return book;
    }

    public static ItemStack ingredientStack(String ingredient) {
        return new ItemStack(item(ingredient));
    }

    @Override
    protected RecipeSerializer<MKUCraftingRecipe> serializer() {
        return SERIALIZER;
    }
}
