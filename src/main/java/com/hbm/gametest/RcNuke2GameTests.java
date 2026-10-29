package com.hbm.gametest;

import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.ModWatzItems;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIngredientItems.PlateFuelType;
import com.hbm.items.ModPwrItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.tileentity.machine.TileEntityWasteDrum;
import com.hbm.tileentity.machine.TileEntityWatz;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKAutoloader;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.RBMKColor;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.BlockEntities;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntConsumer;

import net.minecraft.core.BlockPos;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDisplay;

/**
 * Second reactor audit: running machines for many real ticks against numbers worked out from the 1.7 classes.
 */
public final class RcNuke2GameTests {

    private RcNuke2GameTests() {
    }

    private static void ticks(GameTestHelper helper, int count, IntConsumer afterTick, Runnable done) {
        step(helper, 0, count, afterTick, done);
    }

    private static void step(GameTestHelper helper, int i, int count, IntConsumer afterTick, Runnable done) {
        if (i >= count) {
            done.run();
            return;
        }
        helper.runAfterDelay(1, () -> {
            afterTick.accept(i);
            step(helper, i + 1, count, afterTick, done);
        });
    }

    /**
     * 1.7 BobMathUtil.squirt.
     */
    private static double squirt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }

    private static void roundTrip(GameTestHelper helper, BlockEntity be, String what) {
        var level = helper.getLevel();
        CompoundTag saved = BlockEntities.save(be, level.registryAccess());
        BlockState state = be.getBlockState();
        BlockEntity fresh = ((EntityBlock) state.getBlock()).newBlockEntity(be.getBlockPos(), state);
        fresh.setLevel(level);
        BlockEntities.loadWithComponents(fresh, saved, level.registryAccess());
        helper.assertTrue(saved.equals(BlockEntities.save(fresh, level.registryAccess())),
                what + " does not round trip");
    }


    public static final int FUSION_PADDING = 24;
    public static final int WASTE_PADDING = 24;

    /**
     * The ten cycles against 1.7 FusionRecipes: ignition, output, flux (breeder capacity / divisor), power and duration.
     */
    public static void fusionRecipeTable(GameTestHelper helper) {
        FusionRecipes.INSTANCE.registerRecipes();
        double cap = TileEntityFusionBreeder.CAPACITY;
        Object[][] rows = {
                {"fus.dd", 750_000L, 1_000_000L, cap / 200}, {"fus.do", 250_000L, 1_250_000L, cap / 200},
                {"fus.dt", 750_000L, 3_750_000L, cap / 100}, {"fus.tcl", 2_500_000L, 6_250_000L, cap / 20},
                {"fus.h3", 500_000L, 3_750_000L, 0D}, {"fus.th4", 875_000L, 4_000_000L, cap / 20},
                {"fus.cl", 3_750_000L, 10_000_000L, cap / 10}, {"fus.dhc", 10_000_000L, 25_000_000L, cap / 5},
                {"fus.bf", 1_000_000L, 12_500_000L, cap / 5}, {"fus.stellar", 10_000_000L, 50_000_000L, cap}};
        for (Object[] row : rows) {
            FusionRecipe recipe = (FusionRecipe) FusionRecipes.INSTANCE.getRecipe((String) row[0]);
            helper.assertTrue(recipe != null, row[0] + " is missing");
            helper.assertTrue(recipe.ignitionTemp == (long) row[1], row[0] + " ignition " + recipe.ignitionTemp);
            helper.assertTrue(recipe.outputTemp == (long) row[2], row[0] + " output " + recipe.outputTemp);
            helper.assertTrue(recipe.neutronFlux == (double) row[3], row[0] + " flux " + recipe.neutronFlux);
            helper.assertTrue(recipe.power == 25_000L && recipe.duration == 100, row[0] + " power/duration");
        }
        helper.succeed();
    }

    /**
     * D-D burn through the real chain: klystron east (750,000 KyU), boiler north, collector west, breeder south.
     * 1.7 numbers per active tick: 25,000 HE and 20 mB deuterium; plasma 1,000,000, all of it to the one receiver (the
     * boiler: breeder and collector do not count), so 5,000 mB water to steam; 50 neutrons into the breeder; progress
     * 0.01 and, with one collector, a bonus of 0.005 per tick.
     */
    public static void fusionChainRun(GameTestHelper helper) {
        FusionRecipes.INSTANCE.registerRecipes();
        BlockPos c = GameTests.paddedCentre(FUSION_PADDING, 1);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.fusion_torus.get(), c);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.fusion_klystron.get(), c.east(12), Direction.EAST);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.fusion_boiler.get(), c.north(12), Direction.NORTH);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.fusion_collector.get(), c.west(10), Direction.WEST);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.fusion_breeder.get(), c.south(10), Direction.SOUTH);
        TileEntityFusionTorus torus = GameTestHelpers.blockEntity(helper, c, TileEntityFusionTorus.class);
        TileEntityFusionKlystron klystron = GameTestHelpers.blockEntity(helper, c.east(12),
                TileEntityFusionKlystron.class);
        TileEntityFusionBoiler boiler = GameTestHelpers.blockEntity(helper, c.north(12), TileEntityFusionBoiler.class);
        TileEntityFusionBreeder breeder = GameTestHelpers.blockEntity(helper, c.south(10),
                TileEntityFusionBreeder.class);

        torus.fusionModule.recipe = "fus.dd";
        torus.fusionModule.setupTanks(torus.fusionModule.getRecipe());
        torus.coolantTanks[0].setFill(torus.coolantTanks[0].getMaxFill());
        torus.temperature = TileEntityCooledBase.temperature_target;
        torus.power = torus.getMaxPower();
        torus.tanks[0].setFill(torus.tanks[0].getMaxFill());
        klystron.outputTarget = 750_000L;
        klystron.power = 75_000_000L;
        klystron.compair.setFill(klystron.compair.getMaxFill());
        boiler.tanks[0].setFill(32_000);
        breeder.inventory.setStackInSlot(1, new ItemStack(ModAbilityToolItems.meteorite_sword_irradiated.get()));

        long[] lastPower = {torus.power};
        int[] lastDeut = {torus.tanks[0].getFill()};
        int[] active = {0};
        ticks(helper, 225, k -> {
            if (torus.didProcess) {
                active[0]++;
                helper.assertTrue(lastPower[0] - torus.power == 25_000L,
                        "power per tick " + (lastPower[0] - torus.power));
                helper.assertTrue(lastDeut[0] - torus.tanks[0].getFill() == 20,
                        "deuterium per tick " + (lastDeut[0] - torus.tanks[0].getFill()));
                helper.assertTrue(torus.plasmaEnergy == 1_000_000L, "plasma " + torus.plasmaEnergy);
                helper.assertTrue(torus.klystronEnergySync() == 750_000L,
                        "klystron energy " + torus.klystronEnergySync());
                helper.assertTrue(torus.fuelConsumption == 1D, "fuel factor " + torus.fuelConsumption);
                int steam = Math.min(32_000, 5_000 * active[0]);
                helper.assertTrue(boiler.tanks[1].getFill() == steam && boiler.tanks[0].getFill() == 32_000 - steam,
                        "boiler steam " + boiler.tanks[1].getFill() + " water " + boiler.tanks[0].getFill() + " after " + active[0] + " active ticks");
                if (active[0] < 200) {
                    helper.assertTrue(breeder.progress == 50D * active[0],
                            "breeder progress " + breeder.progress + " after " + active[0]);
                }
            }
            lastPower[0] = torus.power = torus.getMaxPower();
            lastDeut[0] = torus.tanks[0].getMaxFill();
            torus.tanks[0].setFill(lastDeut[0]);
            klystron.power = 75_000_000L;
            klystron.compair.setFill(klystron.compair.getMaxFill());
            torus.coolantTanks[0].setFill(torus.coolantTanks[0].getMaxFill());
            torus.coolantTanks[1].setFill(0);
            helper.assertTrue(klystron.output == 750_000L || k < 3, "klystron output " + klystron.output);
        }, () -> {
            int a = active[0];
            helper.assertTrue(a >= 205 && a <= 225, "the torus must burn nearly every tick, burned " + a);
            // 2 whole cycles (progress 0.01 a tick) plus one banked collector bonus (0.005 a tick), 1,000 mB helium each
            helper.assertTrue(torus.tanks[3].getFill() == 3_000, "helium-4 " + torus.tanks[3].getFill());
            helper.assertTrue(breeder.inventory.getStackInSlot(2).is(ModAbilityToolItems.meteorite_sword_fused.get())
                            && breeder.inventory.getStackInSlot(1).isEmpty(),
                    "the irradiated sword must be fused after 10,000 neutrons");
            roundTrip(helper, torus, "torus");
            roundTrip(helper, breeder, "breeder");
            roundTrip(helper, boiler, "boiler");
            roundTrip(helper, klystron, "klystron");

            // 700,000 is below the 750,000 ignition: nothing burns, nothing is drawn
            klystron.outputTarget = 700_000L;
            long[] base = new long[2];
            ticks(helper, 8, k -> {
                klystron.power = 70_000_000L;
                if (k == 3) {
                    base[0] = torus.power;
                    base[1] = torus.tanks[0].getFill();
                }
                if (k > 3) {
                    helper.assertFalse(torus.didProcess, "700,000 KyU cannot ignite D-D");
                    helper.assertTrue(torus.power == base[0] && torus.tanks[0].getFill() == base[1],
                            "an unlit torus draws nothing");
                    helper.assertTrue(torus.plasmaEnergy == 0L, "no plasma without ignition");
                    helper.assertTrue(klystron.output == 700_000L, "klystron output " + klystron.output);
                }
            }, helper::succeed);
        });
    }


    private static final int CYCLOTRON_PADDING = 12;

    private static void stock(TileEntityMachineCyclotron te) {
        te.inventory.setStackInSlot(0, new ItemStack(ModItems.part_lithium.get(), 2));
        te.inventory.setStackInSlot(1, new ItemStack(ModItems.part_beryllium.get(), 2));
        te.inventory.setStackInSlot(2, new ItemStack(ModItems.part_copper.get(), 2));
        for (int i = 3; i < 6; i++) te.inventory.setStackInSlot(i, new ItemStack(ModItems.powder_strontium.get(), 2));
    }

    /**
     * Level-ticked cyclotron: 1,000,000 HE, 500 mB coolant and 1 progress a tick, 690 for a cycle, 50 + 25 + 15 antimatter.
     */
    public static void cyclotronRun(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(CYCLOTRON_PADDING, 3);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_cyclotron.get(), c);
        TileEntityMachineCyclotron te = GameTestHelpers.blockEntity(helper, c, TileEntityMachineCyclotron.class);
        stock(te);
        te.power = TileEntityMachineCyclotron.MAX_POWER;
        te.tanks[0].setFill(32_000);
        long[] last = {te.power};
        int[] active = {0};
        ticks(helper, 700, k -> {
            if (te.power != last[0]) {
                active[0]++;
                helper.assertTrue(last[0] - te.power == 1_000_000L, "power per tick " + (last[0] - te.power));
                helper.assertTrue(te.tanks[0].getFill() == 32_000 - 500 && te.tanks[1].getFill() == 500,
                        "coolant " + te.tanks[0].getFill() + " spent " + te.tanks[1].getFill());
                if (active[0] < 690)
                    helper.assertTrue(te.progress == active[0], "progress " + te.progress + " after " + active[0]);
            }
            te.power = TileEntityMachineCyclotron.MAX_POWER;
            last[0] = te.power;
            te.tanks[0].setFill(32_000);
            te.tanks[1].setFill(0);
        }, () -> {
            helper.assertTrue(active[0] >= 691, "ran " + active[0] + " ticks");
            helper.assertTrue(
                    te.inventory.getStackInSlot(6).is(ModItems.powder_zirconium.get()) && te.inventory.getStackInSlot(6)
                                                                                                      .getCount() == 1,
                    "slot 6 " + te.inventory.getStackInSlot(6));
            helper.assertTrue(
                    te.inventory.getStackInSlot(7).is(ModItems.powder_niobium.get()) && te.inventory.getStackInSlot(7)
                                                                                                    .getCount() == 1,
                    "slot 7 " + te.inventory.getStackInSlot(7));
            helper.assertTrue(
                    te.inventory.getStackInSlot(8).is(ModItems.powder_neodymium.get()) && te.inventory.getStackInSlot(8)
                                                                                                      .getCount() == 1,
                    "slot 8 " + te.inventory.getStackInSlot(8));
            for (int i = 0; i < 6; i++) {
                helper.assertTrue(te.inventory.getStackInSlot(i).getCount() == 1,
                        "slot " + i + " must have given up exactly one item");
            }
            helper.assertTrue(te.tanks[2].getFill() == 90, "antimatter " + te.tanks[2].getFill());
            roundTrip(helper, te, "cyclotron");
            helper.succeed();
        });
    }

    /**
     * Speed 3 and power 1: 4 progress and 900,000 HE a tick, a cycle in 173 ticks; the effect upgrade divides the coolant.
     */
    public static void cyclotronUpgrades(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(CYCLOTRON_PADDING, 3);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_cyclotron.get(), c);
        TileEntityMachineCyclotron te = GameTestHelpers.blockEntity(helper, c, TileEntityMachineCyclotron.class);
        stock(te);
        te.inventory.setStackInSlot(10, new ItemStack(ModUpgradeItems.upgrade_speed_3.get()));
        te.inventory.setStackInSlot(11, new ItemStack(ModUpgradeItems.upgrade_power_1.get()));
        te.power = TileEntityMachineCyclotron.MAX_POWER;
        te.tanks[0].setFill(32_000);
        long[] last = {te.power};
        int[] active = {0};
        ticks(helper, 180, k -> {
            if (te.power != last[0]) {
                active[0]++;
                helper.assertTrue(last[0] - te.power == 900_000L, "power per tick " + (last[0] - te.power));
                helper.assertTrue(te.tanks[0].getFill() == 32_000 - 2_000 && te.tanks[1].getFill() == 2_000,
                        "coolant " + te.tanks[0].getFill() + " spent " + te.tanks[1].getFill());
                if (active[0] < 173) helper.assertTrue(te.progress == 4 * active[0], "progress " + te.progress);
                if (active[0] == 173) {
                    helper.assertTrue(te.progress == 0 && te.inventory.getStackInSlot(6).getCount() == 1,
                            "the cycle must complete on tick 173, progress " + te.progress);
                }
            }
            te.power = TileEntityMachineCyclotron.MAX_POWER;
            last[0] = te.power;
            te.tanks[0].setFill(32_000);
            te.tanks[1].setFill(0);
        }, () -> {
            helper.assertTrue(active[0] >= 173, "ran " + active[0] + " ticks");
            // 500 / (effect + 1) * speed, integer division, with speed 4 and effect 2
            te.inventory.setStackInSlot(10, new ItemStack(ModUpgradeItems.upgrade_speed_3.get()));
            te.inventory.setStackInSlot(11, new ItemStack(ModUpgradeItems.upgrade_effect_2.get()));
            te.upgradeManager.checkSlots(te.inventory, 10, 11);
            helper.assertTrue(
                    te.getSpeed() == 4 && te.getCoolantConsumption() == 664 && te.getConsumption() == 1_000_000,
                    "speed " + te.getSpeed() + " coolant " + te.getCoolantConsumption() + " power " + te.getConsumption());
            te.inventory.setStackInSlot(10, new ItemStack(ModUpgradeItems.upgrade_power_1.get()));
            te.inventory.setStackInSlot(11, new ItemStack(ModUpgradeItems.upgrade_power_1.get()));
            te.upgradeManager.checkSlots(te.inventory, 10, 11);
            helper.assertTrue(te.getConsumption() == 800_000 && te.getSpeed() == 1 && te.getCoolantConsumption() == 500,
                    "two power upgrades: " + te.getConsumption());
            helper.succeed();
        });
    }

    /**
     * A hopper feeds the machine through a side cell and a hopper below takes the finished product.
     */
    public static void cyclotronHoppers(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(CYCLOTRON_PADDING, 3);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_cyclotron.get(), c);
        TileEntityMachineCyclotron te = GameTestHelpers.blockEntity(helper, c, TileEntityMachineCyclotron.class);
        BlockPos feed = c.east(3);
        helper.setBlock(feed, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        HopperBlockEntity feeder = GameTestHelpers.blockEntity(helper, feed, HopperBlockEntity.class);
        feeder.setItem(0, new ItemStack(ModItems.part_beryllium.get(), 3));
        BlockPos take = c.east(2).below();
        helper.setBlock(take.below(), Blocks.CHEST.defaultBlockState());
        helper.setBlock(take, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        te.inventory.setStackInSlot(8, new ItemStack(ModItems.powder_neodymium.get(), 5));
        ticks(helper, 60, k -> {
        }, () -> {
            helper.assertTrue(
                    te.inventory.getStackInSlot(1).is(ModItems.part_beryllium.get()) && te.inventory.getStackInSlot(1)
                                                                                                    .getCount() == 3,
                    "the east middle cell takes particles into slot 1 (1.7 row {1, 4, 6, 7, 8}), got " + te.inventory.getStackInSlot(
                            1));
            helper.assertTrue(feeder.isEmpty(), "the feeding hopper must be empty");
            Container chest = GameTestHelpers.blockEntity(helper, take.below(), ChestBlockEntity.class);
            helper.assertTrue(
                    te.inventory.getStackInSlot(8).isEmpty() && chest.countItem(ModItems.powder_neodymium.get()) == 5,
                    "the hopper below must empty output slot 8 into the chest, left " + te.inventory.getStackInSlot(8));
            helper.succeed();
        });
    }


    /**
     * One schrabidium pellet in a running Watz, against the 1.7 loop: burn = 1.5 x (2,000 + last reaction flux) / sqrt(heat + 100) x 10,
     * 20 heat a flux, 0.005 mud a flux, heat x 0.99 each tick.
     */
    public static void watzPelletRun(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(8, 1);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.watz.get(), c);
        helper.setBlock(c.above(3), ModMachineBlocks.watz_pump.get().defaultBlockState());
        // 1.7 getIndirectPowerLevelTo(x, y + 5, z, 0): the redstone block sits on y + 5 itself
        helper.setBlock(c.above(5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        TileEntityWatz watz = GameTestHelpers.blockEntity(helper, c, TileEntityWatz.class);
        ItemWatzPellet item = ModWatzItems.fresh(EnumWatzType.SCHRABIDIUM);
        watz.inventory.setStackInSlot(0, item.make());
        double[] yield = {500_000_000D};
        double[] fluxLast = {0D};
        int[] heat = {0};
        int[] mud = {0};
        double[] lastYield = {500_000_000D};
        int[] ticks = {0};
        ticks(helper, 60, k -> {
            double now = ItemWatzPellet.getYield(watz.inventory.getStackInSlot(0));
            if (now == lastYield[0]) return;
            lastYield[0] = now;
            ticks[0]++;
            double div = squirt(heat[0] + 100D) * 0.1D;
            double burn = (2000D + fluxLast[0]) * 1.5D / div;
            yield[0] -= burn;
            fluxLast[0] = burn;
            mud[0] += (int) Math.round(0.005D * burn);
            heat[0] = (int) (heat[0] + 20D * burn);
            heat[0] = (int) (heat[0] * 0.99D);
            double consumed = 500_000_000D - yield[0];
            helper.assertTrue(Math.abs((500_000_000D - now) - consumed) <= 1e-9 * consumed + 1e-6,
                    "burnt " + (500_000_000D - now) + " vs 1.7 " + consumed + " after " + ticks[0]);
            helper.assertTrue(watz.heat == heat[0], "heat " + watz.heat + " vs 1.7 " + heat[0] + " after " + ticks[0]);
            helper.assertTrue(watz.tanks[2].getFill() == mud[0],
                    "mud " + watz.tanks[2].getFill() + " vs 1.7 " + mud[0]);
            if (ticks[0] == 1) {
                // first reaction, hand worked: burn 3,000, heat 60,000 then x0.99, mud 15
                helper.assertTrue(
                        heat[0] == 59_457 && mud[0] == 15 && Math.abs(500_000_000D - now - 3_002.9426D) < 1e-3,
                        "tick 1 hand values: burn 3,002.94, heat 60,058 x 0.99, mud 15");
            }
        }, () -> {
            helper.assertTrue(ticks[0] >= 55, "the Watz ran " + ticks[0] + " ticks");
            roundTrip(helper, watz, "watz");
            helper.succeed();
        });
    }

    /**
     * No pump: nothing burns. Coolant pulls a fifth of the heat a tick at 300 TU a mB.
     */
    public static void watzCoolingAndIdle(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(8, 1);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.watz.get(), c);
        TileEntityWatz watz = GameTestHelpers.blockEntity(helper, c, TileEntityWatz.class);
        watz.inventory.setStackInSlot(0, ModWatzItems.fresh(EnumWatzType.SCHRABIDIUM).make());
        watz.heat = 60_000;
        watz.tanks[0].setFill(1_000);
        int[] heat = {60_000};
        int[] cold = {1_000};
        int[] hot = {0};
        int[] ran = {0};
        ticks(helper, 40, k -> {
            if (watz.heat == heat[0] && watz.tanks[0].getFill() == cold[0]) return;
            ran[0]++;
            int cycles = Math.min((int) (heat[0] * 0.2D / 300), cold[0]);
            heat[0] -= cycles * 300;
            cold[0] -= cycles;
            hot[0] += cycles;
            heat[0] = (int) (heat[0] * 0.99D);
            helper.assertTrue(
                    watz.heat == heat[0] && watz.tanks[0].getFill() == cold[0] && watz.tanks[1].getFill() == hot[0],
                    "tick " + ran[0] + ": heat " + watz.heat + "/" + heat[0] + " coolant " + watz.tanks[0].getFill() + "/" + cold[0]
                            + " hot " + watz.tanks[1].getFill() + "/" + hot[0]);
            if (ran[0] == 1)
                helper.assertTrue(heat[0] == 47_520 && cold[0] == 960 && hot[0] == 40, "tick 1 hand values");
            helper.assertTrue(ItemWatzPellet.getYield(watz.inventory.getStackInSlot(0)) == 500_000_000D,
                    "an unpumped Watz does not burn");
        }, () -> {
            helper.assertTrue(ran[0] >= 30, "ran " + ran[0]);
            helper.succeed();
        });
    }


    /**
     * Three MEU rods in the fixture reactor: flux, core and hull heat and progress every tick, from 1.7's formulas.
     */
    public static void pwrFuelRun(GameTestHelper helper) {
        TileEntityPWRController pwr = PwrGameTests.build(helper);
        helper.assertTrue(pwr.assembled, "PWR did not assemble");
        BlockPos pos = pwr.getBlockPos();
        pwr.flux = 6_000D;
        pwr.inventory.setStackInSlot(0, new ItemStack(ModPwrItems.fuel(EnumPWRFuel.MEU), 3));
        pwr.setupTanks();

        double connections = 2D;
        double xe = 1D - Math.pow(Math.E, -connections / 300D);
        double used = connections / 10D * (1D - xe) + connections / 150D * xe;
        double approach = (1D - Math.pow(Math.E, -((double) 1 * 5 / 4) / 2D)) / 2D;
        double flux = 6_000D;
        long core = 0, hull = 0;
        double progress = 0D;
        int amount = 0;
        for (int t = 1; t <= 40; t++) {
            pwr.runReactor(helper.getLevel(), pos);
            if (t <= 3) amount++;
            double out = Math.log10(flux / 3D / 2500D + 1D) * 600D;
            double total = out * amount * used;
            core += total * 5.0D;
            int newFlux = 20;
            newFlux += total;
            progress += total;
            long average = (core + hull) / 2;
            core -= (core - average) * approach;
            hull -= (hull - average) * approach;
            core *= 0.999D;
            hull *= 0.999D;
            flux = newFlux;
            helper.assertTrue(
                    pwr.coreHeat == core && pwr.hullHeat == hull && pwr.flux == flux && pwr.amountLoaded == amount
                            && Math.abs(pwr.progress - progress) < 1e-9,
                    "tick " + t + ": core " + pwr.coreHeat + "/" + core + " hull " + pwr.hullHeat + "/" + hull + " flux " + pwr.flux + "/" + flux
                            + " progress " + pwr.progress + "/" + progress);
            if (t == 1) {
                helper.assertTrue(core == 133 && hull == 16 && flux == 50D && Math.abs(progress - 30.4427) < 1e-3,
                        "tick 1 hand values: 30.4427 output, core 133, hull 16, flux 50");
            }
        }
        helper.assertTrue(pwr.inventory.getStackInSlot(0).isEmpty(), "the three rods are loaded");
        helper.assertTrue(pwr.processTime == 1_000_000_000D, "MEU yield 1e9");
        roundTrip(helper, pwr, "pwr controller");
        helper.succeed();
    }


    private static void plates(TileEntityReactorResearch reactor, PlateFuelType type, int... slots) {
        for (int slot : slots)
            reactor.inventory.setStackInSlot(slot, new ItemStack(ModIngredientItems.plateFuel(type)));
    }

    /**
     * Pu-238/Be (passive 50) in slot 5 lights a U-235 plate (sqrt(flux) x 40 / 10) in slot 0 through the hex neighbours:
     * dry, heat = 155 n - 56 (n ticks) and flux 78; the rest is checked against the float cooling formula with water.
     */
    public static void researchReactorRun(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(8, 3);
        FoldedTestSupport.assemble(helper, ModNuclearMiscBlocks.REACTOR_RESEARCH.get(), c);
        TileEntityReactorResearch reactor = GameTestHelpers.blockEntity(helper, c, TileEntityReactorResearch.class);
        plates(reactor, PlateFuelType.U235, 0);
        plates(reactor, PlateFuelType.PU238BE, 5);
        reactor.rodLevel = 1D;
        reactor.targetLevel = 1D;
        ItemStack pu = reactor.inventory.getStackInSlot(5);
        int[] n = {0};
        ticks(helper, 40, k -> {
            int ran = ItemPlateFuel.getLifeTime(reactor.inventory.getStackInSlot(5)) / 50;
            if (ran == n[0]) return;
            n[0] = ran;
            helper.assertTrue(reactor.heat == 155 * ran - 56,
                    "heat " + reactor.heat + " after " + ran + " ticks, 1.7 " + (155 * ran - 56));
            helper.assertTrue(reactor.totalFlux == (ran == 1 ? 50 : 78), "flux " + reactor.totalFlux);
            helper.assertTrue(ItemPlateFuel.getLifeTime(reactor.inventory.getStackInSlot(0)) == 50 * (ran - 1),
                    "u235 plate burnup " + ItemPlateFuel.getLifeTime(reactor.inventory.getStackInSlot(0)));
        }, () -> {
            helper.assertTrue(n[0] >= 30, "ran " + n[0] + " ticks");
            // wet: 14 water blocks around it, 1.7: heat -= heat * 0.07 * water / 12 in float arithmetic
            helper.setBlock(c.above(3), Blocks.WATER.defaultBlockState());
            helper.setBlock(c.below(), Blocks.WATER.defaultBlockState());
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (int i = 0; i < 3; i++) helper.setBlock(c.relative(dir).above(i), Blocks.WATER.defaultBlockState());
            }
            int[] heat = {reactor.heat};
            int[] ran = {ItemPlateFuel.getLifeTime(reactor.inventory.getStackInSlot(5)) / 50};
            int[] wet = {0};
            ticks(helper, 40, k -> {
                int now = ItemPlateFuel.getLifeTime(reactor.inventory.getStackInSlot(5)) / 50;
                if (now == ran[0]) return;
                ran[0] = now;
                wet[0]++;
                int h = heat[0] + 56 + 100;
                byte water = 14;
                h = (int) (h - h * (float) 0.07 * water / 12);
                heat[0] = h;
                helper.assertTrue(reactor.heat == h && reactor.water == 14,
                        "wet heat " + reactor.heat + "/" + h + " water " + reactor.water);
            }, () -> {
                helper.assertTrue(wet[0] >= 30, "wet ran " + wet[0]);
                roundTrip(helper, reactor, "research reactor");
                helper.succeed();
            });
        });
    }

    /**
     * Eight Pu-238/Be plates (400 flux) beside a breeder: 0.0025 x (400 / 200) progress a tick turns lithium into tritium.
     */
    public static void reactorBreederRun(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(8, 3);
        FoldedTestSupport.assemble(helper, ModNuclearMiscBlocks.REACTOR_RESEARCH.get(), c);
        FoldedTestSupport.assemble(helper, ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get(), c.east());
        TileEntityReactorResearch reactor = GameTestHelpers.blockEntity(helper, c, TileEntityReactorResearch.class);
        TileEntityMachineReactorBreeding breeder = GameTestHelpers.blockEntity(helper, c.east(),
                TileEntityMachineReactorBreeding.class);
        plates(reactor, PlateFuelType.PU238BE, 0, 1, 2, 3, 4, 5, 6, 7);
        reactor.rodLevel = 1D;
        reactor.targetLevel = 1D;
        helper.setBlock(c.above(3), Blocks.WATER.defaultBlockState());
        helper.setBlock(c.below(), Blocks.WATER.defaultBlockState());
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST}) {
            for (int i = 0; i < 3; i++) helper.setBlock(c.relative(dir).above(i), Blocks.WATER.defaultBlockState());
        }
        ItemStack lithium = new ItemStack(ModNuclearMiscItems.rod(RodSize.SINGLE, BreedingRodType.LITHIUM));
        breeder.inventory.setStackInSlot(0, lithium);
        float[] mirror = {0F};
        float[] last = {0F};
        int[] steps = {0};
        boolean[] done = {false};
        ticks(helper, 260, k -> {
            if (done[0]) return;
            if (breeder.progress > last[0]) {
                mirror[0] += 0.0025F * 2;
                steps[0]++;
                helper.assertTrue(breeder.flux == 400, "breeder flux " + breeder.flux);
                helper.assertTrue(breeder.progress == mirror[0],
                        "progress " + breeder.progress + " vs 1.7 " + mirror[0]);
                last[0] = breeder.progress;
            } else if (breeder.progress < last[0] || breeder.inventory.getStackInSlot(0).isEmpty()) {
                mirror[0] += 0.0025F * 2;
                steps[0]++;
                helper.assertTrue(mirror[0] >= 1F, "finished early at " + mirror[0]);
                helper.assertTrue(steps[0] >= 199 && steps[0] <= 201,
                        "a 200 flux rod at 400 flux takes 200 ticks, took " + steps[0]);
                helper.assertTrue(breeder.inventory.getStackInSlot(1).is(ModNuclearMiscItems.rod(RodSize.SINGLE,
                                BreedingRodType.TRITIUM)),
                        "tritium rod expected, got " + breeder.inventory.getStackInSlot(1));
                done[0] = true;
            }
        }, () -> {
            helper.assertTrue(done[0], "the lithium rod was never bred, progress " + breeder.progress);
            roundTrip(helper, breeder, "reactor breeder");
            helper.succeed();
        });
    }


    /**
     * 1.7 TileEntityWasteDrum: a hot item cools with chance 1 in 72,000 / water faces each tick, never without water; an RBMK
     * rod cools every tick and may only leave below 50 core and hull heat.
     */
    public static void wasteDrumCooling(GameTestHelper helper) {
        // 40 drums in an 8 x 5 grid on every other block, the gaps and the layers above and below full of water
        BlockPos base = GameTests.paddedCentre(WASTE_PADDING, 3).offset(-8, 0, -6);
        for (int x = -1; x <= 17; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 9; z++) helper.setBlock(base.offset(x, y, z), Blocks.WATER.defaultBlockState());
            }
        }
        TileEntityWasteDrum[] drums = new TileEntityWasteDrum[40];
        for (int i = 0; i < 40; i++) {
            BlockPos at = base.offset(i % 8 * 2, 0, i / 8 * 2);
            helper.setBlock(at, ModMachineBlocks.machine_waste_drum.get().defaultBlockState());
            drums[i] = GameTestHelpers.blockEntity(helper, at, TileEntityWasteDrum.class);
            for (int slot = 0; slot < 12; slot++) {
                drums[i].inventory.setStackInSlot(slot,
                        ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_U235.get(), 1));
            }
        }
        BlockPos dryAt = base.offset(16, 0, 2);
        for (Direction dir : Direction.values()) helper.setBlock(dryAt.relative(dir), Blocks.STONE.defaultBlockState());
        helper.setBlock(dryAt, ModMachineBlocks.machine_waste_drum.get().defaultBlockState());
        TileEntityWasteDrum dry = GameTestHelpers.blockEntity(helper, dryAt, TileEntityWasteDrum.class);
        for (int slot = 0; slot < 12; slot++) {
            dry.inventory.setStackInSlot(slot, ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_U235.get(), 1));
        }
        BlockPos rodAt = base.offset(16, 0, 0);
        helper.setBlock(rodAt, ModMachineBlocks.machine_waste_drum.get().defaultBlockState());
        TileEntityWasteDrum rodDrum = GameTestHelpers.blockEntity(helper, rodAt, TileEntityWasteDrum.class);
        ItemStack rod = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
        ItemRBMKRod.setCoreHeat(rod, 1000D);
        ItemRBMKRod.setHullHeat(rod, 1000D);
        rodDrum.inventory.setStackInSlot(0, rod);
        helper.assertFalse(rodDrum.canTakeItemThroughFace(0, rod, Direction.DOWN),
                "a 1000 degree rod must not leave the drum");
        helper.assertTrue(rodDrum.canPlaceItemThroughFace(1, rod, Direction.UP), "rods go into an empty slot");
        helper.assertFalse(dry.canTakeItemThroughFace(0, dry.inventory.getStackInSlot(0), Direction.DOWN),
                "hot waste does not leave");

        double[] hull = {1000D};
        ticks(helper, 700, k -> {
        }, () -> {
            ItemStack inside = rodDrum.inventory.getStackInSlot(0);
            double hullNow = ItemRBMKRod.getHullHeat(inside);
            double coreNow = ItemRBMKRod.getCoreHeat(inside);
            helper.assertTrue(hullNow < hull[0] && coreNow < 1000D,
                    "the rod cools: core " + coreNow + " hull " + hullNow);
            helper.assertTrue(
                    rodDrum.canTakeItemThroughFace(0, inside, Direction.DOWN) == (coreNow < 50 && hullNow < 50),
                    "rod extraction needs core and hull below 50");
            int cooled = 0;
            for (TileEntityWasteDrum drum : drums) {
                for (int slot = 0; slot < 12; slot++) {
                    ItemStack stack = drum.inventory.getStackInSlot(slot);
                    if (!FuelPoolRecipes.isHot(stack)) {
                        cooled++;
                        helper.assertTrue(stack.is(ModNuclearMiscItems.WASTE_PLATE_U235.get()),
                                "cooled form is the cold plate, got " + stack);
                    }
                }
            }
            // 40 drums x 12 slots, 1 in 12,000 a tick over 700 ticks: expect 28
            helper.assertTrue(cooled >= 8 && cooled <= 70, "cooled " + cooled + " of 480, expected about 28");
            for (int slot = 0; slot < 12; slot++) {
                helper.assertTrue(FuelPoolRecipes.isHot(dry.inventory.getStackInSlot(slot)),
                        "a drum with no water never cools");
            }
            helper.succeed();
        });
    }

    /**
     * The FuelPool cooling table and the invulnerable, never-despawning dropped waste.
     */
    public static void wasteItems(GameTestHelper helper) {
        ItemStack hot = ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_PU238BE.get(), 3);
        helper.assertTrue(FuelPoolRecipes.isHot(hot), "hot plate waste is a pool key");
        ItemStack cold = FuelPoolRecipes.cool(hot);
        helper.assertTrue(
                cold.is(ModNuclearMiscItems.WASTE_PLATE_PU238BE.get()) && cold.getCount() == 3 && !FuelPoolRecipes.isHot(
                        cold),
                "cooling keeps the count and turns the plate cold");
        helper.assertTrue(FuelPoolRecipes.cool(cold).isEmpty(), "a cold plate is not a key");
        for (EnumPWRFuel fuel : EnumPWRFuel.values()) {
            ItemStack rod = new ItemStack(ModPwrItems.hot(fuel));
            helper.assertTrue(FuelPoolRecipes.isHot(rod) && FuelPoolRecipes.cool(rod).is(ModPwrItems.depleted(fuel)),
                    "hot PWR rod " + fuel + " cools to its depleted form");
        }
        // 1.7 ItemNuclearWaste and ItemWasteLong protect their drops; ItemWasteShort extends plain Item.
        var cases = new LinkedHashMap<>(Map.<Item, Boolean>of(
                ModItems.trinitite.get(), true,
                ModItems.nuclear_waste.get(), true,
                ModItems.nuclear_waste_tiny.get(), true,
                ModItems.nuclear_waste_vitrified.get(), true,
                ModItems.nuclear_waste_vitrified_tiny.get(), true,
                hot.getItem(), true,
                Items.STICK, false));
        var families = Map.of(
                ModStorageDrumItems.nuclear_waste_long, true,
                ModStorageDrumItems.nuclear_waste_long_tiny, true,
                ModStorageDrumItems.nuclear_waste_long_depleted, true,
                ModStorageDrumItems.nuclear_waste_long_depleted_tiny, true,
                ModStorageDrumItems.nuclear_waste_short, false,
                ModStorageDrumItems.nuclear_waste_short_tiny, false,
                ModStorageDrumItems.nuclear_waste_short_depleted, false,
                ModStorageDrumItems.nuclear_waste_short_depleted_tiny, false);
        for (var test : families.entrySet()) {
            for (var variant = 0; variant < test.getKey().size(); variant++) {
                cases.put(test.getKey().get(variant), test.getValue());
            }
        }
        var failures = new ArrayList<String>();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        for (var test : cases.entrySet()) {
            var entity = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    new ItemStack(test.getKey()));
            helper.getLevel().addFreshEntity(entity);
            try {
                if (entity.isInvulnerable() != test.getValue()
                        || (entity.getAge() == Short.MIN_VALUE) != test.getValue()) {
                    failures.add(test.getKey() + ": expected protection=" + test.getValue()
                            + ", invulnerable=" + entity.isInvulnerable() + ", age=" + entity.getAge());
                }
            } finally {
                entity.discard();
            }
        }
        helper.assertTrue(failures.isEmpty(), "waste drop protection mismatches: " + failures);
        helper.succeed();
    }


    /**
     * 1.7: start on a 20 tick boundary, 0.005 a tick down (200 ticks), swap, 40 tick delay, 200 ticks back up.
     */
    public static void rbmkAutoloaderRun(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(10, 1);
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_fuel_rod.get(), c);
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_autoloader.get(), c.above(4));
        BlockEntityRBMKRod rod = GameTestHelpers.blockEntity(helper, c, BlockEntityRBMKRod.class);
        BlockEntityRBMKAutoloader loader = GameTestHelpers.blockEntity(helper, c.above(4),
                BlockEntityRBMKAutoloader.class);
        ItemStack old = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
        ItemRBMKRod.setYield(old, 0.3D * ((ItemRBMKRod) old.getItem()).stats().yield());
        rod.inventory.setStackInSlot(0, old);
        loader.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));
        int[] startedAt = {-1};
        int[] swappedAt = {-1};
        int[] homeAt = {-1};
        ticks(helper, 520, k -> {
            if (startedAt[0] < 0 && loader.piston > 0) startedAt[0] = k;
            if (startedAt[0] >= 0 && swappedAt[0] < 0 && !loader.inventory.getStackInSlot(9).isEmpty()) {
                swappedAt[0] = k;
                helper.assertTrue(loader.piston >= 0.99D, "the swap happens with the piston down, at " + loader.piston);
                helper.assertTrue(ItemRBMKRod.getEnrichment(rod.inventory.getStackInSlot(0)) == 1D,
                        "the new rod is the fresh one");
                helper.assertTrue(ItemRBMKRod.getEnrichment(loader.inventory.getStackInSlot(9)) < 0.31D,
                        "the old rod comes out");
                helper.assertTrue(loader.inventory.getStackInSlot(0).isEmpty(), "the fresh rod left slot 0");
            }
            if (swappedAt[0] >= 0 && homeAt[0] < 0 && loader.piston <= 0D) homeAt[0] = k;
        }, () -> {
            helper.assertTrue(startedAt[0] >= 0 && startedAt[0] <= 22,
                    "starts at the next 20 tick mark, started at " + startedAt[0]);
            int down = swappedAt[0] - startedAt[0];
            helper.assertTrue(down >= 198 && down <= 203, "200 ticks down, took " + down);
            int up = homeAt[0] - swappedAt[0];
            helper.assertTrue(up >= 238 && up <= 244, "a 40 tick delay then 200 ticks up, took " + up);
            helper.assertTrue(loader.piston == 0D, "the piston is home");
            roundTrip(helper, loader, "autoloader");
            helper.succeed();
        });
    }

    /**
     * Cycle 50: a rod above 50 percent, or a hull above 1000, leaves the loader idle; the GUI clamps the cycle to 5..95 in steps of 5.
     */
    public static void rbmkAutoloaderGates(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(10, 1);
        BlockPos[] columns = {c, c.east(4), c.east(8)};
        for (BlockPos column : columns) {
            FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_fuel_rod.get(), column);
            FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_autoloader.get(), column.above(4));
        }
        double yield = ((ItemRBMKRod) ModRBMKItems.rbmk_fuel_ueu.get()).stats().yield();
        ItemStack healthy = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
        ItemRBMKRod.setYield(healthy, 0.8D * yield);
        GameTestHelpers.blockEntity(helper, columns[0], BlockEntityRBMKRod.class).inventory.setStackInSlot(0, healthy);
        ItemStack hot = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
        ItemRBMKRod.setYield(hot, 0.2D * yield);
        ItemRBMKRod.setHullHeat(hot, 1_200D);
        BlockEntityRBMKRod hotColumn = GameTestHelpers.blockEntity(helper, columns[1], BlockEntityRBMKRod.class);
        hotColumn.inventory.setStackInSlot(0, hot);
        hotColumn.heat = 1_200D;
        BlockEntityRBMKAutoloader[] loaders = new BlockEntityRBMKAutoloader[3];
        for (int i = 0; i < 3; i++) {
            loaders[i] = GameTestHelpers.blockEntity(helper, columns[i].above(4), BlockEntityRBMKAutoloader.class);
            loaders[i].inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));
        }
        // third column is empty: it must start
        helper.assertTrue(loaders[0].isItemValidForSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get())),
                "fresh rod accepted in slot 0");
        ItemStack spent = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
        ItemRBMKRod.setYield(spent, 0.4D * yield);
        helper.assertFalse(loaders[0].isItemValidForSlot(0, spent), "a rod under the cycle percentage is refused");
        helper.assertFalse(loaders[0].isItemValidForSlot(9, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get())),
                "output slots take nothing");
        helper.assertTrue(
                loaders[0].canTakeItemThroughFace(9, spent, Direction.DOWN) && !loaders[0].canTakeItemThroughFace(0,
                        spent, Direction.DOWN),
                "only the output half can be emptied");
        loaders[0].piston = 0.5D;
        helper.assertTrue(loaders[0].getSlotsForFace(Direction.UP).length == 0,
                "no automation while the piston is out");
        loaders[0].piston = 0D;
        helper.assertTrue(loaders[0].getSlotsForFace(Direction.UP).length == 18, "all 18 slots while it is home");
        ticks(helper, 60, k -> {
        }, () -> {
            helper.assertTrue(loaders[0].piston == 0D, "an 80 percent rod is left alone");
            helper.assertTrue(loaders[1].piston == 0D, "a rod over 1000 degrees is left alone");
            helper.assertTrue(loaders[2].piston > 0D, "an empty column is loaded");
            helper.succeed();
        });
    }


    private static int consoleIndex(int dx, int dz) {
        return (dz + 7) * 15 + dx + 7;
    }

    /**
     * Seven columns in a row around the console's target: what the desk scans (type, data, 1,500 max heat), what its screens
     * average, and what the level, color and toggle commands reach. 1.7: the slider moves manual rods only.
     */
    public static void rbmkConsoleScan(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(16, 1);
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_fuel_rod.get(), c.east(-3));
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_control.get(), c.east(-2));
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_boiler.get(), c.east(-1));
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_control_auto.get(), c);
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_absorber.get(), c.east(1));
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_fuel_rod.get(), c.east(2));
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_control.get(), c.east(3));
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_console.get(), c.north(10), Direction.SOUTH);
        BlockEntityRBMKRod fuelA = GameTestHelpers.blockEntity(helper, c.east(-3), BlockEntityRBMKRod.class);
        BlockEntityRBMKRod fuelB = GameTestHelpers.blockEntity(helper, c.east(2), BlockEntityRBMKRod.class);
        BlockEntityRBMKControlManual manualA = GameTestHelpers.blockEntity(helper, c.east(-2),
                BlockEntityRBMKControlManual.class);
        BlockEntityRBMKControlManual manualB = GameTestHelpers.blockEntity(helper, c.east(3),
                BlockEntityRBMKControlManual.class);
        BlockEntityRBMKControlAuto auto = GameTestHelpers.blockEntity(helper, c, BlockEntityRBMKControlAuto.class);
        BlockEntityRBMKBoiler boiler = GameTestHelpers.blockEntity(helper, c.east(-1), BlockEntityRBMKBoiler.class);
        BlockEntityRBMKConsole console = GameTestHelpers.blockEntity(helper, c.north(10), BlockEntityRBMKConsole.class);
        double yield = ((ItemRBMKRod) ModRBMKItems.rbmk_fuel_ueu.get()).stats().yield();
        ItemStack worn = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
        ItemRBMKRod.setYield(worn, 0.8D * yield);
        fuelA.inventory.setStackInSlot(0, worn);
        fuelB.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));
        manualA.level = manualA.targetLevel = 0.4D;
        manualB.level = manualB.targetLevel = 0.9D;
        boiler.feed.setFill(3_000);
        boiler.steam.setFill(500);
        BlockPos abs = helper.absolutePos(c);
        console.setTarget(abs.getX(), abs.getY(), abs.getZ());
        int a = consoleIndex(-3, 0), mA = consoleIndex(-2, 0), bo = consoleIndex(-1, 0), au = consoleIndex(0, 0),
                ab = consoleIndex(1, 0), b = consoleIndex(2, 0), mB = consoleIndex(3, 0);
        ticks(helper, 30, k -> {
        }, () -> {
            helper.assertTrue(
                    console.columns[a] instanceof RBMKColumn.FuelColumn && console.columns[a].type == RBMKColumnType.FUEL,
                    "fuel column type");
            helper.assertTrue(
                    console.columns[mA].type == RBMKColumnType.CONTROL && console.columns[au].type == RBMKColumnType.CONTROL_AUTO,
                    "control types " + console.columns[mA].type + " " + console.columns[au].type);
            helper.assertTrue(
                    console.columns[bo].type == RBMKColumnType.BOILER && console.columns[ab].type == RBMKColumnType.ABSORBER,
                    "boiler and absorber types");
            helper.assertTrue(
                    console.columns[consoleIndex(-3, 1)] == null && console.columns[consoleIndex(4, 0)] == null,
                    "empty cells scan as null");
            for (int i : new int[]{a, mA, bo, au, ab, b, mB}) {
                helper.assertTrue(console.columns[i].maxHeat == 1_500D,
                        "1.7 maxHeat() is 1500, got " + console.columns[i].maxHeat);
            }
            RBMKColumn.FuelColumn fuel = (RBMKColumn.FuelColumn) console.columns[a];
            helper.assertTrue(Math.abs(fuel.enrichment - 0.8D) < 1e-4, "enrichment " + fuel.enrichment);
            helper.assertTrue(fuel.c_maxHeat == 2_865D, "UEU melting point 2,865, got " + fuel.c_maxHeat);
            helper.assertTrue(Math.abs(((RBMKColumn.FuelColumn) console.columns[b]).enrichment - 1D) < 1e-6,
                    "fresh rod enrichment");
            helper.assertTrue(((RBMKColumn.ControlColumn) console.columns[mA]).level == 0.4D
                    && ((RBMKColumn.ControlColumn) console.columns[mB]).level == 0.9D, "control levels");
            RBMKColumn.BoilerColumn boil = (RBMKColumn.BoilerColumn) console.columns[bo];
            helper.assertTrue(
                    boil.water == 3_000 && boil.maxWater == 10_000 && boil.steam == 500 && boil.maxSteam == 1_000_000,
                    "boiler tanks " + boil.water + "/" + boil.maxWater + " " + boil.steam + "/" + boil.maxSteam);

            console.screens[0].type = BlockEntityRBMKConsole.ScreenType.COL_TEMP;
            console.screens[0].columns = new Integer[]{a, bo};
            console.screens[1].type = BlockEntityRBMKConsole.ScreenType.FUEL_DEPLETION;
            console.screens[1].columns = new Integer[]{a, b, bo};
            console.screens[2].type = BlockEntityRBMKConsole.ScreenType.ROD_EXTRACTION;
            console.screens[2].columns = new Integer[]{mA, mB};
            console.screens[3].type = BlockEntityRBMKConsole.ScreenType.FUEL_POISON;
            console.screens[3].columns = new Integer[]{mA};
            ticks(helper, 12, k -> {
            }, () -> {
                double heat = (console.columns[a].heat + console.columns[bo].heat) / 2D;
                helper.assertTrue(
                        ("rbmk.screen.temp=" + ((int) (heat * 10D)) / 10D + "°C").equals(console.screens[0].display),
                        "temperature screen " + console.screens[0].display + " for " + heat);
                helper.assertTrue("rbmk.screen.depletion=10.0%".equals(console.screens[1].display),
                        "depletion screen " + console.screens[1].display);
                helper.assertTrue("rbmk.screen.rod=65.0%".equals(console.screens[2].display),
                        "rod screen " + console.screens[2].display);
                helper.assertTrue("rbmk.screen.xenon=0.0%".equals(console.screens[3].display),
                        "no fuel on the screen reads 0.0, got " + console.screens[3].display);

                CompoundTag level = new CompoundTag();
                level.putString("action", "level");
                level.putDouble("level", 0.2D);
                level.putIntArray("cols", new int[]{mA, au});
                double autoTarget = auto.targetLevel;
                console.receiveControl(level);
                helper.assertTrue(manualA.targetLevel == 0.2D,
                        "the slider moves a manual rod, target " + manualA.targetLevel);
                helper.assertTrue(auto.targetLevel == autoTarget,
                        "1.7 leaves auto rods alone, target went " + autoTarget + " to " + auto.targetLevel);
                CompoundTag color = new CompoundTag();
                color.putString("action", "color");
                color.putInt("color", 2);
                color.putIntArray("cols", new int[]{mB});
                console.receiveControl(color);
                helper.assertTrue(manualB.color == RBMKColor.VALUES[2], "color assigned");
                CompoundTag toggle = new CompoundTag();
                toggle.putString("action", "toggle");
                toggle.putInt("slot", 4);
                console.receiveControl(toggle);
                helper.assertTrue(console.screens[4].type == BlockEntityRBMKConsole.ScreenType.COL_TEMP,
                        "toggle advances NONE to COL_TEMP");
                console.screens[5].type = BlockEntityRBMKConsole.ScreenType.COL_TEMP;
                console.screens[5].columns = new Integer[0];
                roundTrip(helper, console, "rbmk console");
                helper.succeed();
            });
        });
    }


    /**
     * A hopper above feeds the research reactor's single 1.7 input slot and no other; the breeder takes one rod.
     */
    public static void reactorHoppers(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(10, 3);
        FoldedTestSupport.assemble(helper, ModNuclearMiscBlocks.REACTOR_RESEARCH.get(), c);
        FoldedTestSupport.assemble(helper, ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get(), c.east(4));
        TileEntityReactorResearch reactor = GameTestHelpers.blockEntity(helper, c, TileEntityReactorResearch.class);
        TileEntityMachineReactorBreeding breeder = GameTestHelpers.blockEntity(helper, c.east(4),
                TileEntityMachineReactorBreeding.class);
        helper.setBlock(c.above(3), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity rr = GameTestHelpers.blockEntity(helper, c.above(3), HopperBlockEntity.class);
        rr.setItem(0, new ItemStack(ModIngredientItems.plateFuel(PlateFuelType.U235)));
        rr.setItem(1, new ItemStack(ModIngredientItems.plateFuel(PlateFuelType.MOX)));
        helper.setBlock(c.east(4).above(3),
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        HopperBlockEntity feed = GameTestHelpers.blockEntity(helper, c.east(4).above(3), HopperBlockEntity.class);
        feed.setItem(0, new ItemStack(ModNuclearMiscItems.rod(RodSize.SINGLE, BreedingRodType.LITHIUM)));
        helper.setBlock(c.east(4).below(),
                Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(c.east(4).below(2), Blocks.CHEST.defaultBlockState());
        breeder.inventory.setStackInSlot(1,
                new ItemStack(ModNuclearMiscItems.rod(RodSize.SINGLE, BreedingRodType.TRITIUM)));
        ticks(helper, 80, k -> {
        }, () -> {
            helper.assertTrue(reactor.inventory.getStackInSlot(0).getItem() instanceof ItemPlateFuel,
                    "the first plate reaches slot 0, got "
                            + reactor.inventory.getStackInSlot(0));
            for (int i = 1; i < 12; i++)
                helper.assertTrue(reactor.inventory.getStackInSlot(i).isEmpty(),
                        "1.7 lets automation fill slot 0 only, slot " + i);
            helper.assertTrue(!rr.isEmpty(), "the second plate has nowhere to go and stays in the hopper");
            helper.assertTrue(breeder.inventory.getStackInSlot(0)
                                               .is(ModNuclearMiscItems.rod(RodSize.SINGLE, BreedingRodType.LITHIUM)),
                    "the breeder takes the rod into slot 0");
            Container chest = GameTestHelpers.blockEntity(helper, c.east(4).below(2), ChestBlockEntity.class);
            helper.assertTrue(breeder.inventory.getStackInSlot(1).isEmpty() && chest.countItem(
                            ModNuclearMiscItems.rod(RodSize.SINGLE, BreedingRodType.TRITIUM)) == 1,
                    "the finished rod leaves through a hopper below");
            helper.succeed();
        });
    }

    /**
     * A hopper feeds fuel into the PWR controller and the rod is either loaded or waiting in slot 0.
     */
    public static void pwrHopper(GameTestHelper helper) {
        TileEntityPWRController pwr = PwrGameTests.build(helper);
        helper.assertTrue(pwr.assembled, "PWR did not assemble");
        BlockPos hopperAt = new BlockPos(6, 3, 5);
        helper.setBlock(hopperAt, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.SOUTH));
        HopperBlockEntity hopper = GameTestHelpers.blockEntity(helper, hopperAt, HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(ModPwrItems.fuel(EnumPWRFuel.MEU)));
        ticks(helper, 40, k -> {
        }, () -> {
            helper.assertTrue(hopper.isEmpty(), "the hopper must hand its fuel rod to the controller");
            helper.assertTrue(pwr.amountLoaded == 1 || pwr.inventory.getStackInSlot(0).getItem() == ModPwrItems.fuel(
                            EnumPWRFuel.MEU),
                    "the rod is loaded (" + pwr.amountLoaded + ") or in slot 0 (" + pwr.inventory.getStackInSlot(
                            0) + ")");
            helper.succeed();
        });
    }


    private static void useTool(GameTestHelper helper, Player player, ItemStack tool, BlockPos at) {
        BlockPos abs = helper.absolutePos(at);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.NORTH, abs, false);
        tool.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, tool, hit));
    }

    /**
     * The linking device: a column stores its core, then a console cell, the crane desk or a display panel takes it.
     */
    public static void rbmkToolLinks(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(16, 1);
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_fuel_rod.get(), c);
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_console.get(), c.north(10), Direction.SOUTH);
        FoldedTestSupport.assemble(helper, ModRBMKBlocks.rbmk_crane_console.get(), c.east(8), Direction.WEST);
        helper.setBlock(c.west(6), ModRBMKBlocks.rbmk_display.get().defaultBlockState());
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack tool = new ItemStack(ModRBMKItems.rbmk_tool.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos core = helper.absolutePos(c);

        useTool(helper, player, tool, c.above(2));
        helper.assertTrue(ModDataComponents.RBMK_TOOL_TARGET.get(tool).orElse(-1L) == core.asLong(),
                "a click on any cell of the column stores its core");

        BlockEntityRBMKConsole console = GameTestHelpers.blockEntity(helper, c.north(10), BlockEntityRBMKConsole.class);
        useTool(helper, player, tool, c.north(10));
        helper.assertTrue(
                console.targetX() == core.getX() && console.targetY() == core.getY() && console.targetZ() == core.getZ(),
                "the console core takes the target, got " + console.targetX() + "," + console.targetY() + "," + console.targetZ());
        console.setTarget(0, 0, 0);
        helper.assertTrue(helper.getBlockState(c.north(10).above()).is(ModRBMKBlocks.rbmk_console.get()),
                "the cell above the core belongs to the console");
        useTool(helper, player, tool, c.north(10).above());
        helper.assertTrue(console.targetX() == core.getX() && console.targetZ() == core.getZ(),
                "a click on a console cell finds the core too");

        StringBuilder missed = new StringBuilder();
        int cells = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos at = c.north(10).offset(dx, dy, dz);
                    if (!helper.getBlockState(at).is(ModRBMKBlocks.rbmk_console.get())) continue;
                    cells++;
                    console.setTarget(0, 0, 0);
                    useTool(helper, player, tool, at);
                    if (console.targetX() != core.getX())
                        missed.append(" ").append(dx).append(",").append(dy).append(",").append(dz);
                }
            }
        }
        helper.assertTrue(cells > 4 && missed.length() == 0,
                "every console cell must take the link; missed (of " + cells + "):" + missed);

        BlockEntityCraneConsole crane = GameTestHelpers.blockEntity(helper, c.east(8), BlockEntityCraneConsole.class);
        useTool(helper, player, tool, c.east(8));
        helper.assertTrue(
                crane.setUpCrane && crane.centerX == core.getX() && crane.centerZ == core.getZ() && crane.centerY == core.getY() + 4,
                "the crane desk centres on the column, " + crane.centerX + "," + crane.centerY + "," + crane.centerZ);

        useTool(helper, player, tool, c.west(6));
        CompoundTag display = BlockEntities.save(
                GameTestHelpers.blockEntity(helper, c.west(6), BlockEntityRBMKDisplay.class),
                helper.getLevel().registryAccess());
        helper.assertTrue(display.toString().contains("tX:" + core.getX()),
                "the display panel takes the target: " + display);
        helper.succeed();
    }

    /**
     * 1.7 intake: with at least 100 HE it refills its 1,000 mB air tank to the brim and spends 100 HE, every tick.
     */
    public static void intakeRun(GameTestHelper helper) {
        BlockPos c = GameTests.paddedCentre(12, 1);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_intake.get(), c, Direction.EAST);
        TileEntityMachineIntake intake = GameTestHelpers.blockEntity(helper, c, TileEntityMachineIntake.class);
        intake.power = 550L;
        long[] last = {550L};
        int[] active = {0};
        ticks(helper, 12, k -> {
            if (intake.power != last[0]) {
                active[0]++;
                helper.assertTrue(last[0] - intake.power == 100L, "100 HE a tick, spent " + (last[0] - intake.power));
                helper.assertTrue(intake.compair.getFill() == 1_000,
                        "the tank is full after a powered tick: " + intake.compair.getFill());
            }
            last[0] = intake.power;
            intake.compair.setFill(0);
        }, () -> {
            helper.assertTrue(active[0] == 5 && intake.power == 50L,
                    "550 HE pay for 5 ticks and leave 50, ran " + active[0] + ", left " + intake.power);
            roundTrip(helper, intake, "intake");
            helper.succeed();
        });
    }

    /**
     * Placing the Watz item builds the reactor and drops none of the kit its core returns when broken.
     */
    public static void watzPlacesWithoutDrops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack stack = new ItemStack(ModMachineBlocks.watz.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos ground = helper.absolutePos(new BlockPos(8, 1, 8));
        level.setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
        player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false));

        AABB area = new AABB(ground).inflate(8);
        boolean built = BlockPos.betweenClosedStream(area)
                                .anyMatch(p -> level.getBlockEntity(p) instanceof TileEntityWatz);
        helper.assertTrue(built, "the Watz item placed no reactor");
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area);
        helper.assertTrue(drops.isEmpty(),
                "placing the Watz dropped " + drops.stream().map(ItemEntity::getItem).toList());
        helper.succeed();
    }
}
