package com.hbm.tileentity.bomb;

import com.hbm.inventory.container.ContainerNukeMan;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * The Fat Man ({@code nuke_man}) BlockEntity. Six slots, ported from 1.12.2: slot 0 = igniter, slots
 * 1–4 = explosive lenses, slot 5 = the fissile core. The bomb is "ready" only when all six are filled
 * with the correct parts ({@link #isReady()}). Sits on the {@link TileEntityMachineBase} tower so it
 * gets the inventory + chunk-load sync for free.
 */
public class TileEntityNukeMan extends TileEntityMachineBase implements MenuProvider {

    /** The player who placed the bomb, attributed as the detonator when triggered by redstone. */
    public @Nullable UUID placerID;

    public TileEntityNukeMan(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_MAN.get(), pos, state, 6);
    }

    @Override
    public String getDefaultName() {
        return "container.nukeMan";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() == ModBombItems.man_igniter.get();
            case 1, 2, 3, 4 -> stack.getItem() == ModBombItems.early_explosive_lenses.get();
            case 5 -> stack.getItem() == ModBombItems.man_core.get();
            default -> false;
        };
    }

    public boolean isReady() {
        return exp1() && exp2() && exp3() && exp4()
                && !inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(0).getItem() == ModBombItems.man_igniter.get()
                && !inventory.getStackInSlot(5).isEmpty() && inventory.getStackInSlot(5).getItem() == ModBombItems.man_core.get();
    }

    public boolean exp1() { return isLens(1); }
    public boolean exp2() { return isLens(2); }
    public boolean exp3() { return isLens(3); }
    public boolean exp4() { return isLens(4); }

    private boolean isLens(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        return !stack.isEmpty() && stack.getItem() == ModBombItems.early_explosive_lenses.get();
    }

    public void clearSlots() {
        clearContent();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (placerID != null) output.store("placer", UUIDUtil.CODEC, placerID);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        placerID = input.read("placer", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeMan(containerId, inventory, this);
    }
}
