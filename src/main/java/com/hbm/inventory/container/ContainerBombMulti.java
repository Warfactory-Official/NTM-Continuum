package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.inventory.ModExplosivesMenus;
import com.hbm.tileentity.bomb.TileEntityBombMulti;

/** 1.7 {@code ContainerBombMulti}: six bomb slots in two rows of three, then the player inventory. */
public class ContainerBombMulti extends AbstractContainerMenu {

    private static final int BOMB_SLOTS = 6;

    private final Container bomb;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityBombMulti tile;

    /** Client-side constructor: receives the {@link BlockPos} sent by the server. */
    public ContainerBombMulti(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side constructor: bound to the live block entity. */
    public ContainerBombMulti(int containerId, Inventory playerInventory, TileEntityBombMulti tile) {
        this(containerId, playerInventory, tile, ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()));
    }

    private ContainerBombMulti(int containerId, Inventory playerInventory, @Nullable TileEntityBombMulti tile,
                               ContainerLevelAccess access) {
        super(ModExplosivesMenus.BOMB_MULTI.get(), containerId);
        this.tile = tile;
        this.bomb = tile != null ? tile : new SimpleContainer(BOMB_SLOTS);
        this.access = access;

        this.addSlot(new Slot(bomb, 0, 44, 26));
        this.addSlot(new Slot(bomb, 1, 62, 26));
        this.addSlot(new Slot(bomb, 2, 80, 26));
        this.addSlot(new Slot(bomb, 3, 44, 44));
        this.addSlot(new Slot(bomb, 4, 62, 44));
        this.addSlot(new Slot(bomb, 5, 80, 44));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityBombMulti resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityBombMulti bomb ? bomb : null;
    }

    public int return2type() {
        return tile == null ? 0 : tile.return2type();
    }

    public int return5type() {
        return tile == null ? 0 : tile.return5type();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModExplosivesBlocks.bomb_multi.get());
    }

    /** 1.7 transferStackInSlot: bomb slots shift out to the player; nothing shifts in. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < BOMB_SLOTS) {
                if (!this.moveItemStackTo(stack, BOMB_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }
}
