package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IInfiniteFluidItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemInfiniteFluid;
import dev.golem.core.world.ItemStacks;
import java.util.Random;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared faithful container-to-tank transfer used by the chemical machines. */
public final class MachineFluidContainerIO {

    private MachineFluidContainerIO() {}

    private static final Random RANDOM = new Random();

    /**
     * Slot validation for the 1.7 infinite items (inf_water, chlorine pinwheel, the infinite barrel) as a
     * source: 1.7 {@code FluidTank.loadTank} took them regardless of pressure, typed items only into a
     * matching tank, the untyped barrel into any typed tank.
     */
    public static boolean acceptsInfiniteSource(FluidTankNTM tank, ItemStack stack) {
        if (!(stack.getItem() instanceof IInfiniteFluidItem infinite)) return false;
        if (tank.getTankType() == Fluids.NONE) return infinite.infiniteType() != null;
        return infinite.infiniteType() == null || infinite.infiniteType() == tank.getTankType();
    }

    /** Slot validation for an infinite item as a bottomless drain (1.7 {@code FluidLoaderInfinite.fillItem}). */
    public static boolean acceptsInfiniteSink(FluidTankNTM tank, ItemStack stack) {
        if (!(stack.getItem() instanceof IInfiniteFluidItem infinite)) return false;
        if (!infinite.allowPressure(tank.getPressure())) return false;
        return infinite.infiniteType() == null || infinite.infiniteType() == tank.getTankType();
    }

    /** True when {@code stack} is an infinite item; the tank was filled from it as 1.7 did (never consumed). */
    public static boolean loadInfinite(FluidTankNTM tank, ItemStack stack) {
        if (!(stack.getItem() instanceof IInfiniteFluidItem infinite)) return false;
        // hbm-space FluidLoaderInfinite: a table-bound item that found the wrong water table gives nothing
        if (!ItemInfiniteFluid.canOperate(stack)) return true;
        if (tank.getTankType() == Fluids.NONE) {
            if (infinite.infiniteType() == null) return true;
            tank.setTankType(infinite.infiniteType());
        }
        if (infinite.infiniteType() != null && tank.getTankType() != infinite.infiniteType()) return true;
        if (infinite.infiniteChance() <= 1 || RANDOM.nextInt(infinite.infiniteChance()) == 0) {
            tank.setFill(Math.min(tank.getFill() + infinite.infiniteAmount(), tank.getMaxFill()));
        }
        return true;
    }

    /** True when {@code stack} is an infinite item; the tank was drained into it as 1.7 did. */
    public static boolean unloadInfinite(FluidTankNTM tank, ItemStack stack) {
        if (!(stack.getItem() instanceof IInfiniteFluidItem infinite)) return false;
        if (!ItemInfiniteFluid.canOperate(stack)) return true;
        if (!infinite.allowPressure(tank.getPressure())) return true;
        if (infinite.infiniteType() != null && tank.getTankType() != infinite.infiniteType()) return true;
        if (infinite.infiniteChance() <= 1 || RANDOM.nextInt(infinite.infiniteChance()) == 0) {
            tank.setFill(Math.max(tank.getFill() - infinite.infiniteAmount(), 0));
        }
        return true;
    }

    public static boolean load(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) return false;
        // 1.7 FluidLoaderInfinite.emptyItem: the item is never consumed or moved.
        if (loadInfinite(tank, input)) return true;
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

    public static boolean unload(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        FluidType type = tank.getTankType();
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.getItem() instanceof IInfiniteFluidItem infinite) {
            // 1.7 FluidLoaderInfinite.fillItem: an infinite item in the unload slot is a bottomless drain.
            if (!infinite.allowPressure(tank.getPressure())) return false;
            if (infinite.infiniteType() != null && type != infinite.infiniteType()) return false;
            if (infinite.infiniteChance() <= 1 || RANDOM.nextInt(infinite.infiniteChance()) == 0) {
                tank.setFill(Math.max(tank.getFill() - infinite.infiniteAmount(), 0));
            }
            return true;
        }
        if (type == Fluids.NONE || tank.getFill() <= 0 || input.isEmpty() || tank.getPressure() != 0) return false;
        if (fillArmorMods && ArmorModHandler.hasMods(input)) {
            boolean filled = fillMods(tank, type, inventory, inputSlot, input);
            if (!(input.getItem() instanceof IFillableItem)) return filled;
            input = inventory.getStackInSlot(inputSlot);
        }
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

    /**
     * hbm-space {@code FluidLoaderFillableItem.fill}: an armor piece in the unload slot fills its fillable modules, and
     * each filled module is written back into the armor ({@code ArmorModHandler.applyMod}; 1.7 filled a copy and lost
     * it). ADDON-API-INVENTORY V-185; off in plain HBM, which leaves the modules alone. NTM: Space turns it on (its
     * oxygen tank module refills here).
     */
    public static boolean fillArmorMods = false;

    private static boolean fillMods(FluidTankNTM tank, FluidType type, NTMItemHandler inventory, int slot, ItemStack armor) {
        ItemStack work = armor.copy();
        boolean moved = false;
        for (ItemStack mod : ArmorModHandler.pryMods(work)) {
            if (mod == null || mod.isEmpty() || !(mod.getItem() instanceof IFillableItem fillable)) continue;
            if (tank.getFill() <= 0 || !fillable.acceptsFluid(type, mod)) continue;
            int remainder = fillable.tryFill(type, tank.getFill(), mod);
            if (remainder == tank.getFill()) continue;
            tank.setFill(remainder);
            ArmorModHandler.applyMod(work, mod);
            moved = true;
        }
        if (moved) inventory.setStackInSlot(slot, work);
        return moved;
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
