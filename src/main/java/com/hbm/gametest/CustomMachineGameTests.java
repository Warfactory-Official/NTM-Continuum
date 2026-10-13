package com.hbm.gametest;

import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.blocks.machine.BlockCustomMachine;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CustomMachineCraftingRecipe;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityCMPort;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import dev.golem.core.Ids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * The custom machine system against the 1.7 defaults: the written Paper Press config, its structure in the
 * controller's frame, one paper-press cycle (250 mB water + 1 sawdust + 10 HE/t for 60 ticks → 3 paper), the port
 * proxy's fluid and power forwarding, and the controller's crafting shape.
 */
public final class CustomMachineGameTests {

    private CustomMachineGameTests() {}

    private static MachineConfiguration paperPress(GameTestHelper helper) {
        MachineConfiguration conf = CustomMachineConfigJSON.customMachines.get("paperPress");
        helper.assertTrue(conf != null, "default paperPress config missing");
        return conf;
    }

    /** 1.7 {@code writeDefault}: 25 components (23 casing/sheet cells, two ports) and the machine's numbers. */
    public static void defaultConfig(GameTestHelper helper) {
        MachineConfiguration conf = paperPress(helper);
        helper.assertTrue(conf.components.size() == 25, "component count " + conf.components.size());
        helper.assertTrue(conf.fluidInCount == 1 && conf.fluidInCap == 1000 && conf.itemInCount == 1
                && conf.itemOutCount == 1 && conf.maxPower == 10_000L && !conf.generatorMode, "paper press numbers");
        helper.assertTrue("Papierpresse".equals(conf.localization.get("de_DE")), "de_DE localization");
        int sheets = 0, casings = 0, ports = 0;
        for (var comp : conf.components) {
            Block b = comp.blocks().get(0);
            if (b == ModCustomMachineBlocks.cm_sheet.get(0).get()) sheets++;
            else if (b == ModCustomMachineBlocks.cm_block.get(0).get()) casings++;
            else if (b == ModCustomMachineBlocks.cm_port.get(0).get()) ports++;
        }
        helper.assertTrue(sheets == 7 && casings == 16 && ports == 2, "sheets/casings/ports " + sheets + "/" + casings + "/" + ports);
        var recipes = CustomMachineRecipes.get("paperPress");
        helper.assertTrue(recipes != null && recipes.size() == 1 && recipes.get(0).duration == 60
                && recipes.get(0).consumptionPerTick == 10 && recipes.get(0).inputFluids[0].fill == 250, "paperPress recipe");
        helper.succeed();
    }

    private static TileEntityCustomMachine placeController(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel, ModCustomMachineBlocks.custom_machine.get().defaultBlockState()
                .setValue(BlockCustomMachine.FACING, Direction.NORTH));
        TileEntityCustomMachine tile = (TileEntityCustomMachine) helper.getLevel().getBlockEntity(helper.absolutePos(rel));
        helper.assertTrue(tile != null, "controller block entity missing");
        tile.machineType = paperPress(helper).unlocalizedName;
        tile.init();
        return tile;
    }

    /** The structure check fails on an empty site, passes once every component stands, and links the two ports. */
    public static void structureAndRecipe(GameTestHelper helper) {
        BlockPos rel = new BlockPos(3, 3, 3);
        TileEntityCustomMachine tile = placeController(helper, rel);
        helper.assertFalse(tile.checkStructure(), "empty site passed the structure check");

        tile.buildStructure();
        helper.assertTrue(tile.checkStructure(), "built structure failed the check");
        BlockPos core = helper.absolutePos(rel);
        // facing north: component (x, y, z) sits at core + (x, y, z) (1.7's -dir*z term with dir = north)
        BlockPos lowerPort = TileEntityCustomMachine.componentPos(core, ForgeDirection.NORTH, paperPress(helper).components.get(23));
        helper.assertTrue(lowerPort.equals(core.below()), "lower port at " + lowerPort + " for core " + core);
        helper.assertTrue(helper.getLevel().getBlockEntity(core.below()) instanceof TileEntityCMPort port
                && core.equals(port.getCorePos()), "port was not linked to the controller");

        // supply through the port proxy: water into the tank, power into the buffer
        TileEntityCMPort port = (TileEntityCMPort) helper.getLevel().getBlockEntity(core.below());
        tile.inputTanks[0].setTankType(Fluids.WATER);
        long rejected = port.transferFluid(Fluids.WATER, 0, 1000);
        helper.assertTrue(rejected == 0 && tile.inputTanks[0].getFill() == 1000, "port fluid " + tile.inputTanks[0].getFill());
        long overshoot = port.transferPower(20_000L, false);
        helper.assertTrue(tile.power == 10_000L && overshoot == 10_000L, "port power " + tile.power + " / " + overshoot);

        tile.inventory.setStackInSlot(4, new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of("hbm", "powder_sawdust")), 2));
        for (int i = 0; i < 60; i++) TileEntityCustomMachine.serverTick(helper.getLevel(), core, tile.getBlockState(), tile);

        ItemStack out = tile.inventory.getStackInSlot(16);
        helper.assertTrue(out.is(Items.PAPER) && out.getCount() == 3, "output after 60 ticks " + out);
        helper.assertTrue(tile.inventory.getStackInSlot(4).getCount() == 1, "sawdust left " + tile.inventory.getStackInSlot(4));
        helper.assertTrue(tile.inputTanks[0].getFill() == 750, "water left " + tile.inputTanks[0].getFill());
        helper.assertTrue(tile.power == 10_000L - 600L, "power left " + tile.power);
        helper.succeed();
    }

    /** IPI / PCP / IPI with steel ingots, steel plates and a basic circuit yields the Paper Press controller. */
    public static void controllerRecipe(GameTestHelper helper) {
        ItemStack i = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of("hbm", "ingot_steel")));
        ItemStack p = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of("hbm", "plate_steel")));
        ItemStack c = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of("hbm", "circuit_basic")));
        List<ItemStack> grid = new ArrayList<>(List.of(i, p, i, p, c, p, i, p, i));
        Integer index = CustomMachineCraftingRecipe.find(3, 3, grid);
        helper.assertTrue(index != null && index == 0, "controller recipe did not match: " + index);
        ItemStack result = ModCustomMachineBlocks.custom_machine_item.get().of(index);
        // 1.7 damage niceList index + 100; the configuration's name here
        helper.assertTrue(CustomMachineConfigJSON.niceList.get(0).unlocalizedName
                        .equals(ModDataComponents.CUSTOM_MACHINE.getOr(result, null)),
                "controller names " + ModDataComponents.CUSTOM_MACHINE.getOr(result, null));
        helper.assertTrue("Paper Press".equals(result.getHoverName().getString()), "name " + result.getHoverName().getString());
        grid.set(4, p.copy());
        helper.assertTrue(CustomMachineCraftingRecipe.find(3, 3, grid) == null, "wrong centre still matched");
        helper.succeed();
    }
}
