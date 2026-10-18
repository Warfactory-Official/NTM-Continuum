package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.container.ContainerLaunchpadSoyuz.MachineSlot;
import com.hbm.tileentity.machine.TileEntityLaunchpadLambda;
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
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ContainerLaunchpadLambda}: rocket (35,17), satellite (53,17), gasoline in/out (107,80)/(107,98), peroxide
 * in/out (125,80)/(125,98), battery (89,80), player inventory at (8,144).
 */
public class ContainerLaunchpadLambda extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 7;

    private final ContainerLevelAccess access;
    private final @Nullable TileEntityLaunchpadLambda launcher;

    public ContainerLaunchpadLambda(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(pos) instanceof TileEntityLaunchpadLambda pad ? pad : null,
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerLaunchpadLambda(int containerId, Inventory playerInventory, TileEntityLaunchpadLambda launcher) {
        this(containerId, playerInventory, launcher, ContainerLevelAccess.create(launcher.getLevel(), launcher.getBlockPos()));
    }

    private ContainerLaunchpadLambda(int containerId, Inventory playerInventory, @Nullable TileEntityLaunchpadLambda launcher,
                                     ContainerLevelAccess access) {
        super(ModMenus.LAUNCHPAD_LAMBDA.get(), containerId);
        this.launcher = launcher;
        this.access = access;
        Container machine = launcher != null ? launcher : new SimpleContainer(MACHINE_SLOTS);

        addSlot(new MachineSlot(machine, 0, 35, 17));
        addSlot(new MachineSlot(machine, 1, 53, 17));
        addSlot(new MachineSlot(machine, 2, 107, 80));
        addSlot(new MachineSlot(machine, 3, 107, 98));
        addSlot(new MachineSlot(machine, 4, 125, 80));
        addSlot(new MachineSlot(machine, 5, 125, 98));
        addSlot(new MachineSlot(machine, 6, 89, 80));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 144);
    }

    public @Nullable TileEntityLaunchpadLambda getLauncher() {
        return launcher;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.launchpad_lambda.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MACHINE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
