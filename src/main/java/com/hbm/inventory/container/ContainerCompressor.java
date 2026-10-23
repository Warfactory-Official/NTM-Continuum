package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineCompressorBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Menu for the compressor, using the 1.12.2 {@code ContainerCompressor} layout (176×204): fluid identifier
 * (17,72), battery (152,72), upgrades (52,72)/(70,72); player inventory at 122/180. Button id =
 * the compression level (0..4). */
public class ContainerCompressor extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 4;

    private final Container machine;
    private final @Nullable TileEntityMachineCompressorBase te;

    public ContainerCompressor(int id, Inventory playerInv, BlockPos pos) {
        this(id, playerInv, resolve(playerInv, pos));
    }

    public ContainerCompressor(int id, Inventory playerInv, @Nullable TileEntityMachineCompressorBase te) {
        super(ModMenus.MACHINE_COMPRESSOR.get(), id);
        this.te = te;
        this.machine = te != null ? te : new SimpleContainer(MACHINE_SLOTS);

        this.addSlot(validated(0, 17, 72));   // fluid identifier
        this.addSlot(validated(1, 152, 72));  // battery
        this.addSlot(validated(2, 52, 72));   // upgrade
        this.addSlot(validated(3, 70, 72));   // upgrade

        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++)
            this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
        for (int i = 0; i < 9; i++) this.addSlot(new Slot(playerInv, i, 8 + i * 18, 180));
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerCompressor.this.machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityMachineCompressorBase resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineCompressorBase t ? t : null;
    }

    public @Nullable TileEntityMachineCompressorBase getTile() { return te; }

    /** Old {@code receiveControl("compression")}: the GUI's 5-way selector sends the level as the id. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (te != null) { te.setCompression(id); return true; }
        return false;
    }

    @Override
    public boolean stillValid(Player player) { return this.machine.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 2, 4, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
