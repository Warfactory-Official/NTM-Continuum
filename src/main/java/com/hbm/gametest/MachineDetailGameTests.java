package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Small 1.7 behaviours checked against hand-computed values: diesel smoke, RBMK rod formula, bedrock ore sampling. */
public final class MachineDetailGameTests {

    private MachineDetailGameTests() {}

    /** 1.7 smoke and engine swing: {@code isOn && hasAcceptableFuel() && fill > 0}, redstone or not. */
    public static void dieselSmokeNeedsOn(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModMachineBlocks.machine_diesel.get().defaultBlockState());
        TileEntityMachineDiesel diesel = GameTestHelpers.blockEntity(helper, at, TileEntityMachineDiesel.class);
        diesel.tank.setTankType(Fluids.DIESEL);
        diesel.tank.setFill(1_000);
        diesel.isOn = false;
        helper.assertFalse(diesel.showsRunning(), "a switched-off diesel generator must not smoke");
        diesel.isOn = true;
        helper.assertTrue(diesel.showsRunning(), "a running diesel generator must smoke");
        helper.setBlock(at.east(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(diesel.showsRunning(), "1.7 smokes under redstone too");
        diesel.tank.setFill(0);
        helper.assertFalse(diesel.showsRunning(), "an empty diesel generator must not smoke");
        helper.succeed();
    }

    /**
     * UEU at 25 % yield: RAISING_SLOPE 0.25 + sin(pi / 4) / 2 = 0.60355, shown as the flux factor 0.603 and 60.3 %,
     * while the reactivity stays 15.0.
     */
    public static void rbmkRodDescription(GameTestHelper helper) {
        ItemRBMKRod rod = ModRBMKItems.rbmk_fuel_ueu.get();
        ItemStack stack = new ItemStack(rod);
        ItemRBMKRod.setYield(stack, ItemRBMKRod.getYield(stack) * 0.25D);
        String expected = "log10((x * " + ChatFormatting.YELLOW + "0.603" + ChatFormatting.WHITE + ") + 1) * 0.5 * 15.0"
                + ChatFormatting.GOLD + " (60.3%)";
        String actual = rod.getFuncDescription(stack);
        helper.assertTrue(actual.equals(expected), "UEU at 25% yield: expected " + expected + ", got " + actual);
        helper.assertTrue(rod.getFuncDescription(new ItemStack(rod)).equals("log10(x + 1) * 0.5 * 15.0"),
                "a fresh UEU rod: " + rod.getFuncDescription(new ItemStack(rod)));
        helper.succeed();
    }

    /** 1.7 samples the bedrock ore level at {@code ((x >> 4) << 4) + 8}: one value per chunk. */
    public static void bedrockOreChunkCentre(GameTestHelper helper) {
        boolean differs = false;
        for (BedrockOreType type : BedrockOreType.VALUES) {
            double centre = ItemBedrockOreBase.getOreLevel(120, -456, type);
            helper.assertTrue(ItemBedrockOreBase.getOreLevel(112, -464, type) == centre
                            && ItemBedrockOreBase.getOreLevel(127, -449, type) == centre
                            && ItemBedrockOreBase.getOreLevel(123, -456, type) == centre,
                    type + " differs inside the chunk around (120, -456)");
            if (ItemBedrockOreBase.getOreLevel(128, -456, type) != centre) differs = true;
        }
        helper.assertTrue(differs, "the next chunk has the same level for every type");
        helper.succeed();
    }
}
