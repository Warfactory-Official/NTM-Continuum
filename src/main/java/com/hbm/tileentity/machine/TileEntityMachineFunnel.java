package com.hbm.tileentity.machine;

import com.hbm.inventory.ModProductionMenus;
import com.hbm.inventory.container.ContainerFunnel;
import com.hbm.tileentity.ModProductionBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Faithful port of old {@code TileEntityMachineFunnel}. Each input slot automatically crafts a full 3x3
 * batch, or a 2x2 batch when configured for it, and places the result in its matching output slot.
 */
public final class TileEntityMachineFunnel extends TileEntityMachineBase
        implements MenuProvider, WorldlyContainer {

    public static final int MODE_ALL = 0;
    public static final int MODE_3X3 = 1;
    public static final int MODE_2X2 = 2;
    public int mode;
    private static final int[] INPUT = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OUTPUT = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    public TileEntityMachineFunnel(BlockPos pos, BlockState state) {
        super(ModProductionBlockEntities.FUNNEL.get(), pos, state, 18);
    }

    @Override public String getDefaultName() { return "container.machineFunnel"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineFunnel te) {
        for (int i = 0; i < 9; i++) {
            ItemStack input = te.inventory.getStackInSlot(i);
            if (input.isEmpty()) continue;

            int amount = 9;
            ItemStack result = te.mode == MODE_2X2 || input.getCount() < 9 ? ItemStack.EMPTY : te.from9(level, input);
            if (result.isEmpty()) {
                amount = 4;
                result = te.mode == MODE_3X3 || input.getCount() < 4 ? ItemStack.EMPTY : te.from4(level, input);
            }
            if (result.isEmpty() || input.getCount() < amount) continue;

            ItemStack output = te.inventory.getStackInSlot(i + 9);
            if (output.isEmpty()) {
                te.inventory.setStackInSlot(i + 9, result.copy());
                te.inventory.extractItem(i, amount, false);
            } else if (ItemStacks.sameItemAndData(output, result)
                    && output.getCount() + result.getCount() <= output.getMaxStackSize()) {
                output.grow(result.getCount());
                te.inventory.extractItem(i, amount, false);
            }
        }
        te.syncState();
    }

    private ItemStack from4(Level level, ItemStack ingredient) {
        List<ItemStack> grid = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) grid.add(ingredient.copy());
        return Recipes.craftingResult(level, 2, 2, grid).orElse(ItemStack.EMPTY);
    }

    private ItemStack from9(Level level, ItemStack ingredient) {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) grid.add(ingredient.copy());
        return Recipes.craftingResult(level, 3, 3, grid).orElse(ItemStack.EMPTY);
    }

    public void cycleMode() {
        mode++;
        if (mode > MODE_2X2) mode = MODE_ALL;
        setChanged();
    }

    @Override public void handleButtonPacket(int value, int meta) { if (value == 0) cycleMode(); }

    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); buf.writeInt(mode); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); mode = buf.readInt(); }

    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        mode = Math.max(MODE_ALL, Math.min(MODE_2X2, input.getIntOr("mode", MODE_ALL)));
    }

    @Override protected void saveData(DataOut output) { super.saveData(output); output.putInt("mode", mode); }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return side == 0 ? OUTPUT : INPUT; }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? OUTPUT : INPUT; }

    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN && slot < 9 && isItemValidForSlot(slot, stack);
    }

    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (side == Direction.DOWN) return slot >= 9;
        return side != Direction.UP && slot < 9;
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= 9 || stack.isEmpty()) return false;
        if (!inventory.getStackInSlot(slot).isEmpty()) return true;
        return level == null || level.isClientSide() || !from9(level, stack).isEmpty() || !from4(level, stack).isEmpty();
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerFunnel(id, inventory, this);
    }
}
