package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.MKUCraftingRecipe;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModEntityItems;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.machine.ItemFluidDuct;
import com.hbm.items.machine.ItemFluidIDMulti;
import com.hbm.items.machine.ItemScraps;
import com.hbm.items.machine.ItemZirnoxWaste;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.items.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Ingredients;
import dev.golem.core.world.Recipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fix wave phase 1B (2026-10-03, History §19ej-1b): crafting/smelting recipes and 1.7 IRecipe handlers restored from
 * original-hbm (1.7.10). Expected values are the 1.7 recipe outputs.
 */
public final class Fix1BGameTests {

    private Fix1BGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function RESTORED =
            TESTS.function("fix1b_restored_recipes", 20, () -> Fix1BGameTests::restored);
    public static final GameTests.Function MISMATCHES =
            TESTS.function("fix1b_mismatches", 20, () -> Fix1BGameTests::mismatches);
    public static final GameTests.Function SMELTING =
            TESTS.function("fix1b_smelting", 20, () -> Fix1BGameTests::smelting);
    public static final GameTests.Function GRENADE =
            TESTS.function("fix1b_grenade_handler", 20, () -> Fix1BGameTests::grenade);
    public static final GameTests.Function CARGO =
            TESTS.function("fix1b_cargo_shell_handler", 20, () -> Fix1BGameTests::cargo);
    public static final GameTests.Function MKU =
            TESTS.function("fix1b_mku_handler", 20, () -> Fix1BGameTests::mku);
    public static final GameTests.Function SCRAPS =
            TESTS.function("fix1b_scraps_handlers", 20, () -> Fix1BGameTests::scraps);
    public static final GameTests.Function DUCT =
            TESTS.function("fix1b_fluid_duct_retype", 20, () -> Fix1BGameTests::duct);
    public static final GameTests.Function ZIRNOX =
            TESTS.function("fix1b_zirnox_waste", 20, () -> Fix1BGameTests::zirnox);
    public static final GameTests.Function CONFIG_BRANCHES =
            TESTS.function("fix1b_config_branch_recipes", 20, () -> Fix1BGameTests::configBranches);

    public static void init() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    private static ItemStack of(String id) {
        return new ItemStack(item(id));
    }

    /** A member of a {@code c:} tag (whatever the tag holds first). */
    private static ItemStack tag(GameTestHelper helper, String tag) {
        List<Item> items = Ingredients.tagItems(tag);
        helper.assertTrue(!items.isEmpty(), "tag " + tag + " is empty");
        return new ItemStack(items.get(0));
    }

    /** 1.7 {@code (id, metadata)}: the split family's item (History §19es-itemvariant). */
    private static ItemStack variant(String id, int variant) {
        return of(com.hbm.items.LegacyMetaIds.lookup(id.substring(4), variant));
    }

    private static List<ItemStack> grid(int size, ItemStack... stacks) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < size; i++) grid.add(i < stacks.length && stacks[i] != null ? stacks[i] : ItemStack.EMPTY);
        return grid;
    }

    private static ItemStack craft(GameTestHelper helper, ItemStack... stacks) {
        return Recipes.craftingResult(helper.getLevel(), 3, 3, grid(9, stacks)).orElse(ItemStack.EMPTY);
    }

    private static void expect(GameTestHelper helper, ItemStack result, String id, int count, String what) {
        helper.assertTrue(result.is(item(id)) && result.getCount() == count,
                what + ": expected " + count + "x " + id + ", got " + result);
    }

    public static void restored(GameTestHelper helper) {
        // WeaponRecipes:139 scope = weapon_mod_special variant SCOPE (1)
        ItemStack plate = tag(helper, "c:plates/steel");
        ItemStack plastic = tag(helper, "c:ingots/any_plastic");
        ItemStack pane = tag(helper, "c:glass_panes");
        ItemStack scope = craft(helper, plate, plastic, plate, pane, null, pane, plate, plastic, plate);
        expect(helper, scope, "hbm:weapon_mod_special_scope", 1, "scope");
        // WeaponRecipes:121 IRON_DURA (generic variant 1): gunmetal ingot, iron ingot, duct tape
        ItemStack dura = craft(helper, tag(helper, "c:ingots/gunmetal"), new ItemStack(Items.IRON_INGOT), of("hbm:ducttape"));
        expect(helper, dura, "hbm:weapon_mod_generic_iron_dura", 1, "iron dura mod");
        // ConsumableRecipes:33 carpet bombing caller
        ItemStack tnt = new ItemStack(Items.TNT);
        expect(helper, craft(helper, tnt, tnt, tnt, tnt, of("hbm:rangefinder"), tnt, tnt, tnt, tnt),
                "hbm:bomb_caller_carpet", 1, "carpet caller");
        // ConsumableRecipes:62 coffee on a water bottle
        ItemStack water = Ingredients.potionStack("minecraft:potion", "minecraft:water");
        expect(helper, craft(helper, tag(helper, "c:dusts/coal"), new ItemStack(Items.MILK_BUCKET), water,
                new ItemStack(Items.SUGAR)), "hbm:coffee", 1, "coffee");
        // ConsumableRecipes:118 iodine pills x8
        expect(helper, craft(helper, tag(helper, "c:dusts/iodine"), tag(helper, "c:dusts/fluorite")),
                "hbm:pill_iodine", 8, "iodine pills");
        // CraftingManager:586 empty tanks x8
        ItemStack al = tag(helper, "c:plates/aluminium");
        ItemStack fe = tag(helper, "c:plates/iron");
        expect(helper, craft(helper, al, fe, al, al, pane, al, al, fe, al), "hbm:fluid_tank", 8, "empty tanks");
        // CraftingManager:471 red steel scaffold (variant 1) x8
        ItemStack scaffold = of("hbm:steel_scaffold");
        ItemStack red = craft(helper, scaffold, scaffold, scaffold, scaffold, tag(helper, "c:dyes/red"), scaffold,
                scaffold, scaffold, scaffold);
        expect(helper, red, "hbm:steel_scaffold_red", 8, "red scaffold");
        // ToolRecipes:159 steel minecart carries the steel chassis; :166 a powder cart always comes out wood in 1.7
        ItemStack ingot = tag(helper, "c:ingots/steel");
        ItemStack cart = craft(helper, plate, null, plate, ingot, plate, ingot);
        expect(helper, cart, "hbm:cart_empty", 1, "steel cart");
        helper.assertTrue(ModDataComponents.CART_BASE.getOr(cart, null) == ItemModMinecart.EnumCartBase.STEEL,
                "steel cart chassis, got " + cart);
        ItemStack powder = new ItemStack(Items.GUNPOWDER);
        ItemStack powderCart = craft(helper, powder, powder, powder, powder, cart, powder, powder, powder, powder);
        helper.assertTrue(powderCart.is(item("hbm:cart_powder"))
                        && ModDataComponents.CART_BASE.getOr(powderCart, null) == ItemModMinecart.EnumCartBase.WOOD,
                "1.7 powder cart from a steel cart is wood, got " + powderCart);
        // WeaponRecipes:242 white phosphorus shell (variant 5) from a plain shell (variant 0)
        ItemStack wp = craft(helper, of("hbm:ingot_phosphorus"), null, null, variant("hbm:ammo_arty", 0), null, null,
                of("hbm:ingot_phosphorus"));
        expect(helper, wp, "hbm:ammo_arty_phosphorus", 1, "WP shell");
        // the remaining declarations are present
        for (String id : new String[] {"gun_amat", "gun_stg77", "gun_lasrifle", "weapon_mod_special_canisters",
                "weapon_mod_generic_bronze_dura", "syringe_taint", "siox", "xanax", "radaway_flush", "med_bag_antidote",
                "missile_schrabidium", "ammo_arty_4_mini_nuke", "ammo_fireext_sand",
                "grenade_filling_plasma", "grenade_filling_schrab", "custom_hydro", "custom_amat", "powder_nitan_mix",
                "powder_desh_mix_from_dusts", "struct_launcher", "struct_scaffold", "ams_catalyst_caesium",
                "rbmk_fuel_rod_reasim_moderated", "rbmk_fuel_rod_moderated", "brick_compound_from_reinforced_brick", "blowtorch",
                "cart_destroyer", "cart_crate", "lightstone_tile_slab", "brick_ducrete_slab",
                "nuclear_waste_short_tiny_to_nuclear_waste_short_7", "fluid_duct_neo_from_fluid_duct",
                "ingot_pu_mix_from_nuggets", "crystal_pulsar", "memespoon"}) {
            helper.assertTrue(Recipes.hasRecipe(helper.getLevel(), "hbm", id), "missing recipe hbm:" + id);
        }
        helper.succeed();
    }

    public static void mismatches(GameTestHelper helper) {
        // CraftingManager:426 chiseled lightstone (variant 4) from the unrefined block (variant 0), not from tile
        ItemStack chiseled = craft(helper, variant("hbm:lightstone", 0));
        expect(helper, chiseled, "hbm:lightstone_chiseled", 1, "chiseled lightstone");
        helper.assertTrue(craft(helper, variant("hbm:lightstone", 1)).isEmpty(), "tile lightstone alone crafts nothing");
        // BlockGenericStairs.recipeGen: six blocks give four stairs
        ItemStack brick = of("hbm:brick_concrete");
        expect(helper, craft(helper, brick, null, null, brick, brick, null, brick, brick, brick),
                "hbm:brick_concrete_stairs", 4, "concrete brick stairs");
        ItemStack tile = variant("hbm:lightstone", 1);
        expect(helper, craft(helper, tile, null, null, tile, tile, null, tile, tile, tile),
                "hbm:lightstone_tile_stairs", 4, "lightstone tile stairs");
        helper.assertTrue(!Recipes.hasRecipe(helper.getLevel(), "hbm", "brick_concrete_stairs_1"),
                "no slab-to-stairs recipe in 1.7");
        // SmeltingRecipes:31: only the 6 xp uranium-ore recipe remains
        helper.assertTrue(!Recipes.hasRecipe(helper.getLevel(), "hbm", "ore_uranium_smelting")
                && Recipes.hasRecipe(helper.getLevel(), "hbm", "ingot_uranium"), "uranium ore 0.7 xp duplicate removed");
        helper.succeed();
    }

    public static void smelting(GameTestHelper helper) {
        String[][] pairs = {{"hbm:briquette_wood", "minecraft:charcoal"}, {"hbm:sand_uranium", "hbm:glass_uranium"},
                {"hbm:sand_polonium", "hbm:glass_polonium"}, {"hbm:scrap_plastic_board_blank", "hbm:ingot_polymer"}, {"hbm:scrap_plastic_card_processor", "hbm:ingot_polymer"}}; // fix 3: flattened
        for (String[] pair : pairs) {
            ItemStack out = Recipes.smeltingResult(helper.getLevel(), of(pair[0])).orElse(ItemStack.EMPTY);
            expect(helper, out, pair[1], 1, "smelting " + pair[0]);
        }
        helper.succeed();
    }

    public static void grenade(GameTestHelper helper) {
        ItemStack shell = new ItemStack(ModEntityItems.grenade_shell.get(EnumGrenadeShell.FRAG));
        ItemStack he = new ItemStack(ModEntityItems.grenade_filling.get(EnumGrenadeFilling.HE));
        ItemStack fuze = new ItemStack(ModEntityItems.grenade_fuze.get(EnumGrenadeFuze.S3));
        ItemStack extra = new ItemStack(ModEntityItems.grenade_extra.get(EnumGrenadeExtra.values()[0]));
        ItemStack plain = craft(helper, null, shell, null, he, null, fuze);
        helper.assertTrue(plain.is(ModEntityItems.grenade_universal.get()), "shell+filling+fuze make a grenade, got " + plain);
        GrenadeData data = ItemGrenadeUniversal.getData(plain);
        helper.assertTrue(data.shell() == EnumGrenadeShell.FRAG && data.filling() == EnumGrenadeFilling.HE
                && data.fuze() == EnumGrenadeFuze.S3 && data.extra() == null, "grenade data " + data);
        ItemStack withExtra = craft(helper, shell, he, fuze, extra);
        helper.assertTrue(ItemGrenadeUniversal.getData(withExtra).extra() == EnumGrenadeExtra.values()[0],
                "the extra is carried, got " + withExtra);
        // 1.7 refusals: a filling the shell cannot take, a second part of one kind, a foreign item
        ItemStack plasma = new ItemStack(ModEntityItems.grenade_filling.get(EnumGrenadeFilling.PLASMA));
        helper.assertTrue(craft(helper, shell, plasma, fuze).isEmpty(), "plasma does not fit a frag shell");
        helper.assertTrue(craft(helper, shell, he, fuze, fuze.copy()).isEmpty(), "two fuzes refused");
        helper.assertTrue(craft(helper, shell, he, fuze, new ItemStack(Items.STICK)).isEmpty(), "foreign item refused");
        helper.succeed();
    }

    public static void cargo(GameTestHelper helper) {
        ItemStack shell = variant("hbm:ammo_arty", 8);
        ItemStack loaded = craft(helper, shell, null, null, null, new ItemStack(Items.DIAMOND, 5));
        expect(helper, loaded, "hbm:ammo_arty_cargo", 1, "cargo shell");
        ItemStack cargo = ModDataComponents.ARTY_CARGO.getOr(loaded, ItemStack.EMPTY);
        helper.assertTrue(cargo.is(Items.DIAMOND) && cargo.getCount() == 1, "one diamond aboard, got " + cargo);
        helper.assertTrue(craft(helper, shell, new ItemStack(Items.WATER_BUCKET)).isEmpty(),
                "an item with a container is refused");
        helper.assertTrue(craft(helper, loaded, new ItemStack(Items.DIAMOND)).isEmpty(), "a loaded shell takes no more");
        helper.succeed();
    }

    public static void mku(GameTestHelper helper) {
        String[] arrangement = MKUCraftingRecipe.arrangement(helper.getLevel().getSeed());
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) grid[i] = arrangement[i] == null ? ItemStack.EMPTY : MKUCraftingRecipe.ingredientStack(arrangement[i]);
        expect(helper, craft(helper, grid), "hbm:syringe_mkunicorn", 1, "MKU in the seed's arrangement");
        // swap two slots: the secret arrangement no longer matches
        int a = -1, b = -1;
        for (int i = 0; i < 9 && b < 0; i++) {
            if (arrangement[i] == null) continue;
            if (a < 0) a = i; else if (!arrangement[i].equals(arrangement[a])) b = i;
        }
        ItemStack swap = grid[a]; grid[a] = grid[b]; grid[b] = swap;
        helper.assertTrue(craft(helper, grid).isEmpty(), "a different arrangement crafts nothing");
        // the meteor-dungeon book names the slot of its ingredient on the last page
        ItemStack book = MKUCraftingRecipe.generateBook(helper.getLevel().getSeed(), "powder_iodine");
        helper.assertTrue(book.is(item("hbm:book_lore")), "MKU book is a lore book, got " + book);
        helper.succeed();
    }

    public static void scraps(GameTestHelper helper) {
        ItemStack big = ItemScraps.create(new Mats.MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.quantity * 3));
        ItemStack split = craft(helper, big);
        expect(helper, split, "hbm:scraps", 2, "scraps split");
        Mats.MaterialStack half = ItemScraps.getMats(split);
        helper.assertTrue(half != null && half.material == Mats.MAT_STEEL && half.amount == MaterialShapes.INGOT.quantity * 3 / 2,
                "each half holds 108 steel quanta, got " + split);
        helper.assertTrue(craft(helper, ItemScraps.create(new Mats.MaterialStack(Mats.MAT_STEEL, 1))).isEmpty(),
                "one quantum cannot split");
        // PowderRecipes:61: copper dust and redstone give red copper scraps worth two ingots
        ItemStack mix = craft(helper, tag(helper, "c:dusts/copper"), tag(helper, "c:dusts/redstone"));
        Mats.MaterialStack mats = ItemScraps.getMats(mix);
        helper.assertTrue(mats != null && mats.material == Mats.MAT_RED_COPPER && mats.amount == MaterialShapes.INGOT.quantity * 2,
                "red copper scraps x2 ingots, got " + mix);
        helper.succeed();
    }

    public static void duct(GameTestHelper helper) {
        ItemStack id = new ItemStack(ModUtilityToolItems.fluid_identifier_multi.get());
        ItemFluidIDMulti.setType(id, Fluids.fromName("DIESEL"), true);
        ItemStack neo = of("hbm:fluid_duct_neo");
        ItemStack one = craft(helper, neo, id);
        helper.assertTrue(one.is(ModFluidItems.fluid_duct.get()) && one.getCount() == 1
                && ItemFluidDuct.getFluid(one) == Fluids.fromName("DIESEL"), "one diesel duct, got " + one);
        ItemStack eight = craft(helper, neo, neo, neo, neo, id, neo, neo, neo, neo);
        helper.assertTrue(eight.getCount() == 8 && ItemFluidDuct.getFluid(eight) == Fluids.fromName("DIESEL"),
                "eight diesel ducts, got " + eight);
        helper.assertTrue(craft(helper, neo, neo, id).isEmpty(), "two ducts is not a 1.7 recipe");
        List<ItemStack> left = Recipes.craftingRemainders(helper.getLevel(), 3, 3, grid(9, neo, id)).orElse(List.of());
        // the remainder list follows the trimmed input: the duct leaves nothing, the identifier itself stays
        helper.assertTrue(left.stream().filter(s -> !s.isEmpty()).count() == 1
                && left.stream().anyMatch(s -> s.is(ModUtilityToolItems.fluid_identifier_multi.get())
                        && ItemFluidIDMulti.getType(s, true) == Fluids.fromName("DIESEL")),
                "the identifier stays in the grid, got " + left);
        expect(helper, craft(helper, one), "hbm:fluid_duct_neo", 1, "typed duct back to a universal duct");
        helper.succeed();
    }

    public static void zirnox(GameTestHelper helper) {
        ItemStack rod = of("hbm:rod_zirnox_depleted_uranium_fuel");
        ItemStack waste = craft(helper, rod);
        expect(helper, waste, "hbm:waste_uranium", 2, "zirnox waste");
        helper.assertTrue(ItemZirnoxWaste.isHot(waste), "1.7 waste meta 1 is hot");
        List<ItemStack> left = Recipes.craftingRemainders(helper.getLevel(), 3, 3, grid(9, rod)).orElse(List.of());
        helper.assertTrue(left.stream().anyMatch(s -> s.is(item("hbm:rod_zirnox_empty"))), "the empty rod stays, got " + left);
        helper.succeed();
    }

    /**
     * 1.7 CraftingManager / ArmorRecipes / ToolRecipes / ConsumableRecipes / PowderRecipes config branches
     * ({@code ConfigCraftingRecipes}, {@code "golem:requires"}): each side of a branch is in the recipe manager exactly
     * while its flag holds. Golem Core removes the other side after the datapack load; at default config (LBSM and 528
     * off) the LBSM and 528 recipes are absent and the default ones present.
     */
    public static void configBranches(GameTestHelper helper) {
        boolean crafting = com.hbm.config.GeneralConfig.enableLBSM && com.hbm.config.GeneralConfig.enableLBSMSimpleCrafting;
        boolean armor = com.hbm.config.GeneralConfig.enableLBSM && com.hbm.config.GeneralConfig.enableLBSMSimpleArmorRecipes;
        boolean tool = com.hbm.config.GeneralConfig.enableLBSM && com.hbm.config.GeneralConfig.enableLBSMSimpleToolRecipes;
        boolean medicine = com.hbm.config.GeneralConfig.enableLBSM
                && com.hbm.config.GeneralConfig.enableLBSMSimpleMedicineRecipes;
        boolean m528 = com.hbm.config.GeneralConfig.enable528;
        String[][] cases = {
                {"plate_iron_lbsm", String.valueOf(crafting)}, {"wire_fine_copper_lbsm", String.valueOf(crafting)},
                {"ore_uranium_lbsm", String.valueOf(crafting)}, {"powder_dura_steel_lbsm", String.valueOf(crafting)},
                {"starmetal_helmet_lbsm", String.valueOf(armor)}, {"starmetal_helmet", String.valueOf(!armor)},
                {"schrabidium_sword_lbsm", String.valueOf(tool)}, {"schrabidium_sword", String.valueOf(!tool)},
                {"siox_lbsm", String.valueOf(medicine)}, {"siox", String.valueOf(!medicine)},
                {"rbmk_control_reasim_528", String.valueOf(m528)}, {"rbmk_control", String.valueOf(!m528)},
                {"inf_water", String.valueOf(!m528)},
        };
        for (String[] c : cases) {
            boolean expected = Boolean.parseBoolean(c[1]);
            helper.assertTrue(Recipes.hasRecipe(helper.getLevel(), "hbm", c[0]) == expected,
                    "recipe hbm:" + c[0] + (expected ? " missing" : " present although its config branch is off"));
        }
        helper.succeed();
    }
}
