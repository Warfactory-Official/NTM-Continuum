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
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.inventory.ModNuclearMiscMenus;
import com.hbm.tileentity.machine.TileEntityMachineRadiolysis;

/** The 1.7 {@code ContainerRadiolysis}: ten pellets in two columns, fluid ID in/out, sterilisation in/out, battery. */
public final class ContainerRadiolysis extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 15;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineRadiolysis radiolysis;

    public ContainerRadiolysis(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerRadiolysis(int id, Inventory playerInventory, TileEntityMachineRadiolysis tile) {
        this(id, playerInventory, tile, ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()));
    }

    private ContainerRadiolysis(int id, Inventory playerInventory, @Nullable TileEntityMachineRadiolysis tile, ContainerLevelAccess access) {
        super(ModNuclearMiscMenus.RADIOLYSIS.get(), id);
        this.radiolysis = tile;
        this.machine = tile == null ? new SimpleContainer(MACHINE_SLOTS) : tile;
        this.access = access;

        // RTG
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                addSlot(new Slot(machine, j + i * 5, 188 + i * 18, 8 + j * 18));
            }
        }

        // Fluid IO
        addSlot(new Slot(machine, 10, 34, 17));
        addSlot(new TakeOnly(machine, 11, 34, 53));

        // Sterilization
        addSlot(new Slot(machine, 12, 148, 17));
        addSlot(new TakeOnly(machine, 13, 148, 53));

        // Battery
        addSlot(new Slot(machine, 14, 8, 53));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityMachineRadiolysis resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityMachineRadiolysis found ? found : null;
    }

    public @Nullable TileEntityMachineRadiolysis getRadiolysis() { return radiolysis; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get());
    }

    /** my eye, my eye, coctor coctor coctor */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    private static final class TakeOnly extends Slot {
        private TakeOnly(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
