package com.hbm.deimport.source.de7;

import com.hbm.deimport.engine.Target;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** One palette entry: id, property names and property values from the tables in one step, so no rename chains. */
public final class BlockStates {
    public enum Kind { PLAIN, CELL, REFUND }

    /** {@code dropped}: properties the Continuum block does not have or values it rejects, as DE saved them. */
    public record Converted(CompoundTag entry, Kind kind, String refundItem, CompoundTag dropped) {
        Converted(CompoundTag entry, Kind kind, String refundItem) {
            this(entry, kind, refundItem, new CompoundTag());
        }
    }

    private static final String[] DIRECTIONS = {"down", "up", "north", "south", "west", "east"};

    private BlockStates() {}

    public static Converted convert(CompoundTag entry, Target target) {
        String name = entry.getString("Name");
        if (!name.startsWith("hbm:")) return new Converted(entry, Kind.PLAIN, "");
        String path = name.substring(4);
        CompoundTag props = entry.getCompound("Properties").copy();
        String rule = WorldTables.get().blocks.get(path);
        String outName = name;
        if (rule != null) {
            String[] parts = rule.split(":");
            switch (parts[0]) {
                case "cell", "air" -> {
                    return new Converted(air(), rule.equals("cell") ? Kind.CELL : Kind.PLAIN, "");
                }
                case "ignore" -> {
                    // kept as it is, but still refunded below if Continuum has no such block
                }
                case "refund" -> {
                    return new Converted(air(), Kind.REFUND, parts.length > 2 ? parts[1] + ":" + parts[2] : "");
                }
                case "lit" -> outName = "true".equals(props.getString("lit"))
                        ? parts[1] + ":" + parts[2] : parts[3] + ":" + parts[4];
                case "part" -> {
                    String part = props.getString("part").isEmpty() ? "casing" : props.getString("part");
                    for (int i = 3; i < parts.length; i++) {
                        String[] pair = parts[i].split("=");
                        if (pair[0].equals(part)) part = pair[1];
                    }
                    outName = parts[1] + ":" + parts[2] + part;
                    props = new CompoundTag();
                }
                case "metafacing" -> {
                    int side = List.of("north", "south", "west", "east").indexOf(props.getString("facing"));
                    outName = parts[2] + ":" + parts[3];
                    props = new CompoundTag();
                    props.putString("meta", Integer.toString(Integer.parseInt(parts[1]) + Math.max(side, 0)));
                }
                case "suffix" -> {
                    String value = props.getString(parts[1]);
                    outName = parts[3] + ":" + parts[4] + (value.isEmpty() || value.equals(parts[2]) ? "" : "_" + value);
                    props.remove(parts[1]);
                }
                case "level", "variant" -> {
                    String value = props.getString(parts[0]);
                    outName = parts[1] + ":" + parts[2] + (value.isEmpty() ? (parts[0].equals("level") ? "0" : "base") : value);
                }
                default -> {
                    int bracket = rule.indexOf('[');
                    outName = bracket < 0 ? rule : rule.substring(0, bracket);
                    if (bracket >= 0) {
                        for (String kv : rule.substring(bracket + 1, rule.length() - 1).split(",")) {
                            String[] pair = kv.split("=");
                            props.putString(pair[0], pair[1]);
                        }
                    }
                }
            }
        }
        String propRule = WorldTables.get().props.get(path);
        if (propRule != null) for (String one : propRule.split(";")) renameProperty(props, one);
        if (!target.hasBlock(outName)) return new Converted(air(), Kind.REFUND, "");
        // DE builds tall machines (RBMK columns, the cargo elevator) from one block id with a part; only the core stays
        String part = props.getString("part");
        if (!part.isEmpty() && !part.equals("core") && !part.equals("assembling") && target.isMultiblockCore(outName))
            return new Converted(air(), Kind.CELL, "");
        Map<String, List<String>> valid = target.properties(outName);
        facing(props, valid);
        // a lit, level or variant rule spent its property on the block id
        if (rule != null && rule.matches("(lit|level|variant):.*") && !valid.containsKey(rule.substring(0, rule.indexOf(':'))))
            props.remove(rule.substring(0, rule.indexOf(':')));
        CompoundTag dropped = new CompoundTag();
        for (String key : List.copyOf(props.getAllKeys())) {
            List<String> values = valid.get(key);
            if (values == null || !values.contains(props.getString(key))) {
                dropped.put(key, props.get(key));
                props.remove(key);
            }
        }
        CompoundTag out = new CompoundTag();
        out.putString("Name", outName);
        if (!props.isEmpty()) out.put("Properties", props);
        return new Converted(out, Kind.PLAIN, "", dropped);
    }

    /**
     * A facing Continuum spells the other way: 1.7's metadata number (0 down to 5 east) for a direction name or the
     * name for the number; DE's floodlight adds 6 for its flip.
     */
    private static void facing(CompoundTag props, Map<String, List<String>> valid) {
        String value = props.getString("facing");
        List<String> values = valid.get("facing");
        if (value.isEmpty() || values == null || values.contains(value)) return;
        if (value.matches("\\d+")) {
            int meta = Integer.parseInt(value);
            String name = DIRECTIONS[meta % 6];
            if (values.contains(name)) {
                props.putString("facing", name);
                if (meta >= 6 && valid.containsKey("flip")) props.putString("flip", "true");
            }
        } else {
            for (int i = 0; i < DIRECTIONS.length; i++) {
                if (DIRECTIONS[i].equals(value) && values.contains(Integer.toString(i))) props.putString("facing", Integer.toString(i));
            }
        }
    }

    /** {@code old>new} or {@code old>new:a=b,c=d}; several are joined by {@code ;}. */
    private static void renameProperty(CompoundTag props, String rule) {
        String[] head = rule.split(":", 2);
        String[] names = head[0].split(">");
        if (!props.contains(names[0], Tag.TAG_STRING)) return;
        String value = props.getString(names[0]);
        props.remove(names[0]);
        if (head.length > 1) {
            for (String kv : head[1].split(",")) {
                String[] pair = kv.split("=");
                if (pair[0].equals(value)) {
                    value = pair[1];
                    break;
                }
            }
        }
        props.putString(names[1], value);
    }

    public static CompoundTag air() {
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        return air;
    }
}
