package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineF6Tank;
import com.hbm.blocks.machine.MachineUF6Tank;
import com.hbm.blocks.machine.MachineDeuteriumTower;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineF6Tank;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityDeuteriumExtractor;
import com.hbm.tileentity.machine.TileEntityDeuteriumTower;
import com.hbm.tileentity.machine.TileEntityMachineUF6Tank;
import com.hbm.tileentity.machine.TileEntityMachineF6Tank;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Contracts for the UF6/PuF6 storage and heavy-water processing machines. */
public final class IsotopeProcessingGameTests {

    private IsotopeProcessingGameTests() {}

    public static void f6TankContract(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineUF6Tank tank = placeUf6(helper, new BlockPos(2, 1, 2));
        helper.assertTrue(tank.tank.getTankType() == Fluids.UF6, "UF6 tank has the wrong fluid type");
        helper.assertTrue(tank.tank.getMaxFill() == TileEntityMachineF6Tank.CAPACITY,
                "UF6 tank capacity must be 64,000 mB");
        helper.assertTrue(tank.tank.fill(Fluids.UF6, TileEntityMachineF6Tank.CAPACITY + 1, true)
                        == TileEntityMachineF6Tank.CAPACITY,
                "UF6 tank must accept exactly its capacity");
        helper.assertTrue(tank.tank.getFill() == TileEntityMachineF6Tank.CAPACITY,
                "UF6 tank accepted more than its capacity");
        helper.assertTrue(tank.tank.fill(Fluids.PUF6, 1, true) == 0,
                "UF6 tank accepted plutonium hexafluoride");

        MachineUF6Tank block = ModMachineBlocks.machine_uf6_tank.get();
        BlockPos core = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.assertTrue(block.canConnect(Fluids.UF6, helper.getLevel(), core.getX(), core.getY(), core.getZ(),
                        ForgeDirection.NORTH),
                "UF6 tank must expose its north fluid port");

        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerMachineF6Tank menu = new ContainerMachineF6Tank(0, player.getInventory(), tank);
        helper.assertTrue(menu.slots.size() == 40, "F6 tank menu must have four machine slots and 36 player slots");
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(Items.BUCKET)),
                "F6 tank input slot must accept buckets");
        helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.BUCKET)),
                "F6 tank output slot must be take-only");
        helper.succeed();
    }

    public static void deuteriumExtractorRatio(GameTestHelper helper) {
        Fluids.init();
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, ModMachineBlocks.machine_deuterium_extractor.get().defaultBlockState());
        TileEntityDeuteriumExtractor extractor = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityDeuteriumExtractor.class);
        extractor.tanks[0].setFill(100);
        extractor.power = extractor.getMaxPower() / 20;

        TileEntityDeuteriumExtractor.serverTick(helper.getLevel(), helper.absolutePos(relative),
                extractor.getBlockState(), extractor);

        helper.assertTrue(extractor.tanks[0].getFill() == 0,
                "heavy-water extraction must consume 100 mB of water");
        helper.assertTrue(extractor.tanks[1].getFill() == 2,
                "heavy-water extraction must preserve the 50:1 water ratio");
        helper.assertTrue(extractor.power == 0,
                "heavy-water extraction must consume one twentieth of its maximum power");
        helper.succeed();
    }

    public static void deuteriumTowerContract(GameTestHelper helper) {
        Fluids.init();
        BlockPos relative = new BlockPos(4, 1, 4);
        MachineDeuteriumTower block = ModMachineBlocks.machine_deuterium_tower.get();
        helper.setBlock(relative, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityDeuteriumTower tower = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityDeuteriumTower.class);
        block.formStructure(helper.getLevel(), helper.absolutePos(relative), ForgeDirection.NORTH);

        helper.assertTrue(tower.tanks[0].getMaxFill() == 50_000 && tower.tanks[1].getMaxFill() == 5_000,
                "heavy-water tower capacities must be 50,000 and 5,000 mB");
        DirPos[] ports = tower.getConPos();
        helper.assertTrue(ports.length == 8, "heavy-water tower must expose eight network ports");
        for (DirPos port : ports) {
            BlockPos cell = port.getPos().offset(-port.getDir().offsetX, -port.getDir().offsetY, -port.getDir().offsetZ);
            helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(),
                            port.getDir()),
                    "heavy-water tower port did not accept water at " + port.getPos());
        }
        helper.succeed();
    }

    private static TileEntityMachineUF6Tank placeUf6(GameTestHelper helper, BlockPos relative) {
        MachineUF6Tank block = ModMachineBlocks.machine_uf6_tank.get();
        helper.setBlock(relative, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityMachineUF6Tank tank = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachineUF6Tank.class);
        block.formStructure(helper.getLevel(), helper.absolutePos(relative), ForgeDirection.NORTH);
        return tank;
    }
}
