package com.hbm.tileentity.network;

import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.hbm.blocks.network.CranePartitioner;
import com.hbm.tileentity.ModConveyorBlockEntities;
import com.hbm.util.ConveyorInventories;

/** 1.7 {@code CranePartitioner.TileEntityCranePartitioner}: 45 queue slots and 45 declog slots for anything else. */
public class TileEntityCranePartitioner extends TileEntityMachineBase implements WorldlyContainer {

    /** 1.7's 45 queue and 45 declog slots. */
    public static final int SLOT_COUNT = 45;
    /**
     * Queue ({@code INPUT_COUNT}) and declog ({@code OUTPUT_COUNT}) slot counts, 1.7's 45 and
     * 45 in plain HBM; NTM: Space sets hbm-space's 96 and 8. Set before any partitioner exists (an addon's
     * {@code declare}); the inventory is saved by slot, so a world keeps what fits.
     */
    public static int inputCount = SLOT_COUNT, outputCount = SLOT_COUNT;
    private final int inputs = inputCount;
    private final int[] access = ConveyorInventories.range(inputCount + outputCount);

    public static final Comparator<ItemStack> STACK_SIZE = Comparator.comparingInt(ItemStack::getCount);

    public TileEntityCranePartitioner(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.CRANE_PARTITIONER.get(), pos, state, inputCount + outputCount);
    }

    @Override
    public String getDefaultName() {
        return "container.partitioner";
    }

    /**
     * 1.7 {@code CrystallizerRecipes.getAmount}: the input count of the acidizer recipe taking {@code stack}, or 0.
     * The old map preferred an exact item key over a tag key, and later registrations overwrote earlier ones.
     */
    public static int getAmount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        CrystallizerRecipes.register();
        int exact = 0;
        int tagged = 0;
        for (CrystallizerRecipes.CrystallizerRecipe recipe : CrystallizerRecipes.recipes) {
            if (!recipe.input.matchesRecipe(stack, true)) continue;
            if (recipe.input instanceof ComparableStack) exact = recipe.input.stacksize;
            else tagged = recipe.input.stacksize;
        }
        return exact > 0 ? exact : tagged;
    }

    /** The queue slot count of this partitioner (its declog band starts here). */
    public int inputCount() {
        return inputs;
    }

    public void serverTick() {
        if (level == null) return;

        List<Integer> queued = new ArrayList<>();
        for (int i = 0; i < inputs; i++) if (!getItem(i).isEmpty()) queued.add(i);
        queued.sort(Comparator.comparingInt(slot -> getItem(slot).getCount()));

        for (int slot : queued) {
            ItemStack stack = getItem(slot);
            int amount = getAmount(stack);
            if (amount == 0) amount = stack.getCount(); // eject full stack if invalid type somehow ended up in the queue

            while (stack.getCount() >= amount && amount > 0) {
                ItemStack entityStack = stack.copyWithCount(amount);
                stack.shrink(amount);
                EntityMovingItem item = new EntityMovingItem(level);
                item.setItemStack(entityStack);
                item.snapTo(worldPosition.getX() + 0.5, worldPosition.getY() + 0.25, worldPosition.getZ() + 0.5, 0F, 0F);
                level.addFreshEntity(item);
                setChanged();
            }
            if (stack.isEmpty()) setItem(slot, ItemStack.EMPTY);
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return access;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return access;
    }

    /** 1.7 {@code isItemValidForSlot}: automation may only queue acidizer inputs; the belt path fills the declog band directly. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < inputs && getAmount(stack) >= 1;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** The backing handler also accepts rejected belt items into the declog band. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= inputs || getAmount(stack) >= 1;
    }

    /** 1.7 {@code canExtractItem}: declog only. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= inputs;
    }
}
