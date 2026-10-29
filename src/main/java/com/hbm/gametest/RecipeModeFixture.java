package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.hbm.config.GeneralConfig;
import dev.golem.core.data.DataFiles;
import dev.golem.core.world.RecipeTables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.packs.resources.ResourceManager;

/** Installs synchronous recipe fixtures as a datapack load under the requested configuration would. */
final class RecipeModeFixture {
    private static ResourceManager source;
    private static Map<String, JsonElement> files;
    private static Set<String> removed;

    private RecipeModeFixture() {
    }

    static void under(GameTestHelper helper, boolean expensive, boolean mode528, Runnable body) {
        boolean oldExpensive = GeneralConfig.enableExpensiveMode;
        boolean old528 = GeneralConfig.enable528;
        var tables = GenericRecipeTableGameTests.tables().values();
        List<Runnable> restore = new ArrayList<>();
        for (var table : tables) restore.add(snapshot(table.table));
        try {
            GeneralConfig.enableExpensiveMode = expensive;
            GeneralConfig.enable528 = mode528;
            var resources = helper.getLevel().getServer().getResourceManager();
            if (resources != source) {
                files = DataFiles.read(DataFiles.recipeFolder(), resources);
                removed = new HashSet<>();
                for (var file : DataFiles.read(RecipeTables.REMOVALS, resources).values()) {
                    for (var id : file.getAsJsonObject().getAsJsonArray("recipes")) removed.add(id.getAsString());
                }
                source = resources;
            }
            for (var table : tables) install(table.table, files, removed, helper.getLevel().registryAccess());
            body.run();
        } finally {
            GeneralConfig.enableExpensiveMode = oldExpensive;
            GeneralConfig.enable528 = old528;
            for (var reset : restore) reset.run();
            for (var table : tables) table.registerRecipes();
        }
    }

    private static <T> Runnable snapshot(RecipeTables.Table<T> table) {
        var rows = List.copyOf(table.entries());
        return () -> table.replace(rows);
    }

    private static <T> void install(RecipeTables.Table<T> table, Map<String, JsonElement> files, Set<String> removed,
                                    HolderLookup.Provider registries) {
        List<RecipeTables.Entry<T>> rows = new ArrayList<>();
        for (var file : files.entrySet()) {
            var json = file.getValue().getAsJsonObject();
            if (!json.has("type") || !json.get("type").getAsString().equals(table.id()) || removed.contains(
                    file.getKey())) continue;
            List<String> flags = new ArrayList<>();
            var requires = json.get(RecipeTables.REQUIRES);
            if (requires != null) {
                if (requires.isJsonArray()) for (var flag : requires.getAsJsonArray()) flags.add(flag.getAsString());
                else flags.add(requires.getAsString());
            }
            if (RecipeTables.holds(flags, file.getKey())) {
                rows.add(new RecipeTables.Entry<>(file.getKey(), table.fromJson(json, registries)));
            }
        }
        table.replace(rows);
    }
}
