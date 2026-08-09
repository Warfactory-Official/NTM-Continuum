package com.hbm.inventory.container;

import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Menu for the storage crates (iron / steel / desh). A single menu type serves all three: the layout
 * (slot grid + player-inventory offsets) comes from the bound {@link TileEntityCrate}'s {@link CrateType}.
 * The crate slot grid is followed by the player inventory at the type's configured positions.
 */
public class ContainerCrate extends AbstractContainerMenu {

    private final Container machine;
    private final ContainerLevelAccess access;
    private final CrateType type;
    private final int crateSlots;
    private final @Nullable Block crateBlock;

    /** Client-side: resolve the BlockEntity by position to learn the crate type + bind its inventory. */
    public ContainerCrate(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side: bound to the live BlockEntity. */
    public ContainerCrate(int containerId, Inventory playerInventory, TileEntityCrate crate) {
        this(containerId, playerInventory, crate, ContainerLevelAccess.create(crate.getLevel(), crate.getBlockPos()));
    }

    private ContainerCrate(int containerId, Inventory playerInventory, @Nullable TileEntityCrate crate, ContainerLevelAccess access) {
        super(ModMenus.CRATE.get(), containerId);
        this.type = crate != null ? crate.crateType : CrateType.IRON;
        this.crateSlots = type.slots;
        this.machine = crate != null ? crate : new SimpleContainer(crateSlots);
        this.access = access;
        this.crateBlock = crateBlockFor(type);

        // Crate slot grid (cols × rows) at the type's origin.
        for (int row = 0; row < type.rows; row++) {
            for (int col = 0; col < type.columns; col++) {
                int slot = col + row * type.columns;
                this.addSlot(new Slot(machine, slot, type.crateX + col * 18, type.crateY + row * 18));
            }
        }

        // Player inventory + hotbar at the type's positions (the crates use non-default layouts).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        type.playerInventoryX + col * 18, type.playerInventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, type.playerInventoryX + col * 18, type.hotbarY));
        }
    }

    private static @Nullable TileEntityCrate resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityCrate crate ? crate : null;
    }

    private static @Nullable Block crateBlockFor(CrateType type) {
        return switch (type) {
            case IRON -> ModStorageBlocks.crate_iron.get();
            case STEEL -> ModStorageBlocks.crate_steel.get();
            case DESH -> ModStorageBlocks.crate_desh.get();
        };
    }

    /** The crate's layout, for the screen to size itself + draw the right texture. */
    public CrateType getCrateType() {
        return type;
    }

    @Override
    public boolean stillValid(Player player) {
        return crateBlock == null || stillValid(access, player, crateBlock);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invEnd = crateSlots + 36;
            if (index < crateSlots) {
                if (!this.moveItemStackTo(stack, crateSlots, invEnd, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, crateSlots, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
