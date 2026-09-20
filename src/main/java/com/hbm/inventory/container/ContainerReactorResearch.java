package com.hbm.inventory.container;

import com.hbm.items.machine.ItemPlateFuel;
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
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.inventory.ModNuclearMiscMenus;
import com.hbm.tileentity.machine.TileEntityReactorResearch;

/** The 1.7 {@code ContainerReactorResearch}: twelve rod slots in the hex layout, player inventory 56 lower. */
public final class ContainerReactorResearch extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 12;
    private static final int[][] ROD_SLOTS = {
            {95, 22}, {131, 22},
            {77, 40}, {113, 40}, {149, 40},
            {95, 58}, {131, 58},
            {77, 76}, {113, 76}, {149, 76},
            {95, 94}, {131, 94}
    };

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityReactorResearch reactor;

    public ContainerReactorResearch(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerReactorResearch(int id, Inventory playerInventory, TileEntityReactorResearch tile) {
        this(id, playerInventory, tile, ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()));
    }

    private ContainerReactorResearch(int id, Inventory playerInventory, @Nullable TileEntityReactorResearch tile, ContainerLevelAccess access) {
        super(ModNuclearMiscMenus.REACTOR_RESEARCH.get(), id);
        this.reactor = tile;
        this.machine = tile == null ? new SimpleContainer(MACHINE_SLOTS) : tile;
        this.access = access;

        for (int i = 0; i < ROD_SLOTS.length; i++) {
            addSlot(new Slot(machine, i, ROD_SLOTS[i][0], ROD_SLOTS[i][1]));
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84 + 56);
    }

    private static @Nullable TileEntityReactorResearch resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityReactorResearch found ? found : null;
    }

    public @Nullable TileEntityReactorResearch getReactor() { return reactor; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModNuclearMiscBlocks.REACTOR_RESEARCH.get());
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
            } else if (stack.getItem() instanceof ItemPlateFuel) {
                if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, true)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
