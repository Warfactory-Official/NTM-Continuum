package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.fluid.ModWorldFluids;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModFluidItems;
import com.hbm.items.ModFluidNetItems;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GitHub issues 200 (placed sulfuric acid is HBM's sulfuric acid in every tank, as 1.7's container registry made it)
 * and 201 (the oil drills search drained deposit as 1.7's depth-limited walk, not a capped breadth-first one).
 */
public final class Gh200To201GameTests {

    private Gh200To201GameTests() {}

    private static final int PADDING = 16;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function ACID =
            TESTS.function("gh200_sulfuric_acid_bucket_fills_identified_tanks", 40, () -> Gh200To201GameTests::acid);
    public static final GameTests.Function DEPOSIT =
            TESTS.function("gh201_oil_drill_walks_branched_deposit", 20, PADDING, () -> Gh200To201GameTests::deposit);

    public static void init() {}

    private static void acid(GameTestHelper helper) {
        FluidType placed = Fluids.fromVanillaExact(ModWorldFluids.SULFURIC_ACID.source());
        TileEntityBarrel pinned = barrel(helper, new BlockPos(1, 2, 1));
        pinned.tank.setTankTypeByIdentifier(Fluids.SULFURIC_ACID);
        pinned.inventory.setStackInSlot(2, new ItemStack(ModFluidNetItems.bucket_sulfuric_acid.get()));
        TileEntityBarrel untyped = barrel(helper, new BlockPos(3, 2, 1));
        untyped.inventory.setStackInSlot(2, new ItemStack(ModFluidNetItems.bucket_sulfuric_acid.get()));

        ItemStack universal = new ItemStack(ModFluidItems.fluid_tank.get());
        ModFluidItems.fluid_tank.get().setFluid(universal, Fluids.SULFURIC_ACID, 0);
        helper.assertTrue(ModFluidItems.fluid_tank.get().acceptsFluid(placed, universal),
                "a universal tank typed to sulfuric acid refuses placed sulfuric acid");

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(pinned.tank.getFill() == 1_000 && pinned.tank.getFluid() == Fluids.SULFURIC_ACID
                            && pinned.inventory.getStackInSlot(3).is(Items.BUCKET),
                    "an identified sulfuric acid barrel took " + pinned.tank.getFill() + " mB from the bucket");
            helper.assertTrue(untyped.tank.getFill() == 1_000 && untyped.tank.getFluid() == Fluids.SULFURIC_ACID,
                    "an untyped barrel took the bucket as " + untyped.tank.getFluid().getRegistryName());
            helper.assertTrue(untyped.tank.receive(Fluids.SULFURIC_ACID, 500) == 500,
                    "a barrel filled from the bucket refuses sulfuric acid from a duct");
            helper.succeed();
        });
    }

    private static TileEntityBarrel barrel(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, ModFluidNetBlocks.barrel_steel.get().defaultBlockState());
        return (TileEntityBarrel) helper.getLevel().getBlockEntity(helper.absolutePos(at));
    }

    /**
     * Five comb-shaped layers of drained deposit joined by one column: a tree of 489 cells, so every cell is reached at
     * its one path depth whatever the shuffle. The oil sits 32 steps away, behind every other cell in breadth order.
     */
    private static void deposit(GameTestHelper helper) {
        BlockPos centre = GameTests.paddedCentre(PADDING, 1);
        BlockState empty = ModBlocks.ore_oil_empty.get().defaultBlockState();
        for (int y = 0; y <= 8; y++) helper.setBlock(centre.offset(-6, y, -6), empty);
        for (int y = 0; y <= 8; y += 2) {
            for (int x = -6; x <= 6; x++) helper.setBlock(centre.offset(x, y, -6), empty);
            for (int x = -6; x <= 6; x += 2) for (int z = -5; z <= 6; z++) helper.setBlock(centre.offset(x, y, z), empty);
        }
        helper.setBlock(centre.offset(6, 0, 6), ModOilBlocks.ore_oil.get().defaultBlockState());

        BlockPos head = helper.absolutePos(centre.offset(-6, 8, -6));
        TileEntityMachineOilWell well = new TileEntityMachineOilWell(head.above(),
                ModMachineBlocks.machine_well.get().defaultBlockState());
        well.setLevel(helper.getLevel());
        helper.assertTrue(well.trySuck(head.getY()) && well.tanks[0].getFill() == 500,
                "the derrick did not reach oil 32 steps into a branched deposit, got " + well.tanks[0].getFill() + " mB");
        helper.succeed();
    }
}
