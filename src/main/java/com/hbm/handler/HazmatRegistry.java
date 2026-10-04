package com.hbm.handler;

import java.util.HashMap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;

/**
 * Per-armor-item radiation-resistance coefficients, ported from 1.12.2's {@code HazmatRegistry}. The
 * total per-entity resistance is the sum over the 4 worn pieces; the radiation reduction factor is
 * {@code 10^-resistance} (see {@code ContaminationUtil.calculateRadiationMod}).
 *
 * <p>Coefficients copied verbatim: hazmat yellow 0.6, red 1.0, grey 2.0 (per set — split over the 4
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

    private static final HashMap<Item, Double> entries = new HashMap<>();

    public static void registerHazmat(Item item, double resistance) {
        entries.put(item, resistance);
    }

    public static double getResistance(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return 0;

        double cladding = getCladding(stack);
        Double f = entries.get(stack.getItem());
        return f != null ? f + cladding : cladding;
    }

    /** Returns radiation cladding from the ordered armor-module component, if installed. */
    public static double getCladding(ItemStack stack) {
        ItemStack mod = ArmorModHandler.pryMod(stack, ArmorModHandler.cladding);
        return mod.getItem() instanceof com.hbm.items.armor.ItemModCladding cladding ? cladding.rad : 0D;
    }

    /**
     * 1.7 {@code HazmatRegistry.registerHazmats}, run once every default is registered: with no
     * {@code config/hbmConfig/hbmRadResist.json} it writes the defaults to {@code _hbmRadResist.json} as a template;
     * with one, that file's entries replace the whole table (an item it leaves out has no resistance).
     */
    public static void registerHazmats() {
        java.nio.file.Path folder = com.hbm.config.CustomMachineConfigJSON.configHbmDir();
        java.nio.file.Path config = folder.resolve("hbmRadResist.json");
        if (!java.nio.file.Files.exists(config)) {
            writeDefault(folder.resolve("_hbmRadResist.json"));
        } else {
            HashMap<Item, Double> conf = readConfig(config);
            if (conf != null) {
                entries.clear();
                entries.putAll(conf);
            }
        }
    }

    private static void writeDefault(java.nio.file.Path file) {
        try (java.io.Writer out = java.nio.file.Files.newBufferedWriter(file, java.nio.charset.StandardCharsets.UTF_8);
             com.google.gson.stream.JsonWriter writer = new com.google.gson.stream.JsonWriter(out)) {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("comment").value("Template file, remove the underscore ('_') from the name to enable the config.");
            writer.name("entries").beginArray();
            for (java.util.Map.Entry<Item, Double> entry : entries.entrySet()) {
                writer.beginObject();
                writer.name("item").value(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.getKey()).toString());
                writer.name("resistance").value(entry.getValue());
                writer.endObject();
            }
            writer.endArray();
            writer.endObject();
        } catch (java.io.IOException e) {
            com.hbm.Constants.LOG.error("Unable to write {}", file, e);
        }
    }

    private static HashMap<Item, Double> readConfig(java.nio.file.Path config) {
        try (java.io.Reader reader = java.nio.file.Files.newBufferedReader(config, java.nio.charset.StandardCharsets.UTF_8)) {
            com.google.gson.JsonObject json = new com.google.gson.Gson().fromJson(reader, com.google.gson.JsonObject.class);
            HashMap<Item, Double> conf = new HashMap<>();
            for (com.google.gson.JsonElement element : json.get("entries").getAsJsonArray()) {
                try {
                    com.google.gson.JsonObject object = element.getAsJsonObject();
                    String name = object.get("item").getAsString();
                    // 1.7 names were "hbm:x" / "minecraft:x" as well; a bare name is read as minecraft's.
                    Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(dev.golem.core.Ids.parse(name));
                    double resistance = object.get("resistance").getAsDouble();
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        conf.put(item, resistance);
                    } else {
                        com.hbm.Constants.LOG.error("Tried loading unknown item {} for hazmat entry.", name);
                    }
                } catch (Exception ex) {
                    com.hbm.Constants.LOG.error("Encountered {} trying to read hazmat entry {}", ex, element);
                }
            }
            return conf;
        } catch (Exception ex) {
            com.hbm.Constants.LOG.error("Unable to read {}", config, ex);
        }
        return null;
    }

    public static float getResistance(LivingEntity entity) {
        float res = 0.0F;

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
