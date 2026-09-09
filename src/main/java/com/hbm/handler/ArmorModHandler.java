package com.hbm.handler;

import com.hbm.items.ModDataComponents;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.JetpackBase;
import com.hbm.items.gear.JetpackGlider;
import dev.golem.core.world.CompatArmorItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.item.ItemStack;

/** Ordered armor-module storage and slot validation, preserving the nine legacy module slots. */
public final class ArmorModHandler {

    public static final int helmet_only = 0;
    public static final int plate_only = 1;
    public static final int legs_only = 2;
    public static final int boots_only = 3;
    public static final int servos = 4;
    public static final int cladding = 5;
    public static final int kevlar = 6;
    public static final int extra = 7;
    public static final int battery = 8;
    public static final int MOD_SLOTS = 9;
    public static final String MOD_COMPOUND_KEY = "ntm_armor_mods";
    public static final String MOD_SLOT_KEY = "mod_slot_";

    public static final UUID[] UUIDs = {
            UUID.fromString("8d6e5c77-133e-4056-9c80-a9e42a1a0b65"),
            UUID.fromString("b1b7ee0e-1d14-4400-8037-f7f2e02f21ca"),
            UUID.fromString("30b50d2a-4858-4e5b-88d4-3e3612224238"),
            UUID.fromString("426ee0d0-7587-4697-aaef-4772ab202e78")};

    public static final UUID[] fixedUUIDs = {
            UUID.fromString("e572caf4-3e65-4152-bc79-c4d4048cbd29"),
            UUID.fromString("bed30902-8a6a-4769-9f65-2a9b67469fff"),
            UUID.fromString("baebf7b3-1eda-4a14-b233-068e2493e9a2"),
            UUID.fromString("28016c1b-d992-4324-9409-a9f9f0ffb85c")};

    private ArmorModHandler() {}

    public static boolean isApplicable(ItemStack armor, ItemStack mod) {
        if (armor == null || armor.isEmpty() || armor.getItem() instanceof JetpackBase
                || !(armor.getItem() instanceof CompatArmorItem armorItem)) return false;
        if (mod == null || mod.isEmpty()) return false;

        int type = getModuleSlot(mod);
        if (type == plate_only && (mod.getItem() instanceof JetpackBase || mod.getItem() instanceof JetpackGlider)) {
            return armorItem.armorSlot() == dev.golem.core.world.ArmorSlot.CHESTPLATE;
        }
        if (!(mod.getItem() instanceof ItemArmorMod armorMod)) return false;
        return switch (armorItem.armorSlot()) {
            case HELMET -> armorMod.helmet;
            case CHESTPLATE -> armorMod.chestplate;
            case LEGGINGS -> armorMod.leggings;
            case BOOTS -> armorMod.boots;
        };
    }

    /** Stores one copy in its type slot. Callers validate applicability before calling this method. */
    public static void applyMod(ItemStack armor, ItemStack mod) {
        if (armor == null || armor.isEmpty() || mod == null || mod.isEmpty()) return;
        int type = getModuleSlot(mod);
        if (type < 0 || type >= MOD_SLOTS) return;

        List<ItemStack> values = read(armor);
        values.set(type, mod.copyWithCount(1));
        ModDataComponents.ARMOR_MODS.set(armor, new dev.golem.core.data.ItemContents(values));
    }

    /** Returns the ordered armor-table slot for a supported module, or -1 for a normal item. */
    public static int getModuleSlot(ItemStack mod) {
        if (mod == null || mod.isEmpty()) return -1;
        if (mod.getItem() instanceof ItemArmorMod armorMod) return armorMod.type;
        if (mod.getItem() instanceof JetpackBase) return plate_only;
        if (mod.getItem() instanceof JetpackGlider) return plate_only;
        return -1;
    }

    public static void removeMod(ItemStack armor, int slot) {
        if (armor == null || slot < 0 || slot >= MOD_SLOTS) return;
        List<ItemStack> values = read(armor);
        values.set(slot, ItemStack.EMPTY);
        writeOrClear(armor, values);
    }

    public static void clearMods(ItemStack armor) {
        if (armor != null) ModDataComponents.ARMOR_MODS.remove(armor);
    }

    public static boolean hasMods(ItemStack armor) {
        return armor != null && ModDataComponents.ARMOR_MODS.has(armor);
    }

    public static ItemStack[] pryMods(ItemStack armor) {
        List<ItemStack> values = read(armor);
        ItemStack[] result = new ItemStack[MOD_SLOTS];
        for (int i = 0; i < MOD_SLOTS; i++) result[i] = values.get(i).copy();
        return result;
    }

    public static ItemStack pryMod(ItemStack armor, int slot) {
        if (armor == null || slot < 0 || slot >= MOD_SLOTS) return ItemStack.EMPTY;
        return read(armor).get(slot).copy();
    }

    private static List<ItemStack> read(ItemStack armor) {
        List<ItemStack> stored = armor == null ? List.of() : ModDataComponents.ARMOR_MODS
                .getOr(armor, dev.golem.core.data.ItemContents.EMPTY).copies();
        List<ItemStack> values = new ArrayList<>(MOD_SLOTS);
        for (int i = 0; i < MOD_SLOTS; i++) {
            values.add(i < stored.size() && !stored.get(i).isEmpty() ? stored.get(i).copy() : ItemStack.EMPTY);
        }
        return values;
    }

    private static void writeOrClear(ItemStack armor, List<ItemStack> values) {
        boolean empty = true;
        for (ItemStack value : values) {
            if (!value.isEmpty()) {
                empty = false;
                break;
            }
        }
        if (empty) clearMods(armor);
        else ModDataComponents.ARMOR_MODS.set(armor, new dev.golem.core.data.ItemContents(values));
    }
}
