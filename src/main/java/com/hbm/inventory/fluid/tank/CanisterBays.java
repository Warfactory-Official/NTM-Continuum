package com.hbm.inventory.fluid.tank;

import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.items.machine.ItemFluidBucket;
import dev.golem.core.machine.ItemHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The canister bay every tank-backed machine shares: a container in one slot is emptied into, or filled
 * from, the machine's tank and the result lands in another slot.
 *
 * <p>A bay is bidirectional — a full container drains in, an empty one fills out — so a machine's two
 * bays are interchangeable rather than the fork's directional load/unload pair. Nothing moves unless the
 * out slot can hold the whole result, so a bay never destroys a container it cannot place.
 *
 * <p>Handles the universal tank/barrel ({@link IFillableItem}) and buckets, both HBM's and vanilla's
 * water and lava. A tank holding {@code NONE} adopts whatever arrives, which is what
 * {@code onTypeChange} exists to report: the caller's network node is keyed to the old fluid and has to
 * be torn down.
 */
public final class CanisterBays {

    private CanisterBays() {}

    /** Called with the type the tank held before a bay changed it. */
    @FunctionalInterface
    public interface TypeChanged {
        void accept(FluidType before);
    }

    public static void process(ItemHandler inventory, FluidTankNTM tank, int inSlot, int outSlot,
                               TypeChanged onTypeChange) {
        ItemStack in = inventory.getStackInSlot(inSlot);
        if (in.isEmpty()) return;
        FluidType tankType = tank.getTankType();

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);

            if (held > 0 && heldType != null && heldType != Fluids.NONE
                    && (tankType == Fluids.NONE || tankType == heldType)) {
                int addable = tank.fill(heldType, held, false);
                if (addable > 0) {
                    ItemStack work = single(in);
                    int taken = fillable.tryEmpty(heldType, addable, work);
                    if (taken > 0 && fits(inventory, outSlot, work)) {
                        FluidType before = tank.getTankType();
                        tank.fill(heldType, taken, true);
                        moveToOut(inventory, inSlot, outSlot, work);
                        onTypeChange.accept(before);
                        return;
                    }
                }
            }

            if (tankType != Fluids.NONE && tank.getFill() > 0 && fillable.acceptsFluid(tankType, in)) {
                ItemStack work = single(in);
                int remainder = fillable.tryFill(tankType, tank.getFill(), work);
                int moved = tank.getFill() - remainder;
                if (moved > 0 && fits(inventory, outSlot, work)) {
                    tank.setFill(tank.getFill() - moved);
                    moveToOut(inventory, inSlot, outSlot, work);
                }
            }
            return;
        }

        FluidType bucketFluid = bucketFluid(in);
        if (bucketFluid != Fluids.NONE && (tankType == Fluids.NONE || tankType == bucketFluid)
                && tank.fill(bucketFluid, ItemFluidBucket.VOLUME, false) >= ItemFluidBucket.VOLUME) {
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (fits(inventory, outSlot, empty)) {
                FluidType before = tank.getTankType();
                tank.fill(bucketFluid, ItemFluidBucket.VOLUME, true);
                moveToOut(inventory, inSlot, outSlot, empty);
                onTypeChange.accept(before);
            }
        } else if (in.is(Items.BUCKET) && tankType != Fluids.NONE && tank.getFill() >= ItemFluidBucket.VOLUME) {
            ItemStack filled = tankType == Fluids.WATER ? new ItemStack(Items.WATER_BUCKET)
                    : tankType == Fluids.LAVA ? new ItemStack(Items.LAVA_BUCKET)
                    : ItemFluidBucket.make(tankType);
            if (fits(inventory, outSlot, filled)) {
                tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
                moveToOut(inventory, inSlot, outSlot, filled);
            }
        }
    }

    /** The fluid a full bucket in {@code stack} would give the tank (NONE if it's not a full bucket). */
    public static FluidType bucketFluid(ItemStack stack) {
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

    private static boolean fits(ItemHandler inventory, int outSlot, ItemStack stack) {
        return inventory.insertItem(outSlot, stack.copy(), true).isEmpty();
    }

    private static void moveToOut(ItemHandler inventory, int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
    }
}
