package com.hbm.inventory.container;

import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** The faithful three-slot mass-storage layout and its filter/output click rules. */
public class ContainerMassStorage extends CompatContainerMenu {

    public static final int MACHINE_SLOTS = 3;
    private final Container machine;
    private final @Nullable TileEntityMassStorage storage;

    /** Client-side constructor; the menu type is injected by the eventual central registration row. */
    public ContainerMassStorage(int containerId, Inventory playerInventory, BlockPos pos,
                                Supplier<MenuType<ContainerMassStorage>> menuType) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                menuType, true);
    }

    /** Server-side constructor. */
    public ContainerMassStorage(int containerId, Inventory playerInventory, TileEntityMassStorage storage,
                                Supplier<MenuType<ContainerMassStorage>> menuType) {
        this(containerId, playerInventory, storage,
                menuType, true);
    }

    private ContainerMassStorage(int containerId, Inventory playerInventory, @Nullable TileEntityMassStorage storage,
                                 Supplier<MenuType<ContainerMassStorage>> menuType, boolean direct) {
        super(menuType.get(), containerId);
        this.storage = storage;
        this.machine = storage != null ? storage : new SimpleContainer(MACHINE_SLOTS);
        if (storage != null) storage.openInventory(playerInventory.player);

        addSlot(new Slot(machine, TileEntityMassStorage.INPUT_SLOT, 61, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ContainerMassStorage.this.storage == null
                        || ContainerMassStorage.this.storage.isItemValidForSlot(TileEntityMassStorage.INPUT_SLOT, stack);
            }
        });
        addSlot(new Slot(machine, TileEntityMassStorage.FILTER_SLOT, 61, 53) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }

            @Override
            public void set(ItemStack stack) {
                super.set(stack.isEmpty() ? ItemStack.EMPTY : ItemStacks.copyWithCount(stack, 1));
            }
        });
        addSlot(new Slot(machine, TileEntityMassStorage.OUTPUT_SLOT, 61, 89) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return ContainerMassStorage.this.storage == null
                        || ContainerMassStorage.this.storage.canTakeItemThroughFace(
                        TileEntityMassStorage.OUTPUT_SLOT, getItem(), Direction.UP);
            }
        });

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 139);
    }

    private static @Nullable TileEntityMassStorage resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity entity = playerInventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityMassStorage storage ? storage : null;
    }

    public @Nullable TileEntityMassStorage getStorage() {
        return storage;
    }

    public int getCapacity() {
        return storage == null ? 0 : storage.getCapacity();
    }

    public int getStockpile() {
        return storage == null ? 0 : storage.getStockpile();
    }

    public boolean isOutputEnabled() {
        return storage != null && storage.output;
    }

    public ItemStack getFilterType() {
        return storage == null ? ItemStack.EMPTY : storage.getFilterType();
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction clickType, Player player) {
        if (slotId == TileEntityMassStorage.FILTER_SLOT && storage != null) {
            // The filter is a selector, not a normal inventory slot. It is immutable while stocked,
            // and selecting a new type does not consume the cursor stack (old slotClick behavior).
            if (storage.getStockpile() <= 0) storage.setFilter(getCarried());
            return true;
        }
        return false;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (storage == null) return false;
        switch (id) {
            case 0 -> storage.provide(false);
            case 1 -> storage.provide(true);
            case 2 -> storage.toggleOutput();
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return storage != null && storage.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (storage != null) storage.closeInventory(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, TileEntityMassStorage.INPUT_SLOT, TileEntityMassStorage.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
