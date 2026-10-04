package com.hbm.items;

import com.hbm.registration.Reg;
import com.hbm.registration.Presets;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.food.ItemAppleEuphemium;
import com.hbm.items.food.ItemAppleSchrabidium;
import com.hbm.items.food.ItemBDCL;
import com.hbm.items.food.ItemBowlFood;
import com.hbm.items.food.ItemCanteen;
import com.hbm.items.food.ItemConserve;
import com.hbm.items.food.ItemConserve.EnumFoodType;
import com.hbm.items.food.ItemCottonCandy;
import com.hbm.items.food.ItemFlask;
import com.hbm.items.food.ItemModFood;
import com.hbm.items.food.ItemMuchoMango;
import com.hbm.items.food.ItemPancake;
import com.hbm.items.food.ItemSchnitzelVegan;
import com.hbm.items.food.ItemTemFlakes;
import com.hbm.items.food.ItemWaffle;
import com.hbm.items.machine.ItemBattery;
import com.hbm.items.machine.ItemFELCrystal;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMuffler;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.ItemStamp.StampType;
import com.hbm.items.special.ItemCigarette;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.items.special.ItemEnergy;
import com.hbm.items.special.ItemPotatos;
import com.hbm.items.special.ItemRag;
import com.hbm.items.tool.ItemBalefireMatch;
import com.hbm.items.tool.ItemCraftingDegradation;
import com.hbm.items.tool.ItemFertilizer;
import com.hbm.items.tool.ItemFusionCore;
import com.hbm.items.tool.ItemMatch;
import com.hbm.items.tool.ItemPeas;
import com.hbm.items.tool.ItemRepairKit;
import com.hbm.sound.ModSounds;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatConsumables;
import dev.golem.core.world.Records;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * The final food, consumable, record, battery, loose-material and machine-upgrade wave, straight from 1.7
 * {@code ModItems}. Metadata families are flattened the port's way, {@code <base>_<variant>} in lower case:
 * {@code canned_conserve_beef}, {@code briquette_coal}, {@code item_secret_selenium_steel},
 * {@code circuit_star_piece_cpu_cache}, {@code stamp_book_printing1}, {@code parts_legendary_tier2},
 * {@code flask_infusion_shield}; the page family drops the doubled underscore ({@code page_of_page1}). The
 * numbered apple and tem-flake tiers keep the bare id for meta 0 and name the others by their crafting
 * material or tier ({@code apple_lead_ingot}, {@code tem_flakes_expensive}).
 *
 * <p>Items 1.7 put in a vanilla tab ({@code tabTools}: matches; {@code tabMisc}: records) go to the
 * consumables tab; the port's tabs are HBM's own. Items 1.7 hid ({@code setCreativeTab(null)}) stay hidden.
 */
public final class ModFinalConsumableItems {

    private ModFinalConsumableItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** Every id this class registers, in order: the harness and GameTests walk it. */
    public static final List<String> IDS = new ArrayList<>();

    private static <I extends Item> Reg.Handle<I> reg(String id, HbmTab tab, Function<Item.Properties, I> factory,
                                                      UnaryOperator<Item.Properties> props) {
        Reg.Handle<I> ro = REG.item(id, factory, props);
        if (tab != null) ModTabs.addTo(tab, ro);
        IDS.add(id);
        return ro;
    }

    private static Reg.Handle<Item> plain(String id, HbmTab tab, UnaryOperator<Item.Properties> props) {
        return reg(id, tab, Item::new, props);
    }

    // ------------------------------------------------------------------ food

    /** 1.7 {@code ItemFood(heal, saturation, wolf)}; {@code ItemFood(heal, wolf)} means 0.6 saturation. */
    private static UnaryOperator<Item.Properties> food(int nutrition, float saturation, boolean alwaysEdible) {
        return p -> alwaysEdible ? CompatConsumables.foodAlwaysEdible(p, nutrition, saturation, List.of())
                : CompatConsumables.food(p, nutrition, saturation, List.of());
    }

    private static UnaryOperator<Item.Properties> and(UnaryOperator<Item.Properties> a, UnaryOperator<Item.Properties> b) {
        return p -> b.apply(a.apply(p));
    }

    /** In a holder: {@code Rarity} is game state, and only the properties lambdas read it. */
    private static final class Rarities {
        static final Rarity[] APPLE_RARITY = {Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC};
    }

    /** Every tier of an apple shows the plain apple's sprite. */
    private static RegistryObject<ItemAppleSchrabidium> apple(String id, boolean lead, int tier, int nutrition,
                                                               float saturation) {
        return reg(id, HbmTab.CONSUMABLES, p -> new ItemAppleSchrabidium(p, lead, tier),
                and(food(nutrition, saturation, true), p -> p.rarity(Rarities.APPLE_RARITY[tier])))
                .transform(Presets.sprite(lead ? "hbm:items/apple_lead" : "hbm:items/apple_schrabidium"));
    }

    public static final RegistryObject<ItemAppleSchrabidium> apple_lead = apple("apple_lead", true, 0, 5, 0F);
    public static final RegistryObject<ItemAppleSchrabidium> apple_lead_ingot = apple("apple_lead_ingot", true, 1, 5, 0F);
    public static final RegistryObject<ItemAppleSchrabidium> apple_lead_block = apple("apple_lead_block", true, 2, 5, 0F);
    public static final RegistryObject<ItemAppleSchrabidium> apple_schrabidium = apple("apple_schrabidium", false, 0, 20, 100F);
    public static final RegistryObject<ItemAppleSchrabidium> apple_schrabidium_ingot = apple("apple_schrabidium_ingot", false, 1, 20, 100F);
    public static final RegistryObject<ItemAppleSchrabidium> apple_schrabidium_block = apple("apple_schrabidium_block", false, 2, 20, 100F);
    public static final RegistryObject<ItemAppleEuphemium> apple_euphemium = reg("apple_euphemium", null,
            ItemAppleEuphemium::new, and(food(20, 100F, true), p -> p.stacksTo(1).rarity(Rarity.EPIC)))
            .transform(Presets.sprite());

    private static RegistryObject<ItemBowlFood> soup(String id) {
        return reg(id, HbmTab.CONSUMABLES, ItemBowlFood::new,
                and(food(6, 0.6F, false), p -> CompatConsumables.usingConvertsTo(p.stacksTo(1), Items.BOWL)))
                .transform(Presets.sprite());
    }

    public static final RegistryObject<ItemBowlFood> glowing_stew = soup("glowing_stew");
    public static final RegistryObject<ItemBowlFood> balefire_scrambled = soup("balefire_scrambled");
    public static final RegistryObject<ItemBowlFood> balefire_and_ham = soup("balefire_and_ham");

    public static final RegistryObject<ItemWaffle> bomb_waffle = reg("bomb_waffle", HbmTab.CONSUMABLES,
            ItemWaffle::new, food(20, 0.6F, false))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemSchnitzelVegan> schnitzel_vegan = reg("schnitzel_vegan", HbmTab.CONSUMABLES,
            ItemSchnitzelVegan::new, food(0, 0.6F, false))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCottonCandy> cotton_candy = reg("cotton_candy", HbmTab.CONSUMABLES,
            ItemCottonCandy::new, food(5, 0.6F, true))
            .transform(Presets.handheld());
    public static final RegistryObject<ItemModFood> ingot_smore = reg("ingot_smore", HbmTab.MATERIALS,
            ItemModFood::new, food(10, 20F, false))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemMuchoMango> mucho_mango = reg("mucho_mango", HbmTab.CONSUMABLES,
            ItemMuchoMango::new, food(10, 0.6F, true))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPancake> pancake = reg("pancake", HbmTab.CONSUMABLES,
            ItemPancake::new, food(20, 20F, true))
            .transform(Presets.sprite());

    public static final RegistryObject<ItemTemFlakes> tem_flakes = reg("tem_flakes", HbmTab.CONSUMABLES,
            p -> new ItemTemFlakes(p, 0), food(0, 0F, true))
            .transform(Presets.sprite("hbm:items/tem_flakes"));
    public static final RegistryObject<ItemTemFlakes> tem_flakes_normal = reg("tem_flakes_normal", HbmTab.CONSUMABLES,
            p -> new ItemTemFlakes(p, 1), food(0, 0F, true))
            .transform(Presets.sprite("hbm:items/tem_flakes"));
    public static final RegistryObject<ItemTemFlakes> tem_flakes_expensive = reg("tem_flakes_expensive", HbmTab.CONSUMABLES,
            p -> new ItemTemFlakes(p, 2), food(0, 0F, true))
            .transform(Presets.sprite("hbm:items/tem_flakes"));

    private static final Map<EnumFoodType, RegistryObject<ItemConserve>> CANNED = new EnumMap<>(EnumFoodType.class);

    static {
        for (EnumFoodType type : EnumFoodType.values()) {
            String id = "canned_conserve_" + type.name().toLowerCase(java.util.Locale.US);
            // ItemConserve names its sprites canned_<constant>
            CANNED.put(type, reg(id, HbmTab.CONSUMABLES, p -> new ItemConserve(p, type),
                    food(type.foodLevel, type.saturation, false))
                    .transform(Presets.sprite("hbm:items/canned_" + type.name().toLowerCase(java.util.Locale.US))));
        }
    }

    public static Item canned(EnumFoodType type) {
        return CANNED.get(type).get();
    }

    public static final RegistryObject<ItemCanteen> canteen_vodka = reg("canteen_vodka", HbmTab.CONSUMABLES,
            ItemCanteen::new, p -> p.durability(3 * 60))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemEnergy> chocolate_milk = reg("chocolate_milk", HbmTab.CONSUMABLES,
            p -> new ItemEnergy(p, "chocolate_milk", ItemEnergy.Container.NONE), p -> p)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCigarette> cigarette = reg("cigarette", HbmTab.CONSUMABLES,
            ItemCigarette::new, p -> p.stacksTo(16))
            .transform(Presets.handheld());
    public static final RegistryObject<ItemCigarette> crackpipe = reg("crackpipe", HbmTab.CONSUMABLES,
            ItemCigarette::new, p -> p.stacksTo(1))
            .transform(Presets.handheld());
    public static final RegistryObject<ItemFlask> flask_infusion_shield = reg("flask_infusion_shield",
            HbmTab.CONSUMABLES, ItemFlask::new, p -> p)
            .transform(Presets.sprite("hbm:items/flask_shield"));
    public static final RegistryObject<ItemBDCL> bdcl = reg("bdcl", HbmTab.CONSUMABLES, ItemBDCL::new, p -> p)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPeas> peas = reg("peas", HbmTab.CONSUMABLES, ItemPeas::new, p -> p)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemMatch> matchstick = reg("matchstick", HbmTab.CONSUMABLES,
            ItemMatch::new, p -> p)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemBalefireMatch> balefire_and_steel = reg("balefire_and_steel",
            HbmTab.CONSUMABLES, ItemBalefireMatch::new, p -> p.durability(256))
            .transform(Presets.handheld());
    public static final RegistryObject<ItemRepairKit> gun_kit_1 = reg("gun_kit_1", HbmTab.CONSUMABLES,
            p -> new ItemRepairKit(p, () -> ModSounds.SPRAY.get()), p -> p.durability(10 - 1))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemRepairKit> gun_kit_2 = reg("gun_kit_2", HbmTab.CONSUMABLES,
            p -> new ItemRepairKit(p, () -> ModSounds.REPAIR.get()), p -> p.durability(100 - 1))
            .transform(Presets.sprite());

    // ------------------------------------------------------------------ records

    /** Song path -> (sound, length in seconds). Lengths are the Ogg streams' own. */
    public static final Map<String, RegistryObject<Item>> RECORDS = new LinkedHashMap<>();

    private static RegistryObject<Item> record(String id, String song, Supplier<SoundEvent> sound, int seconds,
                                               HbmTab tab) {
        // 1.7 jukeboxes output (item id + 1 - record_13's id) on a comparator, which for a mod item is far
        // above 15 and so always saturates.
        RegistryObject<Item> ro = reg(id, tab,
                p -> Records.disc(p, Constants.MOD_ID, song, sound, 15, seconds), p -> p.rarity(Rarity.RARE))
                .transform(Presets.sprite());
        RECORDS.put(song, ro);
        return ro;
    }

    public static final RegistryObject<Item> record_lc = record("record_lc", "lc", () -> ModSounds.RECORD_LAMBDA_CORE.get(), 105, HbmTab.CONSUMABLES);
    public static final RegistryObject<Item> record_ss = record("record_ss", "ss", () -> ModSounds.RECORD_SECTOR_SWEEP.get(), 167, HbmTab.CONSUMABLES);
    public static final RegistryObject<Item> record_vc = record("record_vc", "vc", () -> ModSounds.RECORD_VORTAL_COMBAT.get(), 195, HbmTab.CONSUMABLES);
    public static final RegistryObject<Item> record_glass = record("record_glass", "glass", () -> ModSounds.MUSIC_TRANSMISSION.get(), 63, null);

    // ------------------------------------------------------------------ batteries

    public static final RegistryObject<ItemBattery> battery_potato = reg("battery_potato", HbmTab.CONTROL,
            p -> new ItemBattery(p, 1000, 0, 100), p -> p.stacksTo(1))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPotatos> battery_potatos = reg("battery_potatos", HbmTab.CONTROL,
            p -> new ItemPotatos(p, 500000, 0, 100), p -> p.stacksTo(1))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemBattery> cube_power = reg("cube_power", HbmTab.CONTROL,
            p -> new ItemBattery(p, 1000000000000000000L, 1000000000000000L, 1000000000000000L), p -> p.stacksTo(1))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemFusionCore> fusion_core = reg("fusion_core", HbmTab.CONTROL,
            p -> new ItemFusionCore(p, 2500000), p -> p.stacksTo(1))
            .transform(Presets.sprite());
    /** 1.7 {@code ModItems.hev_battery}: {@code new ItemFusionCore(150000)}, stack size 4, control tab. */
    public static final RegistryObject<ItemFusionCore> hev_battery = reg("hev_battery", HbmTab.CONTROL,
            p -> new ItemFusionCore(p, 150000), p -> p.stacksTo(4))
            .transform(Presets.sprite());

    // ------------------------------------------------------------------ loose materials

    public static final RegistryObject<ItemRag> rag = reg("rag", HbmTab.MATERIALS, ItemRag::new, p -> p)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemFertilizer> powder_fertilizer = reg("powder_fertilizer", HbmTab.MATERIALS,
            ItemFertilizer::new, p -> p)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCustomLore> ingot_electronium = reg("ingot_electronium", HbmTab.MATERIALS,
            ItemCustomLore::new, p -> p)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemFELCrystal> laser_crystal_dnt = reg("laser_crystal_dnt", HbmTab.CONTROL,
            p -> new ItemFELCrystal(p, ItemFELCrystal.EnumWavelengths.GAMMA, false), p -> p)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCraftingDegradation> chemistry_set = reg("chemistry_set", HbmTab.CONTROL,
            ItemCraftingDegradation::new, p -> p.durability(100))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCraftingDegradation> chemistry_set_boron = reg("chemistry_set_boron",
            HbmTab.CONTROL, ItemCraftingDegradation::new, p -> p.stacksTo(1))
            .transform(Presets.sprite());

    public static final RegistryObject<Item> parts_legendary_tier1 = plain("parts_legendary_tier1", HbmTab.MATERIALS, p -> p)
            .transform(Presets.sprite("hbm:items/parts_legendary.tier1"));
    public static final RegistryObject<Item> parts_legendary_tier2 = plain("parts_legendary_tier2", HbmTab.MATERIALS, p -> p)
            .transform(Presets.sprite("hbm:items/parts_legendary.tier2"));
    public static final RegistryObject<Item> parts_legendary_tier3 = plain("parts_legendary_tier3", HbmTab.MATERIALS, p -> p)
            .transform(Presets.sprite("hbm:items/parts_legendary.tier3"));

    public static final RegistryObject<Item> briquette_coal = plain("briquette_coal", HbmTab.MATERIALS, p -> p)
            .transform(Presets.sprite("hbm:items/briquette.coal"));
    public static final RegistryObject<Item> briquette_lignite = plain("briquette_lignite", HbmTab.MATERIALS, p -> p)
            .transform(Presets.sprite("hbm:items/briquette.lignite"));
    public static final RegistryObject<Item> briquette_wood = plain("briquette_wood", HbmTab.MATERIALS, p -> p)
            .transform(Presets.sprite("hbm:items/briquette.wood"));

    static {
        // 1.7 FuelHandler: single = 200 ticks; coal x10, lignite x8, sawdust x2
        ModFuelItems.BURN_TIMES.put(briquette_coal, 2000);
        ModFuelItems.BURN_TIMES.put(briquette_lignite, 1600);
        ModFuelItems.BURN_TIMES.put(briquette_wood, 400);
    }

    /** 1.7 {@code EnumSecretType}; hidden from the tabs as in 1.7 ({@code NEIConfig} hid them too). */
    public static final String[] SECRETS = {"canister", "controller", "selenium_steel", "aberrator", "folly"};
    /** 1.7 {@code ItemPlasticScrap.ScrapType}, in enum order. */
    public static final String[] SCRAPS = {"board_blank", "board_transistor", "board_converter", "bridge_north",
            "bridge_south", "bridge_io", "bridge_bus", "bridge_chipset", "bridge_cmos", "bridge_bios", "cpu_register",
            "cpu_clock", "cpu_logic", "cpu_cache", "cpu_ext", "cpu_socket", "mem_socket", "mem_16k_a", "mem_16k_b",
            "mem_16k_c", "mem_16k_d", "card_board", "card_processor"};
    /** 1.7 {@code ItemCircuitStarComponent.CircuitComponentType}. */
    public static final String[] STAR_COMPONENTS = {"chipset", "cpu", "ram", "card"};

    public static final Map<String, RegistryObject<? extends Item>> FLAT = new LinkedHashMap<>();

    static {
        // the 1.7 metadata sprites keep their dotted names: <base>.<subtype>
        for (String s : SECRETS) FLAT.put("item_secret_" + s, plain("item_secret_" + s, null, p -> p)
                .transform(Presets.sprite("hbm:items/item_secret." + s)));
        for (String s : SCRAPS) FLAT.put("circuit_star_piece_" + s, plain("circuit_star_piece_" + s, null, p -> p)
                .transform(Presets.sprite("hbm:items/circuit_star_piece." + s)));
        // 1.7 ItemPlasticScrap: ScrapType metadata, one sprite, one name ("Plastic Scraps"), no creative tab
        for (String s : SCRAPS) FLAT.put("scrap_plastic_" + s, plain("scrap_plastic_" + s, null, p -> p)
                .transform(Presets.sprite("hbm:items/scrap_plastic")));
        for (String s : STAR_COMPONENTS) {
            FLAT.put("circuit_star_component_" + s, plain("circuit_star_component_" + s, null, p -> p)
                    .transform(Presets.sprite("hbm:items/circuit_star_component." + s)));
        }
        // page_of_ is single-texture (multiTexture false); ItemStampBook draws every plate with stamp_book
        for (int i = 1; i <= 8; i++) FLAT.put("page_of_page" + i, plain("page_of_page" + i, null, p -> p.stacksTo(1))
                .transform(Presets.sprite("hbm:items/page_of_")));
        for (int i = 1; i <= 8; i++) {
            StampType type = StampType.valueOf("PRINTING" + i);
            String id = "stamp_book_printing" + i;
            // 1.7 ItemStampBook is an ItemStamp(0, null): unbreakable
            FLAT.put(id, reg(id, null, p -> new ItemStamp(p, type), p -> p.stacksTo(1))
                    .transform(Presets.sprite("hbm:items/stamp_book")));
        }
    }

    public static Item flat(String id) {
        return FLAT.get(id).get();
    }

    // ------------------------------------------------------------------ upgrades

    public static final RegistryObject<ItemMachineUpgrade> upgrade_5g = reg("upgrade_5g", HbmTab.CONTROL,
            p -> new ItemMachineUpgrade(ItemMachineUpgrade.UpgradeType.SPECIAL, 0, p), p -> p.stacksTo(1))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemMuffler> upgrade_muffler = reg("upgrade_muffler", HbmTab.MATERIALS,
            ItemMuffler::new, p -> p)
            .transform(Presets.sprite());

    public static void init() {}
}
