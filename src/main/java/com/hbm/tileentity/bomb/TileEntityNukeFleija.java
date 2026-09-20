package com.hbm.tileentity.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.inventory.container.ContainerNukeFleija;
import com.hbm.items.ModBombs2Items;
import com.hbm.tileentity.ModBombs2BlockEntities;

/**
 * F.L.E.I.J.A. assembly: slots 0-1 igniters, 2-4 propellant, 5-10 cores. As in 1.7 any item may be placed in
 * any slot ({@code isItemValidForSlot} was false and the container used plain slots); readiness alone checks
 * the layout.
 */
public class TileEntityNukeFleija extends TileEntityNukeBomb {

    public TileEntityNukeFleija(BlockPos pos, BlockState state) {
        super(ModBombs2BlockEntities.NUKE_FLEIJA.get(), pos, state, 11);
    }

    /** The item that slot {@code slot} must hold for the bomb to be ready. */
    public Item expected(int slot) {
        if (slot <= 1) return ModBombs2Items.fleija_igniter.get();
        if (slot <= 4) return ModBombs2Items.fleija_propellant.get();
        return ModBombs2Items.fleija_core.get();
    }

    public boolean hasPart(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        return !stack.isEmpty() && stack.getItem() == expected(slot);
    }

    @Override
    public boolean isReady() {
        for (int i = 0; i < 11; i++) {
            if (!hasPart(i)) return false;
        }
        return true;
    }

    @Override
    public String getDefaultName() {
        return "container.nukeFleija";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeFleija(containerId, inventory, this);
    }
}
