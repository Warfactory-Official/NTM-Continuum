package com.hbm.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.Constants;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 1.7 {@code RunningConfig}: the in-game-editable JSON configs behind {@code /ntmserver} and {@code /ntmclient}.
 * Files live in {@code config/hbmConfig/} as in 1.7; the parser is 1.7's "dynamic type parser", which reads each
 * value as the type of its default.
 */
public class RunningConfig {

    public static final Gson gson = new Gson();

    public static Path getConfig(String name) {
        return dev.golem.core.platform.ConfigFiles.configDir().resolve("hbmConfig").resolve(name);
    }

    public static void readConfig(Path config, Map<String, ConfigWrapper<?>> configMap) {
        try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) return;
            for (Map.Entry<String, ConfigWrapper<?>> line : configMap.entrySet()) {
                if (!json.has(line.getKey())) continue;
                try {
                    line.getValue().read(json.get(line.getKey()));
                } catch (Exception ex) {
                    Constants.LOG.warn("Unreadable value for {} in {}", line.getKey(), config, ex);
                }
            }
        } catch (Exception ex) {
            Constants.LOG.warn("Unable to read {}", config, ex);
        }
    }

    public static void writeConfig(Path config, Map<String, ConfigWrapper<?>> configMap, String info) {
        try {
            Files.createDirectories(config.getParent());
            try (Writer file = Files.newBufferedWriter(config, StandardCharsets.UTF_8);
                 JsonWriter writer = new JsonWriter(file)) {
                writer.setIndent("  ");
                writer.beginObject();
                writer.name("info").value(info);
                List<String> keys = new ArrayList<>(configMap.keySet());
                Collections.sort(keys); // 1.7: readability is cool
                for (String key : keys) {
                    Object value = configMap.get(key).get();
                    if (value instanceof String s) writer.name(key).value(s);
                    if (value instanceof Float f) writer.name(key).value(f);
                    if (value instanceof Double d) writer.name(key).value(d);
                    if (value instanceof Integer i) writer.name(key).value(i);
                    if (value instanceof Boolean b) writer.name(key).value(b);
                }
                writer.endObject();
            }
        } catch (Exception e) {
            Constants.LOG.warn("Unable to write {}", config, e);
        }
    }

    /**
     * 1.7 {@code ConfigWrapper<T>}. A wrapper either owns its value or is bound to a static field an older part of
     * the port already reads, so {@code /ntmserver set} reaches the code that uses it; both behave the same.
     */
    public static class ConfigWrapper<T> {
        private T value;
        private final Supplier<T> getter;
        private final Consumer<T> setter;

        public ConfigWrapper(T value) {
            this.value = value;
            this.getter = null;
            this.setter = null;
        }

        private ConfigWrapper(Supplier<T> getter, Consumer<T> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        /** A wrapper over an existing static field. */
        public static <T> ConfigWrapper<T> bound(Supplier<T> getter, Consumer<T> setter) {
            return new ConfigWrapper<>(getter, setter);
        }

        public T get() {
            return getter != null ? getter.get() : value;
        }

        public void set(T value) {
            if (setter != null) setter.accept(value);
            else this.value = value;
        }

        /** 1.7 {@code update(String)}: parses as the current value's type; throws on a malformed number. */
        @SuppressWarnings("unchecked")
        public void update(String param) {
            Object current = get();
            Object parsed = null;
            if (current instanceof String) parsed = param;
            if (current instanceof Float) parsed = Float.parseFloat(param);
            if (current instanceof Double) parsed = Double.parseDouble(param);
            if (current instanceof Integer) parsed = Integer.parseInt(param);
            if (current instanceof Boolean) parsed = Boolean.parseBoolean(param);
            if (parsed != null) set((T) parsed);
        }

        @SuppressWarnings("unchecked")
        void read(JsonElement json) {
            Object current = get();
            if (current instanceof String) set((T) json.getAsString());
            if (current instanceof Float) set((T) Float.valueOf(json.getAsFloat()));
            if (current instanceof Double) set((T) Double.valueOf(json.getAsDouble()));
            if (current instanceof Integer) set((T) Integer.valueOf(json.getAsInt()));
            if (current instanceof Boolean) set((T) Boolean.valueOf(json.getAsBoolean()));
        }

        @Override
        public String toString() {
            return String.valueOf(get());
        }
    }
}
