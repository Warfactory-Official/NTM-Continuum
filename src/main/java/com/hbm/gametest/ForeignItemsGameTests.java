package com.hbm.gametest;

import com.hbm.blocks.machine.MachineMiningLaser;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import dev.golem.core.platform.Platform;
import dev.golem.core.world.ItemExposure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The read-only item view other mods' viewers get on NeoForge: it shows a machine's items from any part and moves none. */
public final class ForeignItemsGameTests {

    private ForeignItemsGameTests() {}

    public static void readOnlyView(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(2, 2, 2));
        MachineMiningLaser block = ModMachineBlocks.machine_mining_laser.get();
        level.setBlock(core, block.defaultBlockState().setValue(BlockMultiblockCore.FACING, Direction.NORTH), 3);
        block.fillSpace(level, core, Direction.NORTH);
        TileEntityMachineMiningLaser laser = (TileEntityMachineMiningLaser) level.getBlockEntity(core);
        laser.inventory.setStackInSlot(9, new ItemStack(Items.COBBLESTONE, 32));
        laser.inventory.setStackInSlot(10, new ItemStack(Items.IRON_INGOT, 5));

        BlockPos part = core.above();
        ItemExposure.Probe view = ItemExposure.probe(level, part, null);
        if (!Platform.name().equals("NeoForge")) {
            helper.assertTrue(view == null, "a view off NeoForge");
            helper.succeed();
            return;
        }
        helper.assertTrue(view != null, "no item view at a part of the mining laser");
        helper.assertTrue(view.stacks().size() == 2 && view.stacks().get(0).is(Items.COBBLESTONE)
                && view.stacks().get(0).getCount() == 32 && view.stacks().get(1).getCount() == 5,
                "the view shows " + view.stacks());
        helper.assertTrue(view.moved() == 0, "the view moved " + view.moved() + " item(s)");
        helper.assertTrue(laser.inventory.getStackInSlot(9).getCount() == 32
                && laser.inventory.getStackInSlot(10).getCount() == 5,
                "the laser now holds " + laser.inventory.getStackInSlot(9) + ", " + laser.inventory.getStackInSlot(10));
        for (int slot = 0; slot < laser.inventory.getSlots(); slot++) {
            if (slot == 9 || slot == 10) continue;
            helper.assertTrue(laser.inventory.getStackInSlot(slot).isEmpty(), "slot " + slot + " gained "
                    + laser.inventory.getStackInSlot(slot));
        }
        for (Direction side : Direction.values()) {
            helper.assertTrue(ItemExposure.probe(level, part, side) == null, "a sided view from " + side);
        }
        helper.succeed();
    }
}
