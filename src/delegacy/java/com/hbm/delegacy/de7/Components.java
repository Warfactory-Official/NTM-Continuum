package com.hbm.delegacy.de7;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** Item components DE and Continuum spell differently, as raw NBT before the game decodes the stack. */
final class Components {
    private static final Map<String, String> CELL_FLUIDS = Map.of("cell_uf6", "hbm:uf6", "cell_puf6", "hbm:puf6",
            "cell_deuterium", "hbm:deuterium", "cell_tritium", "hbm:tritium", "cell_sas3", "hbm:sas3",
            "cell_antimatter", "hbm:amat", "cell_anti_schrabidium", "hbm:aschrab");
    private static final Map<String, String> BEDROCK_SUFFIX = Map.of("light_metal", "light", "heavy_metal", "heavy",
            "rare_earth", "rare", "actinide", "actinide", "non_metal", "nonmetal", "crystalline", "crystal");
    /** Block positions DE packs with {@code BlockPos.asLong} and Continuum writes as {@code [I; x, y, z]}. */
    private static final Map<String, String> POSITIONS = Map.of("hbm:detonator_pos", "hbm:detonator_target",
            "hbm:mirror_tool_target", "hbm:mirror_target", "hbm:radar_linker_target", "hbm:coordinate_position",
            "hbm:anchor_pos", "hbm:anchor_position", "hbm:wire_target", "hbm:wire_start", "hbm:wrench_target",
            "hbm:wire_start", "hbm:drone_link_target", "hbm:drone_linker_position", "hbm:rebar_corner", "hbm:rebar_pos");
    private static final Map<String, String> RENAMES = Map.of("hbm:haz_radiation", "hbm:hazard_radiation",
            "hbm:redd_charge", "hbm:battery_redd_power", "hbm:muffled", "hbm:battery_redd_muffled",
            "hbm:potato_timer", "hbm:potatos_timer", "hbm:rtty_pager_channel", "hbm:pager_channel",
            "hbm:broken_item_source", "hbm:broken_item", "hbm:settings_tool_data", "hbm:settings_tool",
            "hbm:contagion", "hbm:ntm_contagion", "hbm:decay_heat", "hbm:fuel_waste_hot",
            "hbm:reactor_pos", "hbm:reactor_sensor_target");
    /** Items whose contents DE keeps in {@code minecraft:container} and Continuum in its own positional list. */
    private static final Map<String, String> BAGS = Map.of("hbm:ammo_bag", "hbm:ammo_bag",
            "hbm:ammo_bag_infinite", "hbm:ammo_bag", "hbm:casing_bag", "hbm:casing_bag", "hbm:plastic_bag",
            "hbm:plastic_bag", "hbm:containment_box", "hbm:containment_box", "hbm:kit_toolbox", "hbm:toolbox");

    private Components() {}

    static void convert(String deItem, String cItem, CompoundTag c, StackContext ctx, String where) {
        String dePath = deItem.substring(4);
        if (CELL_FLUIDS.containsKey(dePath)) {
            c.putString("hbm:fluid", CELL_FLUIDS.get(dePath));
            c.putInt("hbm:fluid_amount", 1000);
        }
        fluidContent(cItem, c, ctx);
        if (c.contains("hbm:fluid_identifier")) {
            CompoundTag id = c.getCompound("hbm:fluid_identifier");
            CompoundTag multi = new CompoundTag();
            multi.putString("fluid1", Tables.get().fluid(id.getString("primary")));
            multi.putString("fluid2", Tables.get().fluid(id.getString("secondary")));
            c.put("hbm:fluid_multi", multi);
            c.remove("hbm:fluid_identifier");
        }
        machineContents(c);
        energy(cItem, c);
        RENAMES.forEach((from, to) -> move(c, from, to));
        POSITIONS.forEach((from, to) -> {
            if (!c.contains(from, Tag.TAG_LONG)) return;
            c.put(to, unpack(c.getLong(from)));
            c.remove(from);
        });
        if (c.contains("hbm:detonator_pos_list", Tag.TAG_LIST)) {
            ListTag out = new ListTag();
            for (Tag t : c.getList("hbm:detonator_pos_list", Tag.TAG_LONG)) out.add(unpack(((LongTag) t).getAsLong()));
            c.put("hbm:detonator_positions", out);
            c.remove("hbm:detonator_pos_list");
        }
        if (c.contains("hbm:target_designator", Tag.TAG_LONG)) {
            long v = c.getLong("hbm:target_designator");
            c.putInt("hbm:designator_x", (int) (v >> 32));
            c.putInt("hbm:designator_z", (int) v);
            c.remove("hbm:target_designator");
        }
        if (c.contains("hbm:linker_target", Tag.TAG_COMPOUND)) {
            CompoundTag global = c.getCompound("hbm:linker_target");
            int[] pos = global.getIntArray("pos");
            if (pos.length == 3) {
                CompoundTag link = new CompoundTag();
                link.putInt("x", pos[0]);
                link.putInt("y", pos[1]);
                link.putInt("z", pos[2]);
                link.putString("dim", global.getString("dimension"));
                c.put("hbm:tele_link", link);
            }
            c.remove("hbm:linker_target");
        }
        heat(dePath, cItem, c, ctx);
        if (c.contains("hbm:food_additives")) {
            int bits = c.getInt("hbm:food_additives");
            if ((bits & 1) != 0) c.putBoolean("hbm:ntm_cyanide", true);
            if ((bits & 2) != 0) c.putBoolean("hbm:ntm_red_pill", true);
            c.remove("hbm:food_additives");
        }
        for (String damage : List.of("hbm:canteen_cooldown", "hbm:arc_electrode_durability")) {
            if (!c.contains(damage)) continue;
            c.putInt("minecraft:damage", c.getInt(damage));
            c.remove(damage);
        }
        if (c.getBoolean("hbm:polarized")) c.putInt("minecraft:damage", 1);
        c.remove("hbm:polarized");
        c.remove("hbm:roasted");
        kitColors(c);
        if (c.contains("hbm:scrap", Tag.TAG_COMPOUND)) {
            CompoundTag scrap = c.getCompound("hbm:scrap");
            String name = Tables.get().material(scrap.getInt("material"));
            if (name != null) {
                c.putString("hbm:material", name);
                c.putInt("hbm:mat_amount", scrap.getInt("amount"));
                if (scrap.getBoolean("liquid")) c.putInt("minecraft:custom_model_data", 1);
                c.remove("hbm:scrap");
            }
        }
        missile(c);
        armorMods(c);
        gunState(cItem, c, ctx, where);
        if (c.contains("hbm:arty_cargo", Tag.TAG_COMPOUND)) c.put("hbm:arty_cargo", oneSlot(counted(c.getCompound("hbm:arty_cargo"))));
        for (Tag t : c.getList("hbm:kit_contents", Tag.TAG_COMPOUND)) counted((CompoundTag) t);
        String bag = BAGS.get(cItem);
        if (bag != null && c.contains("minecraft:container", Tag.TAG_LIST)) {
            c.put(bag, positional(c.getList("minecraft:container", Tag.TAG_COMPOUND), 0));
            c.remove("minecraft:container");
        }
        bedrockOreSample(c);
        selections(c);
        customMachine(c, ctx, where);
        icfPellet(c);
        loreBook(c);
        tankContents(cItem, c);
        toolPresets(c);
        unstable(c, ctx);
        persistentData(c);
        lockedContents(c, ctx, where);
        if (c.contains("hbm:block_inventory", Tag.TAG_COMPOUND)) {
            CompoundTag inv = c.getCompound("hbm:block_inventory");
            CompoundTag data = inv.getCompound("contents").copy();
            data.putString("id", inv.getString("type"));
            c.put("minecraft:block_entity_data", data);
            c.remove("hbm:block_inventory");
        }
        for (String key : List.copyOf(c.getAllKeys())) {
            if (!key.startsWith("hbm:") || ctx.hasComponent(key)) continue;
            ctx.park("component", where, deItem + " " + key, c.get(key));
            c.remove(key);
        }
    }

    static boolean hasFluid(CompoundTag components) {
        CompoundTag content = components.getCompound("hbm:fluid_content");
        return content.getLong("amount") > 0 && !content.getString("type").isEmpty();
    }

    static @Nullable String bedrockOreId(CompoundTag components) {
        CompoundTag ore = components.getCompound("hbm:bedrock_ore");
        String grade = ore.getString("grade").isEmpty() ? "base" : ore.getString("grade");
        String type = ore.getString("type").isEmpty() ? "light_metal" : ore.getString("type");
        String suffix = BEDROCK_SUFFIX.get(type);
        components.remove("hbm:bedrock_ore");
        return suffix == null ? null : "hbm:bedrock_ore_" + grade + "_" + suffix;
    }

    static String orbitalAssemblyId(CompoundTag components) {
        return "hbm:orbital_assembly_crystal_circuit";
    }

    /** DE's {@code {type, amount, pressure}} on whatever holds it. */
    private static void fluidContent(String cItem, CompoundTag c, StackContext ctx) {
        if (!c.contains("hbm:fluid_content", Tag.TAG_COMPOUND)) return;
        CompoundTag content = c.getCompound("hbm:fluid_content");
        c.remove("hbm:fluid_content");
        String fluid = Tables.get().fluid(content.getString("type"));
        int amount = (int) Math.min(Integer.MAX_VALUE, content.getLong("amount"));
        if (fluid.isEmpty() || amount <= 0) return;
        switch (cItem) {
            case "hbm:blowtorch" -> c.putInt("hbm:fluid_amount", amount);
            case "hbm:fluid_icon" -> {
                c.putString("hbm:fluid", fluid);
                c.putInt("hbm:fluid_amount", amount);
                c.putInt("hbm:fluid_pressure", content.getInt("pressure"));
            }
            default -> {
                if (cItem.contains("pipette")) {
                    CompoundTag pipette = new CompoundTag();
                    pipette.putString("type", fluid);
                    pipette.putInt("fill", amount);
                    pipette.putInt("capacity", c.contains("hbm:pipette_capacity") ? c.getInt("hbm:pipette_capacity") : 1000);
                    c.put("hbm:pipette", pipette);
                    c.remove("hbm:pipette_capacity");
                } else if (cItem.contains("jetpack")) {
                    c.putInt("hbm:jetpack_fuel", amount);
                } else {
                    c.putString("hbm:fluid", fluid);
                    c.putInt("hbm:fluid_amount", amount);
                    CompoundTag filled = ctx.filledComponents(cItem, fluid, amount);
                    if (filled != null) c.merge(filled);
                }
            }
        }
    }

    /** Machine items carry their block entity's carried data in {@code minecraft:custom_data} in Continuum. */
    private static void machineContents(CompoundTag c) {
        CompoundTag data = c.getCompound("minecraft:custom_data");
        boolean changed = false;
        if (c.contains("hbm:fluid_tank_contents", Tag.TAG_COMPOUND)) {
            CompoundTag tank = c.getCompound("hbm:fluid_tank_contents");
            tankKeys(data, "tank", tank.getCompound("tank"), tank.getInt("capacity"));
            data.putShort("mode", (short) tank.getInt("mode"));
            data.putBoolean("exploded", tank.getBoolean("damaged"));
            data.putBoolean("onFire", tank.getBoolean("on_fire"));
            c.remove("hbm:fluid_tank_contents");
            changed = true;
        }
        if (c.contains("hbm:refinery_contents", Tag.TAG_COMPOUND)) {
            CompoundTag refinery = c.getCompound("hbm:refinery_contents");
            ListTag tanks = refinery.getList("tanks", Tag.TAG_COMPOUND);
            for (int i = 0; i < tanks.size(); i++) tankKeys(data, "tank" + i, tanks.getCompound(i), 0);
            data.putBoolean("exploded", refinery.getBoolean("has_exploded"));
            data.putBoolean("onFire", refinery.getBoolean("on_fire"));
            c.remove("hbm:refinery_contents");
            changed = true;
        }
        if (c.contains("hbm:drill_contents", Tag.TAG_COMPOUND)) {
            CompoundTag drill = c.getCompound("hbm:drill_contents");
            ListTag tanks = drill.getList("tanks", Tag.TAG_COMPOUND);
            for (int i = 0; i < tanks.size(); i++) tankKeys(data, "t" + i, tanks.getCompound(i), 0);
            data.putLong("power", drill.getLong("power"));
            c.remove("hbm:drill_contents");
            changed = true;
        }
        if (c.contains("hbm:lock_state", Tag.TAG_COMPOUND)) {
            CompoundTag lock = c.getCompound("hbm:lock_state");
            if (lock.getBoolean("isLocked")) {
                data.putInt("lock", lock.getInt("lock"));
                data.putDouble("lockMod", lock.getDouble("lockMod"));
            }
            c.remove("hbm:lock_state");
            changed = true;
        }
        if (c.contains("hbm:spiders")) {
            data.putBoolean("spiders", true);
            c.remove("hbm:spiders");
            changed = true;
        }
        if (c.contains("hbm:stacklock", Tag.TAG_LONG)) {
            data.putLong("stacklock", c.getLong("hbm:stacklock"));
            c.remove("hbm:stacklock");
            changed = true;
        }
        if (c.contains("hbm:mass_stockpile")) {
            data.putInt("stack", c.getInt("hbm:mass_stockpile"));
            data.putBoolean("output", false);
            data.putBoolean("redstone", false);
            c.remove("hbm:mass_stockpile");
            changed = true;
        }
        if (c.contains("hbm:pneumatic_bulk", Tag.TAG_LIST)) {
            ListTag list = c.getList("hbm:pneumatic_bulk", Tag.TAG_INT);
            int[] amounts = new int[3];
            for (int i = 0; i < 3 && i < list.size(); i++) amounts[i] = list.getInt(i);
            data.putIntArray("amounts", amounts);
            c.remove("hbm:pneumatic_bulk");
            changed = true;
        }
        if (changed) c.put("minecraft:custom_data", data);
    }

    private static void tankKeys(CompoundTag data, String key, CompoundTag stack, int capacity) {
        String fluid = Tables.get().fluid(stack.getString("type"));
        data.putInt(key, (int) Math.min(Integer.MAX_VALUE, stack.getLong("amount")));
        if (capacity > 0) data.putInt(key + "_max", capacity);
        data.putString(key + "_type", fluid.isEmpty() ? "minecraft:empty" : fluid);
    }

    /** A DE FEnSU item is Continuum's REDD, whose charge is an unbounded number of its own; other batteries hold 1 MHE. */
    private static void energy(String cItem, CompoundTag c) {
        if (c.contains("hbm:machine_battery_state", Tag.TAG_COMPOUND)) {
            long power = c.getCompound("hbm:machine_battery_state").getLong("power");
            if (power > 0 && cItem.equals("hbm:machine_battery_redd")) c.putString("hbm:battery_redd_power", Long.toString(power));
            else if (power > 0) c.putLong("hbm:battery_power", Math.min(power, 1_000_000L));
            c.remove("hbm:machine_battery_state");
        }
        if (c.contains("hbm:capacitor_state", Tag.TAG_COMPOUND)) {
            c.putLong("hbm:capacitor_power", c.getCompound("hbm:capacitor_state").getLong("power"));
            c.remove("hbm:capacitor_state");
        }
    }

    /**
     * Continuum keeps a heat count where DE keeps a deadline in game time, or the ticks left while it is held still; the
     * share of DE's own scale that is left becomes the same share of Continuum's. A deadline already past is cold: DE
     * drops it only when the stack ticks in an inventory, so a stack that cooled in a chest still has one.
     */
    private static void heat(String dePath, String cItem, CompoundTag c, StackContext ctx) {
        if (c.contains("hbm:hot_item_heat") || c.contains("hbm:hot_until")) {
            long left = c.contains("hbm:hot_item_heat") ? c.getInt("hbm:hot_item_heat") : c.getLong("hbm:hot_until") - ctx.gameTime();
            int deMax = switch (dePath) {
                case "ingot_chainsteel" -> 100;
                case "ingot_steel_dusted" -> 200 - 10 * c.getInt("hbm:forge_count");
                default -> 200;
            };
            int heat = deMax <= 0 ? 0 : (int) Math.round(Math.clamp((double) left / deMax, 0D, 1D) * ctx.maxHeat(cItem));
            if (heat > 0) c.putInt("hbm:heat", heat);
        }
        c.remove("hbm:hot_item_heat");
        c.remove("hbm:hot_until");
    }

    private static void kitColors(CompoundTag c) {
        if (!c.contains("hbm:kit_color_1")) return;
        int c1 = c.getInt("hbm:kit_color_1"), c2 = c.getInt("hbm:kit_color_2");
        CompoundTag dyed = new CompoundTag();
        dyed.putInt("rgb", c1);
        dyed.putBoolean("show_in_tooltip", false);
        c.put("minecraft:dyed_color", dyed);
        CompoundTag data = c.getCompound("minecraft:custom_data");
        data.putIntArray("GolemCoreModelColors", new int[]{c1, c2});
        c.put("minecraft:custom_data", data);
        c.remove("hbm:kit_color_1");
        c.remove("hbm:kit_color_2");
    }

    private static void missile(CompoundTag c) {
        if (!c.contains("hbm:missile_parts", Tag.TAG_COMPOUND)) return;
        CompoundTag parts = c.getCompound("hbm:missile_parts").copy();
        if (parts.contains("fins")) {
            parts.put("stability", parts.get("fins"));
            parts.remove("fins");
        }
        if (c.contains("hbm:missile_chip", Tag.TAG_STRING)) parts.putString("chip", c.getString("hbm:missile_chip"));
        if (parts.contains("chip") && parts.contains("warhead") && parts.contains("fuselage") && parts.contains("thruster")) {
            c.put("hbm:missile_custom_parts", parts);
            c.remove("hbm:missile_parts");
            c.remove("hbm:missile_chip");
        }
    }

    private static final List<String> GUN_DATA_PREFIXES = List.of("wear_", "mode_", "KEY_MOD_NAMES_", "coincount",
            "coincharge", "colors", "flashlight");

    /**
     * DE keeps a gun's wear, modes, mods and magazines with its transient animation state; Continuum keeps the
     * persistent part in {@code gun_data}, naming the loaded ammo and the mods where DE stores runtime numbers. A
     * chemical thrower's magazine holds a fluid's raw registry number, which no save records, so it is parked.
     */
    private static void gunState(String cItem, CompoundTag c, StackContext ctx, String where) {
        if (!c.contains("hbm:gun_state", Tag.TAG_COMPOUND) || c.contains("hbm:gun_data")) return;
        CompoundTag state = c.getCompound("hbm:gun_state");
        CompoundTag data = new CompoundTag();
        CompoundTag parked = new CompoundTag();
        for (String key : state.getAllKeys()) {
            if (!key.startsWith("magtype")) continue;
            String index = key.substring("magtype".length());
            String ammo = cItem.contains("chemthrower") ? null : Tables.get().magtypes.get(Integer.toString(state.getInt(key)));
            for (String mag : List.of("magtype", "magcount", "magprev", "magafter")) {
                if (!state.contains(mag + index)) continue;
                if (ammo == null) parked.put(mag + index, state.get(mag + index).copy());
                else data.put(mag + index, state.get(mag + index).copy());
            }
            if (ammo != null) data.putString(key, ammo);
        }
        for (String key : state.getAllKeys()) {
            if (key.startsWith("mag")) continue;
            if (key.startsWith("KEY_MOD_LIST_")) {
                List<String> names = new ArrayList<>();
                for (int id : state.getIntArray(key)) {
                    String name = ctx.weaponModName(id);
                    if (name != null) names.add(name);
                }
                data.putString("KEY_MOD_NAMES_" + key.substring("KEY_MOD_LIST_".length()), String.join(",", names));
            } else if (GUN_DATA_PREFIXES.stream().anyMatch(key::startsWith)) {
                data.put(key, state.get(key).copy());
            }
        }
        if (!parked.isEmpty()) ctx.park("gun_magazine", where, cItem, parked);
        if (!data.isEmpty()) c.put("hbm:gun_data", data);
        c.remove("hbm:gun_state");
    }

    /** DE lists {@code {slot, item}}; Continuum keeps nine positions with {@code {}} for an empty one. */
    private static void armorMods(CompoundTag c) {
        if (!c.contains("hbm:armor_mods", Tag.TAG_LIST)) return;
        ListTag mods = c.getList("hbm:armor_mods", Tag.TAG_COMPOUND);
        if (mods.isEmpty() || !mods.getCompound(0).contains("slot")) return;
        c.put("hbm:armor_mods", positional(mods, 9));
    }

    /** DE's item templates leave out a count of one; the walker only knows a stack by its count. */
    private static CompoundTag counted(CompoundTag template) {
        if (template.contains("id", Tag.TAG_STRING) && !template.contains("count")) template.putInt("count", 1);
        return template;
    }

    private static ListTag positional(ListTag entries, int size) {
        List<CompoundTag> slots = new ArrayList<>();
        for (Tag t : entries) {
            CompoundTag e = (CompoundTag) t;
            int slot = e.getInt("slot");
            while (slots.size() <= slot) slots.add(new CompoundTag());
            slots.set(slot, counted(e.getCompound("item")));
        }
        while (slots.size() < size) slots.add(new CompoundTag());
        ListTag out = new ListTag();
        out.addAll(slots);
        return out;
    }

    private static ListTag oneSlot(CompoundTag stack) {
        CompoundTag entry = new CompoundTag();
        entry.putInt("slot", 0);
        entry.put("item", stack);
        ListTag list = new ListTag();
        list.add(entry);
        return list;
    }

    private static final String[] BEDROCK_KINDS = {"light", "heavy", "rare", "actinide", "nonmetal", "crystal"};
    private static final String[] ICF_FUELS = {"hydrogen", "deuterium", "tritium", "helium3", "helium4", "lithium",
            "beryllium", "boron", "carbon", "oxygen", "sodium", "chlorine", "calcium"};
    private static final Set<String> AREA_ABILITIES = Set.of("recursion", "hammer", "hammer_flat", "explosion");

    /** HBM's own six kinds go to the density record, an addon set's kinds to the open composition list. */
    private static void bedrockOreSample(CompoundTag c) {
        if (!(c.get("hbm:bedrock_ore_sample") instanceof CompoundTag sample)) return;
        String set = sample.getString("set");
        if (set.isEmpty() || set.equals("hbm")) {
            CompoundTag density = new CompoundTag();
            for (String kind : BEDROCK_KINDS) density.putDouble(kind, sample.getDouble(kind));
            c.put("hbm:bedrock_ore_density", density);
        } else {
            ListTag amounts = new ListTag();
            for (String kind : sample.getAllKeys()) {
                if (kind.equals("set")) continue;
                CompoundTag amount = new CompoundTag();
                amount.putString("type", kind);
                amount.putDouble("amount", sample.getDouble(kind));
                amounts.add(amount);
            }
            CompoundTag composition = new CompoundTag();
            composition.putString("origin", "");
            composition.put("amounts", amounts);
            c.put("hbm:bedrock_ore_composition", composition);
        }
        c.remove("hbm:bedrock_ore_sample");
    }

    /** The custom machine structure tool's corners and the conveyor wand's run, in 1.7's persistent keys. */
    private static void selections(CompoundTag c) {
        CompoundTag data = c.getCompound("hbm:persistent_data");
        if (c.get("hbm:cm_structure") instanceof CompoundTag selection) {
            putPos(data, selection.getIntArray("anchor"), "anchorX", "anchorY", "anchorZ");
            putPos(data, selection.getIntArray("first"), "x1", "y1", "z1");
            putPos(data, selection.getIntArray("second"), "x2", "y2", "z2");
            c.remove("hbm:cm_structure");
        }
        if (c.get("hbm:conveyor_run") instanceof CompoundTag run) {
            putPos(data, run.getIntArray("anchor"), "run_x", "run_y", "run_z");
            data.putString("run_side", run.getString("side"));
            data.putInt("run_count", run.getInt("budget"));
            c.remove("hbm:conveyor_run");
        }
        if (!data.isEmpty()) c.put("hbm:persistent_data", data);
    }

    private static void putPos(CompoundTag data, int[] pos, String x, String y, String z) {
        if (pos.length != 3) return;
        data.putInt(x, pos[0]);
        data.putInt(y, pos[1]);
        data.putInt(z, pos[2]);
    }

    private static void customMachine(CompoundTag c, StackContext ctx, String where) {
        if (!c.contains("hbm:custom_machine_type", Tag.TAG_STRING)) return;
        String name = ctx.customMachineName(c.getString("hbm:custom_machine_type"));
        if (name == null) return;
        c.putString("hbm:custom_machine", name);
        c.remove("hbm:custom_machine_type");
    }

    private static void icfPellet(CompoundTag c) {
        if (!(c.get("hbm:icf_pellet") instanceof CompoundTag pellet)) return;
        int type1 = pellet.getInt("type1"), type2 = pellet.getInt("type2");
        if (type1 >= 0 && type1 < ICF_FUELS.length) c.putString("hbm:icf_type1", ICF_FUELS[type1]);
        if (type2 >= 0 && type2 < ICF_FUELS.length) c.putString("hbm:icf_type2", ICF_FUELS[type2]);
        c.putBoolean("hbm:icf_muon", pellet.getBoolean("muon"));
        c.putLong("hbm:icf_depletion", pellet.getLong("depletion"));
        c.remove("hbm:icf_pellet");
    }

    /** DE's five lore book components to 1.7's one compound. */
    private static void loreBook(CompoundTag c) {
        if (!c.contains("hbm:lore_book_key")) return;
        CompoundTag book = new CompoundTag();
        book.putString("k", c.getString("hbm:lore_book_key"));
        book.putInt("p", c.getInt("hbm:lore_book_pages"));
        book.putInt("cov_col", c.getInt("hbm:lore_book_cover"));
        book.putInt("tit_col", c.getInt("hbm:lore_book_title"));
        CompoundTag args = c.getCompound("hbm:lore_book_args");
        for (String page : args.getAllKeys()) {
            CompoundTag pageArgs = new CompoundTag();
            ListTag list = args.getList(page, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) pageArgs.putString("a" + (i + 1), list.getString(i));
            book.put("p" + page, pageArgs);
        }
        c.put("hbm:book_lore", book);
        for (String key : List.of("key", "pages", "cover", "title", "args")) c.remove("hbm:lore_book_" + key);
    }

    /** Only the acetylene torch keeps its tanks on the item in Continuum; the machines' items stay parked. */
    private static void tankContents(String cItem, CompoundTag c) {
        if (!cItem.equals("hbm:acetylene_torch") || !(c.get("hbm:tank_contents") instanceof ListTag tanks)) return;
        if (tanks.size() > 0) c.putInt("hbm:torch_unsaturateds", (int) tanks.getCompound(0).getLong("amount"));
        if (tanks.size() > 1) c.putInt("hbm:torch_oxygen", (int) tanks.getCompound(1).getLong("amount"));
        c.remove("hbm:tank_contents");
    }

    /** DE's tool presets to 1.7's ability config, abilities named by their 1.7 translation keys. */
    private static void toolPresets(CompoundTag c) {
        if (!(c.get("hbm:tool_presets") instanceof ListTag presets)) return;
        ListTag out = new ListTag();
        for (Tag t : presets) {
            CompoundTag preset = (CompoundTag) t;
            CompoundTag entry = new CompoundTag();
            entry.putString("area", ability(preset.getString("area")));
            entry.putInt("areaLevel", preset.getInt("areaLevel"));
            String harvest = preset.getString("harvest");
            entry.putString("harvest", ability(harvest.equals("silk") ? "silktouch" : harvest));
            entry.putInt("harvestLevel", preset.getInt("harvestLevel"));
            out.add(entry);
        }
        CompoundTag config = new CompoundTag();
        config.putInt("ability", c.getInt("hbm:tool_preset_index"));
        config.put("abilityPresets", out);
        c.put("hbm:tool_ability", config);
        c.remove("hbm:tool_presets");
        c.remove("hbm:tool_preset_index");
    }

    private static String ability(String name) {
        return name.isEmpty() || name.equals("none") ? "" : "tool.ability." + name;
    }

    /** DE keeps the tick a fuse ends; Continuum the ticks since it was armed, firing at exactly 200. */
    private static void unstable(CompoundTag c, StackContext ctx) {
        if (!c.contains("hbm:unstable_at", Tag.TAG_ANY_NUMERIC)) return;
        long left = c.getLong("hbm:unstable_at") - ctx.gameTime();
        c.putInt("hbm:unstable_timer", (int) Math.max(0, Math.min(199, 200 - left)));
        c.remove("hbm:unstable_at");
    }

    private static void persistentData(CompoundTag c) {
        if (c.contains("hbm:armor_mod_nv_active") || c.contains("hbm:no9_lamp")) {
            CompoundTag data = c.getCompound("hbm:persistent_data");
            if (c.contains("hbm:armor_mod_nv_active")) data.putBoolean("ITEM_MOD_NV_ACTIVE", true);
            if (c.contains("hbm:no9_lamp")) data.putBoolean("isOn", c.getBoolean("hbm:no9_lamp"));
            c.put("hbm:persistent_data", data);
            c.remove("hbm:armor_mod_nv_active");
            c.remove("hbm:no9_lamp");
        }
        if (!c.contains("hbm:persistent_data", Tag.TAG_COMPOUND)) return;
        CompoundTag data = c.getCompound("hbm:persistent_data");
        if (data.contains("depletion")) c.putDouble("hbm:pile_rod_depletion", data.getDouble("depletion"));
        if (data.contains("pins")) c.putInt("hbm:key_pins", data.getInt("pins"));
        if (data.contains("filter_stack", Tag.TAG_COMPOUND)) c.put("hbm:gas_mask_filter", oneSlot(data.getCompound("filter_stack")));
        data.remove("depletion");
        data.remove("pins");
        data.remove("filter_stack");
    }

    /** DE seals crate contents with AES-GCM under the world's key; the crate opens as a plain container. */
    private static void lockedContents(CompoundTag c, StackContext ctx, String where) {
        if (!c.contains("hbm:locked_contents", Tag.TAG_COMPOUND)) return;
        CompoundTag locked = c.getCompound("hbm:locked_contents");
        byte[] key = ctx.lockedContentsKey();
        try {
            if (key == null) throw new GeneralSecurityException("no key file");
            byte[] sealed = locked.getByteArray("sealed");
            ByteBuffer aad = ByteBuffer.allocate(14);
            aad.put((byte) (locked.getBoolean("heavy") ? 1 : 0));
            aad.putInt(Float.floatToIntBits(locked.getFloat("radiation")));
            aad.putLong(locked.contains("minimum_deadline") ? locked.getLong("minimum_deadline") : Long.MAX_VALUE);
            aad.put((byte) (locked.getBoolean("has_references") ? 1 : 0));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, sealed, 0, 12));
            cipher.updateAAD(aad.array());
            byte[] plain = cipher.doFinal(sealed, 12, sealed.length - 12);
            CompoundTag contents = NbtIo.readCompressed(new ByteArrayInputStream(plain), NbtAccounter.unlimitedHeap());
            c.put("minecraft:container", contents.getList("items", Tag.TAG_COMPOUND));
            c.remove("hbm:locked_contents");
            ctx.count("locked crates opened");
        } catch (GeneralSecurityException | IOException e) {
            ctx.park("locked_contents", where, e.getMessage(), locked);
            c.remove("hbm:locked_contents");
        }
    }

    private static void move(CompoundTag c, String from, String to) {
        if (!c.contains(from)) return;
        c.put(to, c.get(from));
        c.remove(from);
    }

    private static IntArrayTag unpack(long packed) {
        return new IntArrayTag(new int[]{(int) (packed >> 38), (int) (packed << 52 >> 52), (int) (packed << 26 >> 38)});
    }
}
