package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.config.GeneralConfig;
import com.hbm.items.ModArmorSetItems;
import com.hbm.items.armor.ArmorFSB;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Effects;
import dev.golem.core.world.StepHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** GitHub issue 225: a mob in a full Trenchmaster set gets its bonuses only with {@code 1.94_mobSetBonuses} on. */
public final class Gh225GameTests {

    private Gh225GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function MOB_SET =
            TESTS.function("gh225_mob_set_bonuses_follow_config", 60, 3, () -> Gh225GameTests::mobSet);

    public static void init() {}

    private static void mobSet(GameTestHelper helper) {
        boolean before = GeneralConfig.mobSetBonuses;
        GeneralConfig.mobSetBonuses = false;
        LivingEntity off = suited(helper, new BlockPos(0, 2, 0));
        LivingEntity on = suited(helper, new BlockPos(2, 2, 2));
        helper.runAfterDelay(10, () -> {
            boolean offStrength = Effects.has(off, Effects.strength());
            GeneralConfig.mobSetBonuses = true;
            helper.runAfterDelay(10, () -> {
                GeneralConfig.mobSetBonuses = before;
                helper.assertTrue(ArmorFSB.hasFSBArmor(on), "a pig in the full set is not a set wearer");
                helper.assertFalse(offStrength, "the set bonus reached a mob with the switch off");
                helper.assertTrue(Effects.has(on, Effects.strength()) && Effects.has(on, Effects.speed()),
                        "the set bonus did not reach a mob with the switch on");
                helper.assertTrue(StepHeight.get(on) > 1D, "the set's step height did not reach the mob");
                helper.succeed();
            });
        });
    }

    private static LivingEntity suited(GameTestHelper helper, BlockPos pos) {
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, pos);
        pig.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModArmorSetItems.trenchmaster_helmet.get()));
        pig.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModArmorSetItems.trenchmaster_plate.get()));
        pig.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModArmorSetItems.trenchmaster_legs.get()));
        pig.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModArmorSetItems.trenchmaster_boots.get()));
        return pig;
    }
}
