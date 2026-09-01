package com.hbm.tileentity.machine.storage;

import dev.golem.core.data.Tags;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.CustomData;
//?}

/** Reads the state carried by a mass-storage block item, without exposing item-era details to callers. */
public final class MassStorageItemData {

    private MassStorageItemData() {}

    public static ItemStack filter(ItemStack stack) {
        //? if >=1.20.5 {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
            contents.copyInto(items);
            return items.get(1);
        }
        //?} else {
        /*CompoundTag tag = legacyTag(stack);
        if (tag.contains("inventory", 10)) {
            NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("inventory"), items);
            return items.get(1);
        }
        if (tag.contains("slot1", 10)) return ItemStack.of(tag.getCompound("slot1"));
        *///?}
        return ItemStack.EMPTY;
    }

    public static int stockpile(ItemStack stack) {
        //? if >=1.20.5 {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Tags.getIntOr(data.copyTag(), "stack", 0);
        //?} else {
        /*return legacyTag(stack).getInt("stack");
        *///?}
    }

    public static boolean output(ItemStack stack) {
        //? if >=1.20.5 {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && Tags.getBooleanOr(data.copyTag(), "output", false);
        //?} else {
        /*return legacyTag(stack).getBoolean("output");
        *///?}
    }

    //? if <1.20.5 {
    /*private static CompoundTag legacyTag(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return new CompoundTag();
        return tag.contains("BlockEntityTag", 10) ? tag.getCompound("BlockEntityTag") : tag;
    }
    *///?}
}
