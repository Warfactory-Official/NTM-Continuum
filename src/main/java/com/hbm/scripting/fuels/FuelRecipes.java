package com.hbm.scripting.fuels;

import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.scripting.ScriptedContent;
import dev.golem.core.world.DataPaths;
import dev.golem.core.world.RecipeJson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Crafting recipes declared by fuel builders, emitted into the KubeJS data pack as {@code hbm:scripted/<item>[_n]}.
 * Ingredients {@code "ns:item"} | {@code "#ns:tag"}.
 *
 * <pre>
 * .shapeless('hbm:rod_empty', '#c:billets/uranium')
 * .shaped(['F', 'I', 'F'], {F: '#c:billets/uranium', I: 'hbm:plate_polymer'})
 * </pre>
 */
public final class FuelRecipes {

    private static final Map<String, JsonObject> RECIPES = new LinkedHashMap<>();
    private static final Map<String, JsonObject> VIEW = Collections.unmodifiableMap(RECIPES);
    private static boolean hooked;

    private FuelRecipes() {}

    /** Recipe id path (under {@code hbm:}) to its JSON, in declaration order. */
    public static Map<String, JsonObject> all() {
        return VIEW;
    }

    /** {@code result}: {@code "ns:item"}, or a bare {@code hbm:} path. */
    static void shapeless(String result, int count, String... ingredients) {
        add(result, RecipeJson.shapeless(id(result), count, Arrays.asList(ingredients)));
    }

    static void shaped(String result, int count, List<String> pattern, Map<Character, String> key) {
        add(result, RecipeJson.shaped(id(result), count, pattern, key));
    }

    private static String id(String result) {
        return result.indexOf(':') < 0 ? Constants.MOD_ID + ":" + result : result;
    }

    static void shaped(String result, String[] pattern, Map<?, ?> key) {
        Map<Character, String> symbols = new LinkedHashMap<>();
        key.forEach((symbol, ingredient) -> {
            String s = String.valueOf(symbol);
            if (s.length() != 1) throw new IllegalArgumentException("Recipe key must be one character: " + s);
            symbols.put(s.charAt(0), String.valueOf(ingredient));
        });
        shaped(result, 1, List.of(pattern), symbols);
    }

    static String[] with(String head, String repeated, int n) {
        String[] out = new String[n + 1];
        out[0] = head;
        Arrays.fill(out, 1, n + 1, repeated);
        return out;
    }

    static final class Pending {

        private final List<Consumer<String>> recipes = new ArrayList<>();

        void add(Consumer<String> recipe) {
            recipes.add(recipe);
        }

        void shapeless(String... ingredients) {
            String[] copy = ingredients.clone();
            recipes.add(item -> FuelRecipes.shapeless(item, 1, copy));
        }

        void shaped(String[] pattern, Map<?, ?> key) {
            String[] rows = pattern.clone();
            Map<?, ?> symbols = new LinkedHashMap<>(key);
            recipes.add(item -> FuelRecipes.shaped(item, rows, symbols));
        }

        void emit(String item) {
            recipes.forEach(recipe -> recipe.accept(item));
        }
    }

    private static synchronized void add(String result, JsonObject recipe) {
        String name = result.substring(result.indexOf(':') + 1);
        String id = "scripted/" + name;
        for (int n = 1; RECIPES.containsKey(id); n++) id = "scripted/" + name + "_" + n;
        RECIPES.put(id, recipe);
        if (hooked) return;
        hooked = true;
        ScriptedContent.data(out -> RECIPES.forEach((path, json) -> out.json(Constants.MOD_ID, DataPaths.recipe(path), json)));
    }
}
