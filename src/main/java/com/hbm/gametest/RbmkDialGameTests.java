package com.hbm.gametest;

import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.items.ModRBMKItems;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** The RBMK dials as 1.7 world rules: defaults, the /gamerule command, ranges, and the behaviour they change. */
public final class RbmkDialGameTests {

    private RbmkDialGameTests() {}

    /** Runs /gamerule as the server; false when the command does not parse. */
    private static boolean gamerule(MinecraftServer server, String text) {
        try {
            server.getCommands().getDispatcher().execute("gamerule " + text, server.createCommandSourceStack().withSuppressedOutput());
            return true;
        } catch (CommandSyntaxException expected) {
            return false; // the caller decides whether a rejection is the point of the test
        }
    }

    private static void resetDials(MinecraftServer server) {
        RBMKDials.resetAll(server);
    }

    public static void defaults(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        resetDials(server);
        var level = helper.getLevel();
        helper.assertTrue(RBMKDials.getPassiveCooling(level) == 2.5D && RBMKDials.getPassiveCoolingInner(level) == 0.1D, "passive cooling");
        helper.assertTrue(RBMKDials.getColumnHeatFlow(level) == 0.2D && RBMKDials.getFuelHeatProvision(level) == 0.2D, "heat flow, provision");
        helper.assertTrue(RBMKDials.getFuelDiffusionMod(level) == 1D && RBMKDials.getControlSpeed(level) == 1D, "diffusion, control speed");
        helper.assertTrue(RBMKDials.getReactivityMod(level) == 1D && RBMKDials.getOutgasserMod(level) == 1D, "reactivity, outgasser");
        helper.assertTrue(RBMKDials.getSurgeMod(level) == 1D && RBMKDials.getBoilerHeatConsumption(level) == 0.1D, "surge, boiler consumption");
        helper.assertTrue(RBMKDials.getColumnHeight(level) == 3 && RBMKDials.getColumnHeightRuleValue(level) == 4, "column height 4");
        helper.assertTrue(RBMKDials.getFluxRange(level) == 5 && RBMKDials.getReaSimRange(level) == 10, "flux and ReaSim range");
        helper.assertTrue(RBMKDials.getReaSimBoilerSpeed(level) == 0.05D && RBMKDials.getAbsorberHeatConversion(level) == 0.05D, "ReaSim boiler speed, absorber heat");
        helper.assertTrue(RBMKDials.getModeratorEfficiency(level) == 1D && RBMKDials.getAbsorberEfficiency(level) == 1D
                && RBMKDials.getReflectorEfficiency(level) == 1D, "efficiencies");
        helper.assertTrue(RBMKDials.getPermaScrap(level) && RBMKDials.getDepletion(level) && RBMKDials.getXenon(level), "scrap, depletion, xenon on");
        helper.assertTrue(!RBMKDials.getMeltdownsDisabled(level) && !RBMKDials.getOverpressure(level), "meltdowns on, overpressure off");
        helper.assertTrue(RBMKDials.SAVE_DIALS.get(level), "dialSaveDials true");
        helper.succeed();
    }

    /** /gamerule takes a double, an int and a boolean, reads back what was typed and clamps like 1.7 when read. */
    public static void commandsAndRanges(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        var level = helper.getLevel();
        try {
            gamerule(server, "dialControlSpeed 2.5");
            helper.assertTrue(RBMKDials.getControlSpeed(level) == 2.5D, "double rule " + RBMKDials.getControlSpeed(level));
            gamerule(server, "dialControlSpeed -3");
            helper.assertTrue(RBMKDials.getControlSpeed(level) == 0D, "a negative speed reads as the 1.7 minimum 0");
            gamerule(server, "dialColumnHeatFlow 7.5");
            helper.assertTrue(RBMKDials.getColumnHeatFlow(level) == 1D, "heat flow clamps to 1");
            gamerule(server, "dialColumnHeight 6");
            helper.assertTrue(RBMKDials.getColumnHeight(level) == 5 && RBMKDials.getColumnHeightRuleValue(level) == 6, "int rule 6 gives 5 above the core");
            gamerule(server, "dialColumnHeight 99");
            helper.assertTrue(RBMKDials.getColumnHeightRuleValue(level) == 16, "height clamps to 16");
            gamerule(server, "dialColumnHeight 1");
            helper.assertTrue(RBMKDials.getColumnHeightRuleValue(level) == 2, "height clamps to 2");
            gamerule(server, "dialFluxRange 500");
            helper.assertTrue(RBMKDials.getFluxRange(level) == 100, "flux range clamps to 100");
            gamerule(server, "dialDisableXenon true");
            helper.assertTrue(!RBMKDials.getXenon(level), "boolean rule");
            gamerule(server, "dialDisableXenon false");
            helper.assertTrue(RBMKDials.getXenon(level), "boolean rule back");
            helper.assertFalse(gamerule(server, "dialControlSpeed fast"), "a non-number is not a double");
            helper.assertTrue(RBMKDials.getControlSpeed(level) == 0D, "a rejected value changes nothing");
            RBMKDials.PASSIVE_COOLING.reset(server);
            helper.assertTrue(RBMKDials.getPassiveCooling(level) == 2.5D, "reset is the default");
        } finally {
            resetDials(server);
        }
        helper.succeed();
    }

    /** dialControlSpeed multiplies the rod speed 0.00277 per tick. */
    public static void controlSpeed(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try {
            gamerule(server, "dialControlSpeed 2.5");
            BlockPos pos = new BlockPos(2, 1, 2);
            helper.setBlock(pos, ModRBMKBlocks.rbmk_control.get().defaultBlockState());
            BlockEntityRBMKControlManual control = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKControlManual.class);
            control.setTarget(1D);
            for (int i = 0; i < 100; i++) control.tickServer();
            helper.assertTrue(Math.abs(control.level - 100 * 0.00277D * 2.5D) < 1e-9, "100 ticks at 2.5x move 0.6925, got " + control.level);
        } finally {
            resetDials(server);
        }
        helper.succeed();
    }

    /** An overheated fuel column melts unless dialDisableMeltdowns is on. */
    public static void meltdowns(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        BlockPos pos = new BlockPos(2, 1, 2);
        try {
            gamerule(server, "dialDisableMeltdowns true");
            helper.setBlock(pos, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
            BlockEntityRBMKRod rod = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKRod.class);
            rod.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));
            rod.heat = 1_000_000D;
            rod.tickServer();
            helper.assertTrue(helper.getBlockState(pos).is(ModRBMKBlocks.rbmk_fuel_rod.get()), "the column survives with meltdowns disabled");
            gamerule(server, "dialDisableMeltdowns false");
            rod.heat = 1_000_000D;
            rod.tickServer();
            helper.assertTrue(helper.getBlockState(pos).is(ModFluidNetBlocks.corium_block.get()),
                    "the column melts with meltdowns enabled, got " + helper.getBlockState(pos));
            helper.getLevel().getEntitiesOfClass(EntityRBMKDebris.class, new AABB(helper.absolutePos(pos)).inflate(40)).forEach(EntityRBMKDebris::discard);
        } finally {
            resetDials(server);
        }
        helper.succeed();
    }

    /** dialColumnHeight sets how many blocks a column occupies, core included. */
    public static void columnHeight(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        try {
            gamerule(server, "dialColumnHeight 6");
            var block = ModRBMKBlocks.rbmk_blank.get();
            helper.assertTrue(block.getDimensions()[0] == 5, "a column of 6 reaches 5 above the core, got " + block.getDimensions()[0]);
            BlockPos core = new BlockPos(2, 1, 2);
            helper.setBlock(core, block.defaultBlockState());
            block.fillSpace(helper.getLevel(), helper.absolutePos(core), Direction.NORTH);
            helper.assertTrue(helper.getBlockState(core.above(5)).is(block) && helper.getBlockState(core.above(5)).getValue(RBMKBase.PART) == RBMKBase.Part.TOP,
                    "the sixth block is the top cell, got " + helper.getBlockState(core.above(5)));
            helper.assertTrue(helper.getBlockState(core.above(6)).isAir(), "nothing above the sixth block");
        } finally {
            resetDials(server);
        }
        helper.succeed();
    }
}
