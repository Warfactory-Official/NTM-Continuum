package com.hbm.scripting.recipes;

import com.hbm.Constants;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.material.Mats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Recipe ids: {@code hbm:<machine>/<key>}. Keyed tables derive the key from their input; named
 * tables use the name; list tables derive it from the first input, suffixed {@code _2, _3...} in
 * table order when two recipes share it. Counts never enter a key; pressure does.
 */
public final class RecipeIds {

    private RecipeIds() {}

    public static String of(String machine, String key) {
        return Constants.MOD_ID + ":" + machine + "/" + sanitize(key);
    }

    /** {@code minecraft:stone -> minecraft.stone}, {@code #c:ores/x -> tag.c.ores/x}. */
    public static String sanitize(String key) {
        String s = key.toLowerCase(Locale.US).replace("#", "tag.").replace(':', '.');
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-' ? c : '_');
        }
        return out.toString();
    }

    /** Views for {@code recipes} in table order, ids derived from {@code key} and made unique. */
    public static <R> List<RecipeView> views(String machine, List<R> recipes, Function<R, String> key,
            Function<R, List<String>> inputs, Function<R, List<String>> outputs) {
        Map<String, Integer> seen = new HashMap<>();
        return recipes.stream().map(recipe -> {
            String id = of(machine, uncounted(key.apply(recipe)));
            int n = seen.merge(id, 1, Integer::sum);
            return new RecipeView(n == 1 ? id : id + "_" + n, machine, inputs.apply(recipe), outputs.apply(recipe), recipe);
        }).toList();
    }

    /** A stack id or tag in spec form, for keys and view rows. */
    public static String spec(Object thing) {
        if (thing == null) return "";
        if (thing instanceof ItemStack stack) return count(stack.getCount(), BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (thing instanceof Item item) return BuiltInRegistries.ITEM.getKey(item).toString();
        if (thing instanceof TagStack tag) return count(tag.stacksize, "#" + tag.tag.location());
        if (thing instanceof ComparableStack stack) return count(stack.stacksize, BuiltInRegistries.ITEM.getKey(stack.item).toString());
        if (thing instanceof MaterialStack stack) return count(stack.stacksize, stack.material.name + "." + stack.shape.name().toLowerCase(Locale.US));
        if (thing instanceof AStack stack) return count(stack.stacksize, spec(stack.getDisplayStack().getItem()));
        if (thing instanceof FluidStack fluid) return count(fluid.fill, fluid.type.getName().toLowerCase(Locale.US))
                + (fluid.pressure != 0 ? "@" + fluid.pressure : "");
        if (thing instanceof FluidType type) return type.getName().toLowerCase(Locale.US);
        if (thing instanceof Mats.MaterialStack stack) return count(stack.amount, stack.material.name);
        return String.valueOf(thing);
    }

    /** A spec without its {@code Nx } count, so an id survives an amount change. */
    public static String uncounted(String spec) {
        String s = spec.trim();
        int x = s.indexOf("x ");
        return x > 0 && s.substring(0, x).chars().allMatch(Character::isDigit) ? s.substring(x + 2).trim() : s;
    }

    /** The key part of a spec: no count, no pressure. */
    public static String bare(String spec) {
        String s = spec.trim();
        int x = s.indexOf("x ");
        if (x > 0 && s.substring(0, x).chars().allMatch(Character::isDigit)) s = s.substring(x + 2).trim();
        int at = s.indexOf('@');
        return at >= 0 ? s.substring(0, at) : s;
    }

    static boolean sameSpec(String a, String b) {
        return bare(a).equalsIgnoreCase(bare(b));
    }

    private static String count(int count, String id) {
        return count == 1 ? id : count + "x " + id;
    }
}
