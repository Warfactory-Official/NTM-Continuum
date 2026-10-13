package com.hbm.lib;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.platform.Lifecycle;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Writes {@link ItemIds}, {@link VanillaIds}, {@link TagIds} and {@link ComponentIds} out of the
 * live registry.
 *
 * <p>Those four classes are how a recipe declaration names an ingredient. They are generated because a
 * hand-kept list quietly misses an item, and the registry is the only complete answer. The declarations
 * run in a plain JVM at build time where {@code ModItems} and half its siblings cannot be loaded, so the
 * roster is taken from a running game and written into {@code src/generated/java}, where it is
 * committed and compiled like any other source.
 *
 * <pre>{@code ./gradlew :26.1-neoforge:runGenerateIds}</pre>
 *
 * <p>{@code IdGameTests} checks that the committed files still say what {@link #roster()} says, so a
 * stale file is a failing test naming the item that moved, not a recipe that silently stopped existing.
 *
 * <p>The roster is narrower than the registry. Items and tags are taken from the three namespaces a
 * recipe in this mod can mean ({@code hbm}, {@code minecraft} and the {@code c} convention), so that
 * another mod on the dev runtime cannot widen what gets committed. A tag also has to have a member: an
 * empty ingredient tag takes a recipe out of the game exactly as a misspelled one does, so it gets no
 * constant.
 */
public final class IdConstants {

    /** The system property a generating run sets: where to write the sources. */
    public static final String OUTPUT = "hbm.generateIds";

    private static final String PACKAGE = "com.hbm.lib";
    private static final String CONVENTION = "c";

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null");

    private IdConstants() {}

    /** One generated class: its name, its javadoc, and the constants it declares. */
    public record Family(String className, String javadoc, Map<String, String> constants) {}

    /**
     * Arms the generator if this run was started to generate.
     *
     * <p>Called from common init on every loader, and does nothing at all unless the property is
     * set, so an ordinary game pays one property read for it.
     */
    public static void install() {
        String output = System.getProperty(OUTPUT);
        if (output == null || output.isBlank()) {
            return;
        }
        Lifecycle.onServerStarted(server -> {
            write(Path.of(output));
            Constants.LOG.info("[IDS] generated into {}; stopping", output);
            server.halt(false);
        });
    }

    /**
     * The four families as the live registry has them.
     *
     * <p>This is the one definition of what the constants are, used both to write them and to check
     * the written ones, so the two cannot disagree about anything but staleness.
     */
    public static List<Family> roster() {
        Map<String, String> items = new TreeMap<>();
        Map<String, String> vanilla = new TreeMap<>();
        Map<String, String> tags = new TreeMap<>();
        Map<String, String> components = new TreeMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (id.startsWith(Constants.MOD_ID + ":")) {
                put(items, name(path(id)), id);
            } else if (id.startsWith("minecraft:")) {
                String canonical = Ids.canonicalItemId(id);
                put(vanilla, name(path(canonical)), canonical);
            }
            BuiltInRegistries.ITEM.wrapAsHolder(item).tags().forEach(tag -> {
                String tagId = tag.location().toString();
                String namespace = tagId.substring(0, tagId.indexOf(':'));
                if (!CONVENTION.equals(namespace) && !"minecraft".equals(namespace)
                        && !Constants.MOD_ID.equals(namespace)) {
                    return;
                }
                put(tags, tagName(namespace, path(tagId)), "#" + tagId);
            });
        }

        for (String key : GolemComponent.declaredKeys()) {
            if (key.startsWith(Constants.MOD_ID + ":")) {
                put(components, name(path(key)), key);
            }
        }

        List<Family> roster = new ArrayList<>();
        roster.add(new Family("ItemIds", """
                Every item this mod registers, as a constant rather than a string.

                <p>A recipe declaration names its ingredients from here. It cannot name
                {@code ModItems.plate_steel} instead: the declarations are run in a plain JVM at build
                time, where a {@code RegistryObject} is reachable but half the registry classes are not
                — {@code ModSteelBlocks}, {@code ModArmorItems} and a dozen others touch
                {@code SoundType}, {@code SoundEvents} or {@code BuiltInRegistries} while their statics
                initialise, and throw. An id is the part of an item that survives that.

                <p>Constants are named after the id, so {@code ItemIds.plate_steel} reads the way
                {@code ModItems.plate_steel} does and a rename shows up as both of them moving.""",
                items));
        roster.add(new Family("VanillaIds", """
                Every vanilla item, as a constant.

                <p>Apart from {@link ItemIds} because the namespace is the whole difference between
                {@code minecraft:copper_ingot} and this mod's copper, and a string buries it.
                {@code Items.GUNPOWDER} is not the alternative: it needs a bootstrapped game, and the
                declarations run before there is one.""",
                vanilla));
        roster.add(new Family("TagIds", """
                Every item tag a recipe can ask for, carrying the leading {@code #} an ingredient
                position means by a tag.

                <p>A tag is the ingredient that survives another mod being installed —
                {@code #c:ingots/copper} takes anybody's copper — so most of this mod's own materials
                are spelled as one. Only tags that actually have a member are here; an empty tag reads
                as a recipe that cannot be crafted.

                <p>Convention tags drop their {@code c:}, which is the namespace almost all of them are
                in. Any other namespace keeps it, so a vanilla tag cannot be mistaken for a convention
                one of the same name.""",
                tags));
        roster.add(new Family("ComponentIds", """
                Every data component this mod declares, as a constant.

                <p>These are what a component recipe matches an ingredient on and what it hands to its
                result: which variant of a multi-item something is, what a container holds and how
                much, how hot it came out of a furnace. The components themselves are
                {@code GolemComponent}s, which a declaration cannot reach for the reason given in
                {@link ItemIds}.""",
                components));
        return roster;
    }

    /** The families keyed by class name, for a caller holding the committed classes against them. */
    public static Map<String, Map<String, String>> rosterByClass() {
        Map<String, Map<String, String>> families = new LinkedHashMap<>();
        for (Family family : roster()) {
            families.put(family.className(), family.constants());
        }
        return families;
    }

    /**
     * The tags this mod puts its own items into.
     *
     * <p>Which convention tags exist at all is the loader's business: NeoForge and Fabric API each
     * ship their own {@code c:} set, and they are not the same set, so "every tag the game has"
     * is not something the same constants can be true about on every node. A tag holding one of this
     * mod's items is this mod's own, and that is the set worth requiring a constant for.
     */
    public static Set<String> backedTags() {
        Set<String> backed = new TreeSet<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!BuiltInRegistries.ITEM.getKey(item).toString().startsWith(Constants.MOD_ID + ":")) {
                continue;
            }
            BuiltInRegistries.ITEM.wrapAsHolder(item).tags()
                    .forEach(tag -> backed.add("#" + tag.location().toString()));
        }
        return backed;
    }

    /** Writes every family into {@code <root>/com/hbm/lib/<Class>.java}. */
    public static void write(Path root) {
        Path directory = root.resolve(PACKAGE.replace('.', '/'));
        try {
            Files.createDirectories(directory);
            for (Family family : roster()) {
                Files.writeString(directory.resolve(family.className() + ".java"), source(family),
                        StandardCharsets.UTF_8);
                Constants.LOG.info("[IDS] {} — {} constants", family.className(),
                        family.constants().size());
            }
        } catch (IOException failure) {
            throw new UncheckedIOException("could not write the id constants to " + root, failure);
        }
    }

    /** One family as Java source. */
    public static String source(Family family) {
        StringBuilder text = new StringBuilder();
        text.append("package ").append(PACKAGE).append(";\n\n");
        text.append("/**\n");
        for (String line : family.javadoc().split("\n", -1)) {
            text.append(line.isBlank() ? " *" : " * " + line).append('\n');
        }
        text.append(" *\n");
        text.append(" * <p>Generated from the live registry by {@link IdConstants}; do not edit. Regenerate with\n");
        text.append(" * {@code ./gradlew :26.1-neoforge:runGenerateIds}, which is also what {@code IdGameTests}\n");
        text.append(" * will tell you to do when this file has gone stale.\n");
        text.append(" */\n");
        text.append("public final class ").append(family.className()).append(" {\n\n");
        text.append("    private ").append(family.className()).append("() {}\n\n");
        family.constants().forEach((name, id) ->
                text.append("    public static final String ").append(name)
                        .append(" = \"").append(id).append("\";\n"));
        text.append("}\n");
        return text.toString();
    }

    private static void put(Map<String, String> constants, String name, String id) {
        String existing = constants.put(name, id);
        if (existing != null && !existing.equals(id)) {
            throw new IllegalStateException(
                    "'" + name + "' would name both " + existing + " and " + id);
        }
    }

    private static String path(String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    private static String tagName(String namespace, String path) {
        return name(CONVENTION.equals(namespace) ? path : namespace + "_" + path);
    }

    /**
     * An id's path as the name of a constant.
     *
     * <p>Ids are lower-case and underscored already, so this mostly has to flatten a tag's slashes.
     * The character test is spelled out rather than asked of {@code Character}: the method that would
     * answer it is named after something the Stonecutter rename table renames, and the table is a
     * substring swap over the whole file, so on the other nodes it rewrites the call into one the JDK
     * does not have.
     */
    private static String name(String path) {
        StringBuilder name = new StringBuilder(path.length());
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
            name.append(plain(character) ? character : '_');
        }
        if (name.isEmpty() || name.charAt(0) < 'a' || name.charAt(0) > 'z'
                || KEYWORDS.contains(name.toString())) {
            name.insert(0, '_');
        }
        return name.toString();
    }

    private static boolean plain(char character) {
        return character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9'
                || character == '_';
    }
}
