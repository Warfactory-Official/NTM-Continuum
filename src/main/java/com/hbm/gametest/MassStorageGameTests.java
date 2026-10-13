package com.hbm.gametest;

import com.hbm.blocks.machine.storage.BlockMassStorage;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.inventory.container.ContainerMassStorage;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.storage.MassStorageType;
import com.hbm.tileentity.machine.storage.MassStorageItemData;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.world.ItemStacks;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** GameTest contracts for the four registered mass-storage variants and shared behavior. */
public final class MassStorageGameTests {

    private MassStorageGameTests() {}

    public static void capacities(GameTestHelper helper) {
        // 1.7 BlockMassStorage.getCapacity: wood (meta 3) 100, iron 10,000, desh 100,000, mass storage 1,000,000.
        helper.assertTrue(MassStorageType.WOOD.capacity == 100, "wood mass-storage capacity must be 1.7's 100");
        helper.assertTrue(MassStorageType.IRON.capacity == 10_000, "iron mass-storage capacity changed");
        helper.assertTrue(MassStorageType.DESH.capacity == 100_000, "desh mass-storage capacity changed");
        helper.assertTrue(MassStorageType.MASS.capacity == 1_000_000, "mass-storage capacity changed");

        for (int i = 0; i < MassStorageType.values().length; i++) {
            MassStorageType type = MassStorageType.values()[i];
            TileEntityMassStorage storage = place(helper, type, new BlockPos(2 + i * 2, 1, 2));
            storage.setStockpile(type.capacity);
            helper.assertTrue(storage.getComparatorPower() == 15,
                    "full comparator output changed for " + type.id);
            storage.setStockpile(0);
            helper.assertTrue(storage.getComparatorPower() == 0,
                    "empty comparator output changed for " + type.id);
        }
        helper.succeed();
    }

    public static void filteringAndProcessing(GameTestHelper helper) {
        TileEntityMassStorage storage = place(helper, MassStorageType.WOOD, new BlockPos(2, 1, 2));
        ItemStack filter = new ItemStack(Items.STONE);
        storage.setFilter(filter);

        storage.setItem(TileEntityMassStorage.INPUT_SLOT, new ItemStack(Items.DIRT, 8));
        tick(storage, helper);
        helper.assertTrue(storage.getStockpile() == 0 && storage.getItem(TileEntityMassStorage.INPUT_SLOT).getCount() == 8,
                "mass storage accepted an item outside its filter");

        storage.setItem(TileEntityMassStorage.INPUT_SLOT, new ItemStack(Items.STONE, 8));
        tick(storage, helper);
        helper.assertTrue(storage.getStockpile() == 8 && storage.getItem(TileEntityMassStorage.INPUT_SLOT).isEmpty(),
                "mass storage did not consume its exact filtered item");

        storage.output = true;
        tick(storage, helper);
        helper.assertTrue(storage.getStockpile() == 0
                        && storage.getItem(TileEntityMassStorage.OUTPUT_SLOT).getCount() == 8,
                "automatic mass-storage output changed");
        helper.succeed();
    }

    public static void componentIdentity(GameTestHelper helper) {
        ItemStack stone = new ItemStack(Items.STONE);
        ItemStack dirt = new ItemStack(Items.DIRT);
        helper.assertFalse(ItemStacks.sameItemAndData(stone, dirt), "mass-storage identity ignored item type");

        //? if >=1.20.5 {
        net.minecraft.nbt.CompoundTag first = new net.minecraft.nbt.CompoundTag();
        first.putInt("variant", 1);
        net.minecraft.nbt.CompoundTag second = new net.minecraft.nbt.CompoundTag();
        second.putInt("variant", 2);
        stone.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(first));
        ItemStack otherComponents = new ItemStack(Items.STONE);
        otherComponents.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(second));
        helper.assertFalse(ItemStacks.sameItemAndData(stone, otherComponents),
                "mass-storage identity ignored item components");
        //?}
        helper.succeed();
    }

    public static void carriedState(GameTestHelper helper) {
        TileEntityMassStorage storage = place(helper, MassStorageType.IRON, new BlockPos(2, 1, 2));
        ItemStack filter = new ItemStack(Items.GOLD_INGOT);
        storage.setFilter(filter);
        storage.setStockpile(321);
        storage.output = true;
        ItemStack carried = new ItemStack(ModStorageBlocks.mass_storage_iron.get().asItem());
        storage.saveToItemCompat(carried);
        helper.assertTrue(ItemStacks.sameItemAndData(MassStorageItemData.filter(carried), filter)
                        && MassStorageItemData.stockpile(carried) == 321
                        && MassStorageItemData.output(carried),
                "mass-storage item did not retain filter/stock/output state");
        helper.succeed();
    }

    public static void manualOutputAndSync(GameTestHelper helper) {
        TileEntityMassStorage source = place(helper, MassStorageType.IRON, new BlockPos(2, 1, 2));
        source.setFilter(new ItemStack(Items.COBBLESTONE));
        source.setStockpile(20);
        source.output = false;
        source.redstone = 7;
        source.provide(false);
        helper.assertTrue(source.getStockpile() == 19
                        && source.getItem(TileEntityMassStorage.OUTPUT_SLOT).getCount() == 1,
                "manual one-item output changed");
        source.provide(true);
        helper.assertTrue(source.getStockpile() == 0
                        && source.getItem(TileEntityMassStorage.OUTPUT_SLOT).getCount() == 20,
                "manual stack output changed");

        TileEntityMassStorage copy = place(helper, MassStorageType.IRON, new BlockPos(5, 1, 2));
        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        source.serialize(buffer);
        copy.deserialize(buffer);
        helper.assertTrue(copy.getStockpile() == source.getStockpile() && copy.output == source.output
                        && copy.redstone == source.redstone
                        && ItemStacks.sameItemAndData(copy.getFilterType(), source.getFilterType()),
                "mass-storage synced fields did not round-trip");
        helper.succeed();
    }

    public static void menuAndControls(GameTestHelper helper) {
        TileEntityMassStorage storage = place(helper, MassStorageType.WOOD, new BlockPos(2, 1, 2));
        storage.setFilter(new ItemStack(Items.IRON_INGOT));
        storage.setStockpile(10);
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerMassStorage menu = new ContainerMassStorage(1, player.getInventory(), storage,
                ModMenus.MASS_STORAGE::get);
        helper.assertTrue(menu.slots.size() == 39, "mass-storage menu must expose three plus 36 slots");
        helper.assertTrue(menu.clickMenuButton(player, 0) && storage.getStockpile() == 9
                        && storage.getItem(TileEntityMassStorage.OUTPUT_SLOT).getCount() == 1,
                "mass-storage provide button changed");
        helper.assertTrue(menu.clickMenuButton(player, 2) && storage.output,
                "mass-storage output toggle changed");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] assets = {
                "assets/hbm/blockstates/mass_storage_wood.json",
                "assets/hbm/blockstates/mass_storage_iron.json",
                "assets/hbm/blockstates/mass_storage_desh.json",
                "assets/hbm/blockstates/mass_storage.json",
                "assets/hbm/models/block/mass_storage_wood.json",
                "assets/hbm/models/block/mass_storage_iron.json",
                "assets/hbm/models/block/mass_storage_desh.json",
                "assets/hbm/models/block/mass_storage.json",
                "assets/hbm/models/item/mass_storage_wood.json",
                "assets/hbm/models/item/mass_storage_iron.json",
                "assets/hbm/models/item/mass_storage_desh.json",
                "assets/hbm/models/item/mass_storage.json",
                "assets/hbm/items/mass_storage_wood.json",
                "assets/hbm/items/mass_storage_iron.json",
                "assets/hbm/items/mass_storage_desh.json",
                "assets/hbm/items/mass_storage.json",
                "assets/hbm/textures/gui/storage/gui_mass_storage.png",
                "assets/hbm/textures/blocks/mass_storage_front_wood.png",
                "assets/hbm/textures/blocks/mass_storage_front_iron.png",
                "assets/hbm/textures/blocks/mass_storage_front_desh.png",
                "assets/hbm/textures/blocks/mass_storage_front.png",
                "data/hbm/loot_table/blocks/mass_storage_wood.json",
                "data/hbm/loot_table/blocks/mass_storage_iron.json",
                "data/hbm/loot_table/blocks/mass_storage_desh.json",
                "data/hbm/loot_table/blocks/mass_storage.json"
        };
        for (String asset : assets) {
            helper.assertTrue(resourceExists(asset), "mass-storage asset is missing: " + asset);
        }
        helper.succeed();
    }

    /**
     * 1.7 keeps the facing in metadata and draws the front on that side. The default is north (1.7's
     * meta 0, and what a block saved before the property existed loads as); rotation turns the front.
     */
    public static void facing(GameTestHelper helper) {
        for (MassStorageType type : MassStorageType.values()) {
            TileEntityMassStorage storage = place(helper, type, new BlockPos(2 + type.ordinal() * 2, 1, 2));
            BlockState state = storage.getBlockState();
            helper.assertTrue(state.hasProperty(BlockMassStorage.FACING)
                            && state.getValue(BlockMassStorage.FACING) == Direction.NORTH,
                    "mass storage must default to a north front: " + type.id);
            helper.assertTrue(state.rotate(Rotation.CLOCKWISE_90)
                            .getValue(BlockMassStorage.FACING) == Direction.EAST,
                    "rotating mass storage must turn its front: " + type.id);
        }
        helper.succeed();
    }

    private static TileEntityMassStorage place(GameTestHelper helper, MassStorageType type, BlockPos relativePos) {
        BlockMassStorage block = switch (type) {
            case WOOD -> ModStorageBlocks.mass_storage_wood.get();
            case IRON -> ModStorageBlocks.mass_storage_iron.get();
            case DESH -> ModStorageBlocks.mass_storage_desh.get();
            case MASS -> ModStorageBlocks.mass_storage.get();
        };
        BlockPos pos = helper.absolutePos(relativePos);
        BlockState state = block.defaultBlockState();
        helper.getLevel().setBlock(pos, state, 3);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof TileEntityMassStorage,
                "registered mass-storage block did not create its BE: " + type.id);
        return (TileEntityMassStorage) entity;
    }

    private static void tick(TileEntityMassStorage storage, GameTestHelper helper) {
        TileEntityMassStorage.serverTick(helper.getLevel(), storage.getBlockPos(),
                storage.getBlockState(), storage);
    }

    private static boolean resourceExists(String path) {
        return GameTestHelpers.resourceExists(MassStorageGameTests.class, path);
    }
}
