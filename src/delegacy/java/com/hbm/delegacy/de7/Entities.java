package com.hbm.delegacy.de7;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Entity NBT that DE and Continuum save differently, players included. */
public final class Entities {
    /** DE living and player props keys as Continuum's single {@code hbm:living_props} names them. */
    private static final Map<String, String> PROPS = Map.ofEntries(
            Map.entry("radiation", "rads"), Map.entry("digamma", "digamma"), Map.entry("asbestos", "asbestos"),
            Map.entry("blacklung", "blacklung"), Map.entry("contagion", "contagion"), Map.entry("oil", "oil"),
            Map.entry("fire", "fire"), Map.entry("phosphorus", "phosphorus"), Map.entry("balefire", "balefire"),
            Map.entry("blackFire", "blackfire"), Map.entry("shield", "shield"), Map.entry("maxShield", "maxShield"),
            Map.entry("reputation", "reputation"), Map.entry("enableHUD", "enableHUD"),
            Map.entry("enableBackpack", "enableBackpack"), Map.entry("enableMagnet", "enableMagnet"),
            Map.entry("maskManTimer", "maskManTimer"), Map.entry("fbiMark", "fbiMark"), Map.entry("radMark", "radMark"),
            Map.entry("contamination", "cont"));
    private static final String[] CART_BASE = {"vanilla", "wood", "steel", "painted"};
    private static final String[] BOMBER_PAYLOAD = {"carpet", "napalm", "chlorine", "orange", "abomb", "stinger", "boxcar", "pc"};
    private static final String[] SOYUZ_SKIN = {"normal", "lunar", "post_war"};
    private static final String[] HIMARS = {"small", "large", "small_he", "small_wp", "small_tb", "large_tb", "small_mini_nuke",
            "small_lava"};
    private static final String[] SHELL = {"normal", "classic", "explosive", "mini_nuke", "nuke", "phosphorus", "mini_nuke_multi",
            "phosphorus_multi", "cargo", "chlorine", "phosgene", "mustard"};

    private Entities() {}

    static void reshape(CompoundTag tag, String deId, StackContext ctx, String where) {
        // Continuum missiles restore their motion and position from their own keys after vanilla's read.
        if ((deId.startsWith("hbm:missile") || deId.contains("_missile")) && !deId.equals("hbm:entity_missile_anti")) {
            copyVector(tag, "Motion", "mo");
            copyVector(tag, "Pos", "po");
        }
        switch (deId) {
            case "hbm:entity_cart_crate", "hbm:entity_cart_destroyer", "hbm:entity_cart_ore", "hbm:entity_cart_powder",
                 "hbm:entity_cart_semtex", "hbm:entity_minecart_test" -> ordinal(tag, "base", "base", CART_BASE);
            case "hbm:entity_bomber" -> ordinal(tag, "type", "payload", BOMBER_PAYLOAD);
            case "hbm:c130" -> ordinal(tag, "payload", "payload", new String[]{"supplies", "weapons", "a_fucking_fuel_truck"});
            case "hbm:entity_miner_lander" -> rename(tag, "slots", "items");
            case "hbm:entity_soyuz" -> ordinal(tag, "skin", "skin", SOYUZ_SKIN);
            case "hbm:entity_soyuz_capsule" -> ordinal(tag, "soyuz", "soyuz", SOYUZ_SKIN);
            // from DE's number only, so an entity already in Continuum's shape is left as it is
            case "hbm:entity_undead_soldier" -> {
                if (tag.contains("type", Tag.TAG_ANY_NUMERIC)) tag.putString("type", tag.getByte("type") == 1 ? "skeleton" : "zombie");
            }
            case "hbm:entity_himars" -> ordinal(tag, "type", "type", HIMARS);
            case "hbm:entity_artillery_shell" -> ordinal(tag, "type", "type", SHELL);
            case "hbm:entity_stray_cog" -> {
                if (tag.contains("meta", Tag.TAG_ANY_NUMERIC)) tag.putBoolean("steel", tag.getInt("meta") == 1);
                tag.remove("meta");
            }
            case "hbm:rbmk_debris" -> ordinal(tag, "debtype", "debtype", new String[]{"blank", "element", "fuel", "rod", "graphite", "lid"});
            case "hbm:zirnox_debris" -> ordinal(tag, "debtype", "type",
                    new String[]{"blank", "element", "shrapnel", "graphite", "concrete", "exchanger"});
            case "hbm:entity_stray_saw" -> {
                rename(tag, "rot", "orientation");
                tag.putBoolean("stuck", tag.getBoolean("inGround"));
                tag.remove("inGround");
            }
            case "hbm:entity_delivery_drone" -> {
                CompoundTag fluid = tag.getCompound("fluid");
                if (!fluid.isEmpty()) {
                    fluid.putString("type", Tables.get().fluid(fluid.getString("type")));
                    fluid.putInt("amount", (int) Math.min(fluid.getLong("amount"), Integer.MAX_VALUE));
                }
            }
            case "hbm:entity_falling_multiblock" -> {
                ListTag states = tag.getList("states", Tag.TAG_COMPOUND);
                for (int i = 0; i < states.size(); i++) states.set(i, ctx.blockState(states.getCompound(i)));
                if (tag.get("be") instanceof CompoundTag be && !states.isEmpty() && !ctx.blockEntity(be, states.getCompound(0), where))
                    tag.remove("be");
            }
            default -> { }
        }
        living(tag, ctx, where);
    }

    /** A DE enum ordinal to Continuum's lower-case name, under {@code to}; an ordinal out of range removes it. */
    private static void ordinal(CompoundTag tag, String from, String to, String[] names) {
        if (!tag.contains(from, Tag.TAG_ANY_NUMERIC)) return;
        int i = tag.getInt(from);
        tag.remove(from);
        if (i >= 0 && i < names.length) tag.putString(to, names[i]);
    }

    private static void rename(CompoundTag tag, String from, String to) {
        if (!tag.contains(from)) return;
        tag.put(to, tag.get(from));
        tag.remove(from);
    }

    /** Attachments and attribute modifiers that every living entity, player or mob, may carry. */
    public static void living(CompoundTag tag, StackContext ctx, String where) {
        CompoundTag attachments = tag.getCompound("neoforge:attachments");
        CompoundTag living = attachments.getCompound("hbm:living_props");
        CompoundTag player = attachments.getCompound("hbm:player_props");
        if (!living.isEmpty() && !living.contains("fmt") || !player.isEmpty()) {
            CompoundTag out = new CompoundTag();
            out.putString("fmt", "v1");
            for (CompoundTag source : List.of(living, player)) {
                for (String key : source.getAllKeys()) {
                    String to = PROPS.get(key);
                    if (to != null) out.put(to, source.get(key).copy());
                }
            }
            CompoundTag rest = living.copy();
            rest.merge(player);
            for (String key : PROPS.keySet()) rest.remove(key);
            if (!rest.isEmpty()) ctx.park("living_props", where, tag.getString("id"), rest);
            attachments.put("hbm:living_props", out);
            attachments.remove("hbm:player_props");
            tag.put("neoforge:attachments", attachments);
            ctx.count("living props converted");
        }
        for (Tag t : tag.getList("attributes", Tag.TAG_COMPOUND)) {
            for (Tag m : ((CompoundTag) t).getList("modifiers", Tag.TAG_COMPOUND)) {
                CompoundTag modifier = (CompoundTag) m;
                if (modifier.getString("id").equals("hbm:trigamma_health")) modifier.putString("id", "hbm:trigamma");
            }
        }
    }

    private static void copyVector(CompoundTag tag, String from, String prefix) {
        ListTag list = tag.getList(from, Tag.TAG_DOUBLE);
        if (list.size() != 3 || tag.contains(prefix + "X")) return;
        tag.putDouble(prefix + "X", list.getDouble(0));
        tag.putDouble(prefix + "Y", list.getDouble(1));
        tag.putDouble(prefix + "Z", list.getDouble(2));
    }
}
