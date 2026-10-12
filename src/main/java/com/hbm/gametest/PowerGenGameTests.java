package com.hbm.gametest;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemCassette;
import com.hbm.items.machine.ItemCassette.TrackType;
import com.hbm.blocks.machine.MachineChungus;
import com.hbm.blocks.machine.MachineHephaestus;
import com.hbm.blocks.machine.MachineIGenerator;
import com.hbm.blocks.machine.MachineLPW2;
import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.items.ModPowerGenItems;
import com.hbm.tileentity.machine.TileEntityChungus;
import com.hbm.tileentity.machine.TileEntityMachineHephaestus;
import com.hbm.tileentity.machine.TileEntityMachineIGenerator;
import com.hbm.tileentity.machine.TileEntityMachineLPW2;
import com.hbm.tileentity.machine.TileEntityMachineSiren;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** Focused contracts for the large power-generation family, one per machine. */
public final class PowerGenGameTests {

    private PowerGenGameTests() {}

    /**
     * The Leviathan eats every drop it holds in one tick: 1,000,000 mB steam is 10,000 cooling ops of 100 mB,
     * each returning 1 mB spent steam and 200 HE at 85%, and the lever cycles the grade without resizing.
     */
    public static void chungusConversion(GameTestHelper helper) {
        MachineChungus block = ModPowerGenBlocks.MACHINE_CHUNGUS.get();
        BlockPos core = FoldedTestSupport.assemble(helper, block, new BlockPos(12, 1, 12), Direction.NORTH);
        TileEntityChungus turbine = (TileEntityChungus) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(turbine != null, "Leviathan core block entity was not created");

        // 1.7 fillSpace: the front connector at (core + 4·dir, y+2) and the power connector at core − 10·dir.
        FoldedTestSupport.assertPort(helper, new BlockPos(12, 1, 12), new BlockPos(12, 3, 8));
        FoldedTestSupport.assertPort(helper, new BlockPos(12, 1, 12), new BlockPos(12, 1, 22));
        FoldedTestSupport.assertCell(helper, new BlockPos(12, 1, 12), new BlockPos(12, 5, 10));

        turbine.tanks[0].setFill(1_000_000);
        tick(helper, turbine);
        helper.assertTrue(turbine.tanks[0].getFill() == 0, "Leviathan must consume all of its steam in one tick");
        helper.assertTrue(turbine.tanks[1].getTankType() == Fluids.SPENTSTEAM && turbine.tanks[1].getFill() == 10_000,
                "Leviathan must return 10,000 mB spent steam, got " + turbine.tanks[1].getFill());
        helper.assertTrue(turbine.powerBuffer == 1_700_000L,
                "Leviathan must buffer 1,700,000 HE at 85% efficiency, got " + turbine.powerBuffer);
        helper.assertTrue(turbine.operational, "Leviathan must report operational while converting");

        tick(helper, turbine);
        helper.assertTrue(!turbine.operational && turbine.powerBuffer == 0L,
                "Leviathan must idle with an empty buffer once the steam is gone");

        turbine.onLeverPull();
        helper.assertTrue(turbine.tanks[0].getTankType() == Fluids.HOTSTEAM
                        && turbine.tanks[1].getTankType() == Fluids.STEAM
                        && turbine.tanks[0].getMaxFill() == TileEntityChungus.inputTankSize,
                "Lever must step STEAM to HOTSTEAM without resizing the tanks");
        helper.succeed();
    }

    /**
     * The exchanger scans one layer per tick keyed on world time; four lava blocks in that layer are 20 TU,
     * two heating ops of oil at 10 TU per mB.
     */
    public static void hephaestusHeating(GameTestHelper helper) {
        MachineHephaestus block = ModPowerGenBlocks.MACHINE_HEPHAESTUS.get();
        BlockPos relativeCore = new BlockPos(12, 12, 12);
        BlockPos core = FoldedTestSupport.assemble(helper, block, relativeCore, Direction.NORTH);
        TileEntityMachineHephaestus heatex = (TileEntityMachineHephaestus) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(heatex != null, "Hephaestus core block entity was not created");
        FoldedTestSupport.assertPort(helper, relativeCore, relativeCore.offset(1, 0, 0));
        FoldedTestSupport.assertPort(helper, relativeCore, relativeCore.offset(0, 11, -1));

        // 1.7 getTotalHeat triples the sum while worldTime - fissureScanTime < 20, and fissureScanTime starts at 0,
        // so in the first 20 ticks of a world every heat exchanger reads as on a fissure (1.7 does the same). A short
        // filtered GameTest run reaches this test before world tick 20; wait it out instead of reading 3x.
        long early = 20 - helper.getLevel().getGameTime();
        if (early > 0) {
            helper.runAfterDelay(early, () -> hephaestusHeatingMeasure(helper, core, heatex));
            return;
        }
        hephaestusHeatingMeasure(helper, core, heatex);
    }

    private static void hephaestusHeatingMeasure(GameTestHelper helper, BlockPos core, TileEntityMachineHephaestus heatex) {
        int height = (int) (helper.getLevel().getGameTime() % 10);
        int y = core.getY() - 1 - height;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                helper.getLevel().setBlock(new BlockPos(core.getX() + dx + 3, y, core.getZ() + dz + 3),
                        Blocks.LAVA.defaultBlockState(), 3);
            }
        }

        heatex.input.setFill(100);
        TileEntityMachineHephaestus.serverTick(helper.getLevel(), core, helper.getLevel().getBlockState(core), heatex);
        helper.assertTrue(heatex.getTotalHeat() == 20, "Four lava blocks must read as 20 TU, got " + heatex.getTotalHeat());
        helper.assertTrue(heatex.input.getFill() == 98 && heatex.output.getTankType() == Fluids.HOTOIL
                        && heatex.output.getFill() == 2,
                "Hephaestus must heat 2 mB oil per tick at 20 TU, got " + heatex.output.getFill());
        helper.succeed();
    }

    /** A one-shot track latches on the rising edge and releases when the signal drops; a loop never latches. */
    public static void sirenLatch(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        helper.setBlock(relative, ModPowerGenBlocks.MACHINE_SIREN.get().defaultBlockState());
        BlockPos pos = helper.absolutePos(relative);
        TileEntityMachineSiren siren = (TileEntityMachineSiren) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(siren != null, "siren block entity was not created");
        helper.assertTrue(siren.getCurrentType() == TrackType.NULL, "an empty siren reads NULL");

        siren.inventory.setStackInSlot(0, new ItemStack(ModPowerGenItems.track(TrackType.RAZORTRAIN)));
        helper.assertTrue(siren.getCurrentType() == TrackType.RAZORTRAIN, "the cassette in the slot is the track");
        helper.assertTrue(ItemCassette.getType(siren.inventory.getStackInSlot(0)).getType() == ItemCassette.SoundType.SOUND,
                "Razortrain Horn is a one-shot SOUND track");

        tick(helper, siren);
        helper.assertFalse(siren.lock, "no signal, no latch");
        helper.setBlock(relative.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        tick(helper, siren);
        helper.assertTrue(siren.lock, "a one-shot track latches on the rising edge");
        helper.setBlock(relative.east(), Blocks.AIR.defaultBlockState());
        tick(helper, siren);
        helper.assertFalse(siren.lock, "the latch releases when the signal drops");

        siren.inventory.setStackInSlot(0, new ItemStack(ModPowerGenItems.track(TrackType.AIR_RAID)));
        helper.setBlock(relative.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        tick(helper, siren);
        helper.assertFalse(siren.lock, "a LOOP track follows the signal and never latches");
        helper.succeed();
    }

    /** The LPW-2 is a 20x7x7 footprint of plain cells with no port anywhere. */
    public static void lpw2Layout(GameTestHelper helper) {
        MachineLPW2 block = ModPowerGenBlocks.MACHINE_LPW2.get();
        BlockPos relativeCore = new BlockPos(12, 1, 12);
        BlockPos core = FoldedTestSupport.assemble(helper, block, relativeCore, Direction.NORTH);
        helper.assertTrue(helper.getLevel().getBlockEntity(core) instanceof TileEntityMachineLPW2,
                "LPW-2 core block entity was not created");

        int cells = 0;
        for (int x = -11; x <= 11; x++) {
            for (int y = 0; y <= 6; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = core.offset(x, y, z);
                    if (pos.equals(core)) continue;
                    if (helper.getLevel().getBlockState(pos).getBlock() == block) {
                        cells++;
                        helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), pos)),
                                "LPW-2 cell at " + pos + " is not owned by the core");
                    }
                }
            }
        }
        helper.assertTrue(cells == 20 * 7 * 7 - 1, "LPW-2 must place 979 cells, placed " + cells);
        helper.succeed();
    }

    /**
     * The LPW-2's addon API (ADDON-API-INVENTORY P-14): HBM's own tile is a synced {@code TileEntityLoadedBase} whose
     * renderer clock is 1.7's world time ({@code getTotalWorldTime() % 1000000 + interp}).
     */
    public static void lpw2AnimationClock(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, ModPowerGenBlocks.MACHINE_LPW2.get().defaultBlockState());
        TileEntityMachineLPW2 te = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, relative, TileEntityMachineLPW2.class);
        helper.assertTrue(te instanceof com.hbm.tileentity.TileEntityLoadedBase, "LPW-2 tile is a TileEntityLoadedBase");
        long time = helper.getLevel().getGameTime() % 1000000;
        helper.assertTrue(te.animationTime(0.25F) == (double) (time + 0.25F), "LPW-2 clock is world time + partial ticks");
        helper.succeed();
    }

    /** The memorial is one block that swallows its click and reads the dedication. */
    public static void igenMemorial(GameTestHelper helper) {
        MachineIGenerator block = ModPowerGenBlocks.MACHINE_INDUSTRIAL_GENERATOR.get();
        BlockPos core = FoldedTestSupport.assemble(helper, block, new BlockPos(2, 1, 2), Direction.NORTH);
        helper.assertTrue(helper.getLevel().getBlockEntity(core) instanceof TileEntityMachineIGenerator,
                "memorial block entity was not created");
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getBlockState(core.relative(side)).getBlock() != block,
                    "the memorial places no cells");
        }
        ILookOverlay.Data data = block.getLookOverlay(helper.getLevel(), core, null);
        helper.assertTrue(data != null && "Industrial Generator Memorial".equals(data.title())
                        && data.lines().equals(java.util.List.of("In memory of all that we have lost")),
                "the memorial's look overlay is the dedication");
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, TileEntityChungus turbine) {
        TileEntityChungus.serverTick(helper.getLevel(), turbine.getBlockPos(),
                helper.getLevel().getBlockState(turbine.getBlockPos()), turbine);
    }

    private static void tick(GameTestHelper helper, TileEntityMachineSiren siren) {
        TileEntityMachineSiren.serverTick(helper.getLevel(), siren.getBlockPos(),
                helper.getLevel().getBlockState(siren.getBlockPos()), siren);
    }
}
