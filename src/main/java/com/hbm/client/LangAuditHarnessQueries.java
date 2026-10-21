package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.harness.HarnessQueryRegistry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Harness query {@code hbm_lang_audit}: translation keys that would show raw. {@code mode=stacks, from, to} checks the
 * hover name and every tooltip line of each icon-audit stack; {@code mode=registries} checks block, entity, effect and
 * fluid keys.
 */
public final class LangAuditHarnessQueries {

    private static final Pattern RAW = Pattern.compile(
            "\\b(?:item|block|tile|desc|container|entity|effect|death|gui|hbm|rbmk|pollution|potion|hbmfluid|itemGroup|key|commands)\\.[A-Za-z0-9_$]+(?:\\.[A-Za-z0-9_$]+)*");

    private LangAuditHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_lang_audit", LangAuditHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        String mode = params.has("mode") ? params.get("mode").getAsString() : "stacks";
        return mode.equals("registries") ? registries() : stacks(mc, params);
    }

    private static JsonObject stacks(Minecraft mc, JsonObject params) {
        List<ItemStack> all = IconAuditHarnessQueries.allStacks();
        int from = params.has("from") ? params.get("from").getAsInt() : 0;
        int to = Math.min(all.size(), params.has("to") ? params.get("to").getAsInt() : all.size());
        JsonArray flagged = new JsonArray();
        JsonArray dump = new JsonArray();
        boolean dumping = params.has("dump");
        int lines = 0;
        for (int i = from; i < to; i++) {
            ItemStack stack = all.get(i);
            String id = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            Set<String> bad = new LinkedHashSet<>();
            JsonArray texts = new JsonArray();
            JsonArray allLines = new JsonArray();
            try {
                check(stack.getHoverName(), bad);
                if (rawLooking(stack.getHoverName().getString())) bad.add("name:" + stack.getHoverName().getString());
                for (Component line : Screen.getTooltipFromItem(mc, stack)) {
                    lines++;
                    check(line, bad);
                    String text = line.getString();
                    allLines.add(text);
                    var m = RAW.matcher(text);
                    while (m.find()) if (!Language.getInstance().has(m.group())) bad.add("text:" + m.group());
                    if (!bad.isEmpty()) texts.add(text);
                }
            } catch (RuntimeException e) {
                bad.add("EXC:" + e);
            }
            if (dumping) {
                JsonObject d = new JsonObject();
                d.addProperty("index", i);
                d.addProperty("id", id);
                d.addProperty("name", stack.getHoverName().getString());
                d.add("lines", allLines);
                dump.add(d);
            }
            if (!bad.isEmpty()) {
                JsonObject o = new JsonObject();
                o.addProperty("index", i);
                o.addProperty("id", id);
                o.addProperty("name", stack.getHoverName().getString());
                JsonArray keys = new JsonArray();
                bad.forEach(keys::add);
                o.add("keys", keys);
                o.add("lines", texts);
                flagged.add(o);
            }
        }
        JsonObject json = new JsonObject();
        json.addProperty("total", all.size());
        json.addProperty("checked", to - from);
        json.addProperty("lines", lines);
        json.add("flagged", flagged);
        if (dumping) json.add("dump", dump);
        return json;
    }

    private static JsonObject registries() {
        Language lang = Language.getInstance();
        JsonObject json = new JsonObject();
        JsonArray bad = new JsonArray();
        int n = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) continue;
            n++;
            if (!lang.has(block.getDescriptionId())) bad.add("block:" + block.getDescriptionId());
        }
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!Constants.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) continue;
            n++;
            if (!lang.has(type.getDescriptionId())) bad.add("entity:" + type.getDescriptionId());
        }
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            if (!Constants.MOD_ID.equals(BuiltInRegistries.MOB_EFFECT.getKey(effect).getNamespace())) continue;
            n++;
            if (!lang.has(effect.getDescriptionId())) bad.add("effect:" + effect.getDescriptionId());
        }
        for (FluidType fluid : Fluids.getInNiceOrder()) {
            n++;
            if (!lang.has(fluid.getTranslationKey())) bad.add("fluid:" + fluid.getTranslationKey());
        }
        json.addProperty("checked", n);
        json.add("missing", bad);
        return json;
    }

    private static void check(Component component, Set<String> out) {
        if (component.getContents() instanceof TranslatableContents t) {
            if (t.getFallback() == null && !Language.getInstance().has(t.getKey())) out.add(t.getKey());
            for (Object arg : t.getArgs()) if (arg instanceof Component c) check(c, out);
        }
        for (Component sibling : component.getSiblings()) check(sibling, out);
    }

    private static boolean rawLooking(String s) {
        return s.indexOf(' ') < 0 && s.matches("[A-Za-z0-9_$]+(\\.[A-Za-z0-9_$]+)+");
    }
}
