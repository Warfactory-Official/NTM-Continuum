package com.hbm.gametest;

import com.hbm.blocks.machine.MachineMiningLaser;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.items.ModFuelItems;
import com.hbm.items.machine.ModMoldItems;
import com.hbm.items.machine.ModStampItems;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import com.hbm.tileentity.machine.TileEntityMachinePress;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
import dev.golem.core.platform.Platform;
import dev.golem.core.world.ItemExposure;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** What other mods get through the item capability: a read-only view side-less, a hopper's access on a face. */
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
        helper.succeed();
    }

    /**
     * Pipes and storage networks (Refined Storage, AE2 buses) get a hopper's access on a face: castings out of the strand
     * caster but never its mold, the press's output from below and fuel from the side.
     */
    public static void hopperRules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos casterCore = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_strand_caster.get(), new BlockPos(5, 2, 5));
        TileEntityMachineStrandCaster caster = (TileEntityMachineStrandCaster) level.getBlockEntity(casterCore);
        caster.setItem(0, new ItemStack(ModMoldItems.mold_ingot.get()));
        caster.setItem(1, new ItemStack(Items.IRON_INGOT, 10));
        List<ItemStack> castings = ItemExposure.extract(level, casterCore, Direction.EAST, 64);
        helper.assertTrue(castings != null, "nothing answers on the strand caster's east face");
        int iron = castings.stream().filter(s -> s.is(Items.IRON_INGOT)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(iron == 10 && castings.size() == 1, "took " + castings + " from the strand caster");
        helper.assertTrue(caster.getItem(0).is(ModMoldItems.mold_ingot.get()) && caster.getItem(1).isEmpty(),
                "the strand caster now holds " + caster.getItem(0) + ", " + caster.getItem(1));
        helper.assertTrue(ItemExposure.insert(level, casterCore, Direction.EAST, new ItemStack(Items.COBBLESTONE)) == 0,
                "cobblestone went into the strand caster");

        BlockPos pressCore = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_press.get(), new BlockPos(1, 2, 1));
        TileEntityMachinePress press = (TileEntityMachinePress) level.getBlockEntity(pressCore);
        press.inventory.setStackInSlot(1, new ItemStack(ModStampItems.stamp_iron_flat.get()));
        press.inventory.setStackInSlot(3, new ItemStack(ModFuelItems.ingot_graphite.get(), 4));
        List<ItemStack> output = ItemExposure.extract(level, pressCore, Direction.DOWN, 64);
        helper.assertTrue(output != null && output.size() == 1 && output.get(0).is(ModFuelItems.ingot_graphite.get())
                && output.get(0).getCount() == 4, "took " + output + " from below the press");
        helper.assertTrue(press.inventory.getStackInSlot(1).is(ModStampItems.stamp_iron_flat.get()), "the stamp came out");
        helper.assertTrue(ItemExposure.insert(level, pressCore, Direction.NORTH, new ItemStack(Items.COAL, 8)) == 8
                && press.inventory.getStackInSlot(0).is(Items.COAL), "the press side took no coal: " + press.inventory.getStackInSlot(0));
        helper.succeed();
    }

    /**
     * Holo Inventory adds up every distinct view of a block (side-less and six faces), so at the core and at a part each
     * item must count once. A part's face still moves only that face's slots.
     */
    public static void viewerTotals(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos casterCore = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_strand_caster.get(), new BlockPos(5, 2, 5));
        TileEntityMachineStrandCaster caster = (TileEntityMachineStrandCaster) level.getBlockEntity(casterCore);
        caster.setItem(0, new ItemStack(ModMoldItems.mold_ingot.get()));
        caster.setItem(1, new ItemStack(Items.IRON_INGOT, 10));
        BlockPos pressCore = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_press.get(), new BlockPos(1, 2, 1));
        TileEntityMachinePress press = (TileEntityMachinePress) level.getBlockEntity(pressCore);
        press.inventory.setStackInSlot(0, new ItemStack(Items.COAL, 3));
        press.inventory.setStackInSlot(1, new ItemStack(ModStampItems.stamp_iron_flat.get()));
        press.inventory.setStackInSlot(3, new ItemStack(ModFuelItems.ingot_graphite.get(), 4));
        if (ItemExposure.read(level, casterCore, null) == null) {
            helper.assertTrue(!Platform.name().equals("NeoForge"), "no item view on NeoForge");
            helper.succeed();
            return;
        }

        BlockPos casterPart = partOf(level, casterCore);
        for (BlockPos pos : new BlockPos[] {casterCore, casterPart}) {
            List<ItemStack> seen = merged(level, pos);
            helper.assertTrue(count(seen, Items.IRON_INGOT) == 10 && count(seen, ModMoldItems.mold_ingot.get()) == 1
                    && seen.size() == 2, "the strand caster at " + pos + " shows " + seen);
        }
        for (BlockPos pos : new BlockPos[] {pressCore, partOf(level, pressCore)}) {
            List<ItemStack> seen = merged(level, pos);
            helper.assertTrue(count(seen, Items.COAL) == 3 && count(seen, ModFuelItems.ingot_graphite.get()) == 4
                    && count(seen, ModStampItems.stamp_iron_flat.get()) == 1 && seen.size() == 3,
                    "the press at " + pos + " shows " + seen);
        }

        List<ItemStack> castings = ItemExposure.extract(level, casterPart, Direction.EAST, 64);
        helper.assertTrue(castings != null && castings.size() == 1 && count(castings, Items.IRON_INGOT) == 10,
                "took " + castings + " from a strand caster part");
        helper.assertTrue(caster.getItem(0).is(ModMoldItems.mold_ingot.get()), "the mold came out of a part");
        helper.assertTrue(ItemExposure.insert(level, casterPart, Direction.EAST, new ItemStack(Items.COBBLESTONE)) == 0,
                "cobblestone went into a strand caster part");
        helper.succeed();
    }

    /**
     * Holo Inventory's read: a {@code Container} block entity directly, else the side-less view and each face's, skipping
     * a view equal to one already counted.
     */
    private static List<ItemStack> merged(ServerLevel level, BlockPos pos) {
        List<List<ItemStack>> views = new ArrayList<>();
        if (level.getBlockEntity(pos) instanceof Container container) {
            List<ItemStack> all = new ArrayList<>();
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!container.getItem(slot).isEmpty()) all.add(container.getItem(slot));
            }
            views.add(all);
        } else {
            List<Direction> sides = new ArrayList<>();
            sides.add(null);
            sides.addAll(List.of(Direction.values()));
            for (Direction side : sides) {
                List<ItemStack> view = ItemExposure.read(level, pos, side);
                if (view == null || view.isEmpty() || views.stream().anyMatch(seen -> sameView(seen, view))) continue;
                views.add(view);
            }
        }
        List<ItemStack> total = new ArrayList<>();
        for (List<ItemStack> view : views) {
            for (ItemStack stack : view) {
                ItemStack match = total.stream().filter(t -> ItemStacks.sameItemAndData(t, stack)).findFirst().orElse(null);
                if (match != null) match.grow(stack.getCount());
                else total.add(stack.copy());
            }
        }
        return total;
    }

    private static boolean sameView(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).getCount() != b.get(i).getCount() || !ItemStacks.sameItemAndData(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** A cell of the multiblock cored at {@code core} that has no block entity of its own. */
    private static BlockPos partOf(ServerLevel level, BlockPos core) {
        Block block = level.getBlockState(core).getBlock();
        for (BlockPos pos : BlockPos.betweenClosed(core.offset(-3, 0, -7), core.offset(3, 3, 7))) {
            if (level.getBlockState(pos).is(block) && level.getBlockEntity(pos) == null) return pos.immutable();
        }
        throw new IllegalStateException("no part around " + core);
    }
}
