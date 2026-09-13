package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineSteamEngine;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGearItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineSteamEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Regression tests for the legacy steam engine and its missing construction component. */
public final class SteamEngineGameTests {

    private SteamEngineGameTests() {}


    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineSteamEngine block = ModMachineBlocks.machine_steam_engine.get();
        helper.setBlock(core, block.defaultBlockState());
        TileEntityMachineSteamEngine engine = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, core, TileEntityMachineSteamEngine.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(core), Direction.NORTH);

        helper.assertTrue(engine != null, "Steam engine core BlockEntity was not created");
        assertMeta(helper, core.offset(1, 1, 0), 7);
        assertMeta(helper, core.offset(1, 1, -1), 7);
        assertMeta(helper, core.offset(1, 1, 1), 7);
        helper.succeed();
    }

    public static void ports(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineSteamEngine block = ModMachineBlocks.machine_steam_engine.get();
        TileEntityMachineSteamEngine engine = place(helper, core);
        DirPos[] ports = engine.getConPos();

        helper.assertTrue(ports.length == 3, "Steam engine must expose three ports");
        assertPort(helper, ports[0], 5, 2, 3, ForgeDirection.EAST);
        assertPort(helper, ports[1], 5, 2, 2, ForgeDirection.EAST);
        assertPort(helper, ports[2], 5, 2, 4, ForgeDirection.EAST);

        BlockPos portCell = helper.absolutePos(core.offset(1, 1, 0));
        helper.assertTrue(block.canConnect(helper.getLevel(), portCell, ForgeDirection.EAST),
                "Steam engine must expose a cable port");
        helper.assertTrue(block.canConnect(com.hbm.inventory.fluid.Fluids.STEAM, helper.getLevel(),
                        portCell.getX(), portCell.getY(), portCell.getZ(), ForgeDirection.EAST),
                "Steam engine must accept steam at its port");
        helper.assertTrue(block.canConnect(com.hbm.inventory.fluid.Fluids.SPENTSTEAM, helper.getLevel(),
                        portCell.getX(), portCell.getY(), portCell.getZ(), ForgeDirection.EAST),
                "Steam engine must expose spent steam at its port");
        helper.assertFalse(block.canConnect(com.hbm.inventory.fluid.Fluids.HOTSTEAM, helper.getLevel(),
                        portCell.getX(), portCell.getY(), portCell.getZ(), ForgeDirection.EAST),
                "Steam engine must reject fluids not present in its tanks");
        helper.assertFalse(block.canConnect(com.hbm.inventory.fluid.Fluids.WATER, helper.getLevel(),
                        portCell.getX(), portCell.getY(), portCell.getZ(), ForgeDirection.EAST),
                "Steam engine must reject non-cooling fluids");
        helper.succeed();
    }

    public static void conversion(GameTestHelper helper) {
        TileEntityMachineSteamEngine engine = place(helper, new BlockPos(3, 1, 3));
        engine.tanks[0].setFill(200);
        TileEntityMachineSteamEngine.serverTick(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)),
                engine.getBlockState(), engine);

        helper.assertTrue(engine.tanks[0].getFill() == 0, "Steam engine consumed the wrong steam amount");
        helper.assertTrue(engine.tanks[1].getFill() == 2, "Steam engine produced the wrong spent-steam amount");
        helper.assertTrue(engine.powerBuffer == 340, "Steam engine produced the wrong HE amount");
        helper.assertTrue(engine.rotor == 0.1F, "Steam engine acceleration did not match the legacy tick");
        helper.succeed();
    }

    public static void gearAndRecipe(GameTestHelper helper) {
        ItemStack regular = ModGearItems.gear_large.get().of(0);
        ItemStack steel = ModGearItems.gear_large.get().of(1);
        helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(regular, -1) == 0,
                "Large gear default variant was not preserved");
        helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(steel, -1) == 1,
                "Large steel gear variant was not preserved");
        helper.assertTrue(steel.getHoverName().getString().equals("Large Steel Gear"),
                "Large steel gear has the wrong display name");

        AnvilRecipes.AnvilConstructionRecipe recipe = AnvilRecipes.getConstruction().stream()
                .filter(candidate -> candidate.output.get(0).stack.is(ModMachineBlocks.machine_steam_engine.get().asItem()))
                .findFirst().orElse(null);
        helper.assertTrue(recipe != null && recipe.tierLower == 2 && recipe.input.size() == 5,
                "Steam engine construction recipe is missing or malformed");
        if (recipe != null && recipe.input.size() == 5) {
            helper.assertTrue(recipe.input.get(4).matchesRecipe(regular, false),
                    "Steam engine must require regular large gear");
            helper.assertFalse(recipe.input.get(4).matchesRecipe(steel, false),
                    "Steam engine must reject large steel gear");
        }
        helper.succeed();
    }

    private static TileEntityMachineSteamEngine place(GameTestHelper helper, BlockPos pos) {
        MachineSteamEngine block = ModMachineBlocks.machine_steam_engine.get();
        helper.setBlock(pos, block.defaultBlockState());
        TileEntityMachineSteamEngine engine = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, pos, TileEntityMachineSteamEngine.class);
        block.fillSpace(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH);
        return engine;
    }

    private static void assertMeta(GameTestHelper helper, BlockPos pos, int expected) {
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(BlockDummyable.META) == expected,
                "Steam engine extra port cell mismatch at " + pos);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, int x, int y, int z,
                                   ForgeDirection direction) {
        BlockPos expected = helper.absolutePos(new BlockPos(x, y, z));
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Steam engine port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }
}
