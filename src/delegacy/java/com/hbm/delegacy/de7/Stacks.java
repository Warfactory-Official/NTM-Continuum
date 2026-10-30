package com.hbm.delegacy.de7;

import com.hbm.delegacy.DeAddons;
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
        CompoundTag lodestone = stack.getCompound("components").getCompound("minecraft:lodestone_tracker").getCompound("target");
        if (lodestone.contains("dimension", Tag.TAG_STRING)) lodestone.putString("dimension", DeAddons.rename(lodestone.getString("dimension")));
        if (!walker.source(id)) {
            // an addon's item its alias loaded before the converter saw it, so its components are still DE's
            if (!id.startsWith("hbm:") && DeAddons.isTarget(id)) components(id, stack, stack.getCompound("components"), where);
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
        String rule = Tables.get().items.get(DeAddons.key(id));
        if (rule == null && !id.startsWith("hbm:")) rule = DeAddons.rename(id);
        if (rule != null) {
            CompoundTag deComponents = rule.startsWith("split:") ? components.copy() : null;
            String target = resolve(rule, id, components);
            for (DeAddons.Addon addon : DeAddons.all())
                if (target == null && deComponents != null) target = addon.split(rule.split(":")[1], deComponents, components);
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
        components(id, stack, components, where);
        walkComponents(stack, where);
        return decodable(stack, where);
    }

    private void components(String deId, CompoundTag stack, CompoundTag components, String where) {
        if (components.isEmpty()) return;
        Components.convert(deId, stack.getString("id"), components, ctx, where);
        for (DeAddons.Addon addon : DeAddons.all()) addon.components(deId, components, ctx, where);
        Components.parkUnknown(deId, components, ctx, where);
        if (components.isEmpty()) stack.remove("components");
        else stack.put("components", components);
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
        if (DeAddons.isTarget(rule)) return rule.split("\\[")[0];
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
