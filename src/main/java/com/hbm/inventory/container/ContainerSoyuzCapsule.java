package com.hbm.inventory.container;

import com.hbm.inventory.ModRadarUtilityMenus;
import com.hbm.tileentity.machine.storage.TileEntitySoyuzCapsule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ContainerSoyuzCapsule}: the payload in three rows of six from (62,18), the rocket slot at (17,36),
 * and the player inventory at (8,104).
 */
public final class ContainerSoyuzCapsule extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntitySoyuzCapsule capsule;

    public ContainerSoyuzCapsule(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntitySoyuzCapsule c ? c : null);
    }

    public ContainerSoyuzCapsule(int containerId, Inventory playerInventory, @Nullable TileEntitySoyuzCapsule capsule) {
        super(ModRadarUtilityMenus.SOYUZ_CAPSULE.get(), containerId);
        this.capsule = capsule;
        this.machine = capsule != null ? capsule : new SimpleContainer(19);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                this.addSlot(new Slot(machine, j + i * 6, 26 + j * 18 + 18 * 2, 18 + i * 18));
            }
        }

        this.addSlot(new Slot(machine, 18, 17, 36));

        dev.golem.core.world.Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 104);
    }

    public @Nullable TileEntitySoyuzCapsule getCapsule() { return capsule; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index <= 18) {
                if (!this.moveItemStackTo(stack, 19, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 19, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }
}
