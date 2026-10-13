package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.ContainerSILEX;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.SILEXRecipes.SILEXRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.machine.TileEntitySILEX;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * GameTests pinning the SILEX to the 1.7.10 behaviour: the wavelength speed formula, the two solution
 * loading paths, the weighted output distribution, the queue, the slot matrix, the menu layout, and
 * the "mode resets every tick" invariant that makes the FEL link work.
 */
public final class SilexGameTests {

    private SilexGameTests() {}

    /** Places a bare SILEX core and returns its BlockEntity. */
    private static TileEntitySILEX place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModMachineBlocks.machine_silex.get().defaultBlockState()
                .setValue(BlockMultiblockCore.FACING, Direction.NORTH));
        TileEntitySILEX te = GameTestHelpers.blockEntity(helper, pos, TileEntitySILEX.class);
        helper.assertTrue(te != null, "SILEX BlockEntity was not created");
        return te;
    }

    private static void tick(GameTestHelper helper, TileEntitySILEX te, BlockPos pos) {
        TileEntitySILEX.serverTick(helper.getLevel(), helper.absolutePos(pos), te.getBlockState(), te);
    }

    /** Drives the machine for {@code ticks} ticks, re-asserting the laser every tick like a real FEL. */
    private static void run(GameTestHelper helper, TileEntitySILEX te, BlockPos pos, EnumWavelengths laser, int ticks) {
        for (int i = 0; i < ticks; i++) {
            te.mode = laser;
            tick(helper, te, pos);
        }
    }

    // Process speed

    /**
     * The speed formula {@code progress += 2^(mode − required + 1) / 2}: exactly +1/tick at a matching
     * wavelength, +2 one step stronger, +4 two steps, and <b>nothing at all</b> below the requirement.
     *
     * <p>Uses the uranium recipe (needs VISIBLE) and observes {@code progress} directly, so a change to
     * the exponent or the {@code / 2} shows up immediately.
     */
    public static void silexProcess(GameTestHelper helper) {
        SILEXRecipes.register();
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntitySILEX te = place(helper, pos);

        SILEXRecipe uranium = SILEXRecipes.getOutput(new ItemStack(ModItems.ingot_uranium.get()));
        helper.assertTrue(uranium != null, "The uranium SILEX recipe is missing");
        helper.assertTrue(uranium.laserStrength == EnumWavelengths.VISIBLE,
                "Uranium separation must require a VISIBLE laser, not " + uranium.laserStrength);

        // too weak: IR is below VISIBLE, so nothing happens at all
        primePool(te, uranium);
        te.progress = 0;
        run(helper, te, pos, EnumWavelengths.IR, 5);
        helper.assertTrue(te.progress == 0,
                "An IR laser must not drive a VISIBLE recipe, but progress reached " + te.progress);

        assertRate(helper, te, pos, uranium, EnumWavelengths.VISIBLE, 1);
        assertRate(helper, te, pos, uranium, EnumWavelengths.UV, 2);
        assertRate(helper, te, pos, uranium, EnumWavelengths.GAMMA, 4);
        assertRate(helper, te, pos, uranium, EnumWavelengths.DRX, 8);

        helper.succeed();
    }

    private static void assertRate(GameTestHelper helper, TileEntitySILEX te, BlockPos pos,
                                   SILEXRecipe recipe, EnumWavelengths laser, int expectedPerTick) {
        primePool(te, recipe);
        te.progress = 0;
        te.inventory.setStackInSlot(4, ItemStack.EMPTY);
        run(helper, te, pos, laser, 3);
        helper.assertTrue(te.progress == expectedPerTick * 3,
                laser + " should advance " + expectedPerTick + "/tick (3 ticks -> " + (expectedPerTick * 3)
                        + "), but progress was " + te.progress);
    }

    /** Fills the solution pool directly so the test measures processing, not loading. */
    private static void primePool(TileEntitySILEX te, SILEXRecipe recipe) {
        te.current = ItemStacks.copyWithCount(recipe.input.getDisplayStack(), 1);
        te.currentFill = TileEntitySILEX.maxFill;
    }

    // Loading

    /**
     * The item-dissolving path: with peroxide in the tank, one input item becomes exactly
     * {@code recipe.fluidProduced} mB of solution and costs the same amount of peroxide, and it only
     * happens on the tick the 20-tick {@code loadDelay} wraps to 0, never every tick.
     */
    public static void silexLoad(GameTestHelper helper) {
        SILEXRecipes.register();
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntitySILEX te = place(helper, pos);

        SILEXRecipe uranium = SILEXRecipes.getOutput(new ItemStack(ModItems.ingot_uranium.get()));
        te.tank.setTankType(Fluids.PEROXIDE);
        te.tank.setFill(te.tank.getMaxFill());
        te.inventory.setStackInSlot(0, new ItemStack(ModItems.ingot_uranium.get(), 4));

        int peroxideBefore = te.tank.getFill();
        // The dissolve step fires on the tick loadDelay wraps to 0. Upstream increments FIRST, then
        // resets when the counter exceeds 20, so the counter runs 1..20 without firing and only wraps
        // on the 21st tick. The period is 21 ticks, not 20; that off-by-one is upstream's and is kept.
        run(helper, te, pos, EnumWavelengths.NULL, 20);
        helper.assertTrue(te.currentFill == 0,
                "Nothing should dissolve in the first 20 ticks — loadDelay has not wrapped yet");
        run(helper, te, pos, EnumWavelengths.NULL, 1);

        helper.assertTrue(te.currentFill == uranium.fluidProduced,
                "One dissolved ingot should yield " + uranium.fluidProduced + " mB, got " + te.currentFill);
        helper.assertTrue(peroxideBefore - te.tank.getFill() == uranium.fluidProduced,
                "Dissolving must consume peroxide 1:1 with the solution produced");
        helper.assertTrue(te.inventory.getStackInSlot(0).getCount() == 3,
                "Exactly one input item should have been consumed in 20 ticks, "
                        + (4 - te.inventory.getStackInSlot(0).getCount()) + " were");

        helper.succeed();
    }

    /**
     * The direct-fluid path: a tank holding a fluid that is itself a SILEX input feeds the pool at
     * <b>50 mB/tick</b> with no item involved, and the pool identifies as that fluid.
     */
    public static void silexLoadFluid(GameTestHelper helper) {
        SILEXRecipes.register();
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntitySILEX te = place(helper, pos);

        te.tank.setTankType(Fluids.UF6);
        te.tank.setFill(te.tank.getMaxFill());
        run(helper, te, pos, EnumWavelengths.NULL, 4);

        helper.assertTrue(te.currentFill == 200,
                "Direct fluid loading runs at 50 mB/tick (4 ticks -> 200 mB), got " + te.currentFill);
        helper.assertTrue(te.current != null && SILEXRecipes.getOutput(te.current) != null,
                "The pool must identify as a dissolvable substance after direct fluid loading");

        helper.succeed();
    }

    // Weights

    /**
     * 1.7.10 output selection is deterministic: {@code recipeIndex} walks the weighted table in steps of
     * 137, so over 12 outputs of the 1:11 uranium table (gcd(137,12) = 1) exactly one U-235 appears. Also
     * pins the 1.7 process time of 100 (1.12.2 used 80).
     */
    public static void silexOutputWeights(GameTestHelper helper) {
        SILEXRecipes.register();
        SILEXRecipe uranium = SILEXRecipes.getOutput(new ItemStack(ModItems.ingot_uranium.get()));
        helper.assertTrue(uranium.outputs.size() == 2, "Uranium should have exactly 2 weighted outputs");
        helper.assertTrue(uranium.totalWeight() == 12, "Uranium weights should total 12, got " + uranium.totalWeight());

        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntitySILEX te = place(helper, pos);
        helper.assertTrue(te.processTime == 100, "1.7 SILEX processTime is 100, was " + te.processTime);
        helper.assertTrue(TileEntitySILEX.PRIME == 137, "1.7 SILEX output step is 137");

        int u235 = 0;
        for (int i = 0; i < 12; i++) {
            primePool(te, uranium);
            for (int q = 4; q <= 10; q++) te.inventory.setStackInSlot(q, ItemStack.EMPTY);
            te.progress = te.processTime - 1;
            run(helper, te, pos, EnumWavelengths.VISIBLE, 1);
            boolean found = false;
            for (int q = 4; q <= 10; q++) {
                ItemStack out = te.inventory.getStackInSlot(q);
                if (out.is(ModItems.nugget_u235.get())) u235++;
                if (!out.isEmpty()) found = true;
            }
            helper.assertTrue(found, "Output " + i + " was not produced at progress 100");
        }
        helper.assertTrue(u235 == 1, "12 cycled outputs of a 1:11 table must give exactly 1 U-235, got " + u235);
        helper.succeed();
    }

    // Queue + slots

    /**
     * The output queue: a finished item merges into a matching partial stack first, then takes the
     * first empty slot, and a completely full queue stalls the machine with the item stuck in slot 4.
     */
    public static void silexQueue(GameTestHelper helper) {
        SILEXRecipes.register();
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntitySILEX te = place(helper, pos);

        // merge into a partial stack
        te.inventory.setStackInSlot(7, new ItemStack(ModItems.nugget_u238.get(), 5));
        te.inventory.setStackInSlot(4, new ItemStack(ModItems.nugget_u238.get(), 1));
        tick(helper, te, pos);
        helper.assertTrue(te.inventory.getStackInSlot(7).getCount() == 6,
                "The output should merge into the matching partial queue stack (expected 6, got "
                        + te.inventory.getStackInSlot(7).getCount() + ")");
        helper.assertTrue(te.inventory.getStackInSlot(4).isEmpty(), "Slot 4 should be cleared after dequeue");

        // a non-matching item takes the first EMPTY slot instead
        te.inventory.setStackInSlot(4, new ItemStack(ModItems.nugget_u235.get(), 1));
        tick(helper, te, pos);
        helper.assertTrue(te.inventory.getStackInSlot(5).is(ModItems.nugget_u235.get()),
                "A non-matching output should go to the first empty queue slot (5)");

        // full queue -> the item stays in slot 4 and the machine stalls
        for (int i = 5; i <= 10; i++) {
            te.inventory.setStackInSlot(i, new ItemStack(ModItems.nugget_au198.get(), 64));
        }
        te.inventory.setStackInSlot(4, new ItemStack(ModItems.nugget_u235.get(), 1));
        tick(helper, te, pos);
        helper.assertTrue(!te.inventory.getStackInSlot(4).isEmpty(),
                "With a full queue the output must stay in slot 4 (that is the machine's stall state)");

        helper.succeed();
    }

    /**
     * The slot matrix and sided access: only slot 0 accepts insertion (and only a real recipe input),
     * and automation may only pull from the 5–10 queue, never the raw input or the staging output.
     */
    public static void silexSlots(GameTestHelper helper) {
        SILEXRecipes.register();
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntitySILEX te = place(helper, pos);

        helper.assertTrue(te.isItemValidForSlot(0, new ItemStack(ModItems.ingot_uranium.get())),
                "Slot 0 must accept a dissolvable input");
        helper.assertFalse(te.isItemValidForSlot(0, new ItemStack(Items.STICK)),
                "Slot 0 must reject an item with no SILEX recipe");
        helper.assertTrue(te.isItemValidForSlot(1, ItemFluidId.make(Fluids.PEROXIDE)),
                "Slot 1 must accept a fluid identifier");
        helper.assertFalse(te.isItemValidForSlot(4, new ItemStack(ModItems.nugget_u235.get())),
                "Slot 4 is machine-written output and must reject insertion");
        for (int i = 5; i <= 10; i++) {
            helper.assertFalse(te.isItemValidForSlot(i, new ItemStack(ModItems.nugget_u235.get())),
                    "Queue slot " + i + " must reject insertion");
        }

        int[] sided = te.getSlotsForFace(Direction.NORTH);
        helper.assertTrue(sided.length == 7, "Sided access should expose 7 slots (0 + the queue), got " + sided.length);
        helper.assertTrue(te.canPlaceItemThroughFace(0, new ItemStack(ModItems.ingot_uranium.get()), Direction.NORTH),
                "Automation must be able to insert into slot 0");
        helper.assertFalse(te.canTakeItemThroughFace(0, ItemStack.EMPTY, Direction.NORTH),
                "Automation must NOT be able to pull the raw input back out of slot 0");
        helper.assertTrue(te.canTakeItemThroughFace(5, ItemStack.EMPTY, Direction.NORTH),
                "Automation must be able to pull from the queue");

        helper.succeed();
    }

    /** The 11 machine slots at the original 1.12.2 coordinates, with the queue take-only. */
    public static void silexMenu(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntitySILEX te = place(helper, pos);
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerSILEX menu = new ContainerSILEX(1, player.getInventory(), te);

        int[][] expected = {{80, 12}, {8, 24}, {26, 24}, {44, 24}, {116, 90},
                {134, 72}, {152, 72}, {134, 90}, {152, 90}, {134, 108}, {152, 108}};
        for (int i = 0; i < expected.length; i++) {
            var slot = menu.getSlot(i);
            helper.assertTrue(slot.x == expected[i][0] && slot.y == expected[i][1],
                    "Slot " + i + " should sit at (" + expected[i][0] + ", " + expected[i][1] + ") but is at ("
                            + slot.x + ", " + slot.y + ")");
        }
        for (int i = 3; i <= 10; i++) {
            helper.assertFalse(menu.getSlot(i).mayPlace(new ItemStack(ModItems.nugget_u235.get())),
                    "Slot " + i + " must be take-only");
        }
        helper.assertTrue(menu.slots.size() == ContainerSILEX.MACHINE_SLOTS + 36,
                "The menu should hold 11 machine slots plus the 36 player slots");

        helper.succeed();
    }

    // Layout + the FEL link

    /**
     * The footprint and the two fluid ports at {@code ±2} along the axis perpendicular to the facing,
     * at {@code y+1}, built through the real placement path, not by poking blocks.
     */
    public static void silexLayout(GameTestHelper helper) {
        var block = ModMachineBlocks.machine_silex.get();
        var clicked = new BlockPos(3, 1, 1);
        // setPlacedBy resolves facing from the placer's yaw (0 -> NORTH) and puts the core `offset`
        // blocks along it: NORTH with offset 1 -> one block +Z from the clicked cell.
        var core = clicked.offset(0, 0, 1);
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new ItemStack(block));

        TileEntitySILEX te = GameTestHelpers.blockEntity(helper, core, TileEntitySILEX.class);
        helper.assertTrue(te != null, "SILEX did not form through the real placement path");

        DirPos[] ports = te.getConPos();
        helper.assertTrue(ports.length == 2, "The SILEX exposes exactly 2 fluid ports, found " + ports.length);
        for (DirPos dp : ports) {
            helper.assertTrue(dp.getPos().getY() == helper.absolutePos(core).getY() + 1,
                    "Both fluid ports sit one block above the core");
        }
        // The ports must be on OPPOSITE sides, 4 blocks apart across the machine.
        int dx = Math.abs(ports[0].getPos().getX() - ports[1].getPos().getX());
        int dz = Math.abs(ports[0].getPos().getZ() - ports[1].getPos().getZ());
        helper.assertTrue(dx + dz == 4, "The two ports should be 4 blocks apart across the machine, got " + (dx + dz));

        // A duct placed at either port must actually connect. A DirPos direction is the machine's side
        // FACING the pipe, so the machine cell is one step back along it. If the direction were stored
        // inverted (as the FEL's was), isPort would reject the match and no duct would ever attach.
        Fluids.init();
        for (DirPos dp : ports) {
            var outward = dp.getDir();
            BlockPos pipe = dp.getPos();
            BlockPos machineCell = pipe.subtract(new Vec3i(
                    outward.offsetX, outward.offsetY, outward.offsetZ));
            helper.assertTrue(helper.getLevel().getBlockState(machineCell).is(block),
                    "The SILEX fluid-port cell " + machineCell + " is not part of the machine");
            helper.assertTrue(block.canConnect(Fluids.PEROXIDE, helper.getLevel(),
                            machineCell.getX(), machineCell.getY(), machineCell.getZ(), outward),
                    "MachineSILEX.canConnect must accept the fluid port on side " + outward
                            + " — a duct placed at " + pipe + " will not connect otherwise");
        }

        helper.succeed();
    }

    /**
     * The invariant the whole FEL link rests on: {@code mode} is cleared at the end of every server
     * tick. A SILEX with no laser on it goes dark by itself, which is why an unpowered or misaimed FEL
     * stops it instantly rather than leaving it latched on.
     */
    public static void silexModeReset(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        TileEntitySILEX te = place(helper, pos);

        te.mode = EnumWavelengths.DRX;
        tick(helper, te, pos);
        helper.assertTrue(te.mode == EnumWavelengths.NULL,
                "mode must reset to NULL every tick (the FEL re-asserts it); it stayed " + te.mode);

        helper.succeed();
    }
}
