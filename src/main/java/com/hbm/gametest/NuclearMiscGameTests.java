package com.hbm.gametest;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIngredientItems.PlateFuelType;
import com.hbm.items.ModRtgContent;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.inventory.container.ContainerMachineReactorBreeding;
import com.hbm.inventory.container.ContainerRadiolysis;
import com.hbm.inventory.container.ContainerReactorControl;
import com.hbm.inventory.container.ContainerReactorResearch;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.tileentity.machine.TileEntityMachineRadiolysis;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;
import com.hbm.tileentity.machine.TileEntityReactorControl;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import com.hbm.items.machine.ItemReactorSensor;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;
import com.hbm.tileentity.machine.pile.TileEntityPileFuel;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Processing and menu contracts for the radiolysis chamber, the legacy reactors and the legacy pile. */
public final class NuclearMiscGameTests {

    private NuclearMiscGameTests() {}

    public static void recipes(GameTestHelper helper) {
        helper.assertTrue(RadiolysisRecipes.getRadiolysis(Fluids.WATER).left().type == Fluids.PEROXIDE
                        && RadiolysisRecipes.getRadiolysis(Fluids.WATER).right().type == Fluids.HYDROGEN,
                "radiolysis must split water into peroxide and hydrogen");
        helper.assertTrue(RadiolysisRecipes.getRadiolysis(Fluids.OIL) != null,
                "radiolysis must inherit the cracking recipes");
        helper.assertTrue(BreederRecipes.getAllRecipes().size() == 31,
                "breeder must expose ten rod recipes for each of the three rod sizes, plus the etched meteorite sword");
        BreederRecipes.BreederRecipe quad = BreederRecipes.getOutput(new ItemStack(ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.PU238)));
        helper.assertTrue(quad != null && quad.flux() == 3000
                        && quad.output().getItem() == ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.PU239),
                "quad Pu-238 rod must breed to Pu-239 at three times the single flux");
        helper.succeed();
    }

    /** A lead pellet (600 heat) cracks 100 mB of water every five ticks into 80 peroxide + 20 hydrogen. */
    public static void radiolysisCracking(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        helper.setBlock(relative, ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get().defaultBlockState());
        BlockPos pos = helper.absolutePos(relative);
        TileEntityMachineRadiolysis te = (TileEntityMachineRadiolysis) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(te != null, "radiolysis core block entity was not created");

        te.inventory.setStackInSlot(0, new ItemStack(ModRtgContent.PELLET_RTG_LEAD.get()));
        te.tanks[0].setTankType(Fluids.WATER);
        te.tanks[0].setFill(1_000);

        // The chamber ticks through its registered ticker: cracking keys off the level's game time
        // (1.7 {@code getTotalWorldTime() % crackTime}), so it must be observed across real ticks.
        // The pellet output is the 1.12 half-life curve (see RtgGameTests): a lead pellet (600 heat,
        // 3,253-tick half-life) already reads 599 a few ticks in, so the heat is checked against the
        // pellet's own scaled output rather than a flat 600.
        helper.runAfterDelay(12, () -> {
            ItemRTGPellet lead = ModRtgContent.PELLET_RTG_LEAD.get();
            // heat is read before the pellet decays, so it may sit one point above the stack's current output
            int expected = ItemRTGPellet.getScaledPower(lead, te.inventory.getStackInSlot(0));
            helper.assertTrue(te.heat >= expected && te.heat <= expected + 1 && te.heat >= 590,
                    "the lead pellet must heat the chamber to its scaled output " + expected + ", got " + te.heat);
            helper.assertTrue(te.power > 0, "the chamber must generate power from its pellets");
            helper.assertTrue(te.tanks[1].getTankType() == Fluids.PEROXIDE && te.tanks[2].getTankType() == Fluids.HYDROGEN,
                    "output tanks must follow the water recipe");
            helper.assertTrue(te.tanks[1].getFill() > 0 && te.tanks[1].getFill() % 80 == 0
                            && te.tanks[2].getFill() == te.tanks[1].getFill() / 4,
                    "cracking must move 80/20 per 100 mB, got " + te.tanks[1].getFill() + "/" + te.tanks[2].getFill());
            helper.assertTrue(te.tanks[0].getFill() == 1_000 - te.tanks[1].getFill() / 80 * 100,
                    "every cycle must consume 100 mB of water");
            helper.succeed();
        });
    }

    /** Four passive Ra226Be plates give 120 flux at full level, which drives a breeder next door. */
    public static void researchReactorFeedsBreeder(GameTestHelper helper) {
        BlockPos reactorRel = new BlockPos(2, 1, 2);
        BlockPos breederRel = new BlockPos(3, 1, 2);
        helper.setBlock(reactorRel, ModNuclearMiscBlocks.REACTOR_RESEARCH.get().defaultBlockState());
        helper.setBlock(breederRel, ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get().defaultBlockState());
        BlockPos reactorPos = helper.absolutePos(reactorRel);
        BlockPos breederPos = helper.absolutePos(breederRel);
        TileEntityReactorResearch reactor = (TileEntityReactorResearch) helper.getLevel().getBlockEntity(reactorPos);
        TileEntityMachineReactorBreeding breeder = (TileEntityMachineReactorBreeding) helper.getLevel().getBlockEntity(breederPos);
        helper.assertTrue(reactor != null && breeder != null, "reactor column block entities were not created");

        for (int i = 0; i < 4; i++) reactor.inventory.setStackInSlot(i, new ItemStack(ModIngredientItems.plateFuel(PlateFuelType.RA226BE)));
        reactor.setTarget(1.0D);
        breeder.inventory.setStackInSlot(0, new ItemStack(ModNuclearMiscItems.rod(RodSize.SINGLE, BreedingRodType.CO)));

        for (int i = 0; i < 30; i++) {
            TileEntityReactorResearch.serverTick(helper.getLevel(), reactorPos, helper.getLevel().getBlockState(reactorPos), reactor);
            TileEntityMachineReactorBreeding.serverTick(helper.getLevel(), breederPos, helper.getLevel().getBlockState(breederPos), breeder);
        }

        helper.assertTrue(reactor.rodLevel == 1.0D, "rods must travel 0.04 per tick to the target");
        helper.assertTrue(reactor.totalFlux == 120, "four passive plates must give 120 flux, got " + reactor.totalFlux);
        helper.assertTrue(reactor.heat > 0, "the reaction must heat the reactor");
        helper.assertTrue(breeder.flux == 120, "the breeder must read the neighbouring reactor's flux, got " + breeder.flux);
        helper.assertTrue(breeder.progress > 0F, "a cobalt rod needs 100 flux and must be breeding");
        helper.succeed();
    }

    /** The remote control reads the linked reactor and drives its target level from the heat curve. */
    public static void controllerLinks(GameTestHelper helper) {
        BlockPos reactorRel = new BlockPos(2, 1, 2);
        BlockPos controlRel = new BlockPos(5, 1, 5);
        helper.setBlock(reactorRel, ModNuclearMiscBlocks.REACTOR_RESEARCH.get().defaultBlockState());
        helper.setBlock(controlRel, ModNuclearMiscBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        BlockPos reactorPos = helper.absolutePos(reactorRel);
        BlockPos controlPos = helper.absolutePos(controlRel);
        TileEntityReactorResearch reactor = (TileEntityReactorResearch) helper.getLevel().getBlockEntity(reactorPos);
        TileEntityReactorControl control = (TileEntityReactorControl) helper.getLevel().getBlockEntity(controlPos);
        helper.assertTrue(reactor != null && control != null, "controller block entities were not created");

        ItemStack sensor = new ItemStack(ModNuclearMiscItems.REACTOR_SENSOR.get());
        ItemReactorSensor.TARGET.set(sensor, reactorPos.asLong());
        control.inventory.setStackInSlot(0, sensor);
        control.levelLower = 100D;
        control.levelUpper = 0D;
        control.heatLower = 0D;
        control.heatUpper = 1000D;

        TileEntityReactorControl.serverTick(helper.getLevel(), controlPos, helper.getLevel().getBlockState(controlPos), control);
        helper.assertTrue(control.isLinked, "the controller must link through the sensor position");
        helper.assertTrue(reactor.targetLevel == 1.0D, "at zero heat the linear curve must ask for the lower level, got " + reactor.targetLevel);

        reactor.heat = 500;
        TileEntityReactorControl.serverTick(helper.getLevel(), controlPos, helper.getLevel().getBlockState(controlPos), control);
        helper.assertTrue(Math.abs(reactor.targetLevel - 0.5D) < 1e-9, "halfway up the heat band the linear curve must ask for 50%, got " + reactor.targetLevel);
        helper.succeed();
    }

    /** A rod goes into a drilled channel through its channel face and pushes the chain along. */
    public static void pileInsert(GameTestHelper helper) {
        BlockPos a = new BlockPos(2, 1, 2);
        BlockPos b = new BlockPos(3, 1, 2);
        BlockState drilled = ModNuclearMiscBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()
                .setValue(BlockGraphiteDrilledBase.AXIS, Direction.Axis.X);
        helper.setBlock(a, drilled);
        helper.setBlock(b, drilled);
        helper.setBlock(new BlockPos(4, 1, 2), Blocks.AIR.defaultBlockState());
        BlockPos posA = helper.absolutePos(a);
        BlockPos posB = helper.absolutePos(b);

        BlockGraphiteDrilledBase block = (BlockGraphiteDrilledBase) drilled.getBlock();
        helper.assertFalse(block.insertItem(helper.getLevel(), posA, Direction.NORTH, new ItemStack(ModNuclearMiscItems.PILE_ROD_URANIUM.get())),
                "a rod must not enter through a non-channel face");
        helper.assertTrue(block.insertItem(helper.getLevel(), posA, Direction.EAST, new ItemStack(ModNuclearMiscItems.PILE_ROD_URANIUM.get())),
                "a uranium rod must enter through the channel face");
        helper.assertTrue(helper.getLevel().getBlockState(posA).getBlock() == ModNuclearMiscBlocks.BLOCK_GRAPHITE_FUEL.get(),
                "the channel must become a fuel cell");
        helper.assertTrue(helper.getLevel().getBlockEntity(posA) instanceof TileEntityPileFuel, "the fuel cell must carry its tile");

        BlockGraphiteDrilledBase fuel = (BlockGraphiteDrilledBase) helper.getLevel().getBlockState(posA).getBlock();
        helper.assertTrue(fuel.insertItem(helper.getLevel(), posA, Direction.EAST, new ItemStack(ModNuclearMiscItems.PILE_ROD_SOURCE.get())),
                "a second rod must push the first one along");
        helper.assertTrue(helper.getLevel().getBlockState(posA).getBlock() == ModNuclearMiscBlocks.BLOCK_GRAPHITE_SOURCE.get()
                        && helper.getLevel().getBlockState(posB).getBlock() == ModNuclearMiscBlocks.BLOCK_GRAPHITE_FUEL.get(),
                "the chain must shift by one cell");
        helper.succeed();
    }

    /** A Ra226Be source cell casts twelve rays a tick; a fuel cell touching it along the channel gets hit and breeds. */
    public static void pileNeutrons(GameTestHelper helper) {
        BlockPos sourceRel = new BlockPos(2, 1, 2);
        BlockPos fuelRel = new BlockPos(3, 1, 2);
        helper.setBlock(sourceRel, ModNuclearMiscBlocks.BLOCK_GRAPHITE_SOURCE.get().defaultBlockState()
                .setValue(BlockGraphiteDrilledBase.AXIS, Direction.Axis.X));
        helper.setBlock(fuelRel, ModNuclearMiscBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState()
                .setValue(BlockGraphiteDrilledBase.AXIS, Direction.Axis.X));
        BlockPos fuelPos = helper.absolutePos(fuelRel);
        helper.assertTrue(helper.getLevel().getBlockEntity(fuelPos) instanceof TileEntityPileFuel, "the fuel cell must carry its tile");

        helper.runAfterDelay(40, () -> {
            TileEntityPileFuel fuel = (TileEntityPileFuel) helper.getLevel().getBlockEntity(fuelPos);
            helper.assertTrue(fuel != null && fuel.progress > 0, "the fuel cell must have absorbed neutrons from the source");
            helper.succeed();
        });
    }

    public static void menus(GameTestHelper helper) {
        BlockPos radiolysisRel = new BlockPos(2, 1, 2);
        BlockPos breederRel = new BlockPos(6, 1, 2);
        BlockPos reactorRel = new BlockPos(2, 1, 6);
        BlockPos controlRel = new BlockPos(6, 1, 6);
        helper.setBlock(radiolysisRel, ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get().defaultBlockState());
        helper.setBlock(breederRel, ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get().defaultBlockState());
        helper.setBlock(reactorRel, ModNuclearMiscBlocks.REACTOR_RESEARCH.get().defaultBlockState());
        helper.setBlock(controlRel, ModNuclearMiscBlocks.MACHINE_CONTROLLER.get().defaultBlockState());
        TileEntityMachineRadiolysis radiolysis = (TileEntityMachineRadiolysis) helper.getLevel().getBlockEntity(helper.absolutePos(radiolysisRel));
        TileEntityMachineReactorBreeding breeder = (TileEntityMachineReactorBreeding) helper.getLevel().getBlockEntity(helper.absolutePos(breederRel));
        TileEntityReactorResearch reactor = (TileEntityReactorResearch) helper.getLevel().getBlockEntity(helper.absolutePos(reactorRel));
        TileEntityReactorControl control = (TileEntityReactorControl) helper.getLevel().getBlockEntity(helper.absolutePos(controlRel));
        helper.assertTrue(radiolysis != null && breeder != null && reactor != null && control != null, "machine block entities were not created");

        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = fixture.player();
            BlockPos core = radiolysis.getBlockPos();
            player.setPos(core.getX() + 0.5D, core.getY() + 0.5D, core.getZ() + 0.5D);

            ContainerRadiolysis radiolysisMenu = new ContainerRadiolysis(0, player.getInventory(), radiolysis);
            helper.assertTrue(radiolysisMenu.slots.size() == 15 + 36, "radiolysis menu has the wrong slot count");
            helper.assertFalse(radiolysisMenu.getSlot(11).mayPlace(new ItemStack(ModRtgContent.PELLET_RTG.get())), "the fluid identifier output slot is take-only");

            ContainerMachineReactorBreeding breederMenu = new ContainerMachineReactorBreeding(1, player.getInventory(), breeder);
            helper.assertTrue(breederMenu.slots.size() == 2 + 36 && !breederMenu.getSlot(1).mayPlace(new ItemStack(ModNuclearMiscItems.rod(RodSize.SINGLE, BreedingRodType.CO))),
                    "breeder menu must expose an input and a take-only output");

            ContainerReactorResearch reactorMenu = new ContainerReactorResearch(2, player.getInventory(), reactor);
            helper.assertTrue(reactorMenu.slots.size() == 12 + 36 && reactorMenu.getSlot(11).x == 131 && reactorMenu.getSlot(11).y == 94,
                    "research reactor menu must keep the hex slot layout");

            ContainerReactorControl controlMenu = new ContainerReactorControl(3, player.getInventory(), control);
            helper.assertTrue(controlMenu.slots.size() == 1 + 36, "controller menu must have one sensor slot");

            helper.assertTrue(reactor.hasPermission(player), "a nearby player may control the reactor");
            player.setPos(core.getX() + 100.5D, core.getY() + 0.5D, core.getZ() + 0.5D);
            helper.assertFalse(reactor.hasPermission(player), "a distant player must not control the reactor");
        }
        helper.succeed();
    }
}
