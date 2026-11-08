package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.viewer.FluidIcons;
import net.minecraft.gametest.framework.GameTestHelper;

/** GitHub issue 162: every fluid icon in a recipe viewer stands for its fluid, so the fluid's own entry finds the recipe. */
public final class Gh162GameTests {

    private Gh162GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ICONS =
            TESTS.function("gh162_fluid_icons_stand_for_their_fluid", 20, () -> Gh162GameTests::icons);

    public static void init() {}

    public static void icons(GameTestHelper helper) {
        int checked = 0;
        for (FluidType type : Fluids.getInNiceOrder()) {
            if (type == Fluids.NONE) continue;
            FluidIcons.Amount amount = FluidIcons.fluidOf(ItemFluidIcon.make(new FluidStack(type, 250)));
            helper.assertTrue(amount != null, type.getName() + "'s icon stands for no fluid");
            helper.assertTrue(amount.fluid() == type.getFluid() && Fluids.fromVanillaExact(amount.fluid()) == type,
                    type.getName() + "'s icon stands for " + amount.fluid());
            helper.assertTrue(amount.milliBuckets() == 250, type.getName() + "'s icon lost its amount: " + amount);
            checked++;
        }
        helper.assertTrue(checked > 150, "only " + checked + " fluids checked");
        helper.succeed();
    }
}
