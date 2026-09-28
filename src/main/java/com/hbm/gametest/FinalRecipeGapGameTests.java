package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemEnergy;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Ingredients;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * The recipe and data gaps closed in the final wave, checked through the real recipe manager: the 1.7
 * {@code getDict} fluid-container ingredient (any full container, the empty container left behind), the
 * metadata/component and crafting-tool ingredients, the {@code ContainerUpgradeCraftingHandler} content copy,
 * the furnace burn times on every loader (a real vanilla furnace burning a briquette), and the potion sickness
 * default.
 */
public final class FinalRecipeGapGameTests {

    private FinalRecipeGapGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FLUID =
            TESTS.function("final_recipe_fluid_container", 20, () -> FinalRecipeGapGameTests::fluidContainer);
    public static final GameTests.Function COMPONENT =
            TESTS.function("final_recipe_component_match", 20, () -> FinalRecipeGapGameTests::componentMatch);
    public static final GameTests.Function COPY =
            TESTS.function("final_recipe_content_copy", 20, () -> FinalRecipeGapGameTests::contentCopy);
    public static final GameTests.Function FUEL =
            TESTS.function("final_recipe_fuel_times", 200, () -> FinalRecipeGapGameTests::fuelTimes);
    public static final GameTests.Function SICKNESS =
            TESTS.function("final_recipe_energy_sickness", 20, () -> FinalRecipeGapGameTests::energySickness);

    public static void init() {}

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Ids.parse(id));
    }

    /** The first item of a tag, so the test does not hard-code which material table owns it. */
    private static ItemStack tagged(GameTestHelper helper, String tag) {
        List<Item> items = Ingredients.tagItems(tag);
        helper.assertTrue(!items.isEmpty(), "#" + tag + " has members");
        return new ItemStack(items.get(0));
    }

    private static List<ItemStack> grid(ItemStack... stacks) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(i < stacks.length ? stacks[i] : ItemStack.EMPTY);
        return grid;
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> grid) {
        return Recipes.craftingResult(helper.getLevel(), 3, 3, grid).orElse(ItemStack.EMPTY);
    }

    private static List<ItemStack> remainders(GameTestHelper helper, List<ItemStack> grid) {
        return Recipes.craftingRemainders(helper.getLevel(), 3, 3, grid).orElse(List.of());
    }

    public static void fluidContainer(GameTestHelper helper) {
        helper.assertTrue(Fluids.PEROXIDE.getID() == 35 && Fluids.LUBRICANT.getID() == 18,
                "the declared fluid ids are 1.7's (peroxide 35, lubricant 18)");

        // chocolate milk: pane, cocoa, milk bucket, 1,000 mB nitroglycerin in a universal tank
        ItemStack tank = ModFluidItems.fluid_tank.get().make(Fluids.NITROGLYCERIN, 1_000);
        List<ItemStack> milk = grid(new ItemStack(Items.GLASS_PANE), new ItemStack(Items.COCOA_BEANS),
                new ItemStack(Items.MILK_BUCKET), tank);
        helper.assertTrue(craft(helper, milk).is(item("hbm:chocolate_milk")), "chocolate milk crafts from a full tank");
        List<ItemStack> left = remainders(helper, milk);
        // Remainders come back per slot of the recipe's input, and 1.21+'s CraftingInput.of trims empty
        // rows/columns (this grid becomes 3x2 = 6 slots; 1.20.1's container stays 3x3 = 9). The trim keeps
        // the top-left origin and the full width here, so slot indices still line up with grid().
        helper.assertTrue(left.size() >= 4, "one remainder per input slot, got " + left.size());
        helper.assertTrue(left.get(2).is(Items.BUCKET), "the milk bucket leaves a bucket, got " + left.get(2));
        helper.assertTrue(left.get(3).is(ModFluidItems.fluid_tank.get())
                        && ModFluidItems.fluid_tank.get().getFill(left.get(3)) == 0,
                "the nitroglycerin tank is returned empty, got " + left.get(3));

        // the same getDict accepts a lead tank; a half tank is not a 1,000 mB container
        ItemStack lead = ModFluidNetItems.fluid_tank_lead_full.get().make(Fluids.NITROGLYCERIN, 1_000);
        milk.set(3, lead);
        helper.assertTrue(craft(helper, milk).is(item("hbm:chocolate_milk")), "a full lead tank is also a container");
        helper.assertTrue(remainders(helper, milk).get(3).is(ModItems.fluid_tank_lead_empty.get()),
                "the lead tank leaves fluid_tank_lead_empty");
        milk.set(3, ModFluidItems.fluid_tank.get().make(Fluids.NITROGLYCERIN, 500));
        helper.assertTrue(craft(helper, milk).isEmpty(), "a half-full tank does not match getDict(1000)");

        // BDCL: water bucket as the water container
        List<ItemStack> bdcl = grid(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.WHITE_DYE),
                tagged(helper, "c:tars"));
        helper.assertTrue(craft(helper, bdcl).is(item("hbm:bdcl")), "bdcl crafts with a water bucket");
        helper.assertTrue(remainders(helper, bdcl).get(0).is(Items.BUCKET), "the water bucket leaves a bucket");

        // gun kit: rubber, iron, a woodoil canister-less tank; the kit 2 recipe takes a lubricant canister
        ItemStack woodoil = ModFluidItems.fluid_tank.get().make(Fluids.WOODOIL, 1_000);
        List<ItemStack> kit1 = grid(tagged(helper, "c:ingots/any_rubber"), woodoil, new ItemStack(Items.IRON_INGOT));
        helper.assertTrue(craft(helper, kit1).is(item("hbm:gun_kit_1")), "gun_kit_1 crafts");
        ItemStack canister = ModFluidNetItems.canister_full.get().make(Fluids.LUBRICANT, 1_000);
        List<ItemStack> kit2 = grid(new ItemStack(item("hbm:gun_kit_1")), new ItemStack(item("hbm:wrench")),
                new ItemStack(ModItems.ducttape.get()), canister);
        helper.assertTrue(craft(helper, kit2).is(item("hbm:gun_kit_2")), "gun_kit_2 crafts from a lubricant canister");
        helper.assertTrue(remainders(helper, kit2).get(3).is(ModItems.canister_empty.get()),
                "the canister leaves canister_empty");

        // mucho mango: only the water bottle, consumed whole (1.7's potion had no container item)
        ItemStack water = Ingredients.potionStack("minecraft:potion", "minecraft:water");
        List<ItemStack> mango = grid(water, new ItemStack(Items.SUGAR), new ItemStack(Items.SUGAR),
                new ItemStack(Items.ORANGE_DYE));
        helper.assertTrue(craft(helper, mango).is(item("hbm:mucho_mango")), "mucho mango crafts from a water bottle");
        helper.assertTrue(remainders(helper, mango).get(0).isEmpty(), "the water bottle is consumed whole");
        mango.set(0, Ingredients.potionStack("minecraft:potion", "minecraft:awkward"));
        helper.assertTrue(craft(helper, mango).isEmpty(), "an awkward potion is not a water bottle");

        // the assembler's Watz container (ass.mpw10taint)
        helper.assertTrue(FluidContainerRegistry.INSTANCE.holds(new ItemStack(item("hbm:bucket_mud")), "WATZ", 1_000),
                "the mud bucket is a 1,000 mB Watz container");
        helper.succeed();
    }

    public static void componentMatch(GameTestHelper helper) {
        // holotape: HOLO_DIGAMMA (variant 0) + a screwdriver that stays, one use more worn
        Item holotape = item("hbm:holotape_image");
        ItemStack digamma = new ItemStack(holotape);
        com.hbm.items.ModDataComponents.ITEM_VARIANT.set(digamma, 0);
        ItemStack screwdriver = new ItemStack(item("hbm:screwdriver"));
        List<ItemStack> tape = grid(digamma, screwdriver, new ItemStack(ModItems.ducttape.get()),
                new ItemStack(item("hbm:armor_polish")));
        ItemStack restored = craft(helper, tape);
        helper.assertTrue(restored.is(holotape) && com.hbm.items.ModDataComponents.ITEM_VARIANT.getOr(restored, -1) == 1,
                "the digamma tape restores to HOLO_RESTORED, got " + restored);
        ItemStack worn = remainders(helper, tape).get(1);
        helper.assertTrue(worn.is(item("hbm:screwdriver")) && worn.getDamageValue() == 1,
                "the screwdriver stays in the grid one use more worn, got " + worn);
        tape.set(0, new ItemStack(holotape)); // variant absent reads as HOLO_DIGAMMA
        helper.assertTrue(craft(helper, tape).is(holotape), "an unmarked tape is HOLO_DIGAMMA too");
        ItemStack fe = new ItemStack(holotape);
        com.hbm.items.ModDataComponents.ITEM_VARIANT.set(fe, 2);
        tape.set(0, fe);
        helper.assertTrue(craft(helper, tape).isEmpty(), "another holotape variant does not restore");

        // rigged cell: the full B92 cell is variant 1; peroxide in the tank itself
        ItemStack cell = new ItemStack(item("hbm:gun_b92_ammo"));
        com.hbm.items.ModDataComponents.ITEM_VARIANT.set(cell, 1);
        List<ItemStack> rig = grid(ModFluidItems.fluid_tank.get().make(Fluids.PEROXIDE, 1_000), cell,
                tagged(helper, "c:wires/copper"));
        ItemStack rigged = craft(helper, rig);
        helper.assertTrue(rigged.is(item("hbm:weaponized_starblaster_cell")), "the full cell rigs, got " + rigged);
        ItemStack emptyCell = new ItemStack(item("hbm:gun_b92_ammo"));
        rig.set(1, emptyCell);
        helper.assertTrue(craft(helper, rig).isEmpty(), "a cell below 25 charges (variant 0) does not rig");

        // chemistry set: dynamite leaves the set worn by one
        ItemStack set = new ItemStack(item("hbm:chemistry_set"));
        List<ItemStack> dyn = grid(tagged(helper, "c:dusts/niter"), new ItemStack(Items.SUGAR), new ItemStack(Items.SAND), set);
        ItemStack dynamite = craft(helper, dyn);
        helper.assertTrue(dynamite.is(item("hbm:ball_dynamite")) && dynamite.getCount() == 2, "two dynamite, got " + dynamite);
        ItemStack usedSet = remainders(helper, dyn).get(3);
        helper.assertTrue(usedSet.is(item("hbm:chemistry_set")) && usedSet.getDamageValue() == 1,
                "the chemistry set stays, worn by one, got " + usedSet);
        helper.succeed();
    }

    public static void contentCopy(GameTestHelper helper) {
        ItemStack steel = new ItemStack(item("hbm:crate_steel"));
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < 54; i++) contents.add(ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.DIAMOND, 7));
        contents.set(5, new ItemStack(Items.IRON_INGOT, 64));
        ItemStacks.setCarriedItems(steel, contents);

        ItemStack b = tagged(helper, "c:storage_blocks/tungsten");
        ItemStack p = tagged(helper, "c:plates_cast/copper");
        List<ItemStack> recipe = new ArrayList<>(List.of(b.copy(), p.copy(), b.copy(), p.copy(), steel, p.copy(),
                b.copy(), p.copy(), b.copy()));
        ItemStack tungsten = craft(helper, recipe);
        helper.assertTrue(tungsten.is(item("hbm:crate_tungsten")), "the steel crate upgrades to tungsten, got " + tungsten);
        List<ItemStack> carried = ItemStacks.carriedItems(tungsten, 54);
        helper.assertTrue(carried.get(0).is(Items.DIAMOND) && carried.get(0).getCount() == 7
                        && carried.get(5).is(Items.IRON_INGOT) && carried.get(5).getCount() == 64,
                "the tungsten crate keeps the steel crate's contents, got " + carried.get(0) + " / " + carried.get(5));

        recipe.set(4, new ItemStack(item("hbm:crate_steel")));
        ItemStack fresh = craft(helper, recipe);
        helper.assertTrue(fresh.is(item("hbm:crate_tungsten")) && ItemStacks.carriedItems(fresh, 27).get(0).isEmpty(),
                "an empty steel crate makes an empty tungsten crate");
        helper.succeed();
    }

    public static void fuelTimes(GameTestHelper helper) {
        int checked = 0;
        for (Map.Entry<Supplier<? extends Item>, Integer> entry : ModFuelItems.BURN_TIMES.entrySet()) {
            Item fuel = entry.getKey().get();
            helper.assertTrue(fuel != Items.AIR, "every burn-time entry names a registered item");
            int burn = Compat.burnDuration(helper.getLevel(), new ItemStack(fuel));
            helper.assertTrue(burn == entry.getValue(), BuiltInRegistries.ITEM.getKey(fuel) + " burns "
                    + entry.getValue() + " ticks, got " + burn);
            checked++;
        }
        helper.assertTrue(checked >= 35, "all 35 1.7 FuelHandler fuels checked, got " + checked);
        // 1.7 values where 1.12.2's ItemFuel differed
        helper.assertTrue(Compat.burnDuration(helper.getLevel(), new ItemStack(item("hbm:biomass"))) == 400,
                "biomass burns 400 ticks (1.7 single * 2)");
        helper.assertTrue(Compat.burnDuration(helper.getLevel(), new ItemStack(item("hbm:solid_fuel_presto"))) == 8000,
                "presto log burns 8,000 ticks (1.7 single * 40)");
        helper.assertTrue(Compat.burnDuration(helper.getLevel(), new ItemStack(item("hbm:solid_fuel_presto_triplet"))) == 40000,
                "presto triplet burns 40,000 ticks (1.7 single * 200)");
        // 1.12.2-only fuels that 1.7 never burned
        for (String notFuel : new String[]{"hbm:ingot_graphite", "hbm:powder_coal_tiny", "hbm:ingot_c4",
                "hbm:pellet_coal", "hbm:ball_resin"}) {
            helper.assertTrue(Compat.burnDuration(helper.getLevel(), new ItemStack(item(notFuel))) == 0,
                    notFuel + " is not a fuel in 1.7");
        }
        helper.assertTrue(Compat.burnDuration(helper.getLevel(), new ItemStack(item("hbm:briquette_coal"))) == 2000,
                "a coal briquette burns 2,000 ticks");
        helper.assertTrue(Compat.burnDuration(helper.getLevel(), new ItemStack(item("hbm:coke_coal"))) == 3200,
                "coal coke burns 3,200 ticks (1.7 FuelHandler single * 16)");

        // a real vanilla furnace lights on a briquette
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.FURNACE.defaultBlockState());
        AbstractFurnaceBlockEntity furnace = GameTestHelpers.blockEntity(helper, pos, AbstractFurnaceBlockEntity.class);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(item("hbm:briquette_coal")));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(furnace.getItem(1).isEmpty(), "the furnace consumed the briquette as fuel");
            helper.assertTrue(helper.getBlockState(pos).getValue(BlockStateProperties.LIT), "the furnace is lit");
            helper.succeed();
        });
    }

    public static void energySickness(GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack can = new ItemStack(item("hbm:can_smart"));
        helper.assertTrue(can.getItem() instanceof ItemEnergy, "can_smart is an ItemEnergy");
        can.getItem().finishUsingItem(can, helper.getLevel(), player);
        helper.assertTrue(!player.getActiveEffects().isEmpty(), "the drink applied its own effects (speed etc.)");
        helper.assertTrue(!com.hbm.config.VersatileConfig.hasPotionSickness(player),
                "1.7 PotionConfig.potionSickness defaults OFF: a drink applies no potion sickness");
        helper.succeed();
    }
}
