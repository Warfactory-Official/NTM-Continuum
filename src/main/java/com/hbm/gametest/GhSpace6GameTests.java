package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.handler.jei.UniversalRecipeHandler;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.special.ItemHot;
import com.hbm.items.special.ItemUnstable;
import com.hbm.items.weapon.GunB92;
import com.hbm.items.weapon.GunB92Cell;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.platform.Platform;
import dev.golem.core.world.HeldItemState;
import dev.golem.core.world.HeldItems;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * NTM: Space issues 6 and 7: data that changes while an item is held must not re-equip it (1.7 compared item and
 * damage), and the ore slopper's viewer page lists one recipe per raw ore sample.
 */
public final class GhSpace6GameTests {

    private GhSpace6GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function NEUTRON_DOSE =
            TESTS.function("ghspace6_neutron_dose_keeps_held_item", 20, () -> GhSpace6GameTests::neutronDose);
    public static final GameTests.Function HBM_ITEMS =
            TESTS.function("ghspace6_hbm_items_keep_held_item", 20, () -> GhSpace6GameTests::hbmItems);
    public static final GameTests.Function ORE_SLOPPER =
            TESTS.function("ghspace7_ore_slopper_one_row_per_sample", 20, () -> GhSpace6GameTests::oreSlopper);

    public static void init() {}

    private static void neutronDose(GameTestHelper helper) {
        ItemStack plain = new ItemStack(Items.IRON_INGOT);
        ItemStack dosed = plain.copy();
        ModDataComponents.NEUTRON_ACTIVATION.set(dosed, 2.5F);
        ItemStack more = plain.copy();
        ModDataComponents.NEUTRON_ACTIVATION.set(more, 3.0F);
        helper.assertTrue(HeldItems.sameApartFromIgnored(plain, dosed), "a first dose is the same held item");
        helper.assertTrue(HeldItems.sameApartFromIgnored(dosed, more), "a growing dose is the same held item");
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ModDataComponents.NEUTRON_ACTIVATION.set(gold, 2.5F);
        helper.assertFalse(HeldItems.sameApartFromIgnored(dosed, gold), "another item is a new held item");
        ItemStack two = more.copyWithCount(2);
        helper.assertFalse(HeldItems.sameApartFromIgnored(dosed, two), "a count change is left to vanilla");
        helper.assertFalse(HeldItems.sameApartFromIgnored(plain, plain.copyWithCount(2)),
                "stacks without the dose are left to vanilla");
        helper.succeed();
    }

    private static void hbmItems(GameTestHelper helper) {
        ItemStack hot = ItemHot.heatUp(new ItemStack(ModAbilityToolItems.ingot_meteorite.get()), 1.0D);
        ItemStack cooler = hot.copy();
        ItemHot.setHeat(cooler, ItemHot.getHeatValue(hot) - 1);
        same(helper, hot, cooler, "a cooling hot ingot");

        ItemStack unstable = new ItemStack(ModNuclearMiscItems.INGOT_U238M2.get());
        ItemStack later = unstable.copy();
        ItemUnstable.TIMER.set(later, 20);
        same(helper, unstable, later, "an unstable ingot counting down");

        ItemStack gun = new ItemStack(ModGunItems.gun_b92.get());
        ItemStack fired = gun.copy();
        GunB92.setAnim(fired, 5);
        GunB92.setPower(fired, 3);
        same(helper, gun, fired, "the B92 animating or losing charge");

        ItemStack cell = new ItemStack(ModGunItems.gun_b92_ammo.get());
        ItemStack charged = cell.copy();
        GunB92Cell.setPower(charged, 4);
        same(helper, cell, charged, "a B92 cell charging");

        helper.assertFalse(((HeldItemState) hot.getItem()).compatIsSameHeldItem(hot, new ItemStack(Items.IRON_INGOT)),
                "switching away from a hot ingot is a new held item");
        helper.succeed();
    }

    private static void same(GameTestHelper helper, ItemStack shown, ItemStack held, String what) {
        helper.assertTrue(((HeldItemState) shown.getItem()).compatIsSameHeldItem(shown, held), what + " is the same held item");
    }

    /** Without NTM: Space the one sample is HBM's raw ore: 1.7's single recipe of six types and slop. */
    private static void oreSlopper(GameTestHelper helper) {
        List<UniversalRecipeHandler.Row> rows = UniversalRecipeHandler.oreSlopper();
        for (UniversalRecipeHandler.Row row : rows) {
            helper.assertTrue(row.outputs().size() <= 8, "1.7 lays out at most eight outputs: " + row.outputs().size());
            helper.assertTrue(row.inputs().get(1).get(0).is(ModOreBlocks.bedrock_ore_base.get().asItem()),
                    "the second input is a raw bedrock ore");
        }
        if (!Platform.isModLoaded("ntmspace")) {
            helper.assertTrue(rows.size() == 1 && rows.get(0).outputs().size() == 7,
                    "HBM alone: one recipe, six types and slop: " + rows.size());
        }
        helper.succeed();
    }
}
