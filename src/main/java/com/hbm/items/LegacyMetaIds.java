package com.hbm.items;

import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.blocks.DecoCrateDeclarations;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.blocks.generic.BlockSnowglobe;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.items.machine.ItemChemicalDye;
import com.hbm.items.machine.ItemDrive;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.items.machine.ModMoldItems;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.items.food.ItemConserve;
import com.hbm.items.special.ItemHolotapeImage;
import com.hbm.items.special.ItemWasteStorage;
import com.hbm.items.tool.ItemGuideBook;
import com.hbm.items.weapon.ItemAmmoArty;
import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.world.item.Item;

/**
 * 1.7's {@code (item, metadata)} pairs of the metadata families that are one registered item per type here:
 * the 1.7 item id and its metadata answer the port's item id. For readers of 1.7 data that still
 * name items that way (the QMAW manual pages), never for saves.
 */
public final class LegacyMetaIds {

    private LegacyMetaIds() {}

    private static volatile @Nullable Map<String, String[]> table;

    /** The port id ({@code hbm:...}) of 1.7 {@code hbm:<path>} at {@code metadata}, or null if it is no split family. */
    public static @Nullable String lookup(String path, int metadata) {
        String[] ids = table().get(path);
        if (ids == null) return null;
        if (metadata < 0 || metadata >= ids.length || ids[metadata] == null) return null;
        return "hbm:" + ids[metadata];
    }

    /** Every port id ({@code hbm:...}) of the split family 1.7 called {@code hbm:<path>}, or an empty list. */
    public static List<String> members(String path) {
        String[] ids = table().get(path);
        if (ids == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String id : ids) if (id != null) out.add("hbm:" + id);
        return out;
    }

    /** Whether {@code path} names a 1.7 metadata family that is split into items here. */
    public static boolean isSplit(String path) {
        return table().containsKey(path);
    }

    private static Map<String, String[]> table() {
        Map<String, String[]> t = table;
        if (t != null) return t;
        t = new HashMap<>();
        t.put("cart", family(ModEntityItems.cart));
        t.put("train", family(ModEntityItems.train));
        t.put("circuit", lower("circuit", ItemEnums.EnumCircuitType.VALUES));
        t.put("item_expensive", lower("item_expensive", ItemEnums.EnumExpensiveType.VALUES));
        t.put("part_generic", lower("part_generic", ItemEnums.EnumPartType.values()));
        t.put("chunk_ore", lower("chunk_ore", ItemEnums.EnumChunkType.VALUES));
        t.put("drive", lower("drive", ItemDrive.EnumDriveType.values()));
        t.put("rod_zirnox", names(ZirnoxFuel.values(), fuel -> "rod_zirnox_" + fuel.id));
        for (var size : ItemBreedingRod.RodSize.values())
            t.put(size.base, names(ItemBreedingRod.BreedingRodType.values(), type -> size.base + "_" + type.id));
        t.put("watz_pellet", names(EnumWatzType.VALUES, type -> "watz_pellet_" + type.itemSuffix()));
        t.put("watz_pellet_depleted", names(EnumWatzType.VALUES, type -> "watz_pellet_depleted_" + type.itemSuffix()));
        t.put("mold", indexed(ModMoldItems.legacyIds()));
        t.put("fluid_tank_empty", new String[]{"fluid_tank"});
        t.put("fluid_barrel_empty", new String[]{"fluid_barrel"});
        t.put("pwr_fuel", lower("pwr_fuel", EnumPWRFuel.VALUES));
        t.put("pwr_fuel_hot", lower("pwr_fuel_hot", EnumPWRFuel.VALUES));
        t.put("pwr_fuel_depleted", lower("pwr_fuel_depleted", EnumPWRFuel.VALUES));
        t.put("chemical_dye", lower("chemical_dye", ItemChemicalDye.EnumChemDye.VALUES));
        t.put("crayon", lower("crayon", ItemChemicalDye.EnumChemDye.VALUES));
        t.put("canned_conserve", lower("canned_conserve", ItemConserve.EnumFoodType.values()));
        t.put("battery_pack", names(ItemBatteryPack.EnumBatteryPack.VALUES, pack -> pack.name));
        t.put("battery_sc", names(ItemBatterySC.EnumBatterySC.VALUES, ItemBatterySC.EnumBatterySC::id));
        t.put("ammo_standard", names(GunFactory.EnumAmmo.values(), ammo -> ModAmmoItems.standardAmmoId(ammo.name())));
        t.put("ammo_secret", lower("ammo_secret", GunFactory.EnumAmmoSecret.values()));
        t.put("ammo_arty", names(ItemAmmoArty.ArtilleryShellType.values(), c -> c.name));
        t.put("ammo_himars", names(ItemAmmoHIMARS.HIMARSRocketType.values(), c -> "ammo_himars_" + c.name));
        t.put("satellite", lower("satellite", ItemSatellite.EnumSatType.values()));
        t.put("missile_soyuz", new String[] {"missile_soyuz_0", "missile_soyuz_1", "missile_soyuz_2"});
        t.put("pile_rod", lower("pile_rod_mk2", ItemPileRodMK2.EnumPileRod.values()));
        t.put("pa_coil", lower("pa_coil", ItemPACoil.EnumCoilType.values()));
        t.put("holotape_image", lower("holotape_image", ItemHolotapeImage.EnumHoloImage.values()));
        t.put("book_guide", lower("book_guide", ItemGuideBook.BookType.values()));
        t.put("weapon_mod_test", lower("weapon_mod_test", GunFactory.EnumModTest.values()));
        t.put("weapon_mod_generic", lower("weapon_mod_generic", GunFactory.EnumModGeneric.values()));
        t.put("weapon_mod_special", lower("weapon_mod_special", GunFactory.EnumModSpecial.values()));
        t.put("weapon_mod_caliber", lower("weapon_mod_caliber", GunFactory.EnumModCaliber.values()));
        t.put("clay_tablet", new String[] {"clay_tablet", "clay_tablet_1"});
        t.put("blueprint_folder", new String[] {"blueprint_folder", "blueprint_folder_discover"});
        t.put("gear_large", new String[] {"gear_large", "gear_large_steel"});
        t.put("ammo_container", new String[] {"ammo_container", "ammo_container_alt"});
        t.put("ingot_u238m2", new String[] {"ingot_u238m2", "hs_elements", "hs_arsenic", "hs_vault"});
        t.put("concrete_colored", prefixed("concrete_", "white", "orange", "magenta", "light_blue", "yellow", "lime",
                "pink", "gray", "silver", "cyan", "purple", "blue", "brown", "green", "red", "black"));
        t.put("concrete_colored_ext", prefixed("concrete_ext_", "machine", "machine_stripe", "indigo", "purple", "pink",
                "hazard", "sand", "bronze"));
        t.put("lightstone", prefixed("lightstone_", "unrefined", "tile", "bricks", "bricks_chiseled", "chiseled"));
        t.put("steel_scaffold", new String[] {"steel_scaffold", "steel_scaffold_red", "steel_scaffold_white",
                "steel_scaffold_yellow"});
        t.put("pile_device", new String[] {"pile_loader", "pile_vent", "pile_control"});
        t.put("wand_structure", new String[] {"wand_structure", "wand_structure_load"});
        t.put("bobblehead", DecoCrateDeclarations.trinketIds("bobblehead", BlockBobble.BobbleType.VALUES));
        t.put("plushie", DecoCrateDeclarations.trinketIds("plushie", BlockPlushie.PlushieType.VALUES));
        t.put("snowglobe", DecoCrateDeclarations.trinketIds("snowglobe", BlockSnowglobe.SnowglobeType.VALUES));
        t.put("vending_machine", new String[] {"vending_machine", "vending_machine_snacks"});
        t.put("filing_cabinet", new String[] {"filing_cabinet_green", "filing_cabinet_steel"});
        t.put("glyphid_base", new String[] {"glyphid_base", "glyphid_base_infested", "glyphid_base_rad"});
        t.put("glyphid_spawner", new String[] {"glyphid_spawner", "glyphid_spawner_infested", "glyphid_spawner_rad"});
        for (String base : new String[] {"nuclear_waste_long", "nuclear_waste_long_tiny", "nuclear_waste_long_depleted",
                "nuclear_waste_long_depleted_tiny"}) {
            t.put(base, lower(base, ItemWasteStorage.LongWasteClass.values()));
        }
        for (String base : new String[] {"nuclear_waste_short", "nuclear_waste_short_tiny", "nuclear_waste_short_depleted",
                "nuclear_waste_short_depleted_tiny"}) {
            t.put(base, lower(base, ItemWasteStorage.ShortWasteClass.values()));
        }
        table = t;
        return t;
    }

    private static <E extends Enum<E>> String[] names(E[] values, Function<E, String> id) {
        String[] ids = new String[values.length];
        for (int i = 0; i < values.length; i++) ids[i] = id.apply(values[i]);
        return ids;
    }

    private static <E extends Enum<E>, I extends Item> String[] family(ItemFamily<E, I> family) {
        String[] ids = new String[family.type().getEnumConstants().length];
        for (E kind : family.types()) ids[kind.ordinal()] = family.handle(kind).getId().getPath();
        return ids;
    }

    private static String[] indexed(Map<Integer, String> values) {
        String[] ids = new String[values.keySet().stream().mapToInt(Integer::intValue).max().orElseThrow() + 1];
        values.forEach((meta, id) -> ids[meta] = id);
        return ids;
    }

    private static String[] lower(String base, Enum<?>[] values) {
        String[] ids = new String[values.length];
        for (int i = 0; i < values.length; i++) ids[i] = base + "_" + values[i].name().toLowerCase(Locale.ROOT);
        return ids;
    }

    private static String[] prefixed(String prefix, String... names) {
        String[] ids = new String[names.length];
        for (int i = 0; i < names.length; i++) ids[i] = prefix + names[i];
        return ids;
    }
}
