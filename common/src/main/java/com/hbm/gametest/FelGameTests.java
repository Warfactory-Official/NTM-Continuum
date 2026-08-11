package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerFEL;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.machine.TileEntityFEL;
import com.hbm.tileentity.machine.TileEntitySILEX;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * GameTests pinning the FEL: the beam's SILEX handshake (including the destructive punishment for
 * mis-aiming), the {@code 4^ordinal} power curve, the per-wavelength burn table, the entity effects,
 * the layout, and the slot/menu matrix.
 */
public final class FelGameTests {

    private FelGameTests() {}

    /** FEL cores face NORTH here, i.e. −Z, so the beam runs toward decreasing Z. */
    private static final int FACING_NORTH = BlockDummyable.offset + 2;

    private static TileEntityFEL placeFel(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachineBlocks.machine_fel.get().defaultBlockState()
                .setValue(BlockDummyable.META, FACING_NORTH));
        TileEntityFEL te = helper.getBlockEntity(pos, TileEntityFEL.class);
        helper.assertTrue(te != null, "FEL BlockEntity was not created");
        return te;
    }

    private static TileEntitySILEX placeSilex(GameTestHelper helper, BlockPos pos, int meta) {
        helper.setBlock(pos, ModMachineBlocks.machine_silex.get().defaultBlockState()
                .setValue(BlockDummyable.META, meta));
        TileEntitySILEX te = helper.getBlockEntity(pos, TileEntitySILEX.class);
        helper.assertTrue(te != null, "SILEX BlockEntity was not created");
        return te;
    }

    private static void tick(GameTestHelper helper, TileEntityFEL te, BlockPos pos) {
        TileEntityFEL.serverTick(helper.getLevel(), helper.absolutePos(pos), te.getBlockState(), te);
    }

    /** Arms the laser: crystal in, switched on, buffer full. */
    private static void arm(TileEntityFEL te, EnumWavelengths mode) {
        te.inventory.setStackInSlot(1, new ItemStack(crystalFor(mode)));
        te.isOn = true;
        te.power = TileEntityFEL.maxPower;
    }

    private static net.minecraft.world.item.Item crystalFor(EnumWavelengths mode) {
        return switch (mode) {
            case IR -> ModMachineItems.laser_crystal_co2.get();
            case VISIBLE -> ModMachineItems.laser_crystal_bismuth.get();
            case UV -> ModMachineItems.laser_crystal_cmb.get();
            case GAMMA -> ModMachineItems.laser_crystal_bale.get();
            case DRX -> ModMachineItems.laser_crystal_digamma.get();
            default -> Items.AIR;
        };
    }

    // ---------------------------------------------------------------- the SILEX handshake

    /**
     * A correctly placed SILEX — same axis, at least 5 blocks out — receives the laser's wavelength
     * every tick, and the FEL clears its "no valid SILEX" error flag.
     */
    public static void felBeam(GameTestHelper helper) {
        BlockPos fel = new BlockPos(2, 1, 8);
        // The trace reads the SILEX core one block PAST the cell the beam hits, so with the FEL facing
        // −Z a core at z = felZ − 7 is found by the beam cell at z = felZ − 6, i.e. i = 6 (>= 5). ✓
        BlockPos silex = new BlockPos(2, 1, 1);
        TileEntityFEL te = placeFel(helper, fel);
        TileEntitySILEX chamber = placeSilex(helper, silex, FACING_NORTH);
        // The beam travels at y+1 and must find a solid SILEX cell there.
        helper.setBlock(silex.offset(0, 1, 1), ModMachineBlocks.machine_silex.get().defaultBlockState()
                .setValue(BlockDummyable.META, 0));

        arm(te, EnumWavelengths.UV);
        tick(helper, te, fel);

        helper.assertTrue(chamber.mode == EnumWavelengths.UV,
                "The SILEX should have been driven to UV by the beam, but its mode is " + chamber.mode);
        helper.assertFalse(te.missingValidSilex,
                "Finding a valid SILEX must clear the FEL's error flag");
        helper.succeed();
    }

    /**
     * The punishment: a SILEX on the wrong axis is <b>destroyed</b> rather than ignored. This is the
     * single most surprising behaviour in the machine and the easiest to lose in a port.
     */
    public static void felBeamIllegal(GameTestHelper helper) {
        BlockPos fel = new BlockPos(2, 1, 8);
        BlockPos silex = new BlockPos(2, 1, 1);
        TileEntityFEL te = placeFel(helper, fel);
        // Facing WEST (meta offset + 4) while the FEL faces NORTH: a perpendicular axis, so invalid.
        placeSilex(helper, silex, BlockDummyable.offset + 4);
        helper.setBlock(silex.offset(0, 1, 1), ModMachineBlocks.machine_silex.get().defaultBlockState()
                .setValue(BlockDummyable.META, 0));

        arm(te, EnumWavelengths.UV);
        tick(helper, te, fel);

        helper.assertFalse(helper.getBlockState(silex).is(ModMachineBlocks.machine_silex.get()),
                "A cross-axis SILEX must be DESTROYED by the beam, not merely skipped");
        helper.succeed();
    }

    /** The rotation rule itself: same axis passes (either facing), a perpendicular one does not. */
    public static void felRotation(GameTestHelper helper) {
        int north = 2, south = 3, west = 4, east = 5;
        helper.assertTrue(TileEntityFEL.rotationIsValid(north, north), "Same facing must be valid");
        helper.assertTrue(TileEntityFEL.rotationIsValid(south, north), "Opposite facing must be valid");
        helper.assertFalse(TileEntityFEL.rotationIsValid(west, north), "A perpendicular facing must be invalid");
        helper.assertFalse(TileEntityFEL.rotationIsValid(east, north), "A perpendicular facing must be invalid");
        helper.succeed();
    }

    // ---------------------------------------------------------------- power

    /**
     * The {@code powerReq * 4^ordinal} curve: 4,000 HE/t on IR up to 1,024,000 on DRX, and a buffer
     * that cannot cover the draw blanks the mode <b>and zeroes the buffer</b>.
     */
    public static void felPower(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 8);
        TileEntityFEL te = placeFel(helper, pos);

        assertDrain(helper, te, pos, EnumWavelengths.IR, 4_000L);
        assertDrain(helper, te, pos, EnumWavelengths.VISIBLE, 16_000L);
        assertDrain(helper, te, pos, EnumWavelengths.UV, 64_000L);
        assertDrain(helper, te, pos, EnumWavelengths.GAMMA, 256_000L);
        assertDrain(helper, te, pos, EnumWavelengths.DRX, 1_024_000L);

        // Starved: below the gate the laser blanks itself and dumps whatever is left.
        arm(te, EnumWavelengths.DRX);
        te.power = 1_000L;
        tick(helper, te, pos);
        helper.assertTrue(te.mode == EnumWavelengths.NULL,
                "A starved FEL must blank its mode, but it is " + te.mode);
        helper.assertTrue(te.power == 0L,
                "A starved FEL must zero its buffer (upstream behaviour), but it holds " + te.power);

        helper.succeed();
    }

    private static void assertDrain(GameTestHelper helper, TileEntityFEL te, BlockPos pos,
                                    EnumWavelengths mode, long expected) {
        arm(te, mode);
        long before = te.power;
        tick(helper, te, pos);
        long drained = before - te.power;
        helper.assertTrue(drained == expected,
                mode + " should draw " + expected + " HE/t, but drew " + drained);
    }

    // ---------------------------------------------------------------- burn table

    /**
     * GAMMA burns through soft blocks into <b>balefire</b> (1-in-5 per tick under a hardness limit of
     * 3000), and DRX always burns, leaving {@code fire_digamma} with {@code ash_digamma} beneath it.
     */
    public static void felBurn(GameTestHelper helper) {
        BlockPos fel = new BlockPos(2, 1, 8);
        TileEntityFEL te = placeFel(helper, fel);
        BlockPos target = new BlockPos(2, 2, 2); // on the beam line (y+1), 6 blocks out

        // --- DRX: deterministic, it always burns ---
        helper.setBlock(target, Blocks.STONE.defaultBlockState());
        helper.setBlock(target.below(), Blocks.STONE.defaultBlockState());
        arm(te, EnumWavelengths.DRX);
        tick(helper, te, fel);
        helper.assertTrue(helper.getBlockState(target).is(ModBombBlocks.fire_digamma.get())
                        || helper.getBlockState(target).is(ModBombBlocks.digamma_matter.get()),
                "A DRX beam must always burn through, leaving digamma flame (or matter on the polaroid "
                        + "easter-egg world); found " + helper.getBlockState(target).getBlock());
        helper.assertTrue(helper.getBlockState(target.below()).is(ModFallingBlocks.ash_digamma.get()),
                "A DRX burn must leave digamma ash in the cell below");

        // --- GAMMA: chance-gated, so drive it until it fires ---
        helper.setBlock(target, Blocks.STONE.defaultBlockState());
        helper.setBlock(target.below(), Blocks.STONE.defaultBlockState());
        arm(te, EnumWavelengths.GAMMA);
        boolean burned = false;
        for (int i = 0; i < 400 && !burned; i++) {
            te.power = TileEntityFEL.maxPower;
            tick(helper, te, fel);
            burned = helper.getBlockState(target).is(ModBombBlocks.balefire.get());
        }
        helper.assertTrue(burned,
                "A GAMMA beam must eventually burn stone into balefire (1-in-5 under hardness 3000)");

        helper.succeed();
    }

    /** IR is the one wavelength that does nothing at all — it neither burns blocks nor hurts entities. */
    public static void felBurnInfrared(GameTestHelper helper) {
        BlockPos fel = new BlockPos(2, 1, 8);
        TileEntityFEL te = placeFel(helper, fel);
        BlockPos target = new BlockPos(2, 2, 2);
        helper.setBlock(target, Blocks.STONE.defaultBlockState());

        arm(te, EnumWavelengths.IR);
        for (int i = 0; i < 100; i++) {
            te.power = TileEntityFEL.maxPower;
            tick(helper, te, fel);
        }
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE),
                "An IR beam is blocked by opaque blocks and must never burn them");
        helper.succeed();
    }

    // ---------------------------------------------------------------- layout + slots

    /** The lopsided footprint and the single rear power port, built through the real placement path. */
    public static void felLayout(GameTestHelper helper) {
        var block = ModMachineBlocks.machine_fel.get();
        var clicked = new BlockPos(3, 1, 1);
        // NORTH facing with offset 2 -> the core sits two blocks +Z from the clicked cell.
        var core = clicked.offset(0, 0, 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));

        TileEntityFEL te = helper.getBlockEntity(core, TileEntityFEL.class);
        helper.assertTrue(te != null, "The FEL did not form through the real placement path");

        DirPos[] ports = te.getConPos();
        helper.assertTrue(ports.length == 1, "The FEL exposes exactly one power port, found " + ports.length);
        BlockPos abs = helper.absolutePos(core);
        helper.assertTrue(ports[0].getPos().getY() == abs.getY() + 1, "The power port sits one block up");
        int away = Math.abs(ports[0].getPos().getX() - abs.getX()) + Math.abs(ports[0].getPos().getZ() - abs.getZ());
        helper.assertTrue(away == 5, "The power port sits 5 blocks behind the core, found " + away);

        helper.succeed();
    }

    /**
     * A cable placed at the FEL's rear power port must actually connect to it — both the block-level
     * check the cable's model uses and the real subscription that moves power.
     *
     * <p>The port sits one block <b>behind</b> the machine's rearmost cell, so the machine's side facing
     * the cable is {@code facing.getOpposite()}. Deriving everything from {@code getConPos()} rather
     * than hardcoding a direction keeps this honest about which way the machine actually points.
     */
    public static void felPowerPort(GameTestHelper helper) {
        var block = ModMachineBlocks.machine_fel.get();
        var clicked = new BlockPos(3, 1, 1);
        var core = clicked.offset(0, 0, 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));

        TileEntityFEL te = helper.getBlockEntity(core, TileEntityFEL.class);
        helper.assertTrue(te != null, "The FEL did not form");

        DirPos port = te.getConPos()[0];
        BlockPos cable = port.getPos();                       // absolute — where the cable goes
        var outward = port.getDir();                          // machine cell -> cable
        BlockPos machineCell = cable.subtract(new net.minecraft.core.Vec3i(
                outward.offsetX, outward.offsetY, outward.offsetZ));

        // The machine must actually occupy the cell the port hangs off. If the "extra" dummy never got
        // placed, the whole power connection is impossible and everything below is moot.
        helper.assertTrue(helper.getLevel().getBlockState(machineCell).is(block),
                "The FEL's rear power-port cell (" + machineCell + ") is not part of the machine. Either "
                        + "fillSpace's makeExtra offset disagrees with getDimensions, or getConPos points "
                        + "the wrong way — a DirPos direction is the machine's side FACING the cable.");

        // Block-level check: this is what BlockCable.canConnectTo asks, and what makes a cable visually
        // (and structurally) attach to a multiblock that has no BlockEntity in every cell.
        helper.assertTrue(block.canConnect(helper.getLevel(), machineCell, outward),
                "MachineFEL.canConnect must accept the rear port cell on side " + outward
                        + " — a cable placed there will not connect otherwise");

        // ...and the cable must agree, which is what actually drives its connected blockstate. Placed
        // straight onto the level at the absolute port position (it can sit outside the test area), then
        // shape-refreshed so its six connection booleans are recomputed from its neighbours.
        helper.getLevel().setBlock(cable, ModMachineBlocks.red_cable.get().defaultBlockState(), 3);
        var cableState = net.minecraft.world.level.block.Block.updateFromNeighbourShapes(
                helper.getLevel().getBlockState(cable), helper.getLevel(), cable);
        helper.assertTrue(cableState.is(ModMachineBlocks.red_cable.get()),
                "The cable was not placed at the port position " + cable);
        net.minecraft.core.Direction toMachine = outward.getOpposite().toDirection();
        Boolean connected = cableState.getValue(
                com.hbm.blocks.network.BlockCable.PROPERTY_BY_DIRECTION.get(toMachine));
        helper.assertTrue(connected, "The cable must report a connection toward the FEL on " + toMachine);

        helper.succeed();
    }

    /** Slot 0 takes a battery, slot 1 takes a laser crystal, and nothing else fits either. */
    public static void felSlots(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 8);
        TileEntityFEL te = placeFel(helper, pos);

        helper.assertTrue(te.isItemValidForSlot(1, new ItemStack(ModMachineItems.laser_crystal_bale.get())),
                "Slot 1 must accept a laser crystal");
        helper.assertFalse(te.isItemValidForSlot(1, new ItemStack(Items.STICK)),
                "Slot 1 must reject a non-crystal");
        helper.assertFalse(te.isItemValidForSlot(0, new ItemStack(ModMachineItems.laser_crystal_bale.get())),
                "Slot 0 is the battery bay and must reject a crystal");
        helper.succeed();
    }

    /** The 2 machine slots at the original 1.12.2 coordinates. */
    public static void felMenu(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 8);
        TileEntityFEL te = placeFel(helper, pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ContainerFEL menu = new ContainerFEL(1, player.getInventory(), te);

        helper.assertTrue(menu.getSlot(0).x == 182 && menu.getSlot(0).y == 144,
                "The battery slot belongs at (182, 144)");
        helper.assertTrue(menu.getSlot(1).x == 141 && menu.getSlot(1).y == 23,
                "The crystal slot belongs at (141, 23)");
        helper.assertTrue(menu.slots.size() == ContainerFEL.MACHINE_SLOTS + 36,
                "The menu should hold 2 machine slots plus the 36 player slots");

        // The power switch goes through the vanilla button path.
        boolean was = te.isOn;
        menu.clickMenuButton(player, 2);
        helper.assertTrue(te.isOn != was, "Button id 2 must toggle the laser");

        helper.succeed();
    }
}
