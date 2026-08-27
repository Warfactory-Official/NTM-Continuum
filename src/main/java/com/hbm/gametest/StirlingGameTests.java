package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineStirling;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityStirling;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Focused contracts for the heat-powered Stirling engine family. */
public final class StirlingGameTests {

    private StirlingGameTests() {}

    private static final int FACING_NORTH = BlockDummyable.offset + 2;

    public static void layout(GameTestHelper helper) {
        BlockPos core = new BlockPos(3, 1, 3);
        MachineStirling block = ModMachineBlocks.machine_stirling.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, FACING_NORTH));
        block.formStructure(helper.getLevel(), helper.absolutePos(core), ForgeDirection.NORTH);

        TileEntityStirling stirling = dev.golem.core.gametest.GameTestHelpers.blockEntity(
                helper, core, TileEntityStirling.class);
        helper.assertTrue(stirling != null, "Stirling core BlockEntity was not created");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(core.east())).getValue(BlockDummyable.META) >= 6,
                "Stirling east port was not marked extra");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(core.west())).getValue(BlockDummyable.META) >= 6,
                "Stirling west port was not marked extra");
        helper.succeed();
    }

    public static void ports(GameTestHelper helper) {
        TileEntityStirling stirling = place(helper, new BlockPos(3, 1, 3));
        DirPos[] ports = stirling.getConPos();
        BlockPos core = helper.absolutePos(new BlockPos(3, 1, 3));

        helper.assertTrue(ports.length == 4, "Stirling must expose four ports");
        assertPort(helper, ports[0], core.east(2), ForgeDirection.EAST);
        assertPort(helper, ports[1], core.west(2), ForgeDirection.WEST);
        assertPort(helper, ports[2], core.south(2), ForgeDirection.SOUTH);
        assertPort(helper, ports[3], core.north(2), ForgeDirection.NORTH);
        helper.assertTrue(ModMachineBlocks.machine_stirling.get().canConnect(helper.getLevel(), core.east(), ForgeDirection.EAST),
                "Stirling must expose its east cable port");
        helper.succeed();
    }

    public static void conversion(GameTestHelper helper) {
        TileEntityStirling stirling = place(helper, new BlockPos(3, 1, 3));
        stirling.heat = 1000;
        TileEntityStirling.serverTick(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)),
                helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(3, 1, 3))), stirling);

        helper.assertTrue(stirling.powerBuffer == 499, "Stirling conversion did not preserve the legacy 50% result");
        helper.assertTrue(stirling.heat == 0, "Stirling did not clear its per-tick heat input");
        helper.succeed();
    }

    public static void variantsAndRecipes(GameTestHelper helper) {
        MachineStirling normal = ModMachineBlocks.machine_stirling.get();
        MachineStirling steel = ModMachineBlocks.machine_stirling_steel.get();
        MachineStirling creative = ModMachineBlocks.machine_stirling_creative.get();
        helper.setBlock(new BlockPos(2, 1, 2), normal.defaultBlockState().setValue(BlockDummyable.META, 12));
        helper.setBlock(new BlockPos(5, 1, 2), steel.defaultBlockState().setValue(BlockDummyable.META, 12));
        helper.setBlock(new BlockPos(8, 1, 2), creative.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityStirling normalTe = (TileEntityStirling) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 2)));
        TileEntityStirling steelTe = (TileEntityStirling) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(5, 1, 2)));
        TileEntityStirling creativeTe = (TileEntityStirling) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(8, 1, 2)));
        helper.assertTrue(normalTe.getGearVariant() == 0 && normalTe.maxHeat() == 300,
                "Normal Stirling variant is wrong");
        helper.assertTrue(steelTe.getGearVariant() == 1 && steelTe.maxHeat() == 1500,
                "Steel Stirling variant is wrong");
        helper.assertTrue(creativeTe.isCreative() && creativeTe.maxHeat() == 1500,
                "Creative Stirling variant is wrong");

        AnvilRecipes.AnvilConstructionRecipe normalRecipe = findRecipe(normal.asItem());
        AnvilRecipes.AnvilConstructionRecipe steelRecipe = findRecipe(steel.asItem());
        helper.assertTrue(normalRecipe != null && normalRecipe.input.size() == 5,
                "Normal Stirling construction recipe is missing");
        helper.assertTrue(steelRecipe != null && steelRecipe.input.size() == 5,
                "Steel Stirling construction recipe is missing");
        helper.succeed();
    }

    public static void missingGearPlacement(GameTestHelper helper) {
        MachineStirling block = ModMachineBlocks.machine_stirling.get();
        BlockPos clicked = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.getLevel().setBlock(clicked, block.defaultBlockState(), 3);
        ItemStack stack = new ItemStack(block);
        ModDataComponents.ITEM_VARIANT.set(stack, 1);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(
                helper, net.minecraft.world.level.GameType.SURVIVAL);
        player.setYRot(0F);
        block.setPlacedBy(helper.getLevel(), clicked, block.defaultBlockState(), player, stack);

        BlockPos core = clicked.south();
        TileEntityStirling stirling = (TileEntityStirling) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(stirling != null && !stirling.hasCog,
                "Missing-gear Stirling item must place without its gear");
        ItemStack clone = block.getCloneItemStack(helper.getLevel(), core, helper.getLevel().getBlockState(core));
        helper.assertTrue(ModDataComponents.ITEM_VARIANT.getOr(clone, 0) == 1,
                "Cloning a missing-gear Stirling must preserve its item variant");
        helper.succeed();
    }

    private static TileEntityStirling place(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        MachineStirling block = ModMachineBlocks.machine_stirling.get();
        helper.getLevel().setBlock(pos, block.defaultBlockState().setValue(BlockDummyable.META, FACING_NORTH), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityStirling,
                "Stirling core BlockEntity was not created");
        block.formStructure(helper.getLevel(), pos, ForgeDirection.NORTH);
        return (TileEntityStirling) helper.getLevel().getBlockEntity(pos);
    }

    private static void assertPort(GameTestHelper helper, DirPos port, BlockPos expected, ForgeDirection direction) {
        helper.assertTrue(port.getPos().equals(expected) && port.getDir() == direction,
                "Stirling port mismatch: expected " + expected + "/" + direction
                        + ", got " + port.getPos() + "/" + port.getDir());
    }

    private static AnvilRecipes.AnvilConstructionRecipe findRecipe(net.minecraft.world.item.Item item) {
        return AnvilRecipes.getConstruction().stream()
                .filter(candidate -> candidate.output.get(0).stack.is(item))
                .findFirst().orElse(null);
    }
}
