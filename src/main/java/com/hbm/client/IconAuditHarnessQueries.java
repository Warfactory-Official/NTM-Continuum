package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.ItemStacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;

/**
 * The {@code hbm_icon_audit} harness query read by {@code ItemIconAudit}: every HBM stack a player can see in a slot
 * (each creative-tab entry, each custom tab stack such as enum variants and filled containers, then every remaining
 * registered item in the requested namespace, {@code hbm} by default), paged onto an {@link IconAuditScreen}.
 *
 * <p>Params: {@code {"page":n}} opens page {@code n} and answers its manifest (slot index, label, item id, variant
 * ordinal, display name, GUI origin); {@code {"page":-1}} opens the empty background reference page;
 * {@code "alt":true} draws the page in the alternate measurement colours; {@code {"close":true}} closes the screen. {@code "filter":"<regex>"} (optional, on every call) restricts
 * the paged list to stacks whose label ({@code id} or {@code id@variant}) matches the regex anywhere ({@code find}); labels
 * and variant ordinals are still counted over the full list, so a filtered run's labels equal an unfiltered run's. Every
 * answer carries the totals and the GUI geometry. {@code "namespace":"ntmspace"} selects another registry namespace.
 * Inert unless the harness runs.
 */
public final class IconAuditHarnessQueries {

    private static final Map<String, List<ItemStack>> stacksByNamespace = new HashMap<>();

    private IconAuditHarnessQueries() {
    }

    public static void register() {
        HarnessQueryRegistry.register("hbm_icon_audit", IconAuditHarnessQueries::query);
        HarnessQueryRegistry.register("hbm_sweep_ids", IconAuditHarnessQueries::sweepIds);
        LangAuditHarnessQueries.register();
    }

    /**
     * Every hbm block (id, whether it has a block entity) and entity type (id, class name), sorted by id.
     */
    private static JsonObject sweepIds(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Constants.MOD_ID)) blocks.add(block);
        }
        blocks.sort(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()));
        JsonArray blockList = new JsonArray();
        for (Block block : blocks) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", BuiltInRegistries.BLOCK.getKey(block).toString());
            entry.addProperty("be", block instanceof EntityBlock);
            blockList.add(entry);
        }
        json.add("blocks", blockList);
        List<EntityType<?>> types = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Constants.MOD_ID)) types.add(type);
        }
        types.sort(Comparator.comparing(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).toString()));
        JsonArray entityList = new JsonArray();
        for (EntityType<?> type : types) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            Entity probe = mc.level == null ? null : Entities.createDetached(type, mc.level);
            entry.addProperty("cls", probe == null ? "" : probe.getClass().getSimpleName());
            entityList.add(entry);
        }
        json.add("entities", entityList);
        return json;
    }

    /**
     * Every stack of the icon audit, built once.
     */
    static List<ItemStack> allStacks() {
        return stacksByNamespace.computeIfAbsent(Constants.MOD_ID, IconAuditHarnessQueries::collect);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        if (params.has("close")) {
            mc.setScreen(null);
            json.addProperty("closed", true);
            return json;
        }
        String namespace = params.has("namespace") ? params.get("namespace").getAsString() : Constants.MOD_ID;
        if (params.has("refresh")) stacksByNamespace.remove(namespace);
        List<ItemStack> stacks = stacksByNamespace.computeIfAbsent(namespace, IconAuditHarnessQueries::collect);
        // full-list index of every stack shown (all of them unless a label filter is given)
        List<Integer> view = new ArrayList<>();
        List<String> labels = labels(stacks);
        Pattern filter = params.has("filter") && !params.get("filter").getAsString().isEmpty()
                ? Pattern.compile(params.get("filter").getAsString()) : null;
        for (int i = 0; i < stacks.size(); i++) if (filter == null || filter.matcher(labels.get(i)).find()) view.add(i);
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        int perPage = IconAuditScreen.columns(guiWidth) * IconAuditScreen.rows(guiHeight);
        int pages = (view.size() + perPage - 1) / perPage;
        json.addProperty("total", view.size());
        json.addProperty("total_unfiltered", stacks.size());
        json.addProperty("namespace", namespace);
        if (filter != null) json.addProperty("filter", filter.pattern());
        json.addProperty("per_page", perPage);
        json.addProperty("pages", pages);
        json.addProperty("gui_width", guiWidth);
        json.addProperty("gui_height", guiHeight);
        json.addProperty("cell", IconAuditScreen.CELL);
        if (!params.has("page")) return json;

        int page = params.get("page").getAsInt();
        List<ItemStack> shown = new ArrayList<>();
        JsonArray slots = new JsonArray();
        if (page >= 0) {
            for (int v = page * perPage; v < Math.min(view.size(), (page + 1) * perPage); v++) {
                int i = view.get(v);
                ItemStack stack = stacks.get(i);
                int slot = shown.size();
                shown.add(stack);
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String label = labels.get(i);
                int at = label.indexOf('@');
                JsonObject entry = new JsonObject();
                entry.addProperty("slot", slot);
                entry.addProperty("index", i);
                entry.addProperty("id", id);
                entry.addProperty("variant", at < 0 ? 0 : Integer.parseInt(label.substring(at + 1)));
                entry.addProperty("label", label);
                entry.addProperty("name", stack.getHoverName().getString());
                entry.addProperty("bar", stack.isBarVisible());
                entry.addProperty("x", IconAuditScreen.slotX(slot, guiWidth));
                entry.addProperty("y", IconAuditScreen.slotY(slot, guiWidth));
                slots.add(entry);
            }
        }
        mc.setScreen(new IconAuditScreen(shown, perPage, params.has("alt") && params.get("alt").getAsBoolean()));
        json.addProperty("alt", params.has("alt") && params.get("alt").getAsBoolean());
        json.addProperty("page", page);
        json.add("slots", slots);
        return json;
    }

    /**
     * {@code id} for an item's first stack, {@code id@k} for its k-th further stack, counted over the full list.
     */
    private static List<String> labels(List<ItemStack> all) {
        List<String> out = new ArrayList<>(all.size());
        Map<Item, Integer> ordinal = new HashMap<>();
        for (ItemStack stack : all) {
            int k = ordinal.merge(stack.getItem(), 1, Integer::sum) - 1;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            out.add(k == 0 ? id : id + "@" + k);
        }
        return out;
    }

    /**
     * Tab order first, then any registered item in the namespace no tab shows. One stack per distinct item+data.
     */
    private static List<ItemStack> collect(String namespace) {
        List<ItemStack> out = new ArrayList<>();
        Map<Integer, List<ItemStack>> seen = new HashMap<>();
        for (var list : ModTabs.CONTENTS.values()) {
            for (var handle : list) {
                try {
                    add(out, seen, new ItemStack(handle.get()));
                } catch (RuntimeException ignored) {
                    // an unregistered handle simply is not shown in a tab either
                }
            }
        }
        for (List<Supplier<List<ItemStack>>> suppliers : ModTabs.STACK_CONTENTS.values()) {
            for (Supplier<List<ItemStack>> supplier : suppliers) {
                try {
                    for (ItemStack stack : supplier.get()) add(out, seen, ItemStacks.copyWithCount(stack, 1));
                } catch (RuntimeException ignored) {
                    // a supplier that needs a world state the client lacks is skipped, like a tab build failure
                }
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (!namespace.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) continue;
            add(out, seen, new ItemStack(item));
        }
        out.removeIf(stack -> !namespace.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()));
        return out;
    }

    private static void add(List<ItemStack> out, Map<Integer, List<ItemStack>> seen, ItemStack stack) {
        if (stack.isEmpty()) return;
        List<ItemStack> bucket = seen.computeIfAbsent(ItemStacks.hashItemAndData(stack), h -> new ArrayList<>());
        for (ItemStack other : bucket) if (ItemStacks.sameItemAndData(other, stack)) return;
        bucket.add(stack);
        out.add(stack);
    }
}
