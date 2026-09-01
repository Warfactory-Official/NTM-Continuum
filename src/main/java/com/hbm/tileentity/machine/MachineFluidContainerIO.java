package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemFluidBucket;
import dev.golem.core.world.ItemStacks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared faithful container-to-tank transfer used by the chemical machines. */
final class MachineFluidContainerIO {

    private MachineFluidContainerIO() {}

    static boolean load(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) return false;
        if (input.getItem() instanceof IFillableItem fillable) {
            ItemStack result = single(input);
            FluidType type = tank.getTankType() != Fluids.NONE
                    ? tank.getTankType() : fillable.getFirstFluidType(result);
            int space = tank.getMaxFill() - tank.getFill();
            if (type == Fluids.NONE || space <= 0 || !fillable.providesFluid(type, result)) return false;
            int transferred = fillable.tryEmpty(type, space, result);
            if (transferred <= 0) return false;
            if (input.getCount() > 1) {
                if (!fitsInternally(inventory, outputSlot, result)) return false;
                inventory.extractItem(inputSlot, 1, false);
                if (tank.getTankType() == Fluids.NONE) tank.setTankType(type);
                tank.setFill(tank.getFill() + transferred);
                insertInternally(inventory, outputSlot, result);
                return true;
            }
            boolean preferOutput = isEmpty(result) || tank.getFill() + transferred >= tank.getMaxFill();
            int destination = preferOutput && fitsInternally(inventory, outputSlot, result) ? outputSlot : inputSlot;
            inventory.extractItem(inputSlot, 1, false);
            if (tank.getTankType() == Fluids.NONE) tank.setTankType(type);
            tank.setFill(tank.getFill() + transferred);
            insertInternally(inventory, destination, result);
            return true;
        }
        FluidType type = bucketFluid(input);
        if (type == Fluids.NONE || tank.fill(type, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return false;
        ItemStack result = new ItemStack(Items.BUCKET);
        ItemStacks.copyCustomName(input, result);
        if (!fitsInternally(inventory, outputSlot, result)) return false;
        tank.fill(type, ItemFluidBucket.VOLUME, true);
        inventory.extractItem(inputSlot, 1, false);
        insertInternally(inventory, outputSlot, result);
        return true;
    }

    static boolean unload(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        FluidType type = tank.getTankType();
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (type == Fluids.NONE || tank.getFill() <= 0 || input.isEmpty() || tank.getPressure() != 0) return false;
        if (input.getItem() instanceof IFillableItem fillable) {
            if (!fillable.acceptsFluid(type, input)) return false;
            ItemStack result = single(input);
            int remainder = fillable.tryFill(type, tank.getFill(), result);
            int transferred = tank.getFill() - remainder;
            if (transferred <= 0) return false;
            if (input.getCount() > 1) {
                if (!fitsInternally(inventory, outputSlot, result)) return false;
                inventory.extractItem(inputSlot, 1, false);
                tank.setFill(tank.getFill() - transferred);
                insertInternally(inventory, outputSlot, result);
                return true;
            }
            boolean preferOutput = isFull(result, type) || tank.getFill() - transferred == 0;
            int destination = preferOutput && fitsInternally(inventory, outputSlot, result) ? outputSlot : inputSlot;
            inventory.extractItem(inputSlot, 1, false);
            tank.setFill(tank.getFill() - transferred);
            insertInternally(inventory, destination, result);
            return true;
        }
        if (!input.is(Items.BUCKET) || tank.getFill() < ItemFluidBucket.VOLUME) return false;
        ItemStack result = type == Fluids.WATER ? new ItemStack(Items.WATER_BUCKET)
                : type == Fluids.LAVA ? new ItemStack(Items.LAVA_BUCKET) : ItemFluidBucket.make(type);
        ItemStacks.copyCustomName(input, result);
        if (!fitsInternally(inventory, outputSlot, result)) return false;
        tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
        inventory.extractItem(inputSlot, 1, false);
        insertInternally(inventory, outputSlot, result);
        return true;
    }

    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static boolean isFull(ItemStack stack, FluidType type) {
        if (!(stack.getItem() instanceof IFillableItem fillable) || !fillable.acceptsFluid(type, stack)) return true;
        return fillable.tryFill(type, 1, stack.copy()) >= 1;
    }

    private static boolean isEmpty(ItemStack stack) {
        if (!(stack.getItem() instanceof IFillableItem fillable)) return true;
        FluidType type = fillable.getFirstFluidType(stack);
        return type == null || type == Fluids.NONE || fillable.tryEmpty(type, 1, stack.copy()) <= 0;
    }

    /** Internal machine movement mirrors the old raw ItemStackHandler, not sided automation validation. */
    private static boolean fitsInternally(NTMItemHandler inventory, int slot, ItemStack stack) {
        ItemStack existing = inventory.getStackInSlot(slot);
        int limit = Math.min(inventory.getSlotLimit(slot), stack.getMaxStackSize());
        return existing.isEmpty() ? stack.getCount() <= limit
                : ItemStacks.sameItemAndData(existing, stack) && existing.getCount() + stack.getCount() <= limit;
    }

    private static void insertInternally(NTMItemHandler inventory, int slot, ItemStack stack) {
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(slot, stack);
        } else {
            existing.grow(stack.getCount());
            inventory.setStackInSlot(slot, existing);
        }
    }
}
