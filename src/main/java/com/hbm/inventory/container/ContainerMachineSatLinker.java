package com.hbm.inventory.container;

import com.hbm.inventory.ModSatelliteMenus;
import com.hbm.tileentity.machine.TileEntityMachineSatLinker;
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
 * 1.7 {@code ContainerMachineSatLinker}: the three chip slots at (44,36), (80,36), (116,36) and the player
 * inventory at (8,104). Shift-clicking a machine slot sends it to the player; shift-clicking from the player
 * fills slot 0 only.
 */
public final class ContainerMachineSatLinker extends AbstractContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityMachineSatLinker linker;

    public ContainerMachineSatLinker(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityMachineSatLinker l ? l : null);
    }

    public ContainerMachineSatLinker(int containerId, Inventory playerInventory,
                                     @Nullable TileEntityMachineSatLinker linker) {
        super(ModSatelliteMenus.SATLINKER.get(), containerId);
        this.linker = linker;
        this.machine = linker != null ? linker : new SimpleContainer(3);

        this.addSlot(new Slot(machine, 0, 44, 36));
        this.addSlot(new Slot(machine, 1, 80, 36));
        this.addSlot(new Slot(machine, 2, 116, 36));

        dev.golem.core.world.Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 104);
    }

    public @Nullable TileEntityMachineSatLinker getLinker() {
        return linker;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // 1.7: "par2 <= 0" - only slot 0 counts as the machine side, so slots 1 and 2 move into slot 0
            if (index <= 0) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
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
