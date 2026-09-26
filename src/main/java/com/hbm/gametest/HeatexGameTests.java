package com.hbm.gametest;

import com.hbm.blocks.machine.HeaterHeatex;
import net.minecraft.core.Direction;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerHeaterHeatex;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityHeaterHeatex;
import com.hbm.util.TickPhase;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Focused contracts for the legacy FT_Coolable table and Heater Heatex machine. */
public final class HeatexGameTests {

    private HeatexGameTests() {}

    public static void coolableTable(GameTestHelper helper) {
        Fluids.init();

        assertTrait(helper, Fluids.STEAM, Fluids.SPENTSTEAM, 100, 1, 200, 1D, 0.5D);
        assertTrait(helper, Fluids.HOTSTEAM, Fluids.STEAM, 1, 10, 2, 1D, 0.5D);
        assertTrait(helper, Fluids.SUPERHOTSTEAM, Fluids.HOTSTEAM, 1, 10, 18, 1D, 0.5D);
        assertTrait(helper, Fluids.ULTRAHOTSTEAM, Fluids.SUPERHOTSTEAM, 1, 10, 120, 1D, 0.5D);
        assertTrait(helper, Fluids.HOTOIL, Fluids.OIL, 1, 1, 10, 0D, 1D);
        assertTrait(helper, Fluids.HOTOIL_DS, Fluids.OIL_DS, 1, 1, 10, 0D, 1D);
        assertTrait(helper, Fluids.HOTCRACKOIL, Fluids.CRACKOIL, 1, 1, 10, 0D, 1D);
        assertTrait(helper, Fluids.HOTCRACKOIL_DS, Fluids.CRACKOIL_DS, 1, 1, 10, 0D, 1D);
        assertTrait(helper, Fluids.COOLANT_HOT, Fluids.COOLANT, 1, 1, 300, 0D, 1D);
        assertTrait(helper, Fluids.PERFLUOROMETHYL_HOT, Fluids.PERFLUOROMETHYL, 1, 1, 300, 0D, 1D);
        assertTrait(helper, Fluids.MUG_HOT, Fluids.MUG, 1, 1, 400, 0D, 1D);
        assertTrait(helper, Fluids.BLOOD_HOT, Fluids.BLOOD, 1, 1, 500, 0D, 1D);
        assertTrait(helper, Fluids.HEAVYWATER_HOT, Fluids.HEAVYWATER, 1, 1, 300, 0D, 1D);
        assertTrait(helper, Fluids.SODIUM_HOT, Fluids.SODIUM, 1, 1, 400, 0D, 1D);
        assertTrait(helper, Fluids.LEAD_HOT, Fluids.LEAD, 1, 1, 680, 0D, 1D);
        assertTrait(helper, Fluids.THORIUM_SALT_HOT, Fluids.THORIUM_SALT_DEPLETED, 1, 1, 400, 0D, 1D);

        int coolableCount = 0;
        for (FluidType fluid : Fluids.getAll()) {
            if (fluid.getTrait(FT_Coolable.class) != null) coolableCount++;
        }
        helper.assertTrue(coolableCount == 16, "FT_Coolable table is incomplete or contains an invented entry");

        helper.assertTrue(CoolingRecipes.getAll().size() == 4,
                "CoolingRecipes must project only the four turbine-capable coolable fluids");
        helper.assertTrue(CoolingRecipes.get(Fluids.STEAM).heatEnergy() == 200
                        && CoolingRecipes.get(Fluids.ULTRAHOTSTEAM).output() == Fluids.SUPERHOTSTEAM,
                "CoolingRecipes projection changed the old steam conversions");
        helper.succeed();
    }

    public static void layoutAndPorts(GameTestHelper helper) {
        Fluids.init();
        BlockPos core = new BlockPos(3, 1, 3);
        TileEntityHeaterHeatex heater = place(helper, core);
        HeaterHeatex block = ModMachineBlocks.heater_heatex.get();

        helper.assertTrue(heater.getConPos().length == 4, "Heatex must expose four fluid ports");
        FoldedTestSupport.assertPort(helper, core, core.offset(-1, 0, -1));
        FoldedTestSupport.assertPort(helper, core, core.offset(1, 0, -1));
        FoldedTestSupport.assertPort(helper, core, core.offset(-1, 0, 1));
        FoldedTestSupport.assertPort(helper, core, core.offset(1, 0, 1));
        assertPort(helper, heater.getConPos()[0], new BlockPos(4, 1, 1), ForgeDirection.NORTH);
        assertPort(helper, heater.getConPos()[1], new BlockPos(2, 1, 1), ForgeDirection.NORTH);
        assertPort(helper, heater.getConPos()[2], new BlockPos(4, 1, 5), ForgeDirection.SOUTH);
        assertPort(helper, heater.getConPos()[3], new BlockPos(2, 1, 5), ForgeDirection.SOUTH);

        BlockPos portCell = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.assertTrue(block.canConnect(Fluids.COOLANT_HOT, helper.getLevel(), portCell.getX(), portCell.getY(),
                        portCell.getZ(), Direction.NORTH), "Heatex input port is not connectable");
        helper.assertFalse(block.canConnect(Fluids.COOLANT_HOT, helper.getLevel(), portCell.getX(), portCell.getY(),
                        portCell.getZ(), Direction.EAST), "Heatex exposed a false side port");
        helper.succeed();
    }

    public static void conversionArithmetic(GameTestHelper helper) {
        Fluids.init();
        TileEntityHeaterHeatex heater = place(helper, new BlockPos(3, 1, 3));
        heater.tanks[0].setTankType(Fluids.STEAM);
        heater.tanks[0].setFill(1_000);
        TileEntityHeaterHeatex.serverTick(helper.getLevel(), heater.getBlockPos(), heater.getBlockState(), heater);

        helper.assertTrue(heater.tanks[0].getFill() == 0 && heater.tanks[1].getTankType() == Fluids.SPENTSTEAM
                        && heater.tanks[1].getFill() == 10,
                "Heatex did not apply the exact steam conversion amounts");
        helper.assertTrue(heater.heatEnergy == 1_000,
                "Heatex TU must be 200 * 10 operations * 0.5 efficiency");

        heater.tanks[0].setTankType(Fluids.COOLANT_HOT);
        heater.tanks[0].setFill(2);
        heater.tanks[1].setFill(0);
        heater.heatEnergy = 0;
        TileEntityHeaterHeatex.serverTick(helper.getLevel(), heater.getBlockPos(), heater.getBlockState(), heater);
        helper.assertTrue(heater.tanks[0].getFill() == 0 && heater.tanks[1].getFill() == 2
                        && heater.heatEnergy == 600,
                "Heatex did not apply the exact 1:1 hot-coolant TU arithmetic");
        helper.succeed();
    }

    public static void throttleDelayCapacity(GameTestHelper helper) {
        Fluids.init();
        TileEntityHeaterHeatex heater = place(helper, new BlockPos(3, 1, 3));
        heater.tanks[0].setTankType(Fluids.HOTOIL);
        heater.tanks[0].setFill(10);
        heater.tanks[1].setTankType(Fluids.OIL);
        heater.tanks[1].setFill(23_999);
        heater.amountToCool = 10;
        heater.tickDelay = 2;

        heater.tickDelay = 2;
        while (heater.tickDelay < 64
                && TickPhase.every(helper.getLevel(), heater.getBlockPos(), heater.tickDelay)) {
            heater.tickDelay++;
        }
        helper.assertTrue(heater.tickDelay < 64,
                "no throttle setting left this heatex off its own cadence, so the delayed tick cannot be tested");
        TileEntityHeaterHeatex.serverTick(helper.getLevel(), heater.getBlockPos(), heater.getBlockState(), heater);
        helper.assertTrue(heater.tanks[0].getFill() == 10 && heater.tanks[1].getFill() == 23_999,
                "Heatex converted during a delayed tick");

        heater.tickDelay = 1;
        TileEntityHeaterHeatex.serverTick(helper.getLevel(), heater.getBlockPos(), heater.getBlockState(), heater);
        helper.assertTrue(heater.tanks[0].getFill() == 9 && heater.tanks[1].getFill() == 24_000
                        && heater.heatEnergy == 10,
                "Heatex did not cap operations by output capacity on the eligible tick");

        heater.tanks[0].setFill(10);
        heater.tanks[1].setFill(0);
        heater.amountToCool = 3;
        heater.tickDelay = 1;
        TileEntityHeaterHeatex.serverTick(helper.getLevel(), heater.getBlockPos(), heater.getBlockState(), heater);
        helper.assertTrue(heater.tanks[0].getFill() == 7 && heater.tanks[1].getFill() == 3,
                "Heatex throttle did not cap operation count");
        helper.succeed();
    }

    public static void persistenceAndMenu(GameTestHelper helper) {
        Fluids.init();
        TileEntityHeaterHeatex original = place(helper, new BlockPos(3, 1, 3));
        original.tanks[0].setTankType(Fluids.LEAD_HOT);
        original.tanks[0].setFill(1234);
        original.tanks[1].setTankType(Fluids.LEAD);
        original.tanks[1].setFill(5678);
        original.heatEnergy = 987_654;
        original.amountToCool = 321;
        original.tickDelay = 17;

        var buffer = dev.golem.core.net.GolemBuffers.wrap(io.netty.buffer.Unpooled.buffer(), helper.getLevel());
        original.serialize(buffer);
        TileEntityHeaterHeatex copy = new TileEntityHeaterHeatex(BlockPos.ZERO,
                ModMachineBlocks.heater_heatex.get().defaultBlockState());
        copy.deserialize(buffer);
        helper.assertTrue(copy.tanks[0].getTankType() == original.tanks[0].getTankType()
                        && copy.tanks[0].getFill() == 1234 && copy.tanks[1].getTankType() == Fluids.LEAD
                        && copy.tanks[1].getFill() == 5678 && copy.heatEnergy == 987_654
                        && copy.amountToCool == 321 && copy.tickDelay == 17,
                "Heatex sync fields did not round-trip");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerHeaterHeatex menu = new ContainerHeaterHeatex(0, player.getInventory(), original);
        helper.assertTrue(menu.slots.size() == 37 && menu.slots.get(0).x == 80 && menu.slots.get(0).y == 72
                        && menu.slots.get(1).x == 8 && menu.slots.get(1).y == 122
                        && menu.slots.get(28).x == 8 && menu.slots.get(28).y == 180,
                "Heatex menu coordinates or slot count changed");
        helper.assertTrue(menu.slots.get(0).mayPlace(new ItemStack(Items.IRON_INGOT)),
                "Legacy Heatex identifier slot must retain the old unrestricted ItemStackHandler contract");
        helper.assertFalse(menu.clickMenuButton(player, 0), "Heatex must not invent a menu-button operation");
        helper.assertTrue(original.getAccessibleSlotsFromSide(0).length == 0,
                "Heatex must not expose its identifier slot to sided automation");

        CompoundTag controls = new CompoundTag();
        controls.putInt("toCool", 0);
        controls.putInt("delay", -4);
        original.receiveControl(controls);
        helper.assertTrue(original.amountToCool == 1 && original.tickDelay == 1,
                "Heatex control values were not clamped like the old NBT control packet");
        helper.succeed();
    }

    private static void assertTrait(GameTestHelper helper, FluidType input, FluidType output, int amountReq,
                                     int amountProduced, int heat, double turbine, double exchanger) {
        FT_Coolable trait = input.getTrait(FT_Coolable.class);
        helper.assertTrue(trait != null && trait.coolsTo == output && trait.amountReq == amountReq
                        && trait.amountProduced == amountProduced && trait.heatEnergy == heat
                        && trait.getEfficiency(FT_Coolable.CoolingType.TURBINE) == turbine
                        && trait.getEfficiency(FT_Coolable.CoolingType.HEATEXCHANGER) == exchanger,
                "Incorrect FT_Coolable data for " + input.getName());
    }

    private static TileEntityHeaterHeatex place(GameTestHelper helper, BlockPos pos) {
        HeaterHeatex block = ModMachineBlocks.heater_heatex.get();
        FoldedTestSupport.assemble(helper, block, pos);
        TileEntityHeaterHeatex heater = GameTestHelpers.blockEntity(helper, pos, TileEntityHeaterHeatex.class);
        helper.assertTrue(heater != null, "Heatex core BlockEntity was not created");
        return heater;
    }

    private static void assertPort(GameTestHelper helper, DirPos port, BlockPos expected, ForgeDirection direction) {
        helper.assertTrue(port.getPos().equals(helper.absolutePos(expected)) && port.getDir() == direction,
                "Heatex port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
