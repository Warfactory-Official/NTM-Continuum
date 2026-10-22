package com.hbm.gametest;

import com.hbm.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.platform.Platform;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatSwordItem;
import dev.golem.core.world.CompatToolItem;
import dev.golem.core.world.Recipes;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** Contracts that only the pre-26.1 nodes can break: data shapes and client hooks that have no 26.1 equivalent. */
public final class LegacyCompatGameTests {

    private LegacyCompatGameTests() {}

    /**
     * Forge 1.20.1 reaches an item's custom renderer only through {@code Item#initializeClient}, which
     * Golem Core overrides in its {@code Compat*Item} base classes and nowhere else. An HBM item that
     * draws itself (an OBJ machine, a gun, a BEWLR) but extends a bare vanilla class therefore renders as
     * a blank icon on Forge while every other node looks right; nothing logs. This reads the built
     * item models, finds every item whose parent chain ends at {@code builtin/entity} and requires it to
     * be one of those bases.
     */
    public static void customRendererItemsUseCompatBases(GameTestHelper helper) {
        // Only below 1.21.4 (no item-definition layer) does a builtin/entity model chain decide the renderer. The
        // machine_obj_legacy marker is not a usable signal: it is authored and ships on every node, 26.1 included.
        boolean legacyBuild = !Platform.hasItemDefinitions();
        List<String> offenders = new ArrayList<>();
        int custom = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            var key = BuiltInRegistries.ITEM.getKey(item);
            if (!Constants.MOD_ID.equals(key.getNamespace())) continue;
            if (!selfRendered("hbm:item/" + key.getPath())) continue;
            custom++;
            boolean reachable = item instanceof CompatItem || item instanceof CompatBlockItem
                    || item instanceof CompatArmorItem || item instanceof CompatSwordItem
                    || item instanceof CompatToolItem;
            if (!reachable) offenders.add(key.getPath() + " (" + item.getClass().getSimpleName() + ")");
        }
        Constants.LOG.info("legacy_custom_renderer_items: legacyBuild={} selfRendered={} offenders={}",
                legacyBuild, custom, offenders);
        if (legacyBuild) {
            helper.assertTrue(custom > 0, "no self-rendered item models found on a node without item definitions");
            helper.assertTrue(offenders.isEmpty(), offenders.size() + " of " + custom
                    + " self-rendered items are not Core Compat item bases and get no renderer on Forge 1.20.1: "
                    + offenders);
        }
        helper.succeed();
    }

    /**
     * 1.20.1 serialises a cooking result as a bare item id, which dropped the count: a x16 meteoric
     * smelt produced one ingot. The build now routes count/tag results through {@code hbm:component_smelting};
     * this smelts in a real furnace so the assertion covers the recipe, the serializer and the output slot.
     */
    public static void smeltingKeepsResultCount(GameTestHelper helper) {
        assertSmelt(helper, 1, "ore_meteor_copper", "minecraft:copper_ingot", 16);
        assertSmelt(helper, 3, "crystal_diamond", null, 2);
        assertSmelt(helper, 5, "crystal_sulfur", "sulfur", 6);
        helper.succeed();
    }

    private static void assertSmelt(GameTestHelper helper, int x, String input, String output, int count) {
        BlockPos rel = new BlockPos(x, 1, 1);
        helper.setBlock(rel, Blocks.FURNACE);
        AbstractFurnaceBlockEntity furnace = GameTestHelpers.blockEntity(
                helper, rel, AbstractFurnaceBlockEntity.class);
        Item in = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, input));
        Item out = output == null ? Items.DIAMOND : BuiltInRegistries.ITEM.getValue(
                output.contains(":") ? Ids.parse(output) : Ids.of(Constants.MOD_ID, output));
        // The recipe itself, before the furnace: a count lost here is lost for every consumer of the recipe.
        ItemStack recipeResult = Recipes.smeltingResult(helper.getLevel(), new ItemStack(in))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(recipeResult.is(out) && recipeResult.getCount() == count,
                input + " recipe result is " + recipeResult + ", expected " + count + " x "
                        + BuiltInRegistries.ITEM.getKey(out));
        furnace.setItem(0, new ItemStack(in));
        furnace.setItem(1, new ItemStack(Items.COAL));
        BlockPos pos = helper.absolutePos(rel);
        for (int i = 0; i < 260 && furnace.getItem(2).isEmpty(); i++) {
            AbstractFurnaceBlockEntity.serverTick(helper.getLevel(), pos, furnace.getBlockState(), furnace);
        }
        ItemStack result = furnace.getItem(2);
        helper.assertTrue(result.is(out) && result.getCount() == count,
                input + " smelted to " + result + " (input slot " + furnace.getItem(0) + ", fuel slot "
                        + furnace.getItem(1) + "), expected " + count + " x " + BuiltInRegistries.ITEM.getKey(out));
    }

    /** Whether {@code id}'s model parent chain ends at {@code minecraft:builtin/entity}. */
    private static boolean selfRendered(String id) {
        for (int depth = 0; depth < 8 && id != null; depth++) {
            if (id.equals("minecraft:builtin/entity")) return true;
            JsonObject json = id.startsWith("hbm:") ? model(id) : null;
            if (json == null || !json.has("parent")) return false;
            id = json.get("parent").getAsString();
        }
        return false;
    }

    private static JsonObject model(String id) {
        String path = "/assets/" + id.replace(':', '/').replaceFirst("/", "/models/") + ".json";
        try (var stream = LegacyCompatGameTests.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
