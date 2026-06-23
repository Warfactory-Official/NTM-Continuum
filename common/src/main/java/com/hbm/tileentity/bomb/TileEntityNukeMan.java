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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class TileEntityNukeMan extends TileEntityMachineBase implements MenuProvider {

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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (placerID != null) output.store("placer", UUIDUtil.CODEC, placerID);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
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
