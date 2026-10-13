package com.hbm.inventory.container;

import com.hbm.tileentity.network.TileEntityRadioTorchCounter;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModMenus;

/** Exact old counter layout: three non-takeable one-item pattern slots plus the player inventory. */
public final class RadioCounterMenu extends CompatContainerMenu {
    public static final int MACHINE_SLOTS = TileEntityRadioTorchCounter.MAPPING_SIZE;

    private final Container machine;
    private final @Nullable TileEntityRadioTorchCounter counter;

    public RadioCounterMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), true);
    }

    public RadioCounterMenu(int containerId, Inventory playerInventory, TileEntityRadioTorchCounter counter) {
        this(containerId, playerInventory, counter, true);
    }

    private RadioCounterMenu(int containerId, Inventory playerInventory,
                             @Nullable TileEntityRadioTorchCounter counter, boolean ignored) {
        super(ModMenus.RADIO_COUNTER.get(), containerId);
        this.counter = counter;
        this.machine = counter == null ? new SimpleContainer(MACHINE_SLOTS) : counter;

        for (int i = 0; i < MACHINE_SLOTS; i++) addSlot(new PatternSlot(machine, i, 138, 18 + 44 * i));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 12, 156);
    }

    private static @Nullable TileEntityRadioTorchCounter resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityRadioTorchCounter counter ? counter : null;
    }

    public @Nullable TileEntityRadioTorchCounter counter() {
        return counter;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (slotId >= 0 && slotId < MACHINE_SLOTS) {
            Slot slot = slots.get(slotId);
            if (button == 1 && action == ClickAction.PICKUP && slot.hasItem()) {
                if (counter != null && counter.getLevel() != null) {
                    counter.nextMode(slotId);
                }
                return true;
            }

            ItemStack carried = getCarried();
            machine.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            slot.setChanged();
            if (counter != null && counter.getLevel() != null) {
                counter.matcher.initPatternStandard(counter.getLevel(), machine.getItem(slotId), slotId);
                counter.dataChanged();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return counter != null && !counter.isRemoved()
                && player.distanceToSqr(counter.getBlockPos().getCenter()) <= 128D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static final class PatternSlot extends Slot {
        private PatternSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
