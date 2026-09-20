package com.hbm.modules;

import dev.golem.core.data.Tags;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The three-state item filter used by the old radio counter.
 *
 * <p>The old implementation read Forge ore-dictionary names. The shared port has no ore dictionary;
 * the equivalent cross-mod contract is the {@code c:} item-tag namespace. Tag paths are exposed as
 * legacy-shaped names (for example {@code c:ingots/iron} becomes {@code ingotIron}) so the mode order,
 * prefix selection and persisted mode strings stay faithful to the original matcher.
 */
public final class ModulePatternMatcher {
    public static final String MODE_EXACT = "exact";
    public static final String MODE_WILDCARD = "wildcard";

    public final String[] modes;

    public ModulePatternMatcher() {
        this(1);
    }

    public ModulePatternMatcher(int count) {
        this.modes = new String[count];
    }

    public void initPatternSmart(Level level, ItemStack stack, int index) {
        if (level.isClientSide()) return;
        if (stack == null || stack.isEmpty()) {
            modes[index] = null;
            return;
        }

        List<String> names = tagNames(stack);
        if (iterateAndCheck(names, index, "ingot")) return;
        if (iterateAndCheck(names, index, "block")) return;
        if (iterateAndCheck(names, index, "dust")) return;
        if (iterateAndCheck(names, index, "nugget")) return;
        if (iterateAndCheck(names, index, "plate")) return;

        // Modern variant items have their old metadata identity in the item/components themselves.
        modes[index] = MODE_WILDCARD;
    }

    public void initPatternStandard(Level level, ItemStack stack, int index) {
        if (level.isClientSide()) return;
        if (stack == null || stack.isEmpty()) {
            modes[index] = null;
            return;
        }
        modes[index] = MODE_WILDCARD;
    }

    public void nextMode(Level level, ItemStack pattern, int index) {
        if (level.isClientSide()) return;
        if (pattern == null || pattern.isEmpty()) {
            modes[index] = null;
            return;
        }

        if (modes[index] == null) {
            modes[index] = MODE_EXACT;
        } else if (MODE_EXACT.equals(modes[index])) {
            modes[index] = MODE_WILDCARD;
        } else if (MODE_WILDCARD.equals(modes[index])) {
            List<String> names = tagNames(pattern);
            modes[index] = names.isEmpty() ? MODE_EXACT : names.get(0);
        } else {
            List<String> names = tagNames(pattern);
            if (names.size() < 2 || modes[index].equals(names.get(names.size() - 1))) {
                modes[index] = MODE_EXACT;
                return;
            }
            for (int i = 0; i < names.size() - 1; i++) {
                if (modes[index].equals(names.get(i))) {
                    modes[index] = names.get(i + 1);
                    return;
                }
            }
        }
    }

    /**
     * ntm-next static form: one (mode, filter, input) test without a matcher instance - what the request
     * network stores per demand. Modes are this port's: exact, wildcard (damage-blind) or a tag id.
     */
    public static boolean matches(String mode, ItemStack filter, ItemStack input) {
        if (filter == null || filter.isEmpty() || input == null || input.isEmpty()) return false;
        if (mode == null) mode = MODE_EXACT;
        ItemStack oneFilter = filter.copyWithCount(1);
        ItemStack oneInput = input.copyWithCount(1);
        if (MODE_EXACT.equals(mode)) return dev.golem.core.world.ItemStacks.sameItemAndData(oneInput, oneFilter);
        if (MODE_WILDCARD.equals(mode)) {
            return dev.golem.core.world.ItemStacks.sameItemAndDataIgnoringDamage(oneInput, oneFilter);
        }
        return tagNames(oneInput).contains(mode);
    }

    public boolean isValidForFilter(ItemStack filter, int index, ItemStack input) {
        if (filter == null || filter.isEmpty() || input == null || input.isEmpty()) return false;

        String mode = modes[index];
        if (mode == null) modes[index] = mode = MODE_EXACT;

        ItemStack oneFilter = filter.copyWithCount(1);
        ItemStack oneInput = input.copyWithCount(1);
        if (MODE_EXACT.equals(mode)) return dev.golem.core.world.ItemStacks.sameItemAndData(oneInput, oneFilter);
        if (MODE_WILDCARD.equals(mode)) {
            return dev.golem.core.world.ItemStacks.sameItemAndDataIgnoringDamage(oneInput, oneFilter);
        }
        return tagNames(oneInput).contains(mode);
    }

    public void save(DataOut output) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] != null) output.putString("mode" + i, modes[i]);
        }
    }

    public void load(DataIn input) {
        for (int i = 0; i < modes.length; i++) modes[i] = input.getStringOr("mode" + i, null);
    }

    public void serialize(RegistryFriendlyByteBuf buffer) {
        for (String mode : modes) buffer.writeUtf(mode == null ? "" : mode, 64);
    }

    public void deserialize(RegistryFriendlyByteBuf buffer) {
        for (int i = 0; i < modes.length; i++) {
            String mode = buffer.readUtf(64);
            modes[i] = mode.isEmpty() ? null : mode;
        }
    }

    public static String getLabel(String mode) {
        if (MODE_EXACT.equals(mode)) return "Item and meta match";
        if (MODE_WILDCARD.equals(mode)) return "Item matches";
        return "Ore dict key matches: " + mode;
    }

    private boolean iterateAndCheck(List<String> names, int index, String prefix) {
        for (String name : names) {
            if (name.startsWith(prefix)) {
                modes[index] = name;
                return true;
            }
        }
        return false;
    }

    private static List<String> tagNames(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (var tag : stack.getItem().builtInRegistryHolder().tags().toList()) {
            Identifier id = tag.location();
            if (!"c".equals(id.getNamespace())) continue;
            String[] parts = id.getPath().split("/", 2);
            if (parts.length != 2) continue;
            names.add(singular(parts[0]) + titleCase(parts[1]));
        }
        return names;
    }

    private static String singular(String plural) {
        return switch (plural) {
            case "ingots" -> "ingot";
            case "blocks" -> "block";
            case "dusts" -> "dust";
            case "nuggets" -> "nugget";
            case "plates" -> "plate";
            default -> plural.endsWith("s") ? plural.substring(0, plural.length() - 1) : plural;
        };
    }

    private static String titleCase(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("_")) {
            if (part.isEmpty()) continue;
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
