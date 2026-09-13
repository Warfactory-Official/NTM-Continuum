package com.hbm.gametest;

import com.hbm.blocks.machine.MachineMixer;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMixer;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Focused contracts for the legacy output-fluid-selected Mixer. */
public final class MixerGameTests {

    private MixerGameTests() {}

    public static void recipes(GameTestHelper helper) {
        MixerRecipes.INSTANCE.registerRecipes();
        helper.assertTrue(MixerRecipes.getOutput(Fluids.FRACKSOL).length == 2,
                "Mixer must retain both Fracksol recipes");
        helper.assertTrue(MixerRecipes.getOutput(Fluids.SOLVENT).length == 4,
                "Mixer must retain all four Solvent recipes");
        helper.assertTrue(MixerRecipes.getOutput(Fluids.COOLANT)[0].solidInput.matchesRecipe(
                        new ItemStack(com.hbm.items.ModItems.niter.get()), false),
                "Coolant must retain its saltpeter ingredient");
        helper.assertTrue(MixerRecipes.getOutput(Fluids.NITRIC_ACID)[0].solidInput.matchesRecipe(
                        new ItemStack(com.hbm.items.ModItems.niter.get()), false),
                "Nitric acid must retain its saltpeter ingredient");
        helper.assertTrue(MixerRecipes.getOutput(Fluids.FISHOIL)[0].solidInput != null,
                "Fish oil must retain its fish ingredient");
        helper.assertTrue(MixerRecipes.getOutput(Fluids.BITUMEN)[0].solidInput != null,
                "Bitumen must retain the any-tar ingredient");
        TagKey<Item> glassPanes = TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "glass_panes"));
        helper.assertTrue(new ItemStack(Items.RED_STAINED_GLASS_PANE).is(glassPanes),
                "Mixer construction must retain the old any-glass-pane ingredient");
        helper.succeed();
    }

    public static void layoutAndPorts(GameTestHelper helper) {
        MachineMixer block = ModMachineBlocks.machine_mixer.get();
        BlockPos core = new BlockPos(2, 1, 2);
        BlockPos absolute = helper.absolutePos(core);
        helper.setBlock(core, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), absolute, Direction.NORTH);

        helper.assertTrue(helper.getLevel().getBlockEntity(absolute) instanceof TileEntityMachineMixer,
                "Mixer core block entity was not created");
        helper.assertTrue(block.canConnect(helper.getLevel(), absolute, com.hbm.lib.ForgeDirection.EAST),
                "Mixer east power port did not connect");
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), absolute.getX(), absolute.getY(), absolute.getZ(),
                        com.hbm.lib.ForgeDirection.NORTH),
                "Mixer north fluid port did not connect");
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        TileEntityMachineMixer mixer = place(helper, new BlockPos(2, 1, 2));
        mixer.power = TileEntityMachineMixer.MAX_POWER;
        mixer.inventory.setStackInSlot(2, ItemFluidId.make(Fluids.SALIENT));
        mixer.tanks[0].setTankType(Fluids.SEEDSLURRY);
        mixer.tanks[0].setFill(500);
        mixer.tanks[1].setTankType(Fluids.BLOOD);
        mixer.tanks[1].setFill(500);
        mixer.tanks[2].setTankType(Fluids.SALIENT);
        mixer.progress = 999;

        TileEntityMachineMixer.serverTick(helper.getLevel(), mixer.getBlockPos(), mixer.getBlockState(), mixer);

        helper.assertTrue(mixer.tanks[0].getFill() == 0 && mixer.tanks[1].getFill() == 0,
                "Mixer did not consume both fluid inputs");
        helper.assertTrue(mixer.tanks[2].getFill() == 1_000,
                "Mixer did not produce the selected fluid amount");
        helper.assertTrue(mixer.power == TileEntityMachineMixer.MAX_POWER - 50,
                "Mixer did not draw its base 50 HE/t consumption");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineMixer mixer = place(helper, new BlockPos(2, 1, 2));
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerMixer menu = new ContainerMixer(0, player.getInventory(), mixer);

        helper.assertTrue(menu.slots.size() == ContainerMixer.MACHINE_SLOTS + 36,
                "Mixer menu must contain five machine slots and the player inventory");
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(
                        ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_REDSTONE).get())),
                "Mixer battery slot must accept a battery");
        helper.assertTrue(menu.getSlot(2).mayPlace(ItemFluidId.make(Fluids.SALIENT)),
                "Mixer fluid identifier slot must accept an identifier");
        helper.assertFalse(menu.getSlot(1).mayPlace(new ItemStack(Items.STICK)),
                "Mixer solid input slot must reject unrelated items");
        helper.succeed();
    }

    public static void recipeIndexNormalization(GameTestHelper helper) {
        TileEntityMachineMixer mixer = place(helper, new BlockPos(2, 1, 2));
        mixer.tanks[2].setTankType(Fluids.FRACKSOL);
        mixer.recipeIndex = -1;
        mixer.canProcess();
        helper.assertTrue(mixer.recipeIndex == 1,
                "Mixer must normalize a negative selector to the last output recipe");

        mixer.tanks[2].setTankType(Fluids.SALIENT);
        mixer.recipeIndex = 99;
        mixer.canProcess();
        helper.assertTrue(mixer.recipeIndex == 0,
                "Mixer must normalize a stale selector when the output has one recipe");
        helper.succeed();
    }

    private static TileEntityMachineMixer place(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolute = helper.absolutePos(relativePos);
        MachineMixer block = ModMachineBlocks.machine_mixer.get();
        helper.setBlock(relativePos, block.defaultBlockState());
        return (TileEntityMachineMixer) helper.getLevel().getBlockEntity(absolute);
    }
}
