package com.hbm.deimport.steps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.deimport.engine.Ctx;
import com.hbm.deimport.engine.Step;
import com.hbm.deimport.engine.Unit;
import com.hbm.deimport.source.de7.WorldTables;
import com.hbm.delegacy.de7.Tables;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * {@code advancements/*.json} and {@code stats/*.json}. A renamed advancement keeps its progress; one DE completed is
 * completed in Continuum even where the criteria are named differently. Stats follow the item, block and entity renames.
 */
public final class PlayerFilesStep implements Step {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public String id() {
        return "playerfiles@1";
    }

    @Override
    public boolean applies(Ctx ctx) {
        return Files.isDirectory(ctx.world.resolve("advancements")) || Files.isDirectory(ctx.world.resolve("stats"));
    }

    @Override
    public boolean run(Ctx ctx, @Nullable MinecraftServer server) throws IOException {
        for (Path file : json(ctx.world.resolve("advancements"))) {
            if (ctx.journal.done(id(), ctx.world, file)) continue;
            JsonObject root = read(ctx, file);
            if (root != null) {
                Unit unit = ctx.begin("advancements " + file.getFileName());
                JsonObject converted = null;
                try {
                    converted = advancements(root, ctx, file.getFileName().toString());
                } catch (RuntimeException e) {
                    failed(ctx, unit, file, e);
                }
                if (converted != null) replace(ctx, unit, file, converted);
            }
            ctx.journal.mark(id(), ctx.world, file);
        }
        for (Path file : json(ctx.world.resolve("stats"))) {
            if (ctx.journal.done(id(), ctx.world, file)) continue;
            JsonObject root = read(ctx, file);
            if (root == null) {
                ctx.journal.mark(id(), ctx.world, file);
                continue;
            }
            Unit unit = ctx.begin("stats " + file.getFileName());
            try {
                stats(ctx, file, root);
            } catch (RuntimeException e) {
                failed(ctx, unit, file, e);
                ctx.journal.mark(id(), ctx.world, file);
                continue;
            }
            replace(ctx, unit, file, root);
            ctx.journal.mark(id(), ctx.world, file);
        }
        return true;
    }

    private static void stats(Ctx ctx, Path file, JsonObject root) {
        JsonObject stats = root.has("stats") && root.get("stats").isJsonObject() ? root.getAsJsonObject("stats") : null;
        if (stats == null) return;
        for (Map.Entry<String, JsonElement> group : stats.entrySet()) {
            if (!group.getValue().isJsonObject()) continue;
            Map<String, String> table = switch (group.getKey()) {
                case "minecraft:mined" -> WorldTables.get().blocks;
                case "minecraft:killed", "minecraft:killed_by" -> Tables.get().entities;
                default -> Tables.get().items;
            };
            rename(group.getValue().getAsJsonObject(), table, group.getKey().equals("minecraft:custom"), ctx,
                    "stats " + file.getFileName() + " " + group.getKey());
        }
    }

    /** A file whose JSON the conversion cannot take stays as it is, with its text parked. */
    private static void failed(Ctx ctx, Unit unit, Path file, RuntimeException e) throws IOException {
        ctx.abandon(unit);
        ctx.parking.park("player_file", ctx.world.relativize(file).toString(), "not converted: " + e,
                StringTag.valueOf(Files.readString(file, StandardCharsets.UTF_8)));
        ctx.report.line(ctx.world.relativize(file) + " was not converted (" + e + ") and was left as it is");
    }

    private static JsonObject advancements(JsonObject root, Ctx ctx, String where) {
        JsonObject out = new JsonObject();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            String id = e.getKey();
            if (!id.startsWith("hbm:") || !e.getValue().isJsonObject()) {
                out.add(id, e.getValue());
                continue;
            }
            String target = WorldTables.get().advancements.getOrDefault(id, id);
            List<String> criteria = ctx.target.advancementCriteria(target);
            if (target.equals("park") || criteria == null) {
                ctx.parking.park("advancement", where, id, StringTag.valueOf(e.getValue().toString()));
                continue;
            }
            JsonObject progress = e.getValue().getAsJsonObject();
            JsonObject deCriteria = progress.has("criteria") ? progress.getAsJsonObject("criteria") : new JsonObject();
            JsonObject newCriteria = new JsonObject();
            boolean done = progress.has("done") && progress.get("done").getAsBoolean();
            String date = deCriteria.entrySet().stream().map(c -> c.getValue().getAsString()).findFirst().orElse(null);
            for (String criterion : criteria) {
                if (deCriteria.has(criterion)) newCriteria.add(criterion, deCriteria.get(criterion));
                else if (done && date != null) newCriteria.addProperty(criterion, date);
            }
            JsonObject converted = new JsonObject();
            converted.add("criteria", newCriteria);
            converted.addProperty("done", newCriteria.size() == criteria.size() && !criteria.isEmpty());
            out.add(target, converted);
            ctx.report.count("advancements converted");
        }
        return out;
    }

    /**
     * Renames keys through a DE table; a key with no Continuum home is parked. DE's own custom stats have none. Every
     * key is read before any is written, so a rename onto a key that is itself renamed moves each count once.
     */
    private static void rename(JsonObject group, Map<String, String> table, boolean custom, Ctx ctx, String where) {
        Map<String, Long> moved = new LinkedHashMap<>();
        for (String key : List.copyOf(group.keySet())) {
            if (!key.startsWith("hbm:")) continue;
            String rule = custom ? "park" : table.getOrDefault(key.substring(4), table.get(key));
            if (rule == null) continue;
            JsonElement value = group.remove(key);
            String target = rule.startsWith("hbm:") ? rule.split("\\[")[0] : null;
            if (target == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                ctx.parking.park("stat", where, key, StringTag.valueOf(value.toString()));
                continue;
            }
            moved.merge(target, value.getAsLong(), Long::sum);
        }
        for (Map.Entry<String, Long> e : moved.entrySet()) {
            JsonElement existing = group.get(e.getKey());
            group.addProperty(e.getKey(), e.getValue() + (existing == null ? 0 : existing.getAsLong()));
        }
    }

    private static List<Path> json(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return List.of();
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json")).toList();
        }
    }

    /** The file's object, or null for one vanilla could not read either: it stays as it is and its text is parked. */
    private static @Nullable JsonObject read(Ctx ctx, Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException e) {
            ctx.parking.park("player_file", ctx.world.relativize(file).toString(), "unreadable: " + e, StringTag.valueOf(text));
            ctx.report.line(ctx.world.relativize(file) + " cannot be read and was left as it is");
            return null;
        }
    }

    /** Commits the unit, then swaps the converted file in; a crash in between converts the original again. */
    private static void replace(Ctx ctx, Unit unit, Path file, JsonObject root) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".deimport");
        Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
        ctx.commit(unit);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }
}
