package com.hbm.tileentity.machine;

import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.RTGUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.BlockRtgFurnaceBase;
import com.hbm.inventory.container.ContainerRtgFurnace;
import com.hbm.tileentity.ModProduction2BlockEntities;

/**
 * 1.7 {@code TileEntityRtgFurnace}: a furnace whose cook time advances by the summed heat of up to three
 * RTG pellets per tick (slots 1-3) until it reaches 1,000. Slot 0 is the input, slot 4 the output.
 */
public final class TileEntityRtgFurnace extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    public int dualCookTime;
    public static final int processingSpeed = 1000;

    private static final int[] slots_top = new int[] {0};
    private static final int[] slots_bottom = new int[] {4};
    private static final int[] slots_side = new int[] {1, 2, 3};

    public TileEntityRtgFurnace(BlockPos pos, BlockState state) {
        super(ModProduction2BlockEntities.RTG_FURNACE.get(), pos, state, 5);
    }

    @Override
    public String getDefaultName() {
        return "container.rtgFurnace";
    }

    public boolean isLoaded() {
        for (int slot : slots_side) {
            if (inventory.getStackInSlot(slot).getItem() instanceof ItemRTGPellet) return true;
        }
        return false;
    }

    public int getDiFurnaceProgressScaled(int i) {
        return (dualCookTime * i) / processingSpeed;
    }

    public boolean canProcess() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return false;
        ItemStack itemStack = Recipes.smeltingResult(level, input).orElse(ItemStack.EMPTY);
        if (itemStack.isEmpty()) return false;

        ItemStack output = inventory.getStackInSlot(4);
        if (output.isEmpty()) return true;

        if (!ItemStack.isSameItem(output, itemStack)) return false;

        if (output.getCount() < 64 && output.getCount() < output.getMaxStackSize()) {
            return true;
        } else {
            return output.getCount() < itemStack.getMaxStackSize();
        }
    }

    private void processItem() {
        if (canProcess()) {
            ItemStack input = inventory.getStackInSlot(0);
            ItemStack itemStack = Recipes.smeltingResult(level, input).orElse(ItemStack.EMPTY);

            ItemStack output = inventory.getStackInSlot(4);
            if (output.isEmpty()) {
                inventory.setStackInSlot(4, itemStack.copy());
            } else if (ItemStack.isSameItem(output, itemStack)) {
                output.grow(itemStack.getCount());
                inventory.setStackInSlot(4, output);
            }

            input.shrink(1);
            inventory.setStackInSlot(0, input.isEmpty() ? ItemStack.EMPTY : input);
        }
    }

    public boolean hasPower() {
        return isLoaded();
    }

    public boolean isProcessing() {
        return this.dualCookTime > 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityRtgFurnace te) {
        te.hasPower();
        boolean flag1 = false;

        if (te.hasPower() && te.canProcess()) {
            te.dualCookTime += RTGUtil.updateRTGs(te.inventory, slots_side);

            if (te.dualCookTime >= processingSpeed) {
                te.dualCookTime = 0;
                te.processItem();
                flag1 = true;
            }
        } else {
            te.dualCookTime = 0;
            RTGUtil.updateRTGs(te.inventory, slots_side);
        }

        boolean trigger = true;

        if (te.hasPower() && te.canProcess() && te.dualCookTime == 0) {
            trigger = false;
        }

        if (trigger) {
            flag1 = true;
            BlockRtgFurnaceBase.updateBlockState(te.dualCookTime > 0, level, pos);
        }

        if (flag1) {
            te.setChanged();
        }

        te.networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(dualCookTime);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        dualCookTime = buf.readInt();
    }

    @Override
    protected void loadData(DataIn nbt) {
        super.loadData(nbt);
        // 1.7 reads "CookTime" but writes "cookTime": progress never survived a reload there either.
        dualCookTime = nbt.getShortOr("CookTime", (short) 0);
    }

    @Override
    protected void saveData(DataOut nbt) {
        super.saveData(nbt);
        nbt.putShort("cookTime", (short) dualCookTime);
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? slots_bottom : (side == Direction.UP ? slots_top : slots_side);
    }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return getSlotsForFace(Direction.values()[side]); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }

    /** 1.7 {@code canExtractItem}: {@code j != 0 || i != 1 || item == bucket}, copied from the vanilla furnace. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN || slot != 1 || stack.is(Items.BUCKET);
    }

    @Override public Component getDisplayName() { return Component.translatable("container.rtgFurnace"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerRtgFurnace(containerId, playerInventory, this);
    }
}
