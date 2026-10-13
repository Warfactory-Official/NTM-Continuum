package com.hbm.inventory.container;

import com.hbm.inventory.ModCustomMissileMenus;
import com.hbm.tileentity.machine.TileEntityMachineMissileAssembly;
import dev.golem.core.world.Compat;
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
 * 1.7 {@code ContainerMachineMissileAssembly}: chip (8,36), warhead (26,36), fuselage (44,36), fins (62,36),
 * thruster (80,36), take-only output (152,36); player inventory at y=140 (176x222). Button 0 builds. Shift-click
 * from the inventory tries slots 0-4 in order, upstream's {@code mergeItemStack(0, 5)}.
 */
public class ContainerMachineMissileAssembly extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineMissileAssembly assembly;

    public ContainerMachineMissileAssembly(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineMissileAssembly te ? te : null);
    }

    public ContainerMachineMissileAssembly(int containerId, Inventory playerInventory,
                                           @Nullable TileEntityMachineMissileAssembly assembly) {
        super(ModCustomMissileMenus.MISSILE_ASSEMBLY.get(), containerId);
        this.assembly = assembly;
        this.machine = assembly != null ? assembly : new SimpleContainer(6);

        addSlot(new Slot(machine, 0, 8, 36));
        addSlot(new Slot(machine, 1, 26, 36));
        addSlot(new Slot(machine, 2, 44, 36));
        addSlot(new Slot(machine, 3, 62, 36));
        addSlot(new Slot(machine, 4, 80, 36));
        addSlot(new Slot(machine, 5, 152, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84 + 56);
    }

    public @Nullable TileEntityMachineMissileAssembly getAssembly() {
        return assembly;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (assembly == null) return false;
        assembly.construct();
        return true;
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
            if (index <= 5) {
                if (!moveItemStackTo(stack, 6, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 5, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
