package com.hbm.gametest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes every {@link GenericRecipes} machine table to {@code hbm-recipe-dump.json} in the run directory, in a
 * canonical text form (item ids, tag ids, autogen material shapes, fluid names and amounts), plus the
 * {@link OreDictManager} constant-by-shape table. It is the port half of a scripted comparison
 * against 1.7. A tool, not a test: declared only when the GameTest filter names
 * {@code recipe_table_dump}, like {@link RegistryDumpGameTests}.
 */
public final class RecipeTableDump {

    private RecipeTableDump() {}

    static boolean requested() {
        String filter = System.getProperty("golem.gametest.filter");
        return filter != null && filter.contains("recipe_table_dump");
    }

    static String stack(AStack stack) {
        if (stack == null) return "null";
        if (stack instanceof ComparableStack c) {
            ItemStack display = c.getDisplayStack();
            return "item:" + BuiltInRegistries.ITEM.getKey(display.getItem()) + "x" + c.stacksize
                    + nameSuffix(display);
        }
        if (stack instanceof TagStack t) return "tag:" + t.tag.location() + "x" + t.stacksize;
        if (stack instanceof MaterialStack m) return "mat:" + m.material.name + "/" + m.shape + "x" + m.stacksize;
        return stack.getClass().getSimpleName() + ":" + stack + "x" + stack.stacksize;
    }

    static String nameSuffix(ItemStack stack) {
        String name = stack.getHoverName().getString();
        String plain = new ItemStack(stack.getItem()).getHoverName().getString();
        return name.equals(plain) ? "" : "{" + name + "}";
    }

    static String out(ItemStack stack) {
        if (stack == null) return "null";
        return "item:" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount() + nameSuffix(stack);
    }

    static String fluid(FluidStack stack) {
        if (stack == null) return "null";
        return "fluid:" + stack.type.getName() + "x" + stack.fill + (stack.pressure != 0 ? "@" + stack.pressure : "");
    }

    private static JsonArray arr(Object[] values, Function<Object, String> f) {
        JsonArray a = new JsonArray();
        if (values != null) for (Object v : values) a.add(f.apply(v));
        return a;
    }

    private static JsonObject table(GenericRecipes recipes) {
        if (recipes.recipeOrderedList.isEmpty()) recipes.registerRecipes();
        JsonObject table = new JsonObject();
        for (GenericRecipe r : recipes.recipeOrderedList) {
            JsonObject o = new JsonObject();
            o.addProperty("duration", r.duration);
            o.addProperty("power", r.power);
            o.add("in", arr(r.inputItem, s -> stack((AStack) s)));
            o.add("out", arr(r.outputItem, s -> out((ItemStack) s)));
            if (r.outputChance != null) {
                JsonArray ch = new JsonArray();
                for (float f : r.outputChance) ch.add(f);
                o.add("chance", ch);
            }
            if (r.outputChoices != null) {
                JsonArray ch = new JsonArray();
                for (GenericRecipe.OutputChoice c : r.outputChoices) ch.add(out(c.stack()) + "w" + c.weight());
                o.add("choices", ch);
            }
            o.add("fin", arr(r.inputFluid, s -> fluid((FluidStack) s)));
            o.add("fout", arr(r.outputFluid, s -> fluid((FluidStack) s)));
            if (r.blueprintPools != null) o.add("pools", arr(r.blueprintPools, String::valueOf));
            if (r.autoSwitchGroup != null) o.addProperty("group", r.autoSwitchGroup);
            table.add(r.getInternalName(), o);
        }
        return table;
    }

    private static JsonObject constants() {
        JsonObject consts = new JsonObject();
        Map<String, NTMMaterial> mats = new LinkedHashMap<>();
        for (Class<?> owner : new Class<?>[]{OreDictManager.class, Mats.class}) {
            for (Field f : owner.getFields()) {
                if (!Modifier.isStatic(f.getModifiers()) || f.getType() != NTMMaterial.class) continue;
                try {
                    mats.put(f.getName(), (NTMMaterial) f.get(null));
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        String[] shapes = {"nugget", "billet", "ingot", "gem", "crystal", "dust", "plate", "block", "ore",
                "plateCast", "plateWelded", "shell", "pipe", "wire", "wireDense", "bolt", "heavyComp"};
        for (Map.Entry<String, NTMMaterial> e : mats.entrySet()) {
            JsonObject m = new JsonObject();
            for (String shape : shapes) {
                try {
                    Method method = NTMMaterial.class.getMethod(shape);
                    m.addProperty(shape, stack((AStack) method.invoke(e.getValue())));
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    m.addProperty(shape, "<" + failure.getClass().getSimpleName() + ">");
                }
            }
            consts.add(e.getKey(), m);
        }
        return consts;
    }

    public static void dump(GameTestHelper helper) {
        JsonObject root = new JsonObject();
        Map<String, GenericRecipes> tables = new LinkedHashMap<>();
        tables.put("assembly", AssemblyMachineRecipes.INSTANCE);
        tables.put("chemplant", ChemicalPlantRecipes.INSTANCE);
        tables.put("plasmaforge", PlasmaForgeRecipes.INSTANCE);
        tables.put("precass", PrecAssRecipes.INSTANCE);
        tables.put("purex", PUREXRecipes.INSTANCE);
        tables.put("blastnt", BlastFurnaceRecipesNT.INSTANCE);
        tables.put("crucible", CrucibleRecipes.INSTANCE);
        tables.put("difurnace", DiFurnaceRecipes.INSTANCE);
        tables.put("exposure", ExposureChamberRecipes.INSTANCE);
        tables.put("fluidbreeder", FluidBreederRecipes.INSTANCE);
        tables.put("fusion", FusionRecipes.INSTANCE);
        tables.put("mixer", MixerRecipes.INSTANCE);
        tables.put("outgasser", OutgasserRecipes.INSTANCE);
        JsonObject all = new JsonObject();
        for (Map.Entry<String, GenericRecipes> e : tables.entrySet()) {
            try {
                all.add(e.getKey(), table(e.getValue()));
            } catch (RuntimeException failure) {
                all.addProperty(e.getKey(), "<error " + failure + ">");
            }
        }
        root.add("tables", all);
        root.add("constants", constants());
        Path out = Path.of(System.getProperty("hbm.recipeDump", "hbm-recipe-dump.json")).toAbsolutePath();
        try {
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root),
                    StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        Constants.LOG.info("[RECIPEDUMP] wrote {} tables to {}", all.size(), out);
        helper.succeed();
    }
}
