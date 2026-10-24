package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IInfiniteFluidItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.machine.ItemInfiniteFluid;
import com.hbm.items.special.ItemCell;
import dev.golem.core.world.ItemStacks;
import java.util.Random;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code FluidTank.loadTank} / {@code unloadTank} and their loading handlers, for every machine with container slots. */
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
        return tank.accepts(infinite.infiniteType() == null ? tank.getFluid() : infinite.infiniteType());
    }

    /** Slot validation for an infinite item as a bottomless drain (1.7 {@code FluidLoaderInfinite.fillItem}). */
    public static boolean acceptsInfiniteSink(FluidTankNTM tank, ItemStack stack) {
        if (!(stack.getItem() instanceof IInfiniteFluidItem infinite)) return false;
        if (!infinite.allowPressure(tank.getPressure())) return false;
        return infinite.infiniteType() == null || tank.provides(infinite.infiniteType());
    }

    /**
     * True when {@code stack} is an infinite item feeding the tank as 1.7 did (never consumed). 1.7
     * {@code FluidLoaderInfinite.emptyItem} gives a NONE tank nothing.
     */
    public static boolean loadInfinite(FluidTankNTM tank, ItemStack stack) {
        if (!(stack.getItem() instanceof IInfiniteFluidItem infinite)) return false;
        // hbm-space FluidLoaderInfinite: a table-bound item that found the wrong water table gives nothing
        if (!ItemInfiniteFluid.canOperate(stack)) return true;
        if (tank.getTankType() == Fluids.NONE) return false;
        var incoming = infinite.infiniteType() == null ? tank.getFluid() : infinite.infiniteType();
        if (!tank.accepts(incoming)) return true;
        if (infinite.infiniteChance() <= 1 || RANDOM.nextInt(infinite.infiniteChance()) == 0) {
            tank.receive(incoming, infinite.infiniteAmount());
        }
        return true;
    }

    /** True when {@code stack} is an infinite item; the tank was drained into it as 1.7 did. */
    public static boolean unloadInfinite(FluidTankNTM tank, ItemStack stack) {
        if (!(stack.getItem() instanceof IInfiniteFluidItem infinite)) return false;
        if (!ItemInfiniteFluid.canOperate(stack)) return true;
        if (!infinite.allowPressure(tank.getPressure())) return true;
        if (infinite.infiniteType() != null && !tank.provides(infinite.infiniteType())) return true;
        if (infinite.infiniteChance() <= 1 || RANDOM.nextInt(infinite.infiniteChance()) == 0) {
            tank.setFill(Math.max(tank.getFill() - infinite.infiniteAmount(), 0));
        }
        return true;
    }

    /**
     * 1.7 {@code FluidTank.loadTank(in, out)}: the loading handlers in 1.7's order. A registry container that fits whole
     * empties into the tank and its empty container goes to {@code out} ({@code FluidLoaderStandard}); a fillable item
     * is drained where it sits ({@code FluidLoaderFillableItem}); an infinite item feeds the tank and stays
     * ({@code FluidLoaderInfinite}). Every handler asks for the tank's own type, so a NONE tank takes nothing. Answers
     * whether anything changed.
     */
    public static boolean load(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) return false;
        if (tank.getPressure() != 0 && !input.is(ModFluidNetItems.fluid_barrel_infinite.get())) return false;
        int before = tank.getFill();
        if (emptyContainer(tank, inventory, inputSlot, outputSlot)) return tank.getFill() != before;
        if (emptyFillable(tank, inventory, inputSlot, outputSlot)) return tank.getFill() != before;
        return loadInfinite(tank, input) && tank.getFill() != before;
    }

    /**
     * 1.7 {@code FluidTank.unloadTank(in, out)}: an empty registry container is filled whole from the tank and the full
     * one goes to {@code out}; a fillable item is filled where it sits; an infinite item is a bottomless drain.
     */
    public static boolean unload(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) return false;
        int before = tank.getFill();
        if (fillContainer(tank, inventory, inputSlot, outputSlot)) return true;
        if (fillFillable(tank, inventory, inputSlot, outputSlot)) return tank.getFill() != before;
        return unloadInfinite(tank, inventory.getStackInSlot(inputSlot)) && tank.getFill() != before;
    }

    /** 1.7 {@code FluidLoaderStandard.emptyItem}. */
    private static boolean emptyContainer(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        ItemStack input = inventory.getStackInSlot(inputSlot);
        FluidType type = tank.getFluid();
        if (input.getItem() instanceof IFillableItem fillable) {
            var actual = fillable.getFirstFluidType(input);
            if (tank.accepts(actual) && FluidContainerRegistry.INSTANCE.contentOf(input, actual) > 0) type = actual;
        }
        int amount = FluidContainerRegistry.INSTANCE.contentOf(input, type);
        ItemStack empty = FluidContainerRegistry.INSTANCE.emptyOf(input);
        // port: the port-only universal bucket, which 1.7's registry never had
        if (amount <= 0 && input.getItem() instanceof ItemFluidBucket && tank.accepts(ItemFluidBucket.getFluid(input))) {
            type = ItemFluidBucket.getFluid(input);
            amount = ItemFluidBucket.VOLUME;
            empty = new ItemStack(Items.BUCKET);
        }
        if (amount <= 0 || !tank.accepts(type) || amount > tank.getMaxFill() - tank.getFill()) return false;
        if (!empty.isEmpty()) ItemStacks.copyCustomName(input, empty);
        if (!empty.isEmpty() && !fitsInternally(inventory, outputSlot, empty)) return true;
        tank.receive(type, amount);
        inventory.extractItem(inputSlot, 1, false);
        if (!empty.isEmpty()) insertInternally(inventory, outputSlot, empty);
        return true;
    }

    /** 1.7 {@code FluidLoaderStandard.fillItem}. */
    private static boolean fillContainer(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        if (tank.getPressure() != 0) return false;
        ItemStack input = inventory.getStackInSlot(inputSlot);
        FluidType type = tank.getFluid();
        ItemStack full = FluidContainerRegistry.INSTANCE.fullOf(input, type);
        int amount = FluidContainerRegistry.INSTANCE.contentOf(full, type);
        // port: an empty bucket becomes the port-only universal bucket for fluids 1.7 had no bucket for
        if (full.isEmpty() && input.is(Items.BUCKET) && type != Fluids.NONE) {
            full = ItemFluidBucket.make(type);
            amount = ItemFluidBucket.VOLUME;
        }
        if (full.isEmpty() || amount <= 0 || tank.getFill() < amount) return false;
        ItemStacks.copyCustomName(input, full);
        if (!fitsInternally(inventory, outputSlot, full)) return false;
        tank.setFill(tank.getFill() - amount);
        inventory.extractItem(inputSlot, 1, false);
        insertInternally(inventory, outputSlot, full);
        return true;
    }

    /**
     * 1.7 {@code FluidLoaderFillableItem.empty}: the item is drained in its slot. Registry containers were never fillable
     * items in 1.7; a part-filled one (only the port can make those) is drained here so it is not stuck.
     */
    private static boolean emptyFillable(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (!(input.getItem() instanceof IFillableItem fillable) || input.getItem() instanceof IInfiniteFluidItem) return false;
        if (FluidContainerRegistry.INSTANCE.isRegistryContainer(input) && !partFilled(input)) return false;
        FluidType type = tank.getFluid();
        // Multi-fluid armor can provide a requested module even when another module is first.
        if (!fillable.providesFluid(type, input)) type = fillable.getFirstFluidType(input);
        if (!tank.accepts(type) || !fillable.providesFluid(type, input)) return false;
        ItemStack work = input.copy();
        work.setCount(1);
        int taken = fillable.tryEmpty(type, tank.getMaxFill() - tank.getFill(), work);
        if (taken <= 0 || !canStoreProcessedItem(inventory, inputSlot, outputSlot, work)) return true;
        tank.receive(type, taken);
        storeProcessedItem(inventory, inputSlot, outputSlot, work);
        return true;
    }

    /** 1.7 {@code FluidLoaderFillableItem.fill}: the item is filled in its slot. */
    private static boolean fillFillable(FluidTankNTM tank, NTMItemHandler inventory, int inputSlot, int outputSlot) {
        if (tank.getPressure() != 0) return false;
        ItemStack input = inventory.getStackInSlot(inputSlot);
        FluidType type = tank.getFluid();
        if (type == Fluids.NONE) return false;
        boolean filled = false;
        var stacked = input.getCount() > 1;
        if (fillArmorMods && ArmorModHandler.hasMods(input)) {
            filled = fillMods(tank, type, inventory, inputSlot, outputSlot, input);
            if (filled && stacked) return true;
            input = inventory.getStackInSlot(inputSlot);
        }
        if (!(input.getItem() instanceof IFillableItem fillable) || input.getItem() instanceof IInfiniteFluidItem) return filled;
        if (FluidContainerRegistry.INSTANCE.isRegistryContainer(input) && !partFilled(input)) return filled;
        if (!fillable.acceptsFluid(type, input)) return true;
        ItemStack work = input.copy();
        work.setCount(1);
        int remainder = fillable.tryFill(type, tank.getFill(), work);
        if (remainder != tank.getFill() && canStoreProcessedItem(inventory, inputSlot, outputSlot, work)) {
            tank.setFill(remainder);
            storeProcessedItem(inventory, inputSlot, outputSlot, work);
        }
        return true;
    }

    private static boolean partFilled(ItemStack stack) {
        IFillableItem fillable = (IFillableItem) stack.getItem();
        int fill = fillable.getFill(stack);
        int capacity = stack.getItem() instanceof ItemFluidTank tank ? tank.capacity : ItemCell.CAPACITY;
        return fill > 0 && fill < capacity;
    }

    /**
     * hbm-space {@code FluidLoaderFillableItem.fill}: an armor piece in the unload slot fills its fillable modules, and
     * each filled module is written back into the armor ({@code ArmorModHandler.applyMod}; 1.7 filled a copy and lost
     * it). Off in plain HBM, which leaves the modules alone. NTM: Space turns it on (its
     * oxygen tank module refills here).
     */
    public static boolean fillArmorMods = false;

    private static boolean fillMods(FluidTankNTM tank, FluidType type, NTMItemHandler inventory, int slot, int outputSlot, ItemStack armor) {
        ItemStack work = armor.copy();
        work.setCount(1);
        var remaining = tank.getFill();
        boolean moved = false;
        for (ItemStack mod : ArmorModHandler.pryMods(work)) {
            if (mod == null || mod.isEmpty() || !(mod.getItem() instanceof IFillableItem fillable)) continue;
            if (remaining <= 0 || !fillable.acceptsFluid(type, mod)) continue;
            int remainder = fillable.tryFill(type, remaining, mod);
            if (remainder == remaining) continue;
            remaining = remainder;
            ArmorModHandler.applyMod(work, mod);
            moved = true;
        }
        if (moved && canStoreProcessedItem(inventory, slot, outputSlot, work)) {
            tank.setFill(remaining);
            storeProcessedItem(inventory, slot, outputSlot, work);
        } else {
            moved = false;
        }
        return moved;
    }

    private static boolean canStoreProcessedItem(NTMItemHandler inventory, int inputSlot, int outputSlot, ItemStack processed) {
        return inventory.getStackInSlot(inputSlot).getCount() <= 1
                || inputSlot != outputSlot && (processed.isEmpty() || fitsInternally(inventory, outputSlot, processed));
    }

    private static void storeProcessedItem(NTMItemHandler inventory, int inputSlot, int outputSlot, ItemStack processed) {
        if (inventory.getStackInSlot(inputSlot).getCount() <= 1) {
            inventory.setStackInSlot(inputSlot, processed);
        } else {
            inventory.extractItem(inputSlot, 1, false);
            if (!processed.isEmpty()) insertInternally(inventory, outputSlot, processed);
        }
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
