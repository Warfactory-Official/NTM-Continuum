package com.hbm.inventory.container;

import com.hbm.tileentity.bomb.TileEntityCustomMissileLauncher;
import dev.golem.core.world.Compat;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ContainerCompactLauncher} / {@code ContainerLaunchTable}, which were the same class twice: missile
 * (26,36), designator (26,72), fuel in (116,72), oxidizer in (134,72), solid fuel (152,90), battery (116,108),
 * fuel out (116,90), oxidizer out (134,90), and the player inventory 56 pixels lower than usual (176x222).
 * Every slot is a plain slot, as upstream's were; shift-click sends anything to the first slot that takes it.
 */
public abstract class ContainerCustomMissileLauncher extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 8;
    private final Container machine;
    private final @Nullable TileEntityCustomMissileLauncher launcher;

    protected ContainerCustomMissileLauncher(MenuType<?> type, int containerId, Inventory playerInventory,
                                             @Nullable TileEntityCustomMissileLauncher launcher) {
        super(type, containerId);
        this.launcher = launcher;
        this.machine = launcher != null ? launcher : new SimpleContainer(MACHINE_SLOTS);

        addSlot(new Slot(machine, 0, 26, 36));
        addSlot(new Slot(machine, 1, 26, 72));
        addSlot(new Slot(machine, 2, 116, 90 - 18));
        addSlot(new Slot(machine, 3, 134, 90 - 18));
        addSlot(new Slot(machine, 4, 152, 90));
        addSlot(new Slot(machine, 5, 116, 108));
        addSlot(new Slot(machine, 6, 116, 90));
        addSlot(new Slot(machine, 7, 134, 90));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84 + 56);
    }

    public @Nullable TileEntityCustomMissileLauncher getLauncher() {
        return launcher;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index <= 7) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
