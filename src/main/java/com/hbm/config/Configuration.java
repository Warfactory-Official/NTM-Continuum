package com.hbm.config;

import com.hbm.Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The part of Forge 1.7.10's {@code net.minecraftforge.common.config.Configuration} that 1.7 HBM used for
 * {@code hbm.cfg}, so the 1.7 {@code loadFromConfig} bodies port unchanged and a 1.7 user finds the same file, the same
 * categories ({@code 01_general} .. {@code 18_weapons}, {@code 528}, {@code LESS BULLSHIT MODE}), the same keys and the
 * same comments. The file format is Forge's: a {@code # Configuration file} header, categories in natural string
 * order, properties sorted by name inside each category, {@code B:}/{@code I:}/{@code D:}/{@code S:} type prefixes,
 * {@code <}..{@code >} lists, names containing anything other than letters, digits and {@code ._-} in quotes, and a
 * boxed category comment. Unknown properties already in the file are kept, as Forge kept them.
 *
 * <p>Plain Java on every node: the file lives in the loader's config directory ({@code ConfigFiles.configDir()}), as
 * 1.7's {@code event.getSuggestedConfigurationFile()} did.
 */
public final class Configuration {

    private static final String COMMENT_SEPARATOR =
            "##########################################################################################################";
    private static final String COMMENT_RULE =
            "#--------------------------------------------------------------------------------------------------------#";
    private static final String NL = System.lineSeparator();

    private final Path file;
    private final Map<String, Category> categories = new TreeMap<>();

    public Configuration(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    // properties

    public enum Type {
        STRING('S'), INTEGER('I'), BOOLEAN('B'), DOUBLE('D');

        final char id;

        Type(char id) {
            this.id = id;
        }

        static Type of(char c) {
            for (Type t : values()) if (t.id == c) return t;
            return null;
        }
    }

    /** Forge's {@code Property}: a string value (or list) with a type, a default and a comment. */
    public static final class Property {
        private final String name;
        Type type;
        private String value;
        private String[] values;
        private String defaultValue;
        private String[] defaultValues;
        final boolean isList;
        /** As in Forge, the comment is a public field the 1.7 code assigns directly. */
        public String comment;

        Property(String name, String value, Type type) {
            this.name = name;
            this.value = value;
            this.type = type;
            this.isList = false;
        }

        Property(String name, String[] values, Type type) {
            this.name = name;
            this.values = values;
            this.type = type;
            this.isList = true;
        }

        public String getName() {
            return name;
        }

        public boolean isList() {
            return isList;
        }

        public String getString() {
            return value;
        }

        public String[] getStringList() {
            return values;
        }

        public int getInt() {
            return getInt(parseIntOr(defaultValue, 0));
        }

        public int getInt(int def) {
            return parseIntOr(value, def);
        }

        public boolean getBoolean() {
            return getBoolean(Boolean.parseBoolean(defaultValue));
        }

        public boolean getBoolean(boolean def) {
            return isBooleanValue(value) ? Boolean.parseBoolean(value) : def;
        }

        public double getDouble() {
            return getDouble(parseDoubleOr(defaultValue, 0D));
        }

        public double getDouble(double def) {
            return parseDoubleOr(value, def);
        }

        /** Forge returns the default list when any entry is not an integer; the value was reset to it on {@code get}. */
        public int[] getIntList() {
            int[] out = new int[values.length];
            for (int i = 0; i < values.length; i++) out[i] = Integer.parseInt(values[i].trim());
            return out;
        }

        public void set(String value) {
            this.value = value;
        }

        public void set(String[] values) {
            this.values = values;
        }

        public String getDefault() {
            return defaultValue;
        }

        public String[] getDefaults() {
            return defaultValues;
        }
    }

    private static final class Category {
        final String name;
        String comment;
        final Map<String, Property> properties = new TreeMap<>();

        Category(String name) {
            this.name = name;
        }
    }

    private Category category(String name) {
        return categories.computeIfAbsent(name, Category::new);
    }

    public void addCustomCategoryComment(String category, String comment) {
        category(category).comment = comment;
    }

    /** Every category name, in file order (for checks and tests). */
    public List<String> categoryNames() {
        return new ArrayList<>(categories.keySet());
    }

    /** The property, or null. */
    public Property peek(String category, String key) {
        Category cat = categories.get(category);
        return cat == null ? null : cat.properties.get(key);
    }

    /** Every property of a category, sorted by name (for checks and tests). */
    public Map<String, Property> properties(String category) {
        Category cat = categories.get(category);
        return cat == null ? Map.of() : Collections.unmodifiableMap(cat.properties);
    }

    private Property scalar(String category, String key, String def, String comment, Type type) {
        Category cat = category(category);
        Property prop = cat.properties.get(key);
        if (prop == null || prop.isList) {
            prop = new Property(key, def, type);
            cat.properties.put(key, prop);
        } else if (prop.type != type) {
            prop = new Property(key, prop.value, type);
            cat.properties.put(key, prop);
        }
        prop.defaultValue = def;
        if (comment != null) prop.comment = comment;
        return prop;
    }

    private Property list(String category, String key, String[] def, String comment, Type type) {
        Category cat = category(category);
        Property prop = cat.properties.get(key);
        if (prop == null || !prop.isList) {
            prop = new Property(key, def.clone(), type);
            cat.properties.put(key, prop);
        } else if (prop.type != type) {
            prop = new Property(key, prop.values, type);
            cat.properties.put(key, prop);
        }
        prop.defaultValues = def.clone();
        if (comment != null) prop.comment = comment;
        return prop;
    }

    public Property get(String category, String key, int def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, int def, String comment) {
        Property prop = scalar(category, key, Integer.toString(def), comment, Type.INTEGER);
        if (!isIntValue(prop.value)) prop.value = Integer.toString(def);
        return prop;
    }

    public Property get(String category, String key, boolean def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, boolean def, String comment) {
        Property prop = scalar(category, key, Boolean.toString(def), comment, Type.BOOLEAN);
        if (!isBooleanValue(prop.value)) prop.value = Boolean.toString(def);
        return prop;
    }

    public Property get(String category, String key, double def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, double def, String comment) {
        Property prop = scalar(category, key, Double.toString(def), comment, Type.DOUBLE);
        if (!isDoubleValue(prop.value)) prop.value = Double.toString(def);
        return prop;
    }

    public Property get(String category, String key, String def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, String def, String comment) {
        return scalar(category, key, def, comment, Type.STRING);
    }

    public Property get(String category, String key, int[] def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, int[] def, String comment) {
        String[] strings = Arrays.stream(def).mapToObj(Integer::toString).toArray(String[]::new);
        Property prop = list(category, key, strings, comment, Type.INTEGER);
        for (String v : prop.values) {
            if (!isIntValue(v)) {
                prop.values = strings.clone();
                break;
            }
        }
        return prop;
    }

    public Property get(String category, String key, String[] def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, String[] def, String comment) {
        return list(category, key, def, comment, Type.STRING);
    }

    // value checks

    static boolean isIntValue(String s) {
        if (s == null) return false;
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static boolean isBooleanValue(String s) {
        return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
    }

    static boolean isDoubleValue(String s) {
        if (s == null) return false;
        try {
            Double.parseDouble(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int parseIntOr(String s, int def) {
        return isIntValue(s) ? Integer.parseInt(s.trim()) : def;
    }

    private static double parseDoubleOr(String s, double def) {
        return isDoubleValue(s) ? Double.parseDouble(s.trim()) : def;
    }

    // file

    /** Forge's {@code allowedProperties}: Java letters/digits and {@code ._-}; anything else is quoted. */
    private static String quoted(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '.' && c != '_' && c != '-') return '"' + name + '"';
        }
        return name;
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        return s;
    }

    /** Reads the file if it exists. A malformed line is skipped with a warning, as Forge recovered. */
    public void load() {
        if (!Files.exists(file)) return;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Deque<String> stack = new ArrayDeque<>();
            Property openList = null;
            List<String> listValues = null;
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                String trimmed = line.trim();
                if (openList != null) {
                    if (trimmed.equals(">")) {
                        openList.values = listValues.toArray(new String[0]);
                        openList = null;
                        listValues = null;
                    } else if (!trimmed.isEmpty()) {
                        listValues.add(trimmed);
                    }
                    continue;
                }
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                if (trimmed.endsWith("{")) {
                    String name = unquote(trimmed.substring(0, trimmed.length() - 1));
                    stack.push(stack.isEmpty() ? name : stack.peek() + "." + name);
                    category(stack.peek());
                    continue;
                }
                if (trimmed.equals("}")) {
                    if (!stack.isEmpty()) stack.pop();
                    continue;
                }
                if (stack.isEmpty()) {
                    Constants.LOG.warn("{}:{}: property outside a category, ignored", file, number);
                    continue;
                }
                Type type = null;
                String rest = trimmed;
                if (rest.length() > 2 && rest.charAt(1) == ':' && Type.of(rest.charAt(0)) != null) {
                    type = Type.of(rest.charAt(0));
                    rest = rest.substring(2);
                }
                Category cat = category(stack.peek());
                if (rest.endsWith("<")) {
                    String name = unquote(rest.substring(0, rest.length() - 1));
                    openList = new Property(name, new String[0], type == null ? Type.STRING : type);
                    listValues = new ArrayList<>();
                    cat.properties.put(name, openList);
                    continue;
                }
                int eq = indexOfAssignment(rest);
                if (eq < 0) {
                    Constants.LOG.warn("{}:{}: unreadable line '{}', ignored", file, number, trimmed);
                    continue;
                }
                String name = unquote(rest.substring(0, eq));
                cat.properties.put(name, new Property(name, rest.substring(eq + 1), type == null ? Type.STRING : type));
            }
        } catch (IOException e) {
            Constants.LOG.warn("Unable to read {}", file, e);
        }
    }

    /** The first {@code =} outside a quoted name. */
    private static int indexOfAssignment(String s) {
        boolean quote = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') quote = !quote;
            else if (c == '=' && !quote) return i;
        }
        return -1;
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                out.write("# Configuration file" + NL + NL);
                for (Category cat : categories.values()) {
                    write(out, cat);
                    out.write(NL);
                }
            }
        } catch (IOException e) {
            Constants.LOG.warn("Unable to write {}", file, e);
        }
    }

    private static void write(BufferedWriter out, Category cat) throws IOException {
        String pad1 = "    ";
        String pad2 = "        ";
        if (cat.comment != null && !cat.comment.isEmpty()) {
            out.write(COMMENT_SEPARATOR + NL);
            out.write("# " + cat.name + NL);
            out.write(COMMENT_RULE + NL);
            for (String line : cat.comment.split("\r?\n", -1)) out.write("# " + line + NL);
            out.write(COMMENT_SEPARATOR + NL + NL);
        }
        out.write(quoted(cat.name) + " {" + NL);
        boolean first = true;
        for (Property prop : cat.properties.values()) {
            if (prop.comment != null && !prop.comment.isEmpty()) {
                if (!first) out.write(NL);
                for (String line : prop.comment.split("\r?\n", -1)) out.write(pad1 + "# " + line + NL);
            }
            first = false;
            String name = quoted(prop.name);
            if (prop.isList) {
                out.write(pad1 + prop.type.id + ":" + name + " <" + NL);
                for (String v : prop.values) out.write(pad2 + v + NL);
                out.write(pad1 + " >" + NL);
            } else {
                out.write(pad1 + prop.type.id + ":" + name + "=" + prop.value + NL);
            }
        }
        out.write("}" + NL + NL);
    }
}
