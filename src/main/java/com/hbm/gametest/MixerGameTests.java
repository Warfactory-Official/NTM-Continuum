package com.hbm.gametest;

import com.hbm.blocks.machine.MachineMixer;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMixer;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.registry.BlockEntities;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.StringTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

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
                        new ItemStack(ModItems.niter.get()), false),
                "Coolant must retain its saltpeter ingredient");
        helper.assertTrue(MixerRecipes.getOutput(Fluids.NITRIC_ACID)[0].solidInput.matchesRecipe(
                        new ItemStack(ModItems.niter.get()), false),
                "Nitric acid must retain its saltpeter ingredient");
        helper.assertTrue(MixerRecipes.getOutput(Fluids.FISHOIL)[0].solidInput != null,
                "Fish oil must retain its fish ingredient");
        helper.assertTrue(MixerRecipes.getOutput(Fluids.BITUMEN)[0].solidInput != null,
                "Bitumen must retain the any-tar ingredient");
        TagKey<Item> glassPanes = TagKey.create(Registries.ITEM, Ids.of("c", "glass_panes"));
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
        helper.assertTrue(block.canConnect(helper.getLevel(), absolute, ForgeDirection.EAST),
                "Mixer east power port did not connect");
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), absolute.getX(), absolute.getY(), absolute.getZ(),
                        ForgeDirection.NORTH),
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
        Player player = GameTestHelpers.mockPlayer(helper,
                GameType.SURVIVAL);
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
        MixerRecipes.MixerRecipe[] fracksol = MixerRecipes.getOutput(Fluids.FRACKSOL);
        mixer.recipeName = "mixer.no_such_row";
        mixer.canProcess();
        helper.assertTrue(mixer.recipeName.equals(fracksol[0].name) && mixer.recipeIndex() == 0,
                "Mixer must fall back to the first row for a name the output has no row for, got " + mixer.recipeName);

        CompoundTag toggle = new CompoundTag();
        toggle.putBoolean("toggle", true);
        mixer.receiveControl(toggle);
        helper.assertTrue(mixer.recipeName.equals(fracksol[1].name) && mixer.recipeIndex() == 1,
                "Mixer's button must select the next row by name, got " + mixer.recipeName);
        mixer.receiveControl(toggle);
        helper.assertTrue(mixer.recipeIndex() == 0, "Mixer's button must wrap to the first row");

        mixer.tanks[2].setTankType(Fluids.SALIENT);
        mixer.recipeName = fracksol[1].name;
        mixer.canProcess();
        helper.assertTrue(mixer.recipeName.equals(MixerRecipes.getOutput(Fluids.SALIENT)[0].name),
                "Mixer must normalize a stale selector when the output has one recipe");
        helper.succeed();
    }

    /** A new output identifier keeps 1.7's recipe index, wrapped onto the new fluid's rows. */
    public static void identifierKeepsIndex(GameTestHelper helper) {
        TileEntityMachineMixer mixer = place(helper, new BlockPos(2, 1, 2));
        mixer.inventory.setStackInSlot(2, ItemFluidId.make(Fluids.FRACKSOL));
        tick(helper, mixer);
        mixer.recipeName = MixerRecipes.getOutput(Fluids.FRACKSOL)[1].name;

        mixer.inventory.setStackInSlot(2, ItemFluidId.make(Fluids.SOLVENT));
        tick(helper, mixer);
        helper.assertTrue(mixer.tanks[2].getTankType() == Fluids.SOLVENT, "the identifier retypes the output tank");
        helper.assertTrue(mixer.recipeIndex() == 1, "the second row stays selected, got " + mixer.recipeIndex());

        mixer.inventory.setStackInSlot(2, ItemFluidId.make(Fluids.SALIENT));
        tick(helper, mixer);
        helper.assertTrue(mixer.recipeIndex() == 0, "index 1 wraps onto Salient's single row");
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, TileEntityMachineMixer mixer) {
        TileEntityMachineMixer.serverTick(helper.getLevel(), mixer.getBlockPos(), mixer.getBlockState(), mixer);
    }

    /** The selection is the row's name in the save and in the sync, and an index-era save still reads (M2 R-93). */
    public static void selectionRoundTrip(GameTestHelper helper) {
        TileEntityMachineMixer mixer = place(helper, new BlockPos(2, 1, 2));
        mixer.tanks[2].setTankType(Fluids.FRACKSOL);
        CompoundTag toggle = new CompoundTag();
        toggle.putBoolean("toggle", true);
        mixer.receiveControl(toggle);
        String second = MixerRecipes.getOutput(Fluids.FRACKSOL)[1].name;
        helper.assertTrue(mixer.recipeName.equals(second), "Mixer's button did not select the second Fracksol row");

        CompoundTag saved = BlockEntities.save(mixer, helper.getLevel().registryAccess());
        helper.assertTrue(StringTag.valueOf(second).equals(saved.get("recipe")),
                "Mixer must save the selected row by name, saved " + saved.get("recipe"));
        var loaded = BlockEntities.load(mixer.getBlockPos().east(), mixer.getBlockState(), saved,
                helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof TileEntityMachineMixer restored && restored.recipeName.equals(second)
                        && restored.recipeIndex() == 1,
                "Mixer selection did not survive a save/load round trip");

        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        mixer.serialize(buffer);
        TileEntityMachineMixer synced = new TileEntityMachineMixer(mixer.getBlockPos().north(), mixer.getBlockState());
        synced.deserialize(buffer);
        buffer.release();
        helper.assertTrue(synced.recipeName.equals(second), "Mixer selection did not survive the block-entity sync");

        // a save from the input-signature era ("<in1>+<in2>+<solid>#<occurrence>") still finds its row
        CompoundTag legacy = saved.copy();
        MixerRecipes.MixerRecipe row = MixerRecipes.getOutput(Fluids.FRACKSOL)[1];
        legacy.putString("recipe", (row.input1 == null ? "-" : row.input1.type.getRegistryName()) + "+"
                + (row.input2 == null ? "-" : row.input2.type.getRegistryName()) + "+" + (row.solidInput != null) + "#0");
        var old = BlockEntities.load(mixer.getBlockPos().west(), mixer.getBlockState(), legacy,
                helper.getLevel().registryAccess());
        helper.assertTrue(old instanceof TileEntityMachineMixer restored
                        && restored.recipeName.equals(second),
                "Mixer did not read an input-signature save: " + (old instanceof TileEntityMachineMixer r ? r.recipeName : old));
        helper.succeed();
    }

    private static TileEntityMachineMixer place(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolute = helper.absolutePos(relativePos);
        MachineMixer block = ModMachineBlocks.machine_mixer.get();
        helper.setBlock(relativePos, block.defaultBlockState());
        return (TileEntityMachineMixer) helper.getLevel().getBlockEntity(absolute);
    }
}
