package com.hbm.handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.Constants;
import com.hbm.api.data.ArmorDataMaps;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.items.armor.ItemModCladding;
import com.hbm.util.ShadyUtil;
import dev.golem.core.Ids;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;

import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;
import net.minecraft.world.item.Items;

/**
 * Per-armor-item radiation-resistance coefficients, ported from 1.12.2's {@code HazmatRegistry}. The
 * total per-entity resistance is the sum over the 4 worn pieces; the radiation reduction factor is
 * {@code 10^-resistance} (see {@code ContaminationUtil.calculateRadiationMod}).
 *
 * <p>Coefficients copied: hazmat yellow 0.6, red 1.0, grey 2.0 (per set, split over the 4
 * slots by the {@code helmet .2 / chest .4 / legs .3 / boots .1} weights); vanilla iron/gold 0.0225.
 * Registration happens where the armor items are created ({@code ModGearItems}). The cladding modules and the
 * Rad-X bonus are below; {@link #registerHazmats} is 1.7's {@code hbmRadResist.json} override.
 */
public class HazmatRegistry {

    public static final double helmet = 0.2D;
    public static final double chest = 0.4D;
    public static final double legs = 0.3D;
    public static final double boots = 0.1D;

    // Set coefficients (multiply by the slot weight when registering a piece).
    public static final double iron = 0.0225D;      // 5%
    public static final double gold = 0.0225D;      // 5%
    public static final double hazYellow = 0.6D;    // 50%
    public static final double hazRed = 1.0D;       // 90%
    public static final double hazGray = 2D;        // 99%
    public static final double liquidator = 2.4D;   // 99.6%
    public static final double security = 0.825D;   // 85%
    public static final double star = 1.0D;         // 90%
    public static final double schrab = 3D;         // 99.9%
    public static final double euph = 10D;          // <100%
    public static final double cobalt = 0.125D;     // 25%
    public static final double paa = 1.7D;          // 97%

    /** Code-registered resistances, below the data map's ({@link com.hbm.api.data.ArmorDataMaps#RADIATION_RESISTANCE}). */
    private static final HashMap<Item, Double> entries = new HashMap<>();

    /** {@code config/hbmConfig/hbmRadResist.json}: per-item overrides above everything else. */
    private static volatile Map<Item, Double> configOverrides = Map.of();

    public static void registerHazmat(Item item, double resistance) {
        entries.put(item, resistance);
    }

    /** The code-registered resistances (read-only view). */
    public static Map<Item, Double> codeEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public static double getResistance(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return 0;

        double cladding = getCladding(stack);
        Double f = rawResistance(stack.getItem());
        return f != null ? f + cladding : cladding;
    }

    /** A piece's own resistance: the config override, else the data map, else a code registration; null if none. */
    public static Double rawResistance(Item item) {
        Double f = configOverrides.get(item);
        if (f == null) f = ArmorDataMaps.RADIATION_RESISTANCE.get(item);
        if (f == null) f = entries.get(item);
        return f;
    }

    /** Returns radiation cladding from the ordered armor-module component, if installed. */
    public static double getCladding(ItemStack stack) {
        ItemStack mod = ArmorModHandler.pryMod(stack, ArmorModHandler.cladding);
        return mod.getItem() instanceof ItemModCladding cladding ? cladding.rad : 0D;
    }

    /**
     * 1.7 {@code HazmatRegistry.registerHazmats}, run once every default is registered: with no
     * {@code config/hbmConfig/hbmRadResist.json} it writes the defaults to {@code _hbmRadResist.json} as a template;
     * with one, that file's entries override the items it names. 1.7's file replaced the whole table, which also
     * dropped every resistance another mod had registered; the defaults are a data map, so the config is an
     * override layer above it (an item set to 0 there has none).
     */
    public static void registerHazmats() {
        Path folder = CustomMachineConfigJSON.configHbmDir();
        Path config = folder.resolve("hbmRadResist.json");
        if (!Files.exists(config)) {
            writeDefault(folder.resolve("_hbmRadResist.json"));
        } else {
            HashMap<Item, Double> conf = readConfig(config);
            if (conf != null) configOverrides = Map.copyOf(conf);
        }
    }

    private static void writeDefault(Path file) {
        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
             JsonWriter writer = new JsonWriter(out)) {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("comment").value("Template file, remove the underscore ('_') from the name to enable the config.");
            writer.name("entries").beginArray();
            Map<String, Double> defaults = new TreeMap<>(ArmorDataMaps.RADIATION_RESISTANCE.byId());
            entries.forEach((item, value) -> defaults.putIfAbsent(
                    BuiltInRegistries.ITEM.getKey(item).toString(), value));
            for (Map.Entry<String, Double> entry : defaults.entrySet()) {
                writer.beginObject();
                writer.name("item").value(entry.getKey());
                writer.name("resistance").value(entry.getValue());
                writer.endObject();
            }
            writer.endArray();
            writer.endObject();
        } catch (IOException e) {
            Constants.LOG.error("Unable to write {}", file, e);
        }
    }

    private static HashMap<Item, Double> readConfig(Path config) {
        try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            HashMap<Item, Double> conf = new HashMap<>();
            for (JsonElement element : json.get("entries").getAsJsonArray()) {
                try {
                    JsonObject object = element.getAsJsonObject();
                    String name = object.get("item").getAsString();
                    // 1.7 names were "hbm:x" / "minecraft:x" as well; a bare name is read as minecraft's.
                    Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(name));
                    double resistance = object.get("resistance").getAsDouble();
                    if (item != null && item != Items.AIR) {
                        conf.put(item, resistance);
                    } else {
                        Constants.LOG.error("Tried loading unknown item {} for hazmat entry.", name);
                    }
                } catch (Exception ex) {
                    Constants.LOG.error("Encountered {} trying to read hazmat entry {}", ex, element);
                }
            }
            return conf;
        } catch (Exception ex) {
            Constants.LOG.error("Unable to read {}", config, ex);
        }
        return null;
    }

    public static float getResistance(LivingEntity entity) {
        float res = 0.0F;

        // 1.7: Pu_238 carries 0.4 of his own
        if (ShadyUtil.is(entity.getUUID(), ShadyUtil.Pu_238)) res += 0.4F;

        if (entity instanceof Player player) {
            res += (float) getResistance(player.getItemBySlot(EquipmentSlot.HEAD));
            res += (float) getResistance(player.getItemBySlot(EquipmentSlot.CHEST));
            res += (float) getResistance(player.getItemBySlot(EquipmentSlot.LEGS));
            res += (float) getResistance(player.getItemBySlot(EquipmentSlot.FEET));
        }

        if (Effects.hasRegistered(entity, ModEffects.RADX.get())) res += 0.2F;

        return res;
    }
}
