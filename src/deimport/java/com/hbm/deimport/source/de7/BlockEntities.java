package com.hbm.deimport.source.de7;

import com.hbm.deimport.engine.Ctx;
import com.hbm.delegacy.de7.Tables;
import com.hbm.delegacy.de7.Walker;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Block-entity NBT from DE's shape to Continuum's. Every tag gets the generic pass (Continuum's type id, flat tanks,
 * stacks); families that save differently get a mapper keyed by DE's block-entity id.
 */
public final class BlockEntities {
    private static final String[] PRIORITY = {"LOWEST", "LOW", "NORMAL", "HIGH", "HIGHEST"};
    private static final String[] RBMK_FUNCTION = {"LINEAR", "QUAD_UP", "QUAD_DOWN"};
    private static final String[] PWR_FUEL = {"MEU", "HEU233", "HEU235", "MEN", "HEN237", "MOX", "MEP", "HEP239", "HEP241",
            "MEA", "HEA242", "HES326", "HES327", "BFB_AM_MIX", "BFB_PU241"};

    private static final String[] DIRECTIONS = {"DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST"};
    private static final String[] HIMARS = {"SMALL", "LARGE", "SMALL_HE", "SMALL_WP", "SMALL_TB", "LARGE_TB", "SMALL_MINI_NUKE",
            "SMALL_LAVA"};

    private static final Map<String, BiConsumer<CompoundTag, Ctx>> MAPPERS = Map.ofEntries(
            Map.entry("hbm:crucible", (tag, ctx) -> {
                materialPairs(tag, "rec", ctx);
                materialPairs(tag, "was", ctx);
            }),
            Map.entry("hbm:arc_furnace_large", BlockEntities::arcFurnace),
            Map.entry("hbm:watz", BlockEntities::watzLocks),
            Map.entry("hbm:icf_controller", (tag, ctx) -> {
                tag.putBoolean("assembled", false);
                ctx.report.count("ICF arrays to reassemble");
            }),
            Map.entry("hbm:refinery", (tag, ctx) -> {
                String[] names = {"input", "heavy", "naphtha", "light", "petroleum"};
                for (int i = 0; i < names.length; i++) rename(tag, "tank" + i, names[i]);
            }),
            Map.entry("hbm:assemblymachine", (tag, ctx) -> renames(tag, "tankIn", "i", "tankOut", "o")),
            Map.entry("hbm:precass", (tag, ctx) -> renames(tag, "tankIn", "i", "tankOut", "o")),
            Map.entry("hbm:acidomatic", (tag, ctx) -> rename(tag, "tank", "tankNew")),
            Map.entry("hbm:launchpad_soyuz", (tag, ctx) -> {
                renames(tag, "fuel", "tank0", "oxidizer", "tank1");
                spread(tag, "positions", "p");
                spread(tag, "speed", "s");
                spread(tag, "target", "t");
                // derived: the loaded payload and the previous strut positions are recomputed every tick
                tag.remove("loadedType");
                tag.remove("prevPositions");
            }),
            Map.entry("hbm:launch1", BlockEntities::launchPad),
            Map.entry("hbm:launchpad_large", (tag, ctx) -> {
                launchPad(tag, ctx);
                ordinalToLower(tag, "formFactor", new String[]{"abm", "micro", "strong", "huge", "atlas", "other"});
            }),
            Map.entry("hbm:launchpad_rusted", (tag, ctx) -> {
                launchPad(tag, ctx);
                flattenTriples(tag, "activatedBlocks");
            }),
            Map.entry("hbm:hydrotreater", (tag, ctx) -> remapSlots(tag, new int[]{0, 1, 2, 5, 6, 7, 8, 9, 10})),
            Map.entry("hbm:mass_storage", (tag, ctx) -> rename(tag, "stockpile", "stack")),
            Map.entry("hbm:battery_redd", (tag, ctx) -> {
                rename(tag, "bigPower", "power");
                ordinalToName(tag, "priority", PRIORITY);
            }),
            Map.entry("hbm:battery", (tag, ctx) -> ordinalToName(tag, "priority", PRIORITY)),
            Map.entry("hbm:battery_socket", (tag, ctx) -> ordinalToName(tag, "priority", PRIORITY)),
            Map.entry("hbm:rbmk_control_auto", (tag, ctx) -> ordinalToName(tag, "function", RBMK_FUNCTION)),
            Map.entry("hbm:reactor_remote_control",
                    (tag, ctx) -> ordinalToName(tag, "function", new String[]{"LINEAR", "QUAD", "LOG"})),
            Map.entry("hbm:pwr_controller", BlockEntities::pwr),
            Map.entry("hbm:fluid_drain", (tag, ctx) -> rename(tag, "tank", "t")),
            Map.entry("hbm:refueler", (tag, ctx) -> rename(tag, "tank", "t")),
            Map.entry("hbm:thresher", (tag, ctx) -> rename(tag, "tank", "t")),
            Map.entry("hbm:annihilator", (tag, ctx) -> rename(tag, "tank", "t")),
            Map.entry("hbm:arc_welder", (tag, ctx) -> rename(tag, "tank", "t")),
            Map.entry("hbm:autosaw", (tag, ctx) -> rename(tag, "tank", "t")),
            Map.entry("hbm:pile_vent", (tag, ctx) -> rename(tag, "tank", "t")),
            Map.entry("hbm:silex", BlockEntities::silex),
            Map.entry("hbm:electrolyser", (tag, ctx) -> {
                renames(tag, "t0", "tank0", "t1", "tank1", "t2", "tank2", "t3", "tank3", "progressOre",
                        "progressMetal");
                // derived: Continuum recomputes both process times from the installed upgrades
                tag.remove("processFluidTime");
                tag.remove("processOreTime");
                material(tag, "leftType", ctx);
                material(tag, "rightType", ctx);
            }),
            Map.entry("hbm:mixer", BlockEntities::mixer),
            Map.entry("hbm:ind_turbine", (tag, ctx) -> renames(tag, "water", "input", "steam", "output")),
            Map.entry("hbm:condenser", (tag, ctx) -> renames(tag, "water", "input", "steam", "output")),
            Map.entry("hbm:condenser_powered", (tag, ctx) -> renames(tag, "water", "input", "steam", "output")),
            Map.entry("hbm:cooling_tower_large", (tag, ctx) -> renames(tag, "water", "input", "steam", "output")),
            Map.entry("hbm:cooling_tower_small", (tag, ctx) -> renames(tag, "water", "input", "steam", "output")),
            Map.entry("hbm:solarboiler", (tag, ctx) -> renames(tag, "water", "tank0", "steam", "tank1")),
            Map.entry("hbm:fusion_plasma_forge", (tag, ctx) -> rename(tag, "i", "input")),
            Map.entry("hbm:fracking_tower", (tag, ctx) -> renames(tag, "oil", "t0", "gas", "t1", "fracksol", "t2")),
            Map.entry("hbm:derrick", (tag, ctx) -> renames(tag, "oil", "t0", "gas", "t1")),
            Map.entry("hbm:machine_pumpjack", (tag, ctx) -> renames(tag, "oil", "t0", "gas", "t1")),
            Map.entry("hbm:gasflare", (tag, ctx) -> renames(tag, "power", "powerTime", "tank", "gas")),
            Map.entry("hbm:excavator", (tag, ctx) -> rename(tag, "power", "p")),
            Map.entry("hbm:machine_shredder", (tag, ctx) -> rename(tag, "powerTime", "power")),
            Map.entry("hbm:machine_turbofan", (tag, ctx) -> rename(tag, "power", "powerTime")),
            Map.entry("hbm:machine_field", (tag, ctx) -> rename(tag, "power", "powerTime")),
            Map.entry("hbm:wood_burner",
                    (tag, ctx) -> renames(tag, "ashWood", "ashLevelWood", "ashCoal", "ashLevelCoal",
                            "ashMisc", "ashLevelMisc")),
            Map.entry("hbm:coker", (tag, ctx) -> rename(tag, "progress", "prog")),
            Map.entry("hbm:pyrooven", (tag, ctx) -> rename(tag, "progress", "prog")),
            Map.entry("hbm:rotary_furnace", (tag, ctx) -> material(tag, "outType", ctx)),
            Map.entry("hbm:strand_caster", (tag, ctx) -> {
                renames(tag, "items", "inventory", "water", "w", "steam", "s", "lastProgress", "t");
                material(tag, "type", ctx);
            }),
            Map.entry("hbm:foundry_mold", (tag, ctx) -> {
                rename(tag, "items", "inventory");
                material(tag, "type", ctx);
            }),
            Map.entry("hbm:foundry_basin", (tag, ctx) -> {
                rename(tag, "items", "inventory");
                material(tag, "type", ctx);
            }),
            Map.entry("hbm:foundry_channel", (tag, ctx) -> {
                material(tag, "type", ctx);
                ordinalToName(tag, "flow", DIRECTIONS);
            }),
            Map.entry("hbm:foundry_outlet", (tag, ctx) -> {
                material(tag, "type", ctx);
                material(tag, "filter", ctx);
            }),
            Map.entry("hbm:foundry_slagtap", (tag, ctx) -> {
                material(tag, "type", ctx);
                material(tag, "filter", ctx);
            }),
            Map.entry("hbm:foundry_tank", (tag, ctx) -> material(tag, "type", ctx)),
            Map.entry("hbm:foundry_slag", (tag, ctx) -> material(tag, "mat", ctx)),
            Map.entry("hbm:piston_inserter", (tag, ctx) -> firstSlotTo(tag, "inventory", "stack")),
            Map.entry("hbm:pile_loader", (tag, ctx) -> {
                firstSlotTo(tag, "inventory", "stack");
                rename(tag, "wasRedstone", "redstone");
            }),
            Map.entry("hbm:pile_core", BlockEntities::pileCore),
            Map.entry("hbm:radgen", BlockEntities::radGen),
            Map.entry("hbm:radar", BlockEntities::radarMap),
            Map.entry("hbm:radar_large", BlockEntities::radarMap),
            Map.entry("hbm:ammo_press", (tag, ctx) -> {
                if (tag.getInt("recipe") < 0) tag.remove("recipe");
            }),
            Map.entry("hbm:pa_source", BlockEntities::particleSource),
            Map.entry("hbm:rbmk_console", (tag, ctx) -> {
                String[] screens = {"NONE", "COL_TEMP", "ROD_EXTRACTION", "FUEL_DEPLETION", "FUEL_POISON", "FUEL_TEMP"};
                for (String key : List.copyOf(tag.getAllKeys()))
                    if (key.matches("t\\d+")) ordinalToName(tag, key, screens);
            }),
            Map.entry("hbm:rbmk_control", (tag, ctx) -> {
                if (tag.getInt("color") < 0) tag.remove("color");
                else ordinalToName(tag, "color", new String[]{"RED", "YELLOW", "GREEN", "BLUE", "PURPLE"});
            }),
            Map.entry("hbm:rbmk_cooler", (tag, ctx) -> renames(tag, "t0", "cold", "t1", "hot")),
            Map.entry("hbm:pipe_pump", BlockEntities::pump),
            Map.entry("hbm:pipe_paintable", (tag, ctx) -> {
                if (tag.get("camo") instanceof CompoundTag camo)
                    tag.put("paint", BlockStates.convert(camo, ctx.target).entry());
                tag.remove("camo");
            }),
            Map.entry("hbm:rtty_autocal", (tag, ctx) -> {
                ListTag stack = tag.getList("stack", Tag.TAG_STRING);
                tag.putInt("stackSize", stack.size());
                for (int i = 0; i < stack.size(); i++) tag.putString("st" + i, stack.getString(i));
                tag.remove("stack");
            }),
            Map.entry("hbm:rtty_controller",
                    (tag, ctx) -> renames(tag, "p", "polling", "c", "channel", "r", "receiving")),
            Map.entry("hbm:rtty_counter", BlockEntities::radioTorch),
            Map.entry("hbm:rtty_logic", (tag, ctx) -> {
                renames(tag, "p", "polling", "d", "descending", "l", "lastState", "u", "lastUpdate", "c", "channel");
                for (int i = 0; i < 16; i++) renames(tag, "m" + i, "mapping" + i, "c" + i, "conditions" + i);
            }),
            Map.entry("hbm:rtty_reader", BlockEntities::radioTorch),
            Map.entry("hbm:rtty_rec", BlockEntities::radioTorch),
            Map.entry("hbm:rtty_sender", BlockEntities::radioTorch),
            Map.entry("hbm:turret_himars", (tag, ctx) -> {
                if (tag.getInt("type") < 0) tag.remove("type");
                else ordinalToName(tag, "type", HIMARS);
            }),
            Map.entry("hbm:bedrock_ore", (tag, ctx) -> {
                if (tag.contains("acid")) tag.putString("fluid", Tables.get().fluid(tag.getString("acid")));
                if (tag.contains("acid_amount")) tag.putInt("amount", (int) tag.getLong("acid_amount"));
                tag.remove("acid");
                tag.remove("acid_amount");
            }),
            Map.entry("hbm:door", (tag, ctx) -> flattenTriples(tag, "activatedBlocks")),
            Map.entry("hbm:dungeon_spawner", (tag, ctx) -> tag.putString("type", "aberrator")),
            Map.entry("hbm:glyphid_spawner", (tag, ctx) -> rename(tag, "initial_spawn", "initialSpawn")),
            Map.entry("hbm:logic_block", (tag, ctx) -> {
                int d = tag.getInt("direction");
                tag.putString("direction", d >= 0 && d < 6 ? DIRECTIONS[d].toLowerCase(Locale.ROOT) : "unknown");
            }),
            Map.entry("hbm:snowglobe", (tag, ctx) -> ordinalToLower(tag, "type",
                    new String[]{"none", "rivetcity", "tenpennytower", "lucky38", "sierramadre", "prydwen"})),
            Map.entry("hbm:rebar", BlockEntities::rebar),
            Map.entry("hbm:custom_machine", (tag, ctx) -> {
                String name = ctx.customMachineName(tag.getString("machineType"));
                if (name != null) tag.putString("machineType", name);
            }),
            Map.entry("hbm:furnace_steel", BlockEntities::steelFurnace),
            Map.entry("hbm:heater_heatex", (tag, ctx) -> renames(tag, "t0", "0", "t1", "1")),
            Map.entry("hbm:blast_furnace", (tag, ctx) -> renames(tag, "airblast", "t0", "flue", "t1")),
            Map.entry("hbm:chemicalfactory", (tag, ctx) -> {
                for (int m = 0; m < 4; m++) {
                    for (int k = 0; k < 3; k++) {
                        rename(tag, "i" + m + "abc".charAt(k), "i" + (3 * m + k));
                        rename(tag, "o" + m + "abc".charAt(k), "o" + (3 * m + k));
                    }
                }
            }),
            Map.entry("hbm:gas_centrifuge", (tag, ctx) -> {
                pseudoTank(tag, "inputTank");
                pseudoTank(tag, "outputTank");
            }));

    private BlockEntities() {
    }

    /**
     * Converts {@code de} in place for a block whose Continuum block entity is {@code type}; a null walker leaves its stacks.
     */
    public static void convert(CompoundTag de, String type, Ctx ctx, @Nullable Walker walker, String where) {
        String deId = de.getString("id");
        de.putString("id", type);
        BiConsumer<CompoundTag, Ctx> mapper = MAPPERS.get(deId);
        ctx.at(where);
        try {
            if (mapper != null) mapper.accept(de, ctx);
        } finally {
            ctx.at(null);
        }
        flattenTanks(de);
        if (walker != null) walker.walk(de, where);
        ctx.report.count("block entities converted");
    }

    /**
     * DE's {@code [id, amount, id, amount, ...]} to Continuum's {@code [{mat, amount}]}; an unknown material is parked.
     */
    private static void materialPairs(CompoundTag tag, String key, Ctx ctx) {
        if (!tag.contains(key, Tag.TAG_INT_ARRAY)) return;
        int[] pairs = tag.getIntArray(key);
        ListTag out = new ListTag();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String name = Tables.get().material(pairs[i]);
            if (name == null) {
                ctx.parking.park("material", ctx.where(key), Integer.toString(pairs[i]), IntTag.valueOf(pairs[i + 1]));
                continue;
            }
            CompoundTag stack = new CompoundTag();
            stack.putString("mat", name);
            stack.putInt("amount", pairs[i + 1]);
            out.add(stack);
        }
        tag.put(key, out);
    }

    private static void arcFurnace(CompoundTag tag, Ctx ctx) {
        int[] mats = tag.getIntArray("liquidMats"), amounts = tag.getIntArray("liquidAmounts");
        int count = 0;
        for (int i = 0; i < mats.length && i < amounts.length; i++) {
            String name = Tables.get().material(mats[i]);
            if (name == null) {
                ctx.parking.park("material", ctx.where("liquidMats"), Integer.toString(mats[i]),
                        IntTag.valueOf(amounts[i]));
                continue;
            }
            tag.putString("m" + count, name);
            tag.putInt("a" + count, amounts[i]);
            count++;
        }
        tag.putInt("count", count);
        tag.remove("liquidMats");
        tag.remove("liquidAmounts");
    }

    /**
     * DE keeps the pellet each Watz slot is locked to as a stack; Continuum as the pellet's item id.
     */
    private static void watzLocks(CompoundTag tag, Ctx ctx) {
        for (Tag t : tag.getCompound("locks").getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag stack = (CompoundTag) t;
            tag.putString("lock" + (stack.getByte("Slot") & 255), stack.getString("id"));
        }
        tag.remove("locks");
    }

    /**
     * DE's fuel and oxidizer tanks are Continuum's {@code t0} and {@code t1}; DE's reload counter is Continuum's delay.
     */
    private static void launchPad(CompoundTag tag, Ctx ctx) {
        rename(tag, "fuel", "t0");
        rename(tag, "oxidizer", "t1");
        rename(tag, "reloadDelay", "delay");
        launchPadRedstone(tag);
    }

    /**
     * DE saves one boolean; Continuum counts powered blocks and fires on a rising edge, so a powered pad stays settled.
     * A powered pad counts its core, which the live phase replaces with the blocks really powered; an airship's pad keeps
     * it, so the count still falls to nothing once the core sees the signal go.
     */
    private static void launchPadRedstone(CompoundTag tag) {
        if (!tag.contains("redstone")) return;
        int powered = tag.getBoolean("redstone") ? 1 : 0;
        tag.putInt("redstonePower", powered);
        tag.putInt("prevRedstonePower", powered);
        if (powered == 1 && !tag.contains("activatedBlocks"))
            tag.putIntArray("activatedBlocks", new int[]{tag.getInt("x"), tag.getInt("y"), tag.getInt("z")});
        tag.remove("redstone");
    }

    /**
     * DE's material id (-1 for none) to Continuum's name; an id with no name is parked.
     */
    private static void material(CompoundTag tag, String key, Ctx ctx) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) return;
        int id = tag.getInt(key);
        tag.remove(key);
        if (id < 0) return;
        String name = Tables.get().material(id);
        if (name == null) ctx.parking.park("material", ctx.where(key), Integer.toString(id), IntTag.valueOf(id));
        else tag.putString(key, name);
    }

    /**
     * DE's lists of {@code [x, y, z]} to one flat array of triples.
     */
    private static void flattenTriples(CompoundTag tag, String key) {
        if (!(tag.get(key) instanceof ListTag list)) return;
        List<Integer> out = new ArrayList<>();
        for (Tag t : list) if (t instanceof IntArrayTag triple) for (int v : triple.getAsIntArray()) out.add(v);
        tag.putIntArray(key, out);
    }

    /**
     * A DE list to Continuum's numbered keys {@code prefix0, prefix1, ...}.
     */
    private static void spread(CompoundTag tag, String key, String prefix) {
        if (!(tag.get(key) instanceof ListTag list)) return;
        for (int i = 0; i < list.size(); i++) tag.put(prefix + i, list.get(i));
        tag.remove(key);
    }

    /**
     * A one-slot DE inventory to Continuum's single stack.
     */
    private static void firstSlotTo(CompoundTag tag, String inventory, String to) {
        ListTag items = tag.getCompound(inventory).getList("Items", Tag.TAG_COMPOUND);
        if (!items.isEmpty()) tag.put(to, stripSlot(items.getCompound(0)));
        tag.remove(inventory);
    }

    private static void silex(CompoundTag tag, Ctx ctx) {
        rename(tag, "tank", "tankNew");
        if (tag.get("current") instanceof CompoundTag current) {
            new Walker(ctx).walk(current, "silex");
            tag.putString("item", current.getString("id"));
        } else if (tag.contains("currentFluid")) {
            tag.putString("item", "hbm:fluid_icon");
            tag.putString("fluid", Tables.get().fluid(tag.getString("currentFluid")));
        }
        tag.remove("current");
        tag.remove("currentFluid");
    }

    /**
     * DE saves the mixer's choice as an index into the recipes for its output fluid; Continuum by the recipe's name.
     */
    private static void mixer(CompoundTag tag, Ctx ctx) {
        renames(tag, "t0", "tank0", "t1", "tank1", "t2", "tank2");
        if (!tag.contains("recipe", Tag.TAG_ANY_NUMERIC)) return;
        CompoundTag out = tag.getCompound("tank2");
        String fluid = !out.getString("type").isEmpty() ? out.getString("type") : out.getString("pin");
        if (fluid.isEmpty()) {
            tag.remove("recipe");
            return;
        }
        String name = ctx.target.mixerRecipe(Tables.get().fluid(fluid), tag.getInt("recipe"));
        if (name != null) tag.putString("recipe", name);
    }

    /**
     * DE numbers every channel's fields ({@code f0_x}, {@code f0item2}); Continuum keeps one list per channel kind.
     */
    private static void pileCore(CompoundTag tag, Ctx ctx) {
        ordinalToName(tag, "orientation", new String[]{"NORTH_SOUTH", "EAST_WEST", "NEITHER"});
        tag.put("fuelChannels", pileChannels(tag, "f", "fc", List.of("heat", "neutrons")));
        tag.put("ventChannels", pileChannels(tag, "v", "vc", List.of("air")));
        tag.put("controlChannels", pileChannels(tag, "c", "cc", List.of("control")));
        tag.remove("members");
    }

    private static ListTag pileChannels(CompoundTag tag, String p, String count, List<String> fields) {
        ListTag out = new ListTag();
        int n = tag.getInt(count);
        for (int i = 0; i < n; i++) {
            CompoundTag channel = new CompoundTag();
            for (String axis : List.of("x", "y", "z")) channel.putInt(axis, tag.getInt(p + i + "_" + axis));
            int d = tag.getInt(p + i + "_d");
            channel.putString("dir", DIRECTIONS[Math.floorMod(d, 6)]);
            for (String field : fields) if (tag.contains(p + i + field)) channel.put(field, tag.get(p + i + field));
            ListTag rods = new ListTag();
            String itemPrefix = p + i + "item";
            for (String key : List.copyOf(tag.getAllKeys())) {
                if (!key.startsWith(itemPrefix) || !key.substring(itemPrefix.length()).matches("\\d+")) continue;
                CompoundTag rod = new CompoundTag();
                rod.putInt("slot", Integer.parseInt(key.substring(itemPrefix.length())));
                rod.put("item", tag.get(key));
                rods.add(rod);
                tag.remove(key);
            }
            if (!rods.isEmpty()) channel.put("rods", rods);
            out.add(channel);
            for (String axis : List.of("x", "y", "z", "d")) tag.remove(p + i + "_" + axis);
            for (String field : fields) tag.remove(p + i + field);
        }
        tag.remove(count);
        return out;
    }

    private static void radGen(CompoundTag tag, Ctx ctx) {
        ListTag out = new ListTag();
        for (Tag t : tag.getCompound("processing").getList("Items", Tag.TAG_COMPOUND)) {
            CompoundTag stack = (CompoundTag) t;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", stack.getByte("Slot") & 255);
            entry.put("stack", stripSlot(stack));
            out.add(entry);
        }
        if (!out.isEmpty()) tag.put("progressing", out);
        tag.remove("processing");
    }

    /**
     * DE's Base64 height map to Continuum's four bytes per int.
     */
    private static void radarMap(CompoundTag tag, Ctx ctx) {
        if (!tag.contains("map", Tag.TAG_STRING)) return;
        byte[] bytes = Base64.getDecoder().decode(tag.getString("map"));
        int[] out = new int[(bytes.length + 3) / 4];
        for (int i = 0; i < bytes.length; i++) out[i >> 2] |= (bytes[i] & 0xFF) << ((i & 3) * 8);
        tag.putIntArray("map", out);
    }

    private static void particleSource(CompoundTag tag, Ctx ctx) {
        if (!(tag.get("particle") instanceof CompoundTag particle)) return;
        int[] pos = particle.getIntArray("pos");
        if (pos.length == 3) {
            particle.putInt("x", pos[0]);
            particle.putInt("y", pos[1]);
            particle.putInt("z", pos[2]);
        }
        particle.remove("pos");
        if (particle.contains("dir", Tag.TAG_ANY_NUMERIC))
            particle.putString("dir", DIRECTIONS[Math.floorMod(particle.getInt("dir"), 6)]);
        tag.putBoolean("hasParticle", true);
    }

    /**
     * DE's pump keeps fluid, pressure, rate and stock loose; Continuum one tank {@code t} sized to the rate.
     */
    private static void pump(CompoundTag tag, Ctx ctx) {
        int rate = tag.getInt("rate");
        tag.putInt("buffer", rate);
        tag.putInt("t_max", rate);
        tag.putInt("t", (int) Math.min(tag.getLong("stock"), rate));
        tag.putString("t_type", tag.contains("type") ? Tables.get().fluid(tag.getString("type")) : "minecraft:empty");
        tag.putShort("t_p", (short) tag.getInt("pressure"));
        for (String key : List.of("rate", "stock", "type", "pressure")) tag.remove(key);
        ordinalToName(tag, "p", PRIORITY);
    }

    private static void radioTorch(CompoundTag tag, Ctx ctx) {
        renames(tag, "p", "isPolling", "m", "hasMapping", "l", "lastPower", "u", "lastTime", "c", "channel");
        for (int i = 0; i < 16; i++) rename(tag, "m" + i, "mapping" + i);
    }

    /**
     * DE names the concrete in a rebar by block id; Continuum holds that block's item.
     */
    private static void rebar(CompoundTag tag, Ctx ctx) {
        if (!tag.contains("block", Tag.TAG_STRING)) return;
        CompoundTag probe = new CompoundTag();
        probe.putString("Name", tag.getString("block"));
        String item = ctx.target.itemFor(BlockStates.convert(probe, ctx.target).entry().getString("Name"));
        if (item != null) {
            CompoundTag stack = new CompoundTag();
            stack.putString("id", item);
            stack.putInt("count", 1);
            tag.put("concrete", stack);
            tag.remove("block");
        }
    }

    private static void steelFurnace(CompoundTag tag, Ctx ctx) {
        CompoundTag last = tag.getCompound("lastItems");
        ListTag out = new ListTag();
        for (int i = 0; i < 3; i++) {
            if (!last.contains("lastItem" + i, Tag.TAG_COMPOUND)) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", i);
            entry.put("stack", last.getCompound("lastItem" + i));
            out.add(entry);
        }
        tag.put("lastItems", out);
    }

    /**
     * DE's gas centrifuge tank {@code {fill, stage}} to Continuum's {@code k} and upper-case {@code k_type}.
     */
    private static void pseudoTank(CompoundTag tag, String key) {
        if (!(tag.get(key) instanceof CompoundTag tank)) return;
        tag.putInt(key, tank.getInt("fill"));
        tag.putString(key + "_type",
                tank.contains("stage") ? tank.getString("stage").toUpperCase(Locale.ROOT) : "NONE");
    }

    /**
     * The stacks a block entity holds, for a refund when its block has no Continuum counterpart.
     */
    public static List<CompoundTag> contents(CompoundTag de) {
        List<CompoundTag> out = new ArrayList<>();
        collect(de, out);
        return out;
    }

    private static void collect(CompoundTag tag, List<CompoundTag> out) {
        for (String key : tag.getAllKeys()) {
            Tag child = tag.get(key);
            if (child instanceof CompoundTag c) {
                if (Walker.isStack(c)) out.add(c.copy());
                else collect(c, out);
            } else if (child instanceof ListTag list) {
                for (Tag e : list) {
                    if (e instanceof CompoundTag c) {
                        if (Walker.isStack(c)) out.add(stripSlot(c));
                        else collect(c, out);
                    }
                }
            }
        }
    }

    private static CompoundTag stripSlot(CompoundTag stack) {
        CompoundTag copy = stack.copy();
        copy.remove("Slot");
        return copy;
    }

    /**
     * DE nests {@code {fill, capacity, pressure, type, pin, held}}; Continuum writes {@code k, k_max, k_type, k_p}.
     */
    static void flattenTanks(CompoundTag tag) {
        for (String key : new ArrayList<>(tag.getAllKeys())) {
            if (!(tag.get(key) instanceof CompoundTag tank) || !tank.contains("fill") || !tank.contains("capacity"))
                continue;
            String fluid = !tank.getString("held").isEmpty() ? tank.getString("held")
                    : !tank.getString("type").isEmpty() ? tank.getString("type") : tank.getString("pin");
            int fill = tank.getInt("fill");
            tag.remove(key);
            tag.putInt(key, fluid.isEmpty() ? 0 : fill);
            tag.putInt(key + "_max", tank.getInt("capacity"));
            tag.putString(key + "_type", fluid.isEmpty() ? "minecraft:empty" : Tables.get().fluid(fluid));
            tag.putShort(key + "_p", (short) tank.getInt("pressure"));
        }
    }

    private static void rename(CompoundTag tag, String from, String to) {
        if (!tag.contains(from)) return;
        tag.put(to, tag.get(from));
        tag.remove(from);
    }

    /**
     * Continuum saves enums by lower-case name ({@code SavedIds.putEnum}) and reads them in any case.
     */
    private static void ordinalToName(CompoundTag tag, String key, String[] names) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) return;
        int i = tag.getInt(key);
        tag.putString(key, names[Math.floorMod(i, names.length)].toLowerCase(Locale.ROOT));
    }

    /**
     * As {@link #ordinalToName} in lower case; a negative ordinal (none) removes the key.
     */
    private static void ordinalToLower(CompoundTag tag, String key, String[] names) {
        if (!tag.contains(key, Tag.TAG_ANY_NUMERIC)) return;
        int i = tag.getInt(key);
        if (i < 0 || i >= names.length) tag.remove(key);
        else tag.putString(key, names[i]);
    }

    private static void renames(CompoundTag tag, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) rename(tag, pairs[i], pairs[i + 1]);
    }

    private static void remapSlots(CompoundTag tag, int[] to) {
        ListTag items = tag.getCompound("inventory").getList("Items", Tag.TAG_COMPOUND);
        for (Tag t : items) {
            CompoundTag stack = (CompoundTag) t;
            int slot = stack.getByte("Slot") & 255;
            if (slot < to.length) stack.putByte("Slot", (byte) to[slot]);
        }
    }

    /**
     * DE saves heat as longs under 1.7's names, the loaded fuel as an ordinal and ports and rods as position lists.
     * Continuum rebuilds the casing on assembly, so the reactor comes back unassembled with its rods fully in.
     */
    private static void pwr(CompoundTag tag, Ctx ctx) {
        for (String key : List.of("coreHeat", "hullHeat", "coreHeatCapacity")) {
            if (tag.contains(key, Tag.TAG_LONG)) {
                tag.putLong(key + "L", tag.getLong(key));
                tag.remove(key);
            }
        }
        if (tag.contains("typeLoaded", Tag.TAG_ANY_NUMERIC)) {
            int i = tag.getInt("typeLoaded");
            tag.putString("typeLoaded", i >= 0 && i < PWR_FUEL.length ? PWR_FUEL[i] : "");
        }
        tag.putBoolean("assembled", false);
        tag.putDouble("rodLevel", 100);
        tag.putDouble("rodTarget", 100);
        tag.remove("ports");
        tag.remove("rods");
        tag.remove("members");
        ctx.report.count("PWR reactors scrammed for reassembly");
    }
}
