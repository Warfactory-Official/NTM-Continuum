package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
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

/** The legacy Mixer menu: battery, solid input, output-fluid identifier, and two upgrades. */
public class ContainerMixer extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 5;
    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineMixer mixer;

    public ContainerMixer(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMixer(int containerId, Inventory playerInventory, TileEntityMachineMixer mixer) {
        this(containerId, playerInventory, mixer, ContainerLevelAccess.create(mixer.getLevel(), mixer.getBlockPos()));
    }

    private ContainerMixer(int containerId, Inventory playerInventory, @Nullable TileEntityMachineMixer mixer,
                           ContainerLevelAccess access) {
        super(ModMenus.MACHINE_MIXER.get(), containerId);
        this.mixer = mixer;
        this.machine = mixer != null ? mixer : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(0, 23, 77));
        this.addSlot(validated(1, 43, 77));
        this.addSlot(validated(2, 117, 77));
        this.addSlot(validated(3, 137, 24));
        this.addSlot(validated(4, 137, 42));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 122);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerMixer.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private static @Nullable TileEntityMachineMixer resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineMixer mixer ? mixer : null;
    }

    public @Nullable TileEntityMachineMixer getMixer() { return mixer; }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.machine_mixer.get());
    }

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
                if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId) {
                if (!moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!moveItemStackTo(stack, 3, 5, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
