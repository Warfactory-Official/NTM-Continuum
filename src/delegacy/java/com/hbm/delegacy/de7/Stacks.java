package com.hbm.delegacy.de7;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * One item stack: its id through the item table (renames, splits by component, parking), then its components. Nested
 * stacks inside components are walked afterwards.
 */
public final class Stacks {
    private final StackContext ctx;
    private final Walker walker;

    Stacks(StackContext ctx, Walker walker) {
        this.ctx = ctx;
        this.walker = walker;
    }

    /** False when the stack has no Continuum home: it is parked and must be removed. */
    public boolean convert(CompoundTag stack, String where) {
        String id = stack.getString("id");
        if (!id.startsWith("hbm:")) {
            walkComponents(stack, where);
            // another mod's compound that only looks like a stack, or an item of a mod not installed: left in place, a
            // copy parked in case it was an item vanilla will drop
            if (!ctx.isItem(id)) {
                ctx.park("unknown_item", where, id, stack);
                return true;
            }
            return decodable(stack, where);
        }
        CompoundTag components = stack.getCompound("components");
        if (components.getBoolean("hbm:akimbo_ghost")) return false;
        if (id.equals("hbm:marshmallow") && components.getBoolean("hbm:roasted")) stack.putString("id", "hbm:marshmallow_roasted");
        String rule = Tables.get().items.get(id.substring(4));
        if (rule != null) {
            String target = resolve(rule, id, components);
            if (target == null) {
                ctx.park("item", where, id, stack);
                ctx.count("stacks parked");
                return false;
            }
            if (!target.equals(id)) {
                stack.putString("id", target);
                ctx.count("stacks renamed");
            }
        }
        if (!components.isEmpty()) {
            Components.convert(id, stack.getString("id"), components, ctx, where);
            if (components.isEmpty()) stack.remove("components");
            else stack.put("components", components);
        }
        walkComponents(stack, where);
        return decodable(stack, where);
    }

    /** Parks what Continuum would reject on load: the stack when its item is unknown, else each failing component. */
    private boolean decodable(CompoundTag stack, String where) {
        List<String> bad = ctx.undecodable(stack);
        if (bad == null) {
            ctx.park("item", where, stack.getString("id"), stack);
            ctx.count("stacks parked");
            return false;
        }
        if (bad.isEmpty()) return true;
        CompoundTag components = stack.getCompound("components");
        for (String key : bad) {
            ctx.park("component", where, stack.getString("id") + " " + key, components.get(key));
            ctx.count("components parked");
            components.remove(key);
        }
        if (components.isEmpty()) stack.remove("components");
        return true;
    }

    private void walkComponents(CompoundTag stack, String where) {
        if (stack.contains("components", Tag.TAG_COMPOUND)) walker.walk(stack.getCompound("components"), where);
    }

    /** The Continuum id for a table rule, or null to park the stack. */
    private static String resolve(String rule, String id, CompoundTag components) {
        if (rule.equals("drop") || rule.equals("park")) return null;
        if (rule.equals("ignore")) return id;
        if (rule.startsWith("hbm:")) return rule.split("\\[")[0];
        String[] parts = rule.split(":");
        switch (parts[0]) {
            case "split" -> {
                return switch (parts[1]) {
                    case "cell" -> "hbm:cell";
                    case "container" -> Components.hasFluid(components)
                            ? parts[4] + ":" + parts[5] : parts[2] + ":" + parts[3];
                    case "bedrock_ore" -> Components.bedrockOreId(components);
                    case "orbital_assembly" -> Components.orbitalAssemblyId(components);
                    default -> null;
                };
            }
            case "state" -> {
                CompoundTag state = components.getCompound("minecraft:block_state");
                String value = state.getString(parts[1]);
                components.remove("minecraft:block_state");
                return parts[3] + ":" + parts[4] + (value.isEmpty() ? parts[2] : value);
            }
            default -> {
                return null;
            }
        }
    }
}
