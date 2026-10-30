package com.hbm.delegacy;

import com.hbm.delegacy.de7.StackContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * DE addons whose saves a Continuum addon takes over. Each names DE's namespace and its own; the converter applies the
 * addon's table rows (keyed by whole DE ids) and hooks to that namespace as it does DE's to {@code hbm}, and an id with
 * no row keeps its path in the new namespace.
 */
public final class DeAddons {
    /** DE addons the importer knows by mod id, with the Continuum addon that converts each. */
    public static final Map<String, Known> KNOWN = Map.of("hbmspace", new Known("ntmspace", "NTM: Space"));

    public record Known(String modId, String name) {}
    private static final List<Addon> ADDONS = new CopyOnWriteArrayList<>();

    /** The hooks see every DE record, so each checks its own ids and keys. */
    public interface Addon {
        String source();

        String target();

        /** {@code blocks}, {@code props}, {@code items}, {@code entities}, {@code fluids}, ...; null for none. */
        @Nullable InputStream table(String name);

        /** After DE's own mapper, under Continuum's type id, before the tanks are flattened. */
        default void blockEntity(String deType, CompoundTag be, StackContext ctx, String where) {}

        /** An entity of this addon, renamed, before its contents are walked. */
        default void entity(String deId, CompoundTag tag, StackContext ctx, String where) {}

        /** Any living entity or player, before DE's living props convert. */
        default void living(CompoundTag tag, StackContext ctx, String where) {}

        /** Moves the addon's components out of DE's satellite state into Continuum's {@code extra}. */
        default void satellite(CompoundTag sat, CompoundTag components, CompoundTag extra, StackContext ctx, String where) {}

        /** A split rule DE's own kinds did not resolve ({@code bedrock_ore}), from the stack's DE components; null parks it. */
        default @Nullable String split(String kind, CompoundTag deComponents, CompoundTag components) {
            return null;
        }

        default void components(String deItem, CompoundTag components, StackContext ctx, String where) {}
    }

    private DeAddons() {}

    /** Called while mods are constructed ({@code FMLConstructModEvent}): the registry aliases are read right after. */
    public static void register(Addon addon) {
        ADDONS.add(addon);
    }

    public static List<Addon> all() {
        return ADDONS;
    }

    /** A converter is registered for DE's namespace {@code source}. */
    public static boolean has(String source) {
        return bySource(source + ":") != null;
    }

    public static @Nullable Addon bySource(String id) {
        String ns = namespace(id);
        for (Addon addon : ADDONS) if (addon.source().equals(ns)) return addon;
        return null;
    }

    /** An id in DE's namespace or a registered addon's DE namespace. */
    public static boolean isSource(String id) {
        return id.startsWith("hbm:") || bySource(id) != null;
    }

    /** An id in Continuum's namespace or a registered addon's. */
    public static boolean isTarget(String id) {
        if (id.startsWith("hbm:")) return true;
        String ns = namespace(id);
        for (Addon addon : ADDONS) if (addon.target().equals(ns)) return true;
        return false;
    }

    /** How the tables key a DE id: DE's own by path, an addon's whole. */
    public static String key(String id) {
        return id.startsWith("hbm:") ? id.substring(4) : id;
    }

    /** An addon id in its Continuum namespace, path kept; any other id unchanged. */
    public static String rename(String id) {
        Addon addon = bySource(id);
        return addon == null ? id : addon.target() + id.substring(id.indexOf(':'));
    }

    /** Every registered addon's rows of {@code table}, keyed by whole DE id. */
    public static Map<String, String> rows(String table) {
        Map<String, String> out = new HashMap<>();
        for (Addon addon : ADDONS) {
            try (InputStream in = addon.table(table)) {
                if (in != null) read(in, out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    /** A {@code key<TAB>value} table, {@code #} lines and blank lines skipped. */
    public static void read(InputStream in, Map<String, String> out) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            int tab = line.indexOf('\t');
            out.put(line.substring(0, tab), line.substring(tab + 1));
        }
    }

    private static String namespace(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? "minecraft" : id.substring(0, colon);
    }
}
