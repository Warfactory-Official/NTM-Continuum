package com.hbm.tileentity.bomb;

import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.inventory.container.ContainerBombMulti;
import com.hbm.tileentity.ModExplosivesBlockEntities;

/**
 * 1.7 {@code TileEntityBombMulti}: six single-item slots. Slots 0, 1, 3 and 4 must hold TNT for the
 * bomb to be loaded; slots 2 and 5 are the payload selectors read by {@link #return2type()} and
 * {@link #return5type()}. No slot is reachable from the outside (1.7 exposed no sided slots).
 */
public class TileEntityBombMulti extends TileEntityMachineBase implements MenuProvider {

    public TileEntityBombMulti(BlockPos pos, BlockState state) {
        super(ModExplosivesBlockEntities.BOMB_MULTI.get(), pos, state, 6);
    }

    @Override
    public String getDefaultName() {
        return "container.bombMulti";
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[0];
    }

    private boolean isTnt(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        return !stack.isEmpty() && stack.getItem() == Blocks.TNT.asItem();
    }

    public boolean isLoaded() {
        return isTnt(0) && isTnt(1) && isTnt(3) && isTnt(4);
    }

    public int return2type() {
        return payloadType(inventory.getStackInSlot(2));
    }

    public int return5type() {
        return payloadType(inventory.getStackInSlot(5));
    }

    private static int payloadType(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item == Items.GUNPOWDER) return 1;
        if (item == Blocks.TNT.asItem()) return 2;
        if (item == ModItems.pellet_cluster.get()) return 3;
        if (item == ModFuelItems.powder_fire.get()) return 4;
        if (item == ModItems.powder_poison.get()) return 5;
        if (item == ModItems.pellet_gas.get()) return 6;
        return 0;
    }

    public void clearSlots() {
        clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerBombMulti(containerId, inventory, this);
    }
}
