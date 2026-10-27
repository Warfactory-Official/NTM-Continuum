package com.hbm.deimport.gate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.delegacy.AtomicNbt;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import org.jetbrains.annotations.Nullable;

/**
 * {@code <world>/hbm_de_import.json}: the importer's only record in a world. A step marked done never runs again;
 * {@code declined} and {@code done} stop every later start at the first file read.
 */
public final class ImportState {
    public static final String FILE = "hbm_de_import.json";
    public static final String PREV = FILE + ".prev";
    public static final int FORMAT = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final JsonObject root;

    private ImportState(Path file, JsonObject root) {
        this.file = file;
        this.root = root;
    }

    public static boolean exists(Path world) {
        return Files.isRegularFile(world.resolve(FILE));
    }

    /**
     * The world's state, or null when it has none. A damaged file is set aside and the copy saved before it read
     * instead; with neither, the gate decides again, and every step finds what it already converted.
     */
    public static @Nullable ImportState read(Path world) throws IOException {
        Path file = world.resolve(FILE);
        if (!Files.isRegularFile(file)) return null;
        JsonObject root = parse(file);
        if (root == null) {
            Path prev = world.resolve(PREV);
            root = Files.isRegularFile(prev) ? parse(prev) : null;
            if (root == null) return null;
        }
        return new ImportState(file, root);
    }

    private static @Nullable JsonObject parse(Path file) throws IOException {
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            AtomicNbt.setAside(file, e);
            return null;
        }
    }

    public static ImportState create(Path world, String sourceVersion, int dataVersion, String importer) {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        JsonObject source = new JsonObject();
        source.addProperty("version", sourceVersion);
        source.addProperty("dataVersion", dataVersion);
        root.add("source", source);
        root.addProperty("importer", importer);
        root.addProperty("status", "running");
        root.add("steps", new JsonObject());
        JsonObject legacy = new JsonObject();
        legacy.addProperty("conversions", 0);
        root.add("legacy", legacy);
        return new ImportState(world.resolve(FILE), root);
    }

    public String status() {
        return root.has("status") ? root.get("status").getAsString() : "running";
    }

    public void status(String status) {
        root.addProperty("status", status);
    }

    public boolean finished() {
        return "done".equals(status()) || "declined".equals(status());
    }

    public boolean stepDone(String id) {
        JsonObject steps = root.getAsJsonObject("steps");
        return steps.has(id) && steps.get(id).isJsonPrimitive() && "done".equals(steps.get(id).getAsString());
    }

    /** True once the step has run, waited or deferred in this world; a step added by a later importer is not. */
    public boolean knows(String id) {
        return root.getAsJsonObject("steps").has(id);
    }

    /** Another version of this step ({@code name@version}) is done in this world. */
    public boolean ranEarlier(String id) {
        String name = id.substring(0, id.indexOf('@') + 1);
        for (String key : root.getAsJsonObject("steps").keySet())
            if (!key.equals(id) && key.startsWith(name) && stepDone(key)) return true;
        return false;
    }

    public @Nullable String deferredOn(String id) {
        JsonObject steps = root.getAsJsonObject("steps");
        if (!steps.has(id) || !steps.get(id).isJsonObject()) return null;
        JsonObject step = steps.getAsJsonObject(id);
        return step.has("deferred") ? step.get("deferred").getAsString() : null;
    }

    public void markDone(String id) {
        root.getAsJsonObject("steps").addProperty(id, "done");
    }

    public void markDeferred(String id, String modId) {
        JsonObject step = new JsonObject();
        step.addProperty("deferred", modId);
        root.getAsJsonObject("steps").add(id, step);
    }

    public void put(String key, String value) {
        root.addProperty(key, value);
    }

    public void addLegacyConversions(int count) {
        JsonObject legacy = root.getAsJsonObject("legacy");
        legacy.addProperty("conversions", legacy.get("conversions").getAsLong() + count);
        legacy.addProperty("last", Instant.now().toString());
    }

    public long legacyConversions() {
        return root.getAsJsonObject("legacy").get("conversions").getAsLong();
    }

    public String sourceVersion() {
        return root.getAsJsonObject("source").get("version").getAsString();
    }

    /** Written whole and flushed to disk; the copy it replaces is kept as {@link #PREV}. */
    public void save() throws IOException {
        if (Files.isRegularFile(file)) Files.copy(file, file.resolveSibling(PREV), StandardCopyOption.REPLACE_EXISTING);
        AtomicNbt.write(GSON.toJson(root).getBytes(StandardCharsets.UTF_8), file);
    }
}
