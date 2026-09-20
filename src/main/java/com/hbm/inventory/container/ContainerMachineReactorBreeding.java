package com.hbm.inventory.container;

import com.hbm.items.machine.ItemBreedingRod;
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
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;

/** The 1.7 {@code ContainerMachineReactorBreeding}: input at (35,35), take-only output at (125,35). */
public final class ContainerMachineReactorBreeding extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = 2;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineReactorBreeding breeder;

    public ContainerMachineReactorBreeding(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineReactorBreeding(int id, Inventory playerInventory, TileEntityMachineReactorBreeding tile) {
        this(id, playerInventory, tile, ContainerLevelAccess.create(tile.getLevel(), tile.getBlockPos()));
    }

    private ContainerMachineReactorBreeding(int id, Inventory playerInventory, @Nullable TileEntityMachineReactorBreeding tile, ContainerLevelAccess access) {
        super(ModNuclearMiscMenus.REACTOR_BREEDING.get(), id);
        this.breeder = tile;
        this.machine = tile == null ? new SimpleContainer(MACHINE_SLOTS) : tile;
        this.access = access;

        addSlot(new Slot(machine, 0, 35, 35));
        addSlot(new Slot(machine, 1, 125, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityMachineReactorBreeding resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityMachineReactorBreeding found ? found : null;
    }

    public @Nullable TileEntityMachineReactorBreeding getBreeder() { return breeder; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get());
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
            } else if (stack.getItem() instanceof ItemBreedingRod) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
