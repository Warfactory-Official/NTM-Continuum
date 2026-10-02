package com.hbm.gametest;

import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.blocks.machine.MachineBigAssTank9000;
import com.hbm.blocks.machine.MachineBigAssTank;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineGiantTank;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineBAT9000;
import com.hbm.tileentity.machine.TileEntityMachineBigAssTank;
import com.hbm.tileentity.machine.TileEntityMachineGiantTank;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import dev.golem.core.Ids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** Focused contracts for both legacy giant fluid-storage machines. */
public final class GiantTankGameTests {

    private static final TagKey<Block> PICKAXE =
            TagKey.create(Registries.BLOCK, Ids.of("minecraft", "mineable/pickaxe"));

    private GiantTankGameTests() {}

    public static void layoutsAndPorts(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineBigAssTank big = placeBigAss(helper, new BlockPos(8, 1, 8));
        MachineBigAssTank bigBlock = ModMachineBlocks.machine_bigasstank.get();
        helper.assertTrue(big.tankNew.getMaxFill() == 16_000_000, "Big Ass Tank capacity changed");
        helper.assertTrue(big.getConPos().length == 2, "Big Ass Tank must expose two end ports");
        assertPort(helper, big.getConPos()[0], new BlockPos(8, 1, 1), ForgeDirection.NORTH);
        assertPort(helper, big.getConPos()[1], new BlockPos(8, 1, 15), ForgeDirection.SOUTH);
        FoldedTestSupport.assertCell(helper, new BlockPos(8, 1, 8), new BlockPos(4, 1, 8));
        BlockPos bigPortCell = helper.absolutePos(new BlockPos(8, 1, 2));
        helper.assertTrue(bigBlock.canConnect(Fluids.WATER, helper.getLevel(), bigPortCell.getX(), bigPortCell.getY(),
                        bigPortCell.getZ(), ForgeDirection.NORTH),
                "Big Ass Tank north port must connect at its real port cell");
        BlockPos falseBigPortCell = helper.absolutePos(new BlockPos(8, 1, 3));
        helper.assertFalse(bigBlock.canConnect(Fluids.WATER, helper.getLevel(), falseBigPortCell.getX(),
                        falseBigPortCell.getY(), falseBigPortCell.getZ(), ForgeDirection.NORTH),
                "Big Ass Tank must not expose a false intermediate port");

        TileEntityMachineBAT9000 bat = placeBat(helper, new BlockPos(8, 1, 8));
        MachineBigAssTank9000 batBlock = ModMachineBlocks.machine_bat9000.get();
        helper.assertTrue(bat.tankNew.getMaxFill() == 2_048_000, "BAT-9000 capacity changed");
        helper.assertTrue(bat.getConPos().length == 8, "BAT-9000 must expose eight perimeter ports");
        assertPort(helper, bat.getConPos()[0], new BlockPos(9, 1, 11), ForgeDirection.SOUTH);
        assertPort(helper, bat.getConPos()[4], new BlockPos(11, 1, 9), ForgeDirection.EAST);
        BlockPos batPortCell = helper.absolutePos(new BlockPos(9, 1, 10));
        helper.assertTrue(batBlock.canConnect(Fluids.WATER, helper.getLevel(), batPortCell.getX(), batPortCell.getY(),
                        batPortCell.getZ(), ForgeDirection.SOUTH),
                "BAT-9000 south port must connect at its real port cell");
        helper.succeed();
    }

    public static void containerBehavior(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineGiantTank tank = placeBat(helper, new BlockPos(4, 1, 4));
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerMachineGiantTank menu = new ContainerMachineGiantTank(0, player.getInventory(), tank);
        helper.assertTrue(menu.slots.size() == 42, "Giant tank menu must have six machine + 36 player slots");
        helper.assertTrue(menu.slots.get(0).x == 8 && menu.slots.get(0).y == 17
                        && menu.slots.get(5).x == 125 && menu.slots.get(5).y == 53,
                "Giant tank menu slot layout changed");
        helper.assertFalse(menu.slots.get(1).mayPlace(new ItemStack(Items.BUCKET)),
                "Identifier output must be take-only");
        helper.assertFalse(menu.slots.get(3).mayPlace(new ItemStack(Items.BUCKET)),
                "Load output must be take-only");
        helper.assertFalse(menu.slots.get(5).mayPlace(new ItemStack(Items.BUCKET)),
                "Unload output must be take-only");

        tank.setTankType(Fluids.WATER);
        tank.inventory.setStackInSlot(2, new ItemStack(Items.WATER_BUCKET));
        TileEntityMachineGiantTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        helper.assertTrue(tank.tankNew.getFill() == 1_000 && tank.inventory.getStackInSlot(2).isEmpty()
                        && tank.inventory.getStackInSlot(3).is(Items.BUCKET),
                "Directional input bay did not drain a bucket into the tank");

        tank.inventory.setStackInSlot(4, new ItemStack(Items.BUCKET));
        TileEntityMachineGiantTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        helper.assertTrue(tank.tankNew.getFill() == 0 && tank.inventory.getStackInSlot(5).is(Items.WATER_BUCKET),
                "Directional output bay did not fill a bucket from the tank");
        helper.succeed();
    }

    public static void modeAndAntimatter(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineBAT9000 tank = placeBat(helper, new BlockPos(8, 1, 8));
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerMachineGiantTank menu = new ContainerMachineGiantTank(0, player.getInventory(), tank);
        helper.assertTrue(tank.mode == 0 && menu.clickMenuButton(player, 0) && tank.mode == 1,
                "Giant tank mode button did not enter buffer mode");
        helper.assertTrue(tank.getReceivingTanks().length == 1 && tank.getSendingTanks().length == 1,
                "Buffer mode must expose both fluid directions");

        BlockPos explosionCore = new BlockPos(8, 1, 8);
        tank = placeBat(helper, explosionCore);
        tank.setTankType(Fluids.AMAT);
        tank.tankNew.setFill(1);
        TileEntityMachineGiantTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        helper.assertTrue(helper.getLevel().isEmptyBlock(helper.absolutePos(explosionCore)),
                "Antimatter must destroy the tank core before exploding");
        helper.succeed();
    }

    public static void tiltedOutputShutdown(GameTestHelper helper) {
        Fluids.init();
        ServerLevel level = helper.getLevel();
        TileEntityMachineBAT9000 tank = placeBat(helper, new BlockPos(8, 1, 8));
        tank.setTankType(Fluids.WATER);
        tank.mode = 2;
        tank.tankNew.setFill(1_000);
        tank.tilted = true;

        BlockPos ductPos = tank.getConPos()[0].getPos();
        level.setBlock(ductPos, com.hbm.blocks.ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState(), 3);
        TileEntityFluidDuctNT duct = (TileEntityFluidDuctNT) level.getBlockEntity(ductPos);
        duct.setFluidType(Fluids.WATER);

        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        NodeNetwork<DuctData> net = graph.networkAt(ductPos.asLong());
        helper.assertTrue(net != null, "Tilted-output test could not create the external fluid network");
        TileEntityMachineGiantTank.serverTick(level, tank.getBlockPos(), tank.getBlockState(), tank);
        FluidNetwork.distribute(level, graph, net);

        helper.assertTrue(tank.getFluidAvailable(Fluids.WATER, tank.tankNew.getPressure()) == 0,
                "A tilted giant tank must offer a network nothing; the net finds it either way now, so the "
                        + "tilt has to be in what it supplies");
        helper.assertTrue(tank.tankNew.getFill() == 1_000,
                "A tilted giant tank lost " + (1_000 - tank.tankNew.getFill()) + "mB to a network");
        helper.succeed();
    }

    public static void dropsAndReplacesWithFluid(GameTestHelper helper) {
        Fluids.init();
        ServerLevel level = helper.getLevel();
        MachineBigAssTank bigBlock = ModMachineBlocks.machine_bigasstank.get();
        MachineBigAssTank9000 batBlock = ModMachineBlocks.machine_bat9000.get();
        helper.assertTrue(bigBlock.defaultBlockState().is(PICKAXE) && batBlock.defaultBlockState().is(PICKAXE),
                "Both giant tanks must be in the mineable/pickaxe tag");

        // One core, reused four times. A Big Ass Tank reaches four blocks out and five up, and the
        // four placements used to sit at x = 8, 24, 40 and 56 — three of them past the 24-wide plot,
        // in whatever the runner packed alongside. Clearing between placements keeps it all at home.
        BlockPos core = new BlockPos(12, 1, 12);

        TileEntityMachineBigAssTank big = placeBigAss(helper, core);
        big.setTankType(Fluids.WATER);
        big.tankNew.withPressure(2);
        big.tankNew.setFill(12_345);
        big.mode = 1;
        ItemStack bigDrop = bigBlock.getDrops(big.getBlockState(), lootParams(level, big.getBlockPos())).get(0);
        assertDrop(helper, bigDrop, bigBlock, 12_345, 2, 1);
        clearTanks(helper, bigBlock, batBlock, helper.absolutePos(core));
        placeAndAssertBig(helper, level, bigBlock, bigDrop, helper.absolutePos(core.north(6)));
        clearTanks(helper, bigBlock, batBlock, helper.absolutePos(core));

        TileEntityMachineBAT9000 bat = placeBat(helper, core);
        bat.setTankType(Fluids.WATER);
        bat.tankNew.withPressure(3);
        bat.tankNew.setFill(6_789);
        bat.mode = 2;
        ItemStack batDrop = batBlock.getDrops(bat.getBlockState(), lootParams(level, bat.getBlockPos())).get(0);
        assertDrop(helper, batDrop, batBlock, 6_789, 3, 2);
        clearTanks(helper, bigBlock, batBlock, helper.absolutePos(core));
        placeAndAssertBat(helper, level, batBlock, batDrop, helper.absolutePos(core.north(2)));
        helper.succeed();
    }

    /** Removes both tanks' blocks around a core, so the next placement sees an empty plot. */
    private static void clearTanks(GameTestHelper helper, MachineBigAssTank bigBlock,
                                   MachineBigAssTank9000 batBlock, BlockPos core) {
        for (int x = -6; x <= 6; x++) {
            for (int y = -1; y <= 7; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = core.offset(x, y, z);
                    Block block = helper.getLevel().getBlockState(pos).getBlock();
                    if (block == bigBlock || block == batBlock) helper.getLevel().removeBlock(pos, false);
                }
            }
        }
    }

    private static LootParams.Builder lootParams(ServerLevel level, BlockPos origin) {
        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(origin))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.DIAMOND_PICKAXE));
    }

    private static void assertDrop(GameTestHelper helper, ItemStack drop, Block block, int amount, int pressure,
                                   int mode) {
        helper.assertTrue(drop.getItem() == block.asItem()
                        && ModDataComponents.fluidId(drop) == Fluids.WATER.getID()
                        && ModDataComponents.fluidAmount(drop) == amount
                        && ModDataComponents.fluidPressure(drop) == pressure
                        && ModDataComponents.GIANT_TANK_MODE.getOr(drop, -1) == mode,
                "Giant tank drop did not preserve its fluid components");
    }

    private static void placeAndAssertBig(GameTestHelper helper, ServerLevel level, MachineBigAssTank block,
                                          ItemStack stack, BlockPos clicked) {
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        level.setBlock(clicked, block.defaultBlockState(), 3);
        block.setPlacedBy(level, clicked, block.defaultBlockState(), player, stack);
        BlockPos core = clicked.south(6);
        TileEntityMachineBigAssTank restored = (TileEntityMachineBigAssTank) level.getBlockEntity(core);
        helper.assertTrue(restored != null && restored.tankNew.getTankType() == Fluids.WATER
                        && restored.tankNew.getFill() == 12_345 && restored.tankNew.getPressure() == 2
                        && restored.mode == 1,
                "Big Ass Tank survival placement did not restore fluid state");
    }

    private static void placeAndAssertBat(GameTestHelper helper, ServerLevel level, MachineBigAssTank9000 block,
                                          ItemStack stack, BlockPos clicked) {
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        level.setBlock(clicked, block.defaultBlockState(), 3);
        block.setPlacedBy(level, clicked, block.defaultBlockState(), player, stack);
        BlockPos core = clicked.south(2);
        TileEntityMachineBAT9000 restored = (TileEntityMachineBAT9000) level.getBlockEntity(core);
        helper.assertTrue(restored != null && restored.tankNew.getTankType() == Fluids.WATER
                        && restored.tankNew.getFill() == 6_789 && restored.tankNew.getPressure() == 3
                        && restored.mode == 2,
                "BAT-9000 survival placement did not restore fluid state");
    }

    private static TileEntityMachineBigAssTank placeBigAss(GameTestHelper helper, BlockPos relative) {
        MachineBigAssTank block = ModMachineBlocks.machine_bigasstank.get();
        FoldedTestSupport.assemble(helper, block, relative);
        TileEntityMachineBigAssTank tank = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachineBigAssTank.class);
        return tank;
    }

    private static TileEntityMachineBAT9000 placeBat(GameTestHelper helper, BlockPos relative) {
        MachineBigAssTank9000 block = ModMachineBlocks.machine_bat9000.get();
        FoldedTestSupport.assemble(helper, block, relative);
        TileEntityMachineBAT9000 tank = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, relative, TileEntityMachineBAT9000.class);
        return tank;
    }

    private static void assertPort(GameTestHelper helper, DirPos port, BlockPos expected, ForgeDirection direction) {
        BlockPos absolute = helper.absolutePos(expected);
        helper.assertTrue(port.getPos().equals(absolute) && port.getDir() == direction,
                "Giant tank port mismatch: expected " + absolute + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
