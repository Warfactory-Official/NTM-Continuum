package com.hbm.inventory.container;

import com.hbm.tileentity.machine.storage.TemplateCrateLayout;
import com.hbm.tileentity.machine.storage.TileEntityCrateTemplate;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** The old 9x3 ordinary template-crate menu. */
public class ContainerCrateTemplate extends AbstractContainerMenu {

    private final Container crate;
    private final @Nullable TileEntityCrateTemplate tile;

    public ContainerCrateTemplate(int containerId, Inventory playerInventory, BlockPos pos,
                                  Supplier<MenuType<ContainerCrateTemplate>> menuType) {
        this(containerId, playerInventory, resolve(playerInventory, pos), menuType, true);
    }

    public ContainerCrateTemplate(int containerId, Inventory playerInventory, TileEntityCrateTemplate tile,
                                  Supplier<MenuType<ContainerCrateTemplate>> menuType) {
        this(containerId, playerInventory, tile, menuType, true);
    }

    private ContainerCrateTemplate(int containerId, Inventory playerInventory, @Nullable TileEntityCrateTemplate tile,
                                   Supplier<MenuType<ContainerCrateTemplate>> menuType, boolean direct) {
        super(menuType.get(), containerId);
        this.tile = tile;
        this.crate = tile != null ? tile : new SimpleContainer(TemplateCrateLayout.SLOTS);

        for (int row = 0; row < TemplateCrateLayout.ROWS; row++) {
            for (int col = 0; col < TemplateCrateLayout.COLUMNS; col++) {
                addSlot(new Slot(crate, col + row * TemplateCrateLayout.COLUMNS,
                        TemplateCrateLayout.CRATE_X + col * 18, TemplateCrateLayout.CRATE_Y + row * 18));
            }
        }
        Compat.addPlayerInventory(this::addSlot, playerInventory,
                TemplateCrateLayout.PLAYER_INVENTORY_X, TemplateCrateLayout.PLAYER_INVENTORY_Y);
    }

    private static @Nullable TileEntityCrateTemplate resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityCrateTemplate crate ? crate : null;
    }

    public @Nullable TileEntityCrateTemplate getCrate() {
        return tile;
    }

    @Override
    public boolean stillValid(Player player) {
        return tile != null && tile.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < TemplateCrateLayout.SLOTS) {
                if (!moveItemStackTo(stack, TemplateCrateLayout.SLOTS, TemplateCrateLayout.SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, TemplateCrateLayout.SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
