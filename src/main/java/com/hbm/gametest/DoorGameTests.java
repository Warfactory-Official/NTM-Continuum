package com.hbm.gametest;

import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.hbm.tileentity.machine.TileEntityBlastDoor;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The generic door's server-side contract: an assembled door toggles through OPENING to OPEN in
 * {@code timeToOpen} ticks, the cells the moving leaf clears carry the open flag with an empty
 * collision box, and closing puts them back. Rendering, sound and the clicked interaction are not
 * covered here (see the port brief's verification notes).
 */
public final class DoorGameTests {

    private DoorGameTests() {}

    /** The fire door: dims {2,0,0,0,2,1}, range {-1,0,0,3,4,1}: a 3-wide, 4-tall opening moving up the Y axis. */
    public static void fireDoorOpensAndCloses(GameTestHelper helper) {
        BlockDoorGeneric block = ModDoorBlocks.FIRE_DOOR.get();
        BlockPos core = helper.absolutePos(new BlockPos(6, 1, 6));
        Direction facing = Direction.NORTH;
        MultiblockHandlerXR.placeAssembled(helper.getLevel(), core, block, facing);

        TileEntityDoorGeneric door = (TileEntityDoorGeneric) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(door != null, "fire door core has no block entity");
        helper.assertTrue(door.getDoorType() == DoorDecl.FIRE_DOOR, "fire door core carries the wrong declaration");

        BlockPos cell = core.above(1);
        BlockState closed = helper.getLevel().getBlockState(cell);
        helper.assertTrue(closed.getBlock() == block && closed.getValue(BlockDoorGeneric.CELL), "the block above the core is not a door cell");
        helper.assertFalse(closed.getValue(BlockDoorGeneric.OPEN), "a freshly placed door cell is already open");
        helper.assertFalse(closed.getCollisionShape(helper.getLevel(), cell).isEmpty(), "a closed door cell has no collision");

        helper.assertTrue(door.tryToggle(null), "an unlocked closed door refused to toggle");
        helper.assertTrue(door.state == TileEntityDoorGeneric.STATE_OPENING, "toggling a closed door did not start opening it");

        int open = DoorDecl.FIRE_DOOR.timeToOpen();
        helper.runAfterDelay(open + 2, () -> {
            helper.assertTrue(door.state == TileEntityDoorGeneric.STATE_OPEN, "door did not finish opening after " + open + " ticks; state=" + door.state);
            BlockState opened = helper.getLevel().getBlockState(cell);
            helper.assertTrue(opened.getValue(BlockDoorGeneric.OPEN), "the cell above the core was not flagged open");
            helper.assertTrue(opened.getCollisionShape(helper.getLevel(), cell).isEmpty(), "an open door cell still has collision");
            helper.assertTrue(door.tryToggle(null), "an open door refused to close");
            helper.assertTrue(door.state == TileEntityDoorGeneric.STATE_CLOSING, "toggling an open door did not start closing it");
            helper.runAfterDelay(open + 2, () -> {
                helper.assertTrue(door.state == TileEntityDoorGeneric.STATE_CLOSED, "door did not finish closing; state=" + door.state);
                helper.assertFalse(helper.getLevel().getBlockState(cell).getValue(BlockDoorGeneric.OPEN), "a closed door cell kept its open flag");
                helper.succeed();
            });
        });
    }

    /** Every declaration's open ranges stay inside its own footprint so the moving leaf only ever flags its own cells. */
    public static void declarationsAreConsistent(GameTestHelper helper) {
        for (BlockDoorGeneric block : new BlockDoorGeneric[] {
                ModDoorBlocks.VAULT_DOOR.get(), ModDoorBlocks.SLIDING_BLAST_DOOR.get(), ModDoorBlocks.FIRE_DOOR.get(),
                ModDoorBlocks.TRANSITION_SEAL.get(), ModDoorBlocks.SILO_HATCH.get(), ModDoorBlocks.SILO_HATCH_LARGE.get(),
                ModDoorBlocks.SECURE_ACCESS_DOOR.get(), ModDoorBlocks.LARGE_VEHICLE_DOOR.get(), ModDoorBlocks.QE_CONTAINMENT.get(),
                ModDoorBlocks.QE_SLIDING_DOOR.get(), ModDoorBlocks.ROUND_AIRLOCK_DOOR.get(), ModDoorBlocks.SLIDING_SEAL_DOOR.get(),
                ModDoorBlocks.WATER_DOOR.get(), ModDoorBlocks.CARGO_DOOR.get() }) {
            DoorDecl decl = block.type;
            helper.assertTrue(decl.getDimensions().length == 6, decl + " has malformed dimensions");
            for (int[] range : decl.getDoorOpenRanges()) {
                helper.assertTrue(range.length == 6, decl + " has a malformed open range");
            }
            helper.assertTrue(decl.timeToOpen() > 0, decl + " opens in no time");
            for (ForgeDirection dir : new ForgeDirection[] {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST}) {
                helper.assertTrue(block.cellBox(0, 0, 0, dir, false, false) != null, decl + " has no core box facing " + dir);
            }
        }
        helper.succeed();
    }

    /** The old blast door: placing the core stacks six dummy leaf blocks that open one by one. */
    public static void blastDoorLeaf(GameTestHelper helper) {
        BlockPos core = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(core, ModDoorBlocks.BLAST_DOOR.get().defaultBlockState(), 3);
        TileEntityBlastDoor door = (TileEntityBlastDoor) helper.getLevel().getBlockEntity(core);
        helper.assertTrue(door != null, "blast door has no block entity");
        for (int i = 1; i <= 6; i++) door.placeDummy(core.above(i), false);
        for (int i = 1; i <= 6; i++) {
            helper.assertTrue(helper.getLevel().getBlockState(core.above(i)).getBlock() == ModDoorBlocks.DUMMY_BLOCK_BLAST.get(),
                    "leaf block " + i + " was not placed");
        }
        door.tryToggle();
        helper.assertTrue(door.state == 1 && door.isOpening, "blast door did not start opening");
        helper.runAfterDelay(105, () -> {
            helper.assertTrue(door.state == 2, "blast door did not finish opening; state=" + door.state);
            for (int i = 1; i <= 5; i++) {
                helper.assertTrue(helper.getLevel().getBlockState(core.above(i)).isAir(), "leaf block " + i + " was not retracted");
            }
            helper.assertTrue(helper.getLevel().getBlockState(core.above(6)).getBlock() == ModDoorBlocks.DUMMY_BLOCK_BLAST.get(),
                    "the top leaf block must stay");
            helper.succeed();
        });
    }
}
