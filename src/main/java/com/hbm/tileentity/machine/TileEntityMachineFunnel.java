package com.hbm.tileentity.machine;

import com.hbm.inventory.ModProductionMenus;
import com.hbm.inventory.container.ContainerFunnel;
import com.hbm.tileentity.ModProductionBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.platform.Lifecycle;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Faithful port of old {@code TileEntityMachineFunnel}. Each input slot automatically crafts a full 3x3
 * batch, or a 2x2 batch when configured for it, and places the result in its matching output slot.
 */
public class TileEntityMachineFunnel extends TileEntityMachineBase
        implements MenuProvider, WorldlyContainer {

    public static final int MODE_ALL = 0;
    public static final int MODE_3X3 = 1;
    public static final int MODE_2X2 = 2;
    public int mode;
    private static final int[] INPUT = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OUTPUT = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    // 1.7 from4Cache/from9Cache: one recipe search per ingredient, cleared when the recipes may change.
    private static final Map<Key, ItemStack> FROM_4 = new HashMap<>();
    private static final Map<Key, ItemStack> FROM_9 = new HashMap<>();

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
        te.networkPackNT();
    }

    public static void init() {
        Lifecycle.onDatapackReloaded(server -> clearCache());
        Lifecycle.onServerStopping(server -> clearCache());
    }

    public static void clearCache() {
        FROM_4.clear();
        FROM_9.clear();
    }

    /** The cached 2x2 or 3x3 result for {@code ingredient}, or null before the first search for it. */
    public static ItemStack cached(ItemStack ingredient, int size) {
        return (size == 2 ? FROM_4 : FROM_9).get(new Key(ingredient.copyWithCount(1)));
    }

    private ItemStack from4(Level level, ItemStack ingredient) {
        return compress(level, ingredient, 2, FROM_4);
    }

    private ItemStack from9(Level level, ItemStack ingredient) {
        return compress(level, ingredient, 3, FROM_9);
    }

    private static ItemStack compress(Level level, ItemStack ingredient, int size, Map<Key, ItemStack> cache) {
        ItemStack singular = ingredient.copyWithCount(1);
        ItemStack result = cache.computeIfAbsent(new Key(singular), key -> {
            List<ItemStack> grid = new ArrayList<>(size * size);
            for (int i = 0; i < size * size; i++) grid.add(singular.copy());
            return Recipes.craftingResult(level, size, size, grid).orElse(ItemStack.EMPTY);
        });
        return result.copy();
    }

    /** 1.7's singular {@code ComparableStack}, with the item's components standing in for its damage value. */
    private record Key(ItemStack stack) {
        @Override public boolean equals(Object other) {
            return other instanceof Key key && ItemStacks.sameItemAndData(stack, key.stack);
        }

        @Override public int hashCode() { return ItemStacks.hashItemAndData(stack); }
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
