package com.hbm.tileentity.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.inventory.container.ContainerNukeSolinium;
import com.hbm.items.ModBombs2Items;
import com.hbm.tileentity.ModBombs2BlockEntities;

/** The Blue Rinse assembly: igniters at 0/3/5/8, propellant at 1/2/6/7, the core at 4. */
public class TileEntityNukeSolinium extends TileEntityNukeBomb {

    public TileEntityNukeSolinium(BlockPos pos, BlockState state) {
        super(ModBombs2BlockEntities.NUKE_SOLINIUM.get(), pos, state, 9);
    }

    public Item expected(int slot) {
        return switch (slot) {
            case 0, 3, 5, 8 -> ModBombs2Items.solinium_igniter.get();
            case 4 -> ModBombs2Items.solinium_core.get();
            default -> ModBombs2Items.solinium_propellant.get();
        };
    }

    public boolean hasPart(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        return !stack.isEmpty() && stack.getItem() == expected(slot);
    }

    @Override
    public boolean isReady() {
        for (int i = 0; i < 9; i++) {
            if (!hasPart(i)) return false;
        }
        return true;
    }

    @Override
    public String getDefaultName() {
        return "container.nukeSolinium";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeSolinium(containerId, inventory, this);
    }
}
