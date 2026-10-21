package com.hbm.gametest;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.tileentity.machine.TileEntityWatz;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.entity.effect.EntitySpear;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.entity.projectile.EntityRBMKDebris.DebrisType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKCooler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.registry.BlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** RBMK checks against hand-worked 1.7 values: fuel burn pipeline, columns, meltdown debris, saves, lids. */
public final class RcNukeGameTests {

    private RcNukeGameTests() {}

    private static AABB around(GameTestHelper helper, BlockPos rel) {
        return new AABB(helper.absolutePos(rel)).inflate(40);
    }

    public static void fuelDataMatchesCode(GameTestHelper helper) {
        int count = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ItemRBMKRod rod)) continue;
            count++;
            helper.assertTrue(rod.stats().equals(rod.codeStats()),
                    BuiltInRegistries.ITEM.getKey(item) + " data map entry differs from its 1.7 constants: "
                            + rod.stats() + " vs " + rod.codeStats());
        }
        helper.assertTrue(count >= 33, "expected 33 RBMK fuel rods, saw " + count);
        helper.succeed();
    }

    /** UEU rod under a constant 100 flux: the whole column tick against an independent 1.7 simulation. */
    public static void rodRunMatchesFormula(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
        BlockEntityRBMKRod rod = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKRod.class);
        rod.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));

        double yield = 100_000_000D, xenon = 0D, core = 20D, hull = 20D, heat = 20D;
        double passive = RBMKDials.getPassiveCooling(helper.getLevel());
        double provision = RBMKDials.getFuelHeatProvision(helper.getLevel());
        double sumOut = 0D;
        for (int t = 0; t < 200; t++) {
            rod.fluxQuantity = 100D;
            rod.fluxFastRatio = 0D;
            rod.tickServer();

            double in = 100D;
            double burned = xenon - in * in / 50D;
            in *= 1D - xenon / 100D;
            burned += in * 0.5D;
            xenon = Math.max(0D, Math.min(100D, burned));
            double e = yield / 100_000_000D;
            double flux = in * (e + Math.sin(e * Math.PI) / 2D);
            double out = Math.log10(flux + 1D) * 0.5D * 15D;
            yield = Math.max(0D, yield - in);
            core = Math.max(20D, Math.min(1_000_000D, core + out * 0.65D));
            sumOut += out;
            if (core > hull) {
                double mid = (core - hull) / 2D;
                core -= mid * 0.02D;
                hull += mid * 0.02D;
            }
            if (hull > heat) {
                double give = (hull - heat) / 2D * provision;
                hull -= give;
                heat += give;
            }
            heat = Math.max(20D, heat - passive);
        }

        ItemStack stack = rod.inventory.getStackInSlot(0);
        helper.assertTrue(Math.abs(ItemRBMKRod.getYield(stack) - yield) < 1e-3,
                "yield " + ItemRBMKRod.getYield(stack) + " vs 1.7 " + yield);
        helper.assertTrue(Math.abs(ItemRBMKRod.getPoison(stack) - xenon) < 1e-9,
                "xenon " + ItemRBMKRod.getPoison(stack) + " vs 1.7 " + xenon);
        helper.assertTrue(Math.abs(ItemRBMKRod.getCoreHeat(stack) - core) < 1e-6 * core,
                "core heat " + ItemRBMKRod.getCoreHeat(stack) + " vs 1.7 " + core);
        helper.assertTrue(Math.abs(ItemRBMKRod.getHullHeat(stack) - hull) < 1e-6 * hull,
                "hull heat " + ItemRBMKRod.getHullHeat(stack) + " vs 1.7 " + hull);
        helper.assertTrue(Math.abs(rod.heat - heat) < 1e-6 * heat, "column heat " + rod.heat + " vs 1.7 " + heat);
        helper.assertTrue(sumOut > 0D, "the run must have produced flux");
        rod.inventory.setStackInSlot(0, ItemStack.EMPTY);
        rod.tickServer();
        helper.succeed();
    }

    public static void controlRates(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_control.get().defaultBlockState());
        BlockEntityRBMKControlManual control = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKControlManual.class);
        control.setTarget(1D);
        for (int i = 0; i < 100; i++) control.tickServer();
        helper.assertTrue(Math.abs(control.level - 100 * 0.00277D) < 1e-9, "100 ticks move 0.277, got " + control.level);
        double start = control.level;
        control.setTarget(0D);
        control.tickServer();
        double level = start - 0.00277D;
        double surge = Math.sin(Math.pow(1D - level, 15) * Math.PI) * start;
        helper.assertTrue(Math.abs(control.getMult() - (level + surge)) < 1e-9,
                "scram surge " + control.getMult() + " vs 1.7 " + (level + surge));
        for (int i = 0; i < 200; i++) control.tickServer();
        helper.assertTrue(control.level == 0D && control.getMult() == 0D, "rods end fully inserted, got " + control.level);

        // powered ReaSim variant: no power, no movement; each moving tick costs 5000
        BlockPos powered = new BlockPos(4, 1, 2);
        helper.setBlock(powered, ModRBMKBlocks.rbmk_control_reasim.get().defaultBlockState());
        BlockEntityRBMKControlManual reasim = GameTestHelpers.blockEntity(helper, powered, BlockEntityRBMKControlManual.class);
        reasim.setTarget(1D);
        reasim.tickServer();
        helper.assertTrue(reasim.level == 0D && !reasim.hasPower, "an unpowered ReaSim control rod must not move");
        reasim.power = 50_000;
        reasim.tickServer();
        helper.assertTrue(reasim.level > 0D && reasim.power == 45_000, "a moving tick costs 5000, power " + reasim.power);
        helper.succeed();
    }

    public static void controlAutoCurves(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_control_auto.get().defaultBlockState());
        BlockEntityRBMKControlAuto auto = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKControlAuto.class);
        auto.heatLower = 400D;
        auto.heatUpper = 800D;
        auto.levelLower = 0D;
        auto.levelUpper = 100D;
        double[][] cases = {
                {0, 600, 0.50}, {1, 600, 0.25}, {2, 600, 0.75}, {0, 100, 0.0}, {0, 900, 1.0}};
        for (double[] c : cases) {
            auto.function = BlockEntityRBMKControlAuto.RBMKFunction.values()[(int) c[0]];
            auto.heat = c[1];
            auto.tickServer();
            helper.assertTrue(Math.abs(auto.targetLevel - c[2]) < 1e-9,
                    auto.function + " at " + c[1] + ": target " + auto.targetLevel + " vs 1.7 " + c[2]);
        }
        helper.succeed();
    }

    public static void boilerSteamTypes(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_boiler.get().defaultBlockState());
        BlockEntityRBMKBoiler boiler = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKBoiler.class);
        boiler.feed.setFill(1000);
        boiler.heat = 300D;
        boiler.tickServer();
        helper.assertTrue(boiler.consumption == 1000 && boiler.output == 100_000
                        && Math.abs(boiler.heat - (200D - RBMKDials.getPassiveCooling(helper.getLevel()))) < 1e-9,
                "STEAM: " + boiler.consumption + "/" + boiler.output + " heat " + boiler.heat);

        helper.setBlock(pos, ModRBMKBlocks.rbmk_boiler.get().defaultBlockState());
        BlockEntityRBMKBoiler hot = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKBoiler.class);
        hot.steam.setTankType(Fluids.HOTSTEAM);
        hot.feed.setFill(1000);
        hot.heat = 500D;
        hot.tickServer();
        helper.assertTrue(hot.consumption == 1000 && hot.output == 10_000, "HOTSTEAM: " + hot.consumption + "/" + hot.output);

        helper.setBlock(pos, ModRBMKBlocks.rbmk_boiler.get().defaultBlockState());
        BlockEntityRBMKBoiler ultra = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKBoiler.class);
        ultra.steam.setTankType(Fluids.ULTRAHOTSTEAM);
        ultra.feed.setFill(500);
        ultra.heat = 700D;
        ultra.tickServer();
        helper.assertTrue(ultra.consumption == 500 && ultra.output == 50, "ULTRAHOTSTEAM: " + ultra.consumption + "/" + ultra.output);
        helper.succeed();
    }

    public static void coolerAndHeater(GameTestHelper helper) {
        BlockPos coolerPos = new BlockPos(3, 1, 3);
        helper.setBlock(coolerPos, ModRBMKBlocks.rbmk_cooler.get().defaultBlockState());
        BlockEntityRBMKCooler cooler = GameTestHelpers.blockEntity(helper, coolerPos, BlockEntityRBMKCooler.class);
        List<BlockEntityRBMKBase> near = new ArrayList<>();
        for (BlockPos rel : List.of(new BlockPos(4, 1, 3), new BlockPos(1, 1, 5), new BlockPos(5, 1, 1))) {
            helper.setBlock(rel, ModRBMKBlocks.rbmk_blank.get().defaultBlockState());
            BlockEntityRBMKBase be = GameTestHelpers.blockEntity(helper, rel, BlockEntityRBMKBase.class);
            be.heat = 500D;
            near.add(be);
        }
        cooler.cold.setFill(100);
        cooler.tickServer();
        helper.assertTrue(Math.abs(near.get(0).heat - 272D) < 1e-9, "adjacent column: 300 C, then heat flow, got " + near.get(0).heat);
        for (BlockEntityRBMKBase be : near.subList(1, 3)) {
            helper.assertTrue(Math.abs(be.heat - 300D) < 1e-9, "a cooling cycle removes 200 C, got " + be.heat);
        }
        helper.assertTrue(cooler.cold.getFill() == 50 && cooler.hot.getFill() == 50, "50 mB cycle: " + cooler.cold.getFill() + "/" + cooler.hot.getFill());

        BlockPos heaterPos = new BlockPos(3, 1, 6);
        helper.setBlock(heaterPos, ModRBMKBlocks.rbmk_heater.get().defaultBlockState());
        BlockEntityRBMKHeater heater = GameTestHelpers.blockEntity(helper, heaterPos, BlockEntityRBMKHeater.class);
        FT_Heatable trait = Fluids.COOLANT.getTrait(FT_Heatable.class);
        HeatingStep step = trait.getFirstStep();
        heater.feed.setFill(16_000);
        heater.heat = 2000D;
        double eff = trait.getEfficiency(HeatingType.HEATEXCHANGER);
        double perDegree = 2_000D * eff;
        int input = 16_000 / step.amountReq;
        int output = 16_000 / step.amountProduced;
        int temp = (int) Math.floor((2000D - step.typeProduced.temperature) * perDegree / step.heatReq);
        int ops = Math.min(input, Math.min(output, temp));
        heater.tickServer();
        helper.assertTrue(heater.feed.getFill() == 16_000 - step.amountReq * ops && heater.steam.getFill() == step.amountProduced * ops,
                "heat exchanger ops " + ops + ": feed " + heater.feed.getFill() + " out " + heater.steam.getFill());
        helper.succeed();
    }

    /** Every column type melting together: debris counts per type as 1.7's onMelt overrides give them. */
    public static void meltdownDebris(GameTestHelper helper) {
        BlockPos[] cells = {
                new BlockPos(3, 1, 3), new BlockPos(3, 1, 2), new BlockPos(4, 1, 3), new BlockPos(3, 1, 4),
                new BlockPos(2, 1, 3), new BlockPos(4, 1, 2), new BlockPos(2, 1, 2), new BlockPos(4, 1, 4),
                new BlockPos(2, 1, 4)};
        Block[] blocks = {
                ModRBMKBlocks.rbmk_fuel_rod.get(), ModRBMKBlocks.rbmk_control_mod.get(), ModRBMKBlocks.rbmk_absorber.get(),
                ModRBMKBlocks.rbmk_blank.get(), ModRBMKBlocks.rbmk_boiler.get(), ModRBMKBlocks.rbmk_heater.get(),
                ModRBMKBlocks.rbmk_moderator.get(), ModRBMKBlocks.rbmk_reflector.get(), ModRBMKBlocks.rbmk_outgasser.get()};
        for (int i = 0; i < cells.length; i++) helper.setBlock(cells[i], blocks[i].defaultBlockState());
        BlockEntityRBMKRod rod = GameTestHelpers.blockEntity(helper, cells[0], BlockEntityRBMKRod.class);
        rod.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));
        RBMKBase.digamma = false;
        rod.meltdown();

        int[] counts = new int[DebrisType.values().length];
        double maxUp = 0D, minUp = 99D;
        for (EntityRBMKDebris debris : helper.getLevel().getEntitiesOfClass(EntityRBMKDebris.class, around(helper, cells[0]))) {
            counts[debris.getDebrisType().ordinal()]++;
            if (debris.getDebrisType() != DebrisType.LID) {
                maxUp = Math.max(maxUp, debris.getDeltaMovement().y);
                minUp = Math.min(minUp, debris.getDeltaMovement().y);
                debris.discard();
            }
        }
        // blank: absorber 1-2, blank 1-2, boiler 1-2, heater 1-2, reflector 1-2, outgasser 4-5
        int blank = counts[DebrisType.BLANK.ordinal()];
        helper.assertTrue(blank >= 9 && blank <= 15, "BLANK debris " + blank + ", 1.7 gives 9..15");
        // control_mod 2-3 graphite, moderator 2-3
        int graphite = counts[DebrisType.GRAPHITE.ordinal()];
        helper.assertTrue(graphite >= 4 && graphite <= 6, "GRAPHITE debris " + graphite + ", 1.7 gives 4..6");
        int boron = counts[DebrisType.ROD.ordinal()];
        helper.assertTrue(boron >= 2 && boron <= 3, "ROD debris " + boron + ", 1.7 gives 2..3");
        helper.assertTrue(counts[DebrisType.ELEMENT.ordinal()] == 1, "one fuel assembly casing");
        int fuel = counts[DebrisType.FUEL.ordinal()];
        helper.assertTrue(fuel >= 1 && fuel <= RBMKDials.getColumnHeight(helper.getLevel()), "FUEL debris " + fuel);
        helper.assertTrue(minUp >= 0.25D - 1e-9 && maxUp <= 1.5D + 1e-9, "debris launch speed " + minUp + ".." + maxUp + ", 1.7 is 0.25..1.5");
        helper.assertTrue(helper.getBlockState(cells[0]).is(ModFluidNetBlocks.corium_block.get()),
                "a fuelled rod column turns into corium, got " + helper.getBlockState(cells[0]));
        helper.succeed();
    }

    public static void meltdownDigamma(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
        BlockEntityRBMKRod rod = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKRod.class);
        rod.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_drx.get()));
        rod.meltdown();
        helper.assertTrue(helper.getBlockState(pos).is(ModFluidNetBlocks.corium_block.get()),
                "a DRX rod column is corium too, got " + helper.getBlockState(pos));
        List<EntitySpear> spears = helper.getLevel().getEntitiesOfClass(EntitySpear.class, around(helper, pos).inflate(200));
        helper.assertTrue(!spears.isEmpty(), "a DRX meltdown calls the spear");
        spears.forEach(EntitySpear::discard);
        helper.getLevel().getEntitiesOfClass(EntityRBMKDebris.class, around(helper, pos)).forEach(EntityRBMKDebris::discard);
        helper.succeed();
    }

    public static void lidDropsAndScrewdriver(GameTestHelper helper) {
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos pos = new BlockPos(2, 1, 2);
        var block = ModRBMKBlocks.rbmk_blank.get();
        BlockState lidded = block.defaultBlockState().setValue(RBMKBase.LID, RBMKBase.Lid.CONCRETE);
        helper.setBlock(pos, lidded);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(pos), lidded, player, new ItemStack(block));
        helper.getLevel().setBlock(helper.absolutePos(pos), lidded, 3);
        boolean removed = block.onScrew(helper.getLevel(), player, helper.absolutePos(pos), Direction.UP, Vec3.ZERO,
                InteractionHand.MAIN_HAND, ToolType.SCREWDRIVER);
        helper.assertTrue(removed && helper.getBlockState(pos).getValue(RBMKBase.LID) == RBMKBase.Lid.NONE,
                "the screwdriver takes the lid off");
        List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around(helper, pos));
        helper.assertTrue(items.stream().anyMatch(e -> e.getItem().is(ModRBMKItems.rbmk_lid.get())), "the lid drops");
        items.forEach(ItemEntity::discard);

        BlockPos second = new BlockPos(5, 1, 5);
        BlockState glass = block.defaultBlockState().setValue(RBMKBase.LID, RBMKBase.Lid.GLASS);
        helper.setBlock(second, glass);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(second), glass, player, new ItemStack(block));
        helper.getLevel().setBlock(helper.absolutePos(second), glass, 3);
        helper.getLevel().destroyBlock(helper.absolutePos(second), false);
        items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around(helper, second));
        helper.assertTrue(items.stream().anyMatch(e -> e.getItem().is(ModRBMKItems.rbmk_lid_glass.get())),
                "breaking a glass-lidded column drops its lid");
        items.forEach(ItemEntity::discard);
        helper.succeed();
    }

    /** A running column saves and loads to the same data. */
    public static void columnsRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Block> columns = List.of(
                ModRBMKBlocks.rbmk_fuel_rod.get(), ModRBMKBlocks.rbmk_fuel_rod_moderated.get(), ModRBMKBlocks.rbmk_fuel_rod_reasim.get(),
                ModRBMKBlocks.rbmk_control.get(), ModRBMKBlocks.rbmk_control_mod.get(), ModRBMKBlocks.rbmk_control_auto.get(),
                ModRBMKBlocks.rbmk_control_reasim.get(), ModRBMKBlocks.rbmk_control_reasim_auto.get(),
                ModRBMKBlocks.rbmk_boiler.get(), ModRBMKBlocks.rbmk_moderator.get(), ModRBMKBlocks.rbmk_reflector.get(),
                ModRBMKBlocks.rbmk_absorber.get(), ModRBMKBlocks.rbmk_blank.get(), ModRBMKBlocks.rbmk_cooler.get(),
                ModRBMKBlocks.rbmk_storage.get(), ModRBMKBlocks.rbmk_heater.get(), ModRBMKBlocks.rbmk_outgasser.get());
        int n = 0;
        for (Block block : columns) {
            BlockPos rel = new BlockPos(1 + n % 6 * 2, 1, 1 + n / 6 * 2);
            n++;
            helper.setBlock(rel, block.defaultBlockState());
            BlockPos abs = helper.absolutePos(rel);
            BlockEntityRBMKBase be = GameTestHelpers.blockEntity(helper, rel, BlockEntityRBMKBase.class);
            be.heat = 777.5D;
            if (be instanceof BlockEntityRBMKRod rod) {
                ItemStack stack = new ItemStack(ModRBMKItems.rbmk_fuel_heu235.get());
                ItemRBMKRod.setYield(stack, 12_345_678D);
                ItemRBMKRod.setPoison(stack, 12.5D);
                ItemRBMKRod.setCoreHeat(stack, 900D);
                ItemRBMKRod.setHullHeat(stack, 700D);
                rod.inventory.setStackInSlot(0, stack);
            }
            if (be instanceof BlockEntityRBMKControlManual manual) manual.setTarget(0.6D);
            CompoundTag saved = BlockEntities.save(be, level.registryAccess());
            BlockEntity fresh = ((EntityBlock) block).newBlockEntity(abs, block.defaultBlockState());
            fresh.setLevel(level);
            BlockEntities.loadWithComponents(fresh, saved, level.registryAccess());
            CompoundTag again = BlockEntities.save(fresh, level.registryAccess());
            helper.assertTrue(saved.equals(again), BuiltInRegistries.BLOCK.getKey(block) + " does not round trip: " + saved + " vs " + again);
            helper.assertTrue(((BlockEntityRBMKBase) fresh).heat == 777.5D, BuiltInRegistries.BLOCK.getKey(block) + " lost its heat");
        }
        helper.succeed();
    }

    public static void icfSeedSurvivesBare(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModIcfBlocks.struct_icf_core.get().defaultBlockState());
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(helper.getBlockState(pos).is(ModIcfBlocks.struct_icf_core.get()),
                    "a bare ICF seed waits for its vessel, got " + helper.getBlockState(pos));
            helper.succeed();
        });
    }

    public static void boilerVent(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModRBMKBlocks.rbmk_boiler.get().defaultBlockState());
        BlockEntityRBMKBoiler boiler = GameTestHelpers.blockEntity(helper, pos, BlockEntityRBMKBoiler.class);
        boiler.steam.setFill(boiler.steam.getMaxFill() - 10);
        boiler.feed.setFill(1000);
        boiler.heat = 300D;
        boiler.tickServer();
        helper.assertTrue(boiler.steam.getFill() == boiler.steam.getMaxFill(), "the steam tank caps at its maximum");
        helper.assertTrue(boiler.ventDelay >= 19 && boiler.ventDelay <= 29, "an overflow vents and waits 20-29 ticks, delay " + boiler.ventDelay);
        helper.succeed();
    }

    /** Mining a column whose rod hull is at 1500 C melts the neighbours and drops no rod; a cool rod does not melt anything. */
    public static void hotRodRemoval(GameTestHelper helper) {
        BlockPos hot = new BlockPos(2, 1, 2), neighbour = new BlockPos(3, 1, 2);
        BlockPos cold = new BlockPos(2, 1, 5), coldNeighbour = new BlockPos(3, 1, 5);
        helper.setBlock(hot, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
        helper.setBlock(neighbour, ModRBMKBlocks.rbmk_blank.get().defaultBlockState());
        helper.setBlock(cold, ModRBMKBlocks.rbmk_fuel_rod.get().defaultBlockState());
        helper.setBlock(coldNeighbour, ModRBMKBlocks.rbmk_blank.get().defaultBlockState());
        for (BlockPos rel : List.of(hot, cold)) {
            BlockEntityRBMKRod rod = GameTestHelpers.blockEntity(helper, rel, BlockEntityRBMKRod.class);
            ItemStack stack = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
            ItemRBMKRod.setHullHeat(stack, rel.equals(hot) ? 2000D : 100D);
            rod.inventory.setStackInSlot(0, stack);
        }
        helper.getLevel().removeBlock(helper.absolutePos(cold), false);
        helper.assertTrue(helper.getBlockState(coldNeighbour).is(ModRBMKBlocks.rbmk_blank.get()), "a cool rod removal leaves the reactor alone");
        long droppedRods = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around(helper, hot)).stream()
                .filter(item -> item.getItem().getItem() instanceof ItemRBMKRod).count();
        helper.getLevel().removeBlock(helper.absolutePos(hot), false);
        helper.assertFalse(helper.getBlockState(neighbour).is(ModRBMKBlocks.rbmk_blank.get()), "a hot rod removal melts the connected columns");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, around(helper, hot)).stream()
                .filter(item -> item.getItem().getItem() instanceof ItemRBMKRod).count() == droppedRods,
                "a hot rod is consumed by the melt and drops nothing");
        helper.getLevel().getEntitiesOfClass(EntityRBMKDebris.class, around(helper, hot)).forEach(EntityRBMKDebris::discard);
        helper.succeed();
    }

    public static void watzMeltdown(GameTestHelper helper) {
        BlockPos core = new BlockPos(4, 1, 4);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.watz.get(), core);
        TileEntityWatz watz = GameTestHelpers.blockEntity(helper, core, TileEntityWatz.class);
        watz.meltdown(helper.getLevel(), helper.absolutePos(core));
        List<EntityShrapnel> shrapnel = helper.getLevel().getEntitiesOfClass(EntityShrapnel.class, around(helper, core));
        helper.assertTrue(shrapnel.size() == 100, "a Watz meltdown throws 100 shrapnel, saw " + shrapnel.size());
        shrapnel.forEach(EntityShrapnel::discard);
        helper.assertTrue(helper.getBlockState(core).is(ModHazardBlocks.mud_block.get()), "the core becomes mud");
        helper.succeed();
    }

    /** Machines of this domain save and load to the same data. */
    public static void machinesRoundTrip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Block> blocks = List.of(ModMachineBlocks.machine_zirnox.get(), ModMachineBlocks.watz.get(), ModIcfBlocks.icf.get(),
                ModDFCBlocks.dfc_core.get(), ModDFCBlocks.dfc_emitter.get(), ModDFCBlocks.dfc_receiver.get(),
                ModDFCBlocks.dfc_injector.get(), ModDFCBlocks.dfc_stabilizer.get(), ModMachineBlocks.machine_cyclotron.get(),
                ModMachineBlocks.machine_waste_drum.get(), ModMachineBlocks.fusion_torus.get(), ModMachineBlocks.fusion_collector.get(),
                ModMachineBlocks.fusion_klystron.get(), ModMachineBlocks.fusion_boiler.get(), ModMachineBlocks.fusion_breeder.get(),
                ModNuclearMiscBlocks.REACTOR_RESEARCH.get(), ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get());
        int n = 0;
        for (Block block : blocks) {
            BlockPos rel = new BlockPos(1 + n % 6 * 3, 1, 1 + n / 6 * 3);
            n++;
            BlockState state = block.defaultBlockState();
            if (state.hasProperty(BlockMultiblockCore.FACING)) state = state.setValue(BlockMultiblockCore.FACING, Direction.NORTH);
            helper.setBlock(rel, state);
            BlockEntity be = level.getBlockEntity(helper.absolutePos(rel));
            if (be == null) continue;
            CompoundTag saved = BlockEntities.save(be, level.registryAccess());
            BlockEntity fresh = ((EntityBlock) block).newBlockEntity(helper.absolutePos(rel), state);
            fresh.setLevel(level);
            BlockEntities.loadWithComponents(fresh, saved, level.registryAccess());
            CompoundTag again = BlockEntities.save(fresh, level.registryAccess());
            helper.assertTrue(saved.equals(again), BuiltInRegistries.BLOCK.getKey(block) + " does not round trip: " + saved + " vs " + again);
        }
        helper.succeed();
    }

    /** The crane lifts a rod out of a column and sets it back: 25 ticks down, then the action, then back up. */
    public static void craneLoadUnload(GameTestHelper helper) {
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos column = new BlockPos(5, 1, 5), desk = new BlockPos(1, 1, 1);
        var rodBlock = ModRBMKBlocks.rbmk_fuel_rod.get();
        helper.setBlock(column, rodBlock.defaultBlockState());
        rodBlock.setPlacedBy(helper.getLevel(), helper.absolutePos(column), rodBlock.defaultBlockState(), player, new ItemStack(rodBlock));
        BlockEntityRBMKRod rod = GameTestHelpers.blockEntity(helper, column, BlockEntityRBMKRod.class);
        rod.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));
        helper.setBlock(desk, ModRBMKBlocks.rbmk_crane_console.get().defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        BlockEntityCraneConsole crane = GameTestHelpers.blockEntity(helper, desk, BlockEntityCraneConsole.class);
        BlockPos abs = helper.absolutePos(column);
        crane.setTarget(abs.getX(), abs.getY(), abs.getZ());
        helper.assertTrue(crane.isAboveValidTarget(), "the carriage starts above the column");
        crane.requestLoad();
        for (int i = 0; i < 60; i++) crane.tickServer();
        helper.assertTrue(rod.inventory.getStackInSlot(0).isEmpty() && crane.hasItemLoaded(), "the crane lifts the rod out of the column");
        helper.assertTrue(crane.progress == 1D, "the hoist returns to the top, progress " + crane.progress);
        crane.requestLoad();
        for (int i = 0; i < 60; i++) crane.tickServer();
        helper.assertTrue(rod.inventory.getStackInSlot(0).is(ModRBMKItems.rbmk_fuel_ueu.get()) && !crane.hasItemLoaded(),
                "the crane sets the rod back");
        helper.succeed();
    }
}
