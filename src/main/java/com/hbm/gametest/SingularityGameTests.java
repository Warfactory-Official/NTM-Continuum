package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityRagingVortex;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineCyclotron;
import com.hbm.blocks.machine.MachineFluidTank;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.ModBombItems;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.api.fluidmk2.FluidNetwork;
import com.hbm.lib.ForgeDirection;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.ModSingularityItems;
import com.hbm.items.ModSelfchargerItems;
import com.hbm.items.gear.ArmorFSBPowered;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.ItemSingularity;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Focused contracts for the Spark Singularity dependency chain and its destructive field. */
public final class SingularityGameTests {
    private SingularityGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function REGISTRATION_AND_RECIPES =
            TESTS.function("singularity_registration_and_recipes", 20, () -> SingularityGameTests::registrationAndRecipes);
    public static final GameTests.Function CELL =
            TESTS.function("singularity_cell", 20, () -> SingularityGameTests::cell);
    public static final GameTests.Function DROP =
            TESTS.function("singularity_drop", 20, () -> SingularityGameTests::drop);
    public static final GameTests.Function FORMULAS =
            TESTS.function("singularity_formulas", 20, () -> SingularityGameTests::formulas);
    public static final GameTests.Function FLUID_PRODUCTION =
            TESTS.function("singularity_fluid_production", 100, () -> SingularityGameTests::fluidProduction);
    public static final GameTests.Function SELFCHARGER =
            TESTS.function("selfcharger_auxiliary_drain", 40, () -> SingularityGameTests::selfcharger);
    public static final GameTests.Function RUBBLE_NEAREST_IMPACT =
            TESTS.function("rubble_nearest_impact", 20, () -> SingularityGameTests::rubbleNearestImpact);

    public static void init() {}

    public static void registrationAndRecipes(GameTestHelper helper) {
        for (String id : new String[] {"cell", "singularity", "singularity_counter_resonant",
                "singularity_super_heated", "black_hole", "singularity_spark", "pellet_antimatter"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, id)),
                    "missing singularity dependency item " + id);
        }
        for (String id : new String[] {"singularity", "singularity_counter_resonant",
                "singularity_super_heated", "black_hole", "singularity_spark", "singularity_spark_1",
                "pellet_antimatter", "dns_plate"}) {
            helper.assertTrue(Recipes.hasRecipe(helper.getLevel(), Constants.MOD_ID, id),
                    "missing production recipe " + id);
        }
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(Ids.of(Constants.MOD_ID, "entity_black_hole"))
                        && BuiltInRegistries.ENTITY_TYPE.containsKey(Ids.of(Constants.MOD_ID, "entity_vortex"))
                        && BuiltInRegistries.ENTITY_TYPE.containsKey(Ids.of(Constants.MOD_ID, "entity_raging_vortex"))
                        && BuiltInRegistries.ENTITY_TYPE.containsKey(Ids.of(Constants.MOD_ID, "entity_rubble")),
                "singularity entity registrations are incomplete");
        helper.succeed();
    }

    public static void cell(GameTestHelper helper) {
        ItemStack empty = new ItemStack(ModSingularityItems.cell.get());
        ItemStack antimatter = ItemCell.getFullCell(Fluids.AMAT);
        helper.assertTrue(ItemCell.isEmptyCell(empty), "new cell is not empty");
        helper.assertTrue(ItemCell.isFullCell(antimatter, Fluids.AMAT)
                        && ModDataComponents.fluidAmount(antimatter) == ItemCell.CAPACITY
                        && ModDataComponents.fluidId(antimatter) == Fluids.AMAT.getID(),
                "AMAT cell did not retain its legacy fluid id");
        helper.assertTrue(ItemCell.remainder(antimatter).is(ModSingularityItems.cell.get()),
                "full AMAT cell did not return an empty cell");
        helper.assertTrue(ItemCell.remainder(empty).isEmpty(), "empty cell incorrectly has a remainder");
        ItemStack four = ItemCell.getFullCell(Fluids.AMAT, 4);
        helper.assertTrue(four.getCount() == 4 && ItemCell.isFullCell(four, Fluids.AMAT),
                "getFullCell(fluid, count) did not preserve the old stack-count meaning");

        java.util.List<ItemStack> deuteriumGrid = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) deuteriumGrid.add(empty.copy());
        deuteriumGrid.set(4, new ItemStack(ModBombItems.mike_deut.get()));
        ItemStack deuterium = Recipes.craftingResult(helper.getLevel(), 3, 3, deuteriumGrid)
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(deuterium.is(ModSingularityItems.cell.get()) && deuterium.getCount() == 8
                        && ItemCell.isFullCell(deuterium, Fluids.DEUTERIUM)
                        && ModDataComponents.fluidAmount(deuterium) == ItemCell.CAPACITY
                        && ModDataComponents.ITEM_VARIANT.getOr(deuterium, -1) == Fluids.DEUTERIUM.getID(),
                "empty-cell recipe did not produce a model-selecting full deuterium cell");
        deuteriumGrid.set(0, antimatter.copyWithCount(1));
        helper.assertTrue(Recipes.craftingResult(helper.getLevel(), 3, 3, deuteriumGrid).isEmpty(),
                "empty-cell recipe accepted a filled cell");

        java.util.List<ItemStack> syringeGrid = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) syringeGrid.add(ItemStack.EMPTY);
        syringeGrid.set(1, new ItemStack(ModItems.plate_iron.get()));
        syringeGrid.set(4, empty.copy());
        syringeGrid.set(7, new ItemStack(net.minecraft.world.level.block.Blocks.IRON_BARS.asItem()));
        ItemStack syringe = Recipes.craftingResult(helper.getLevel(), 3, 3, syringeGrid)
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(syringe.is(ModItems.syringe_empty.get()) && syringe.getCount() == 6,
                "empty-cell syringe recipe did not accept an empty cell");
        syringeGrid.set(4, ItemCell.getFullCell(Fluids.UF6));
        helper.assertTrue(Recipes.craftingResult(helper.getLevel(), 3, 3, syringeGrid).isEmpty(),
                "empty-cell syringe recipe accepted a filled cell");

        ItemCell cell = ModSingularityItems.cell.get();
        ItemStack full = new ItemStack(ModSingularityItems.cell.get());
        helper.assertTrue(cell.tryFill(Fluids.AMAT, 999, full) == 999 && cell.getFill(full) == 0,
                "cell accepted a sub-cell fill");
        helper.assertTrue(cell.tryFill(Fluids.AMAT, 1_250, full) == 250
                        && cell.getFill(full) == ItemCell.CAPACITY
                        && ItemCell.isFullCell(full, Fluids.AMAT),
                "cell did not consume exactly one whole cell from a larger amount");
        helper.assertTrue(cell.tryEmpty(Fluids.AMAT, 250, full) == 0 && cell.getFill(full) == ItemCell.CAPACITY,
                "cell allowed a partial drain");
        helper.assertTrue(cell.tryEmpty(Fluids.AMAT, 1_250, full) == ItemCell.CAPACITY
                        && ItemCell.isEmptyCell(full)
                        && ModDataComponents.fluidId(full) == 0
                        && ModDataComponents.fluidAmount(full) == 0
                        && !ModDataComponents.ITEM_VARIANT.has(full),
                "whole-cell drain did not clear fluid components");

        ItemStack partial = new ItemStack(ModSingularityItems.cell.get());
        ModDataComponents.fluidId(partial, Fluids.AMAT.getID());
        ModDataComponents.fluidAmount(partial, 1);
        ModDataComponents.ITEM_VARIANT.set(partial, Fluids.AMAT.getID());
        helper.assertTrue(!ItemCell.isFullCell(partial, Fluids.AMAT)
                        && !cell.providesFluid(Fluids.AMAT, partial)
                        && cell.tryFill(Fluids.AMAT, ItemCell.CAPACITY, partial) == ItemCell.CAPACITY
                        && cell.tryEmpty(Fluids.AMAT, ItemCell.CAPACITY, partial) == 0,
                "externally constructed partial cell was accepted as a whole-cell container");
        helper.assertTrue(Recipes.craftingResult(helper.getLevel(), 3, 3, singularityGrid(partial))
                        .orElse(ItemStack.EMPTY).isEmpty(),
                "component singularity recipe matched a partial AMAT cell");
        helper.assertTrue(Recipes.craftingResult(helper.getLevel(), 3, 3, pelletGrid(partial))
                        .orElse(ItemStack.EMPTY).isEmpty(),
                "component antimatter-pellet recipe matched a partial AMAT cell");

        ItemStack pellet = Recipes.craftingResult(helper.getLevel(), 3, 3, pelletGrid(antimatter))
                .orElse(ItemStack.EMPTY);
        java.util.List<ItemStack> pelletRemainders = Recipes.craftingRemainders(
                helper.getLevel(), 3, 3, pelletGrid(antimatter)).orElse(java.util.List.of());
        helper.assertTrue(pellet.is(ModSingularityItems.pellet_antimatter.get())
                        && pelletRemainders.size() == 9
                        && pelletRemainders.stream().allMatch(ItemCell::isEmptyCell),
                "full AMAT cells did not match the pellet recipe with empty-cell remainders");
        helper.succeed();
    }

    public static void drop(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemEntity drop = new ItemEntity(helper.getLevel(), origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D,
                new ItemStack(ModSingularityItems.singularity_spark.get()));
        drop.setOnGround(true);
        helper.getLevel().addFreshEntity(drop);
        ItemSingularity.tickDropped(drop);
        var fields = helper.getLevel().getEntitiesOfClass(EntityRagingVortex.class,
                new AABB(origin.getX(), origin.getY(), origin.getZ(),
                        origin.getX() + 3D, origin.getY() + 3D, origin.getZ() + 3D));
        helper.assertTrue(drop.isRemoved(), "grounded Spark capsule was not consumed");
        helper.assertTrue(fields.size() == 1 && fields.get(0).getSize() == 3.5F,
                "Spark capsule did not create a size-3.5 raging vortex (fields=" + fields.size()
                        + ", size=" + (fields.isEmpty() ? "none" : fields.get(0).getSize())
                        + ", onGround=" + drop.onGround() + ", removed=" + drop.isRemoved() + ")");
        helper.succeed();
    }

    public static void formulas(GameTestHelper helper) {
        int timer = -19;
        float expected = (float) (Math.sin(timer) * Math.PI / 20D) * 0.35F;
        helper.assertTrue(Float.floatToRawIntBits(EntityRagingVortex.pulseForTimer(timer))
                        == Float.floatToRawIntBits(expected), "raging-vortex pulse formula drifted");
        EntityRubble rubble = new EntityRubble(ModEntities.RUBBLE.get(), helper.getLevel());
        rubble.setBlockState(Blocks.STONE.defaultBlockState());
        helper.assertTrue(rubble.getBlockState().is(Blocks.STONE), "rubble block-state carrier drifted");
        Vec3 rotated = com.hbm.entity.effect.EntityBlackHole.rotatePull(new Vec3(1D, 0D, 0D));
        helper.assertTrue(Math.abs(rotated.x - Math.cos(Math.toRadians(15D))) < 1E-12
                        && Math.abs(rotated.z + Math.sin(Math.toRadians(15D))) < 1E-12,
                "black-hole pull yaw has the wrong 1.12 sign");
        helper.assertTrue(dev.golem.core.world.Raycasts.segmentIntersects(
                        new AABB(2D, 0D, 0D, 2.5D, 1D, 1D), new Vec3(0D, 0.5D, 0.5D),
                        new Vec3(4D, 0.5D, 0.5D), 0.3D),
                "projectile sweep did not cross an entity box between endpoints");
        helper.succeed();
    }

    public static void rubbleNearestImpact(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(1, 10, 1));
        var near = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 10, 1));
        var far = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(4, 10, 1));
        near.setPos(origin.getX() + 1.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
        far.setPos(origin.getX() + 3.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
        java.util.List<net.minecraft.world.entity.Entity> hits = dev.golem.core.world.Raycasts.sweepEntities(
                helper.getLevel(), null,
                new Vec3(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D),
                new Vec3(origin.getX() + 6.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D),
                0.3D, candidate -> candidate == near || candidate == far);
        helper.assertTrue(hits.size() == 2 && hits.get(0) == near && hits.get(1) == far,
                "projectile sweep did not return aligned entities nearest-first: " + hits);

        EntityRubble rubble = new EntityRubble(ModEntities.RUBBLE.get(), helper.getLevel());
        rubble.setBlockState(Blocks.STONE.defaultBlockState());
        rubble.setPos(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D);
        rubble.setDeltaMovement(6D, 0D, 0D);
        rubble.tickCount = 3;
        float nearBefore = near.getHealth();
        float farBefore = far.getHealth();
        helper.getLevel().addFreshEntity(rubble);
        rubble.tick();

        helper.assertTrue(near.getHealth() < nearBefore && far.getHealth() == farBefore && rubble.isRemoved(),
                "rubble did not stop at the nearest entity: near=" + near.getHealth()
                        + ", far=" + far.getHealth() + ", removed=" + rubble.isRemoved());
        helper.succeed();
    }

    /** A real cyclotron output is networked through typed ducts into a tank before the recipe consumes it. */
    public static void fluidProduction(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineCyclotron cyclotron = placeCyclotron(helper, new BlockPos(3, 1, 3));
        TileEntityMachineFluidTank tank = placeFluidTank(helper, new BlockPos(10, 1, 4));
        for (BlockPos ductPos : new BlockPos[] {new BlockPos(6, 1, 4), new BlockPos(7, 1, 4),
                new BlockPos(7, 1, 3)}) {
            helper.setBlock(ductPos, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState());
            TileEntityFluidDuctNT duct = (TileEntityFluidDuctNT) helper.getLevel().getBlockEntity(
                    helper.absolutePos(ductPos));
            duct.setFluidType(Fluids.AMAT);
        }
        tank.tank.setTankType(Fluids.AMAT);
        tank.mode = 1;
        TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        FluidNetwork.distributeAll(helper.getLevel());

        cyclotron.power = TileEntityMachineCyclotron.MAX_POWER;
        cyclotron.inventory.setStackInSlot(0, new ItemStack(ModItems.part_plutonium.get(), 4));
        cyclotron.inventory.setStackInSlot(3, new ItemStack(ModItems.pellet_charged.get(), 4));
        for (int i = 0; i < 4; i++) {
            cyclotron.progress = TileEntityMachineCyclotron.PROCESSING_TIME - 1;
            cyclotron.tanks[0].setFill(500);
            cyclotron.tanks[1].setFill(0);
            TileEntityMachineCyclotron.serverTick(helper.getLevel(), cyclotron.getBlockPos(),
                    cyclotron.getBlockState(), cyclotron);
            TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
            FluidNetwork.distributeAll(helper.getLevel());
        }
        helper.assertTrue(tank.tank.getTankType() == Fluids.AMAT && tank.tank.getFill() == 4_000,
                "cyclotron AMAT did not cross the duct network into the ordinary tank: output="
                        + cyclotron.tanks[2].getFill() + ", tank=" + tank.tank.getFill());

        tank.inventory.setStackInSlot(4, new ItemStack(ModSingularityItems.cell.get(), 4));
        for (int i = 0; i < 4; i++) {
            TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        }
        ItemStack cells = tank.inventory.getStackInSlot(5);
        helper.assertTrue(cells.getItem() == ModSingularityItems.cell.get() && cells.getCount() == 4
                        && ItemCell.getFluidType(cells) == Fluids.AMAT
                        && ModSingularityItems.cell.get().getFill(cells) == 1_000,
                "tank did not fill component-backed cells from networked AMAT");

        ItemStack one = cells.copyWithCount(1);
        java.util.List<ItemStack> grid = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
        for (int slot : new int[] {1, 3, 5, 7}) grid.set(slot, one.copy());
        for (int slot : new int[] {0, 2, 6, 8}) grid.set(slot, new ItemStack(ModItems.nugget_euphemium.get()));
        grid.set(4, new ItemStack(com.hbm.blocks.ModHazardBlocks.block_schrabidium.get().asItem()));
        ItemStack result = Recipes.craftingResult(helper.getLevel(), 3, 3, grid).orElse(ItemStack.EMPTY);
        java.util.List<ItemStack> remainders = Recipes.craftingRemainders(helper.getLevel(), 3, 3, grid)
                .orElse(java.util.List.of());
        helper.assertTrue(result.is(ModSingularityItems.singularity.get()) && remainders.size() == 9
                        && remainders.get(1).is(ModSingularityItems.cell.get())
                        && ItemCell.isEmptyCell(remainders.get(1)),
                "network-produced AMAT cells did not assemble the component singularity recipe");
        helper.succeed();
    }

    public static void selfcharger(GameTestHelper helper) {
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new ItemStack(com.hbm.items.ModArmorItems.t51_helmet.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                new ItemStack(com.hbm.items.ModArmorItems.t51_plate.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                new ItemStack(com.hbm.items.ModArmorItems.t51_legs.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                new ItemStack(com.hbm.items.ModArmorItems.t51_boots.get()));
        ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        ArmorFSBPowered powered = (ArmorFSBPowered) helmet.getItem();
        powered.setCharge(helmet, 1_000);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(ModSelfchargerItems.battery_sc_uranium.get()));
        powered.armorTick(helper.getLevel(), player, helmet);
        helper.assertTrue(powered.getCharge(helmet) == 996,
                "uranium self-charger did not reduce the T-51 drain from 5 to 4 HE/t");
        helper.succeed();
    }

    private static TileEntityMachineCyclotron placeCyclotron(GameTestHelper helper, BlockPos relative) {
        MachineCyclotron block = ModMachineBlocks.machine_cyclotron.get();
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.formStructure(helper.getLevel(), absolute, ForgeDirection.NORTH);
        return (TileEntityMachineCyclotron) helper.getLevel().getBlockEntity(absolute);
    }

    private static TileEntityMachineFluidTank placeFluidTank(GameTestHelper helper, BlockPos relative) {
        MachineFluidTank block = ModMachineBlocks.machine_fluidtank.get();
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), absolute, Direction.NORTH);
        return (TileEntityMachineFluidTank) helper.getLevel().getBlockEntity(absolute);
    }

    private static java.util.List<ItemStack> singularityGrid(ItemStack cell) {
        java.util.List<ItemStack> grid = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
        for (int slot : new int[] {1, 3, 5, 7}) grid.set(slot, cell.copy());
        for (int slot : new int[] {0, 2, 6, 8}) grid.set(slot, new ItemStack(ModItems.nugget_euphemium.get()));
        grid.set(4, new ItemStack(com.hbm.blocks.ModHazardBlocks.block_schrabidium.get().asItem()));
        return grid;
    }

    private static java.util.List<ItemStack> pelletGrid(ItemStack cell) {
        java.util.List<ItemStack> grid = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(cell.copy());
        return grid;
    }

}
