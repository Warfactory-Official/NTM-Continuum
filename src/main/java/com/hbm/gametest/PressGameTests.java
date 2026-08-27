package com.hbm.gametest;

import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ModStampItems;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Focused contracts for the currently registered press casing recipes. */
public final class PressGameTests {

    private PressGameTests() {}

    public static void casingRecipes(GameTestHelper helper) {
        PressRecipes.register();
        assertId(helper, ModCasingItems.casing(EnumCasingType.SMALL), "casing_small");
        assertId(helper, ModCasingItems.casing(EnumCasingType.LARGE), "casing_large");
        assertId(helper, ModCasingItems.casing(EnumCasingType.SMALL_STEEL), "casing_small_steel");
        assertId(helper, ModCasingItems.casing(EnumCasingType.LARGE_STEEL), "casing_large_steel");
        assertOutput(helper, ModItems.plate_gunmetal.get(), ModStampItems.stamp_9.get(),
                ModCasingItems.casing(EnumCasingType.SMALL), 4);
        assertOutput(helper, ModItems.plate_gunmetal.get(), ModStampItems.stamp_50.get(),
                ModCasingItems.casing(EnumCasingType.LARGE), 2);
        assertOutput(helper, ModItems.plate_weaponsteel.get(), ModStampItems.stamp_9.get(),
                ModCasingItems.casing(EnumCasingType.SMALL_STEEL), 4);
        assertOutput(helper, ModItems.plate_weaponsteel.get(), ModStampItems.stamp_50.get(),
                ModCasingItems.casing(EnumCasingType.LARGE_STEEL), 2);
        helper.succeed();
    }

    private static void assertOutput(GameTestHelper helper, Item input, Item stamp, Item output, int count) {
        ItemStack result = PressRecipes.getOutput(new ItemStack(input), new ItemStack(stamp));
        helper.assertTrue(result.is(output) && result.getCount() == count,
                "Press recipe produced " + result + ", expected " + count + " " + output);
    }

    private static void assertId(GameTestHelper helper, Item item, String path) {
        helper.assertTrue(dev.golem.core.Ids.of("hbm", path)
                        .equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)),
                "Expected item id hbm:" + path + ", got "
                        + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
    }
}
