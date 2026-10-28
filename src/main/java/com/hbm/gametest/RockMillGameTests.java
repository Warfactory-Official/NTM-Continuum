package com.hbm.gametest;

import com.hbm.blocks.machine.MachineRockMill;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineRockMill;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** The rock mill against 1.7's {@code RockMillRecipes} and {@code TileEntityMachineRockMill}. */
public final class RockMillGameTests {

    private RockMillGameTests() {}

    public static void table(GameTestHelper helper) {
        RockMillRecipes.INSTANCE.registerRecipes();
        helper.assertTrue(RockMillRecipes.INSTANCE.recipeOrderedList.size() == 13,
                "1.7 registers thirteen rock mill rows with the sculk pair, found " + RockMillRecipes.INSTANCE.recipeOrderedList.size());
        GenericRecipe cobble = RockMillRecipes.INSTANCE.getRecipe("rock.cobble");
        helper.assertTrue(cobble != null && cobble.duration == 100 && cobble.power == 25
                        && cobble.inputFluid[0].type == Fluids.WATER && cobble.inputFluid[0].fill == 250
                        && cobble.outputFluid[0].type == Fluids.COLLOID && cobble.outputFluid[0].fill == 250,
                "rock.cobble is 100 ticks at 25 HE/t, 250 mB water to 250 mB colloid");
        helper.assertTrue(cobble.outputChoices.length == 2 && cobble.outputChoices[0].weight() == 95
                        && cobble.outputChoices[1].weight() == 5 && cobble.getIcon().is(Items.COBBLESTONE),
                "rock.cobble rolls gravel 95 to quartz 5 and shows cobblestone");
        GenericRecipe schist = RockMillRecipes.INSTANCE.getRecipe("rock.schist");
        helper.assertTrue(schist.duration == 200 && schist.outputChoices.length == 6, "rock.schist takes 200 ticks and has six outputs");
        GenericRecipe clay = RockMillRecipes.INSTANCE.getRecipe("rock.clay");
        helper.assertTrue(clay.inputItem[0].stacksize == 2 && clay.inputFluid[0].fill == 2_500
                        && clay.outputItem[0].is(Items.CLAY_BALL) && clay.outputItem[0].getCount() == 4 && clay.autoSwitchGroup == null,
                "rock.clay: 2 sand and 2,500 mB colloid make 4 clay, outside the crushing group");
        helper.assertTrue(RockMillRecipes.INSTANCE.autoSwitchGroups.get("autoswitch.crushing").size() == 11,
                "the eleven crushing rows share one auto-switch group");
        GenericRecipe sculk = RockMillRecipes.INSTANCE.getRecipe("rock.sculkgrass");
        GenericRecipe lightSculk = RockMillRecipes.INSTANCE.getRecipe("rock.lightsculkgrass");
        helper.assertTrue(sculk != null && sculk.inputItem[0].matchesRecipe(new ItemStack(Items.SCULK), true)
                        && sculk.outputFluid[0].type == Fluids.SLOP && sculk.outputChoices.length == 3,
                "rock.sculkgrass crushes vanilla sculk into slop and three outputs");
        helper.assertTrue(lightSculk != null && lightSculk.inputItem[0].matchesRecipe(new ItemStack(Items.SCULK_VEIN), true),
                "rock.lightsculkgrass crushes sculk vein");
        helper.assertTrue(cobble.getLocalizedName().startsWith("Crushing of"), "crushing rows read Crushing of <input>");
        helper.succeed();
    }

    /** One cobble cycle: 100 ticks × 25 HE, 250 mB water in, 250 mB colloid and one gravel or quartz out. */
    public static void processing(GameTestHelper helper) {
        BlockPos rel = new BlockPos(4, 1, 4);
        TileEntityMachineRockMill mill = place(helper, rel);
        BlockPos pos = mill.getBlockPos();

        mill.rockMillModule.recipe = "rock.cobble";
        tick(helper, mill);
        helper.assertTrue(mill.inputTanks[0].getTankType() == Fluids.WATER && mill.outputTanks[0].getTankType() == Fluids.COLLOID
                        && mill.inputTanks[0].getMaxFill() == 4_000, "the tanks take the recipe's fluids at 4,000 mB");

        mill.inputTanks[0].setFill(4_000);
        mill.inventory.setStackInSlot(2, new ItemStack(Items.COBBLESTONE));
        mill.power = 2_500;

        for (int i = 0; i < 99; i++) tick(helper, mill);
        helper.assertTrue(mill.inventory.getStackInSlot(5).isEmpty(), "no output before the hundredth tick");
        tick(helper, mill);

        ItemStack out = mill.inventory.getStackInSlot(5);
        helper.assertTrue(out.getCount() == 1 && (out.is(Items.GRAVEL) || out.is(ModItems.powder_quartz.get())),
                "one cycle yields one gravel or quartz, got " + out);
        helper.assertTrue(mill.power == 0, "100 ticks at 25 HE/t drain the 2,500 HE buffer, power is " + mill.power);
        helper.assertTrue(mill.inputTanks[0].getFill() == 3_750 && mill.outputTanks[0].getFill() == 250,
                "250 mB water became 250 mB colloid");
        helper.assertTrue(mill.inventory.getStackInSlot(2).isEmpty(), "the cobblestone was consumed at " + pos);
        helper.succeed();
    }

    /** Gravel in a cobble-set mill switches the recipe within the crushing group. */
    public static void autoSwitch(GameTestHelper helper) {
        TileEntityMachineRockMill mill = place(helper, new BlockPos(4, 1, 4));
        mill.rockMillModule.recipe = "rock.cobble";
        mill.inventory.setStackInSlot(2, new ItemStack(Items.GRAVEL));
        mill.power = 2_500;
        tick(helper, mill);
        helper.assertTrue("rock.gravel".equals(mill.rockMillModule.recipe), "gravel selects rock.gravel, got " + mill.rockMillModule.recipe);
        helper.succeed();
    }

    public static void layout(GameTestHelper helper) {
        TileEntityMachineRockMill mill = place(helper, new BlockPos(4, 1, 4));
        MachineRockMill block = ModMachineBlocks.machine_rockmill.get();
        helper.assertTrue(mill.getPorts().length == 8, "1.7 gives the rock mill eight ports");
        for (DirPos connection : PortDef.connections(mill.getPorts())) {
            BlockPos cell = connection.getPos().offset(-connection.getDir().offsetX, 0, -connection.getDir().offsetZ);
            helper.assertTrue(helper.getLevel().getBlockState(cell).getBlock() == block, "a port connection has no machine cell behind it");
            helper.assertTrue(block.canConnect(helper.getLevel(), cell, connection.getDir()), "a rock mill port does not connect");
        }
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineRockMill mill = place(helper, new BlockPos(4, 1, 4));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerMachineRockMill menu = new ContainerMachineRockMill(0, player.getInventory(), mill);
        helper.assertTrue(menu.slots.size() == ContainerMachineRockMill.MACHINE_SLOTS + 36, "eight machine slots and the player inventory");
        helper.assertFalse(menu.getSlot(2).mayPlace(new ItemStack(Items.COBBLESTONE)), "without a recipe the inputs take nothing, as in 1.7");
        mill.rockMillModule.recipe = "rock.cobble";
        helper.assertTrue(menu.getSlot(2).mayPlace(new ItemStack(Items.COBBLESTONE)), "the cobble recipe accepts cobblestone");
        helper.assertTrue(menu.getSlot(2).mayPlace(new ItemStack(Items.NETHERRACK)), "the crushing group accepts netherrack");
        helper.assertFalse(menu.getSlot(5).mayPlace(new ItemStack(Items.GRAVEL)), "outputs are take-only");
        helper.succeed();
    }

    private static TileEntityMachineRockMill place(GameTestHelper helper, BlockPos rel) {
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_rockmill.get(), rel);
        return GameTestHelpers.blockEntity(helper, rel, TileEntityMachineRockMill.class);
    }

    private static void tick(GameTestHelper helper, TileEntityMachineRockMill mill) {
        TileEntityMachineRockMill.serverTick(helper.getLevel(), mill.getBlockPos(), mill.getBlockState(), mill);
    }
}
