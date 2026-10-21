package com.hbm.gametest;

import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
import com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The oil chain's machines run in the world for a fixed number of ticks and the result is compared with a value
 * worked out from the 1.7 source by hand (the comment on each test shows the arithmetic).
 */
public final class OilChainParityGameTests {

    private OilChainParityGameTests() {}

    private static final BlockPos AT = new BlockPos(3, 2, 3);

    private static <T extends BlockEntity> T place(GameTestHelper helper, Block block, Class<T> type) {
        helper.setBlock(AT, block.defaultBlockState());
        return GameTestHelpers.blockEntity(helper, AT, type);
    }

    private static void expect(GameTestHelper helper, FluidTankNTM tank, FluidType type, int fill, String what) {
        helper.assertTrue(tank.getTankType() == type && tank.getFill() == fill,
                what + ": expected " + fill + " mB of " + type.getName() + ", got " + tank.getFill() + " mB of "
                        + tank.getTankType().getName());
    }

    /** Runs {@code check} after exactly {@code ticks} game ticks, counted from the moment the setup ran. */
    private static void after(GameTestHelper helper, int ticks, Runnable check) {
        helper.runAfterDelay(ticks, () -> {
            check.run();
            helper.succeed();
        });
    }

    /**
     * 1.7 refine(): each tick with power at least 5 and 100 mB of hot oil drains 100 mB and 5 HE and adds 50 / 25 / 15 /
     * 10 mB; every tenth operation also yields the sulfur byproduct. Ten ticks: 9000 left, 500 / 250 / 150 / 100, 950 HE.
     */
    public static void refinery(GameTestHelper helper) {
        TileEntityMachineRefinery te = place(helper, ModMachineBlocks.machine_refinery.get(), TileEntityMachineRefinery.class);
        helper.runAfterDelay(2, () -> {
            te.tanks[0].setTankType(Fluids.HOTOIL);
            te.tanks[0].setFill(10_000);
            te.power = 1_000;
            te.sulfur = 0;
            te.inventory.setStackInSlot(11, ItemStack.EMPTY);
            after(helper, 10, () -> {
                expect(helper, te.tanks[0], Fluids.HOTOIL, 9_000, "input");
                expect(helper, te.tanks[1], Fluids.HEAVYOIL, 500, "heavy oil");
                expect(helper, te.tanks[2], Fluids.NAPHTHA, 250, "naphtha");
                expect(helper, te.tanks[3], Fluids.LIGHTOIL, 150, "light oil");
                expect(helper, te.tanks[4], Fluids.PETROLEUM, 100, "petroleum");
                helper.assertTrue(te.power == 950, "power must be 950, was " + te.power);
                helper.assertTrue(te.inventory.getStackInSlot(11).getCount() == 1,
                        "the tenth operation must yield one byproduct, slot 11 holds " + te.inventory.getStackInSlot(11));
            });
        });
    }

    /** 1.7 refine() stops below 5 HE: 7 HE pays for exactly one operation. */
    public static void refineryPower(GameTestHelper helper) {
        TileEntityMachineRefinery te = place(helper, ModMachineBlocks.machine_refinery.get(), TileEntityMachineRefinery.class);
        helper.runAfterDelay(2, () -> {
            te.tanks[0].setTankType(Fluids.HOTOIL);
            te.tanks[0].setFill(10_000);
            te.power = 7;
            after(helper, 10, () -> {
                expect(helper, te.tanks[0], Fluids.HOTOIL, 9_900, "input");
                helper.assertTrue(te.power == 2, "power must be 2, was " + te.power);
            });
        });
    }

    /** 1.7 vacuum refine(): 100 mB oil and 10,000 HE give 40 / 25 / 20 / 15 mB. Five ticks on 30,000 HE: three operations. */
    public static void vacuum(GameTestHelper helper) {
        TileEntityMachineVacuumDistill te = place(helper, ModMachineBlocks.machine_vacuum_distill.get(),
                TileEntityMachineVacuumDistill.class);
        helper.runAfterDelay(2, () -> {
            te.tanks[0].setTankType(Fluids.OIL);
            te.tanks[0].setFill(1_000);
            te.power = 30_000;
            after(helper, 5, () -> {
                expect(helper, te.tanks[0], Fluids.OIL, 700, "input");
                expect(helper, te.tanks[1], Fluids.HEAVYOIL_VACUUM, 120, "heavy vacuum oil");
                expect(helper, te.tanks[2], Fluids.REFORMATE, 75, "reformate");
                expect(helper, te.tanks[3], Fluids.LIGHTOIL_VACUUM, 60, "light vacuum oil");
                expect(helper, te.tanks[4], Fluids.SOURGAS, 45, "sour gas");
                helper.assertTrue(te.power == 0, "power must be 0, was " + te.power);
            });
        });
    }

    /**
     * 1.7 crack(): every fifth tick, twice over, 100 mB bitumen and 200 mB steam give 80 mB oil, 20 mB aromatics and 2 mB
     * spent steam. Ten ticks hold two such ticks: 4 rounds, 600 / 1200 left, 320 / 80 / 8 made.
     */
    public static void cracker(GameTestHelper helper) {
        TileEntityMachineCatalyticCracker te = place(helper, ModMachineBlocks.machine_catalytic_cracker.get(),
                TileEntityMachineCatalyticCracker.class);
        helper.runAfterDelay(2, () -> {
            te.tanks[0].setTankType(Fluids.BITUMEN);
            te.tanks[0].setFill(1_000);
            te.tanks[1].setFill(2_000);
            after(helper, 10, () -> {
                expect(helper, te.tanks[0], Fluids.BITUMEN, 600, "bitumen");
                expect(helper, te.tanks[1], Fluids.STEAM, 1_200, "steam");
                expect(helper, te.tanks[2], Fluids.OIL, 320, "oil");
                expect(helper, te.tanks[3], Fluids.AROMATICS, 80, "aromatics");
                expect(helper, te.tanks[4], Fluids.SPENTSTEAM, 8, "spent steam");
            });
        });
    }

    /** 1.7 reform(): 100 mB naphtha and 20,000 HE give 50 mB reformate, 15 mB petroleum, 10 mB hydrogen. Two ticks. */
    public static void reformer(GameTestHelper helper) {
        TileEntityMachineCatalyticReformer te = place(helper, ModMachineBlocks.machine_catalytic_reformer.get(),
                TileEntityMachineCatalyticReformer.class);
        helper.runAfterDelay(2, () -> {
            te.tanks[0].setTankType(Fluids.NAPHTHA);
            te.tanks[0].setFill(1_000);
            te.power = 50_000;
            te.inventory.setStackInSlot(10, new ItemStack(ModItems.catalytic_converter.get()));
            after(helper, 2, () -> {
                expect(helper, te.tanks[0], Fluids.NAPHTHA, 800, "input");
                expect(helper, te.tanks[1], Fluids.REFORMATE, 100, "reformate");
                expect(helper, te.tanks[2], Fluids.PETROLEUM, 30, "petroleum");
                expect(helper, te.tanks[3], Fluids.HYDROGEN, 20, "hydrogen");
                helper.assertTrue(te.power == 10_000, "power must be 10000, was " + te.power);
            });
        });
    }

    /**
     * 1.7 reform() of the hydrotreater runs on even ticks: 100 mB oil, 5 mB hydrogen and 20,000 HE give 90 mB desulfurized
     * oil and 15 mB sour gas. Four ticks hold two even ticks.
     */
    public static void hydrotreater(GameTestHelper helper) {
        TileEntityMachineHydrotreater te = place(helper, ModMachineBlocks.machine_hydrotreater.get(),
                TileEntityMachineHydrotreater.class);
        helper.runAfterDelay(2, () -> {
            te.tanks[0].setTankType(Fluids.OIL);
            te.tanks[0].setFill(1_000);
            te.tanks[1].setFill(100);
            te.power = 100_000;
            te.inventory.setStackInSlot(10, new ItemStack(ModItems.catalytic_converter.get()));
            after(helper, 4, () -> {
                expect(helper, te.tanks[0], Fluids.OIL, 800, "input");
                expect(helper, te.tanks[1], Fluids.HYDROGEN, 90, "hydrogen");
                expect(helper, te.tanks[2], Fluids.OIL_DS, 180, "desulfurized oil");
                expect(helper, te.tanks[3], Fluids.SOURGAS, 30, "sour gas");
                helper.assertTrue(te.power == 60_000, "power must be 60000, was " + te.power);
            });
        });
    }

    /** 1.7 fractionate(): every tenth tick 100 mB heavy oil split 30 / 70 into bitumen and smear. */
    public static void fractionTower(GameTestHelper helper) {
        TileEntityMachineFractionTower te = place(helper, ModMachineBlocks.machine_fraction_tower.get(),
                TileEntityMachineFractionTower.class);
        helper.runAfterDelay(2, () -> {
            te.tanks[0].setTankType(Fluids.HEAVYOIL);
            te.tanks[0].setFill(1_000);
            after(helper, 10, () -> {
                expect(helper, te.tanks[0], Fluids.HEAVYOIL, 900, "input");
                expect(helper, te.tanks[1], Fluids.BITUMEN, 30, "bitumen");
                expect(helper, te.tanks[2], Fluids.SMEAR, 70, "smear");
            });
        });
    }

    /**
     * 1.7 flare, venting: a gas flare with the valve open lets 50 mB a tick out and needs no power. Ten ticks from
     * 1000 mB: 500. Burning: 10 mB a tick, {@code 50000 * 10 / 1000 / 5 = 100} HE a tick for a gaseous fuel (1.7 recalculates GAS to
     * 50,000 HE per bucket), so ten ticks leave 900 mB and 1000 HE.
     */
    public static void flare(GameTestHelper helper) {
        TileEntityMachineGasFlare te = place(helper, ModMachineBlocks.machine_flare.get(), TileEntityMachineGasFlare.class);
        helper.runAfterDelay(2, () -> {
            te.tank.setTankType(Fluids.GAS);
            te.tank.setFill(1_000);
            te.isOn = true;
            te.doesBurn = false;
            te.power = 0;
            helper.runAfterDelay(10, () -> {
                expect(helper, te.tank, Fluids.GAS, 500, "vented gas");
                helper.assertTrue(te.power == 0, "venting makes no power, was " + te.power);
                te.tank.setFill(1_000);
                te.doesBurn = true;
                after(helper, 10, () -> {
                    expect(helper, te.tank, Fluids.GAS, 900, "burned gas");
                    helper.assertTrue(te.power == 1_000, "burning must make 1000 HE, was " + te.power);
                });
            });
        });
    }

    /**
     * 1.7 coker: heat leaks by max(heat/1000, 1) a tick below the 100,000 cap, burns heat/100 into progress, and a round
     * finishes at 20,000 progress. The same arithmetic runs here as a model and the machine must agree after 40 ticks.
     */
    public static void coker(GameTestHelper helper) {
        TileEntityMachineCoker te = place(helper, ModMachineBlocks.machine_coker.get(), TileEntityMachineCoker.class);
        CokerRecipes.CokerRecipe recipe = CokerRecipes.getOutput(Fluids.HEAVYOIL);
        helper.assertTrue(recipe != null, "heavy oil must have a coker recipe");
        helper.runAfterDelay(2, () -> {
            te.tanks[0].setTankType(Fluids.HEAVYOIL);
            te.tanks[0].setFill(16_000);
            te.heat = TileEntityMachineCoker.maxHeat;
            te.progress = 0;
            int heat = te.heat;
            int progress = 0;
            int rounds = 0;
            int byproduct = 0;
            int byproductMax = te.tanks[1].getMaxFill();
            int byproductPerRound = recipe.byproduct() == null ? 0 : recipe.byproduct().fill;
            for (int i = 0; i < 40; i++) {
                if (heat < TileEntityMachineCoker.maxHeat) heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
                boolean canProcess = 16_000 - rounds * recipe.amount() >= recipe.amount() && byproduct + byproductPerRound <= byproductMax;
                int burn = canProcess ? heat / 100 : 0;
                if (burn > 0) {
                    progress += burn;
                    heat -= burn;
                    if (progress >= TileEntityMachineCoker.processTime) {
                        progress -= TileEntityMachineCoker.processTime;
                        rounds++;
                        byproduct += byproductPerRound;
                    }
                }
            }
            int expectedHeat = heat;
            int expectedProgress = progress;
            int expectedRounds = rounds;
            helper.assertTrue(expectedRounds >= 1, "the model must finish a round in 40 ticks, progress " + expectedProgress);
            after(helper, 40, () -> {
                helper.assertTrue(te.heat == expectedHeat && te.progress == expectedProgress,
                        "heat/progress must be " + expectedHeat + "/" + expectedProgress + ", were " + te.heat + "/" + te.progress);
                helper.assertTrue(te.tanks[0].getFill() == 16_000 - expectedRounds * recipe.amount(),
                        "each round must take " + recipe.amount() + " mB of heavy oil, left " + te.tanks[0].getFill());
            });
        });
    }

    /**
     * 1.7 barrel: only slots 2 to 5 are reachable from outside and only 3 and 5 can be taken, so a hopper under it
     * leaves an item in the identifier slot alone and empties an output slot.
     */
    public static void barrelHopper(GameTestHelper helper) {
        TileEntityBarrel te = place(helper, ModFluidNetBlocks.barrel_steel.get(), TileEntityBarrel.class);
        helper.setBlock(AT.below(), Blocks.HOPPER.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            te.inventory.setStackInSlot(1, new ItemStack(Items.STICK));
            te.inventory.setStackInSlot(2, new ItemStack(Items.STONE));
            te.inventory.setStackInSlot(3, new ItemStack(Items.DIRT));
            after(helper, 40, () -> {
                helper.assertTrue(!te.inventory.getStackInSlot(1).isEmpty(), "a hopper must not reach the identifier slot");
                helper.assertTrue(!te.inventory.getStackInSlot(2).isEmpty(), "a hopper must not take from the input slot");
                helper.assertTrue(te.inventory.getStackInSlot(3).isEmpty(), "a hopper must empty the output slot 3");
            });
        });
    }

    /**
     * 1.7 liquefactor: coal gives 250 mB coal oil, at 250 HE a tick over 60 ticks. One coal of two is consumed.
     */
    public static void liquefactor(GameTestHelper helper) {
        TileEntityMachineLiquefactor te = place(helper, ModMachineBlocks.machine_liquefactor.get(),
                TileEntityMachineLiquefactor.class);
        helper.runAfterDelay(2, () -> {
            te.power = 100_000;
            te.inventory.setStackInSlot(0, new ItemStack(Items.COAL, 2));
            after(helper, 60, () -> {
                expect(helper, te.tank, Fluids.COALOIL, 250, "coal oil");
                helper.assertTrue(te.inventory.getStackInSlot(0).getCount() == 1,
                        "one coal must be left, slot 0 holds " + te.inventory.getStackInSlot(0));
                helper.assertTrue(te.power == 100_000 - 60 * 250, "power must be 85000, was " + te.power);
            });
        });
    }
}
