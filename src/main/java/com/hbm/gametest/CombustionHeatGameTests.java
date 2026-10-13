package com.hbm.gametest;

import com.hbm.blocks.machine.HeaterElectric;
import com.hbm.blocks.machine.HeaterFirebox;
import com.hbm.blocks.machine.HeaterOilburner;
import com.hbm.blocks.machine.MachineHeatBoilerIndustrial;
import com.hbm.blocks.machine.MachineWoodBurner;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
import com.hbm.tileentity.machine.TileEntityHeaterElectric;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import com.hbm.tileentity.machine.TileEntityHeaterOven;
import com.hbm.tileentity.machine.TileEntityMachineWoodBurner;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Focused contracts for the solid/liquid heaters and the industrial heat boiler. */
public final class CombustionHeatGameTests {

    private CombustionHeatGameTests() {}


    public static void woodBurnerLayoutAndFuel(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        MachineWoodBurner block = ModMachineBlocks.machine_wood_burner.get();
        BlockPos core = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);

        TileEntityMachineWoodBurner burner = GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachineWoodBurner.class);
        helper.assertTrue(burner != null, "Wood burner core BlockEntity was not created");
        FoldedTestSupport.assertPort(helper, relative, relative.south());
        FoldedTestSupport.assertPort(helper, relative, relative.east().south());

        DirPos[] ports = burner.getConPos();
        helper.assertTrue(ports.length == 2, "Wood burner must expose two ports");
        helper.assertTrue(ports[0].getPos().equals(core.south(2)) && ports[0].getDir() == ForgeDirection.SOUTH,
                "Wood burner first port does not match the legacy layout");
        helper.assertTrue(ports[1].getPos().equals(core.east().south(2)) && ports[1].getDir() == ForgeDirection.SOUTH,
                "Wood burner second port does not match the legacy layout");

        burner.inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        burner.isOn = true;
        TileEntityMachineWoodBurner.serverTick(helper.getLevel(), core, burner.getBlockState(), burner);
        helper.assertTrue(burner.burnTime > 0 && burner.inventory.getStackInSlot(0).isEmpty(),
                "Wood burner did not start a solid fuel burn");
        TileEntityMachineWoodBurner.serverTick(helper.getLevel(), core, burner.getBlockState(), burner);
        helper.assertTrue(burner.power == 100 && burner.powerGen == 100,
                "Wood burner did not generate the legacy 100 HE/t");
        helper.succeed();
    }

    public static void fireboxFuelAndHeat(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        HeaterFirebox block = ModMachineBlocks.heater_firebox.get();
        helper.setBlock(relative, block.defaultBlockState());
        TileEntityHeaterFirebox firebox = GameTestHelpers.blockEntity(
                helper, relative, TileEntityHeaterFirebox.class);
        helper.assertTrue(firebox != null, "Firebox core BlockEntity was not created");

        firebox.inventory.setStackInSlot(0, new ItemStack(Items.COAL));
        TileEntityHeaterFirebox.serverTick(helper.getLevel(), helper.absolutePos(relative),
                firebox.getBlockState(), firebox);
        helper.assertTrue(firebox.burnTime > 0 && firebox.burnHeat == 200,
                "Firebox coal burn did not preserve the legacy heat multiplier");
        TileEntityHeaterFirebox.serverTick(helper.getLevel(), helper.absolutePos(relative),
                firebox.getBlockState(), firebox);
        helper.assertTrue(firebox.heatEnergy == 400,
                "Firebox did not add its configured heat on both burn ticks");
        helper.succeed();
    }

    public static void heaterOvenPullsHeat(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 2, 3);
        BlockPos ovenPos = helper.absolutePos(relative);
        helper.setBlock(relative, ModMachineBlocks.heater_oven.get().defaultBlockState());
        helper.setBlock(relative.below(), ModMachineBlocks.heater_firebox.get().defaultBlockState());

        TileEntityHeaterOven oven = GameTestHelpers.blockEntity(
                helper, relative, TileEntityHeaterOven.class);
        TileEntityHeaterFirebox source = GameTestHelpers.blockEntity(
                helper, relative.below(), TileEntityHeaterFirebox.class);
        helper.assertTrue(oven != null && source != null, "Heating oven or source BlockEntity was not created");

        source.heatEnergy = 1_000;
        TileEntityHeaterOven.serverTick(helper.getLevel(), ovenPos, oven.getBlockState(), oven);
        helper.assertTrue(source.heatEnergy == 0 && oven.heatEnergy == 499,
                "Heating oven did not pull half the source heat before its normal cooling tick");
        helper.succeed();
    }

    public static void oilburnerFuelAndSetting(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        HeaterOilburner block = ModMachineBlocks.heater_oilburner.get();
        helper.setBlock(relative, block.defaultBlockState());
        TileEntityHeaterOilburner burner = GameTestHelpers.blockEntity(
                helper, relative, TileEntityHeaterOilburner.class);
        helper.assertTrue(burner != null, "Oil burner core BlockEntity was not created");

        burner.tank.setTankType(Fluids.OIL);
        burner.tank.setFill(10);
        burner.setting = 3;
        burner.isOn = true;
        TileEntityHeaterOilburner.serverTick(helper.getLevel(), helper.absolutePos(relative),
                burner.getBlockState(), burner);

        helper.assertTrue(burner.tank.getFill() == 7 && burner.heatEnergy == 75,
                "Oil burner did not consume setting mB/t at the legacy oil heat rate");
        burner.toggleSettingDown();
        helper.assertTrue(burner.setting == 2, "Oil burner setting did not decrement");
        helper.succeed();
    }

    public static void electricHeaterConversion(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        HeaterElectric block = ModMachineBlocks.heater_electric.get();
        helper.setBlock(relative, block.defaultBlockState());
        TileEntityHeaterElectric heater = GameTestHelpers.blockEntity(
                helper, relative, TileEntityHeaterElectric.class);
        helper.assertTrue(heater != null, "Electric heater core BlockEntity was not created");

        heater.setting = 2;
        heater.power = heater.getConsumption();
        TileEntityHeaterElectric.serverTick(helper.getLevel(), helper.absolutePos(relative),
                heater.getBlockState(), heater);
        helper.assertTrue(heater.power == 0 && heater.heatEnergy == 200 && heater.isOn,
                "Electric heater did not apply the legacy consumption/output formula");
        helper.succeed();
    }

    public static void industrialBoilerConversion(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        MachineHeatBoilerIndustrial block = ModMachineBlocks.machine_industrial_boiler.get();
        BlockPos core = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);
        TileEntityHeatBoilerIndustrial boiler = GameTestHelpers.blockEntity(
                helper, relative, TileEntityHeatBoilerIndustrial.class);
        helper.assertTrue(boiler != null, "Industrial boiler core BlockEntity was not created");

        for (BlockPos port : new BlockPos[]{relative.east(), relative.west(), relative.north(),
                relative.south(), relative.above(4)}) {
            FoldedTestSupport.assertPort(helper, relative, port);
        }

        boiler.tanks[0].setTankType(Fluids.WATER);
        boiler.tanks[0].setFill(1);
        boiler.heat = TileEntityHeatBoilerIndustrial.maxHeat;
        TileEntityHeatBoilerIndustrial.serverTick(helper.getLevel(), core, boiler.getBlockState(), boiler);

        helper.assertTrue(boiler.tanks[0].getFill() == 0 && boiler.tanks[1].getFill() == 100,
                "Industrial boiler consumed or produced the wrong water/steam amount");
        // 1.7 tryPullHeat: with no heat source below, the buffer first cools by heat / 1000, then one
        // water operation costs 200 TU.
        int expected = TileEntityHeatBoilerIndustrial.maxHeat - TileEntityHeatBoilerIndustrial.maxHeat / 1000 - 200;
        helper.assertTrue(boiler.heat == expected && boiler.isOn,
                "Industrial boiler consumed the wrong heat amount: " + boiler.heat + " != " + expected);
        helper.succeed();
    }
}
