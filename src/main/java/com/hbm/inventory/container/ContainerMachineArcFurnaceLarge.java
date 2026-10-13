package com.hbm.inventory.container;

import com.hbm.items.machine.ItemArcElectrode;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
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
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;

/** Large Arc Furnace menu: three electrodes, battery, upgrade, 20-cell grid, and five-cell queue. */
public class ContainerMachineArcFurnaceLarge extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 30;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineArcFurnaceLarge furnace;

    public ContainerMachineArcFurnaceLarge(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineArcFurnaceLarge(int id, Inventory playerInventory,
                                           TileEntityMachineArcFurnaceLarge furnace) {
        this(id, playerInventory, furnace,
                ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerMachineArcFurnaceLarge(int id, Inventory playerInventory,
                                            @Nullable TileEntityMachineArcFurnaceLarge furnace,
                                            ContainerLevelAccess access) {
        super(ModMenus.MACHINE_ARC_FURNACE.get(), id);
        this.furnace = furnace;
        this.machine = furnace == null ? new SimpleContainer(MACHINE_SLOTS) : furnace;
        this.access = access;

        for (int i = 0; i < 3; i++) this.addSlot(validated( i, 62 + i * 18, 22));
        this.addSlot(validated(3, 8, 108));
        this.addSlot(validated(4, 152, 108));
        for (int row = 0; row < 4; row++) for (int col = 0; col < 5; col++) {
            int index = 5 + row * 5 + col;
            this.addSlot(new Slot(machine, index, 44 + col * 18, 54 + row * 18) {
                @Override public boolean mayPlace(ItemStack stack) {
                    // 1.7's SlotArcFurnace allowed any hand-inserted stack in liquid mode;
                    // the recipe gate still applies to the queue and to processing.
                    return furnace != null && furnace.liquidMode || machine.canPlaceItem(index, stack);
                }
                @Override public int getMaxStackSize() {
                    if (furnace == null) return 1;
                    return hasItem() ? furnace.getMaxInputSize() : 1;
                }
            });
        }
        for (int i = 0; i < 5; i++) {
            int index = 25 + i;
            this.addSlot(new Slot(machine, index, 44 + i * 18, 129) {
                // The old queue slots were unfiltered GUI slots; loadIngredients performs the recipe gate.
                @Override public boolean mayPlace(ItemStack stack) { return true; }
            });
        }
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 174);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityMachineArcFurnaceLarge resolve(Inventory inventory, BlockPos pos) {
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        return entity instanceof TileEntityMachineArcFurnaceLarge furnace ? furnace : null;
    }

    public @Nullable TileEntityMachineArcFurnaceLarge getFurnace() { return furnace; }

    @Override public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_arc_furnace.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemArcElectrode) {
                if (!this.moveItemStackTo(stack, 0, 3, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemMachineUpgrade) {
                if (!this.moveItemStackTo(stack, 4, 5, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 5, 30, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
