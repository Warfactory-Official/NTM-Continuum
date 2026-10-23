package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.ModMachineItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
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

/** Pos-synchronised menu for the fusion torus: a battery slot, a blueprint slot and the product slot. */
public class ContainerFusionTorus extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = TileEntityFusionTorus.SLOTS;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFusionTorus torus;

    /** Client-side constructor; {@code pos} comes from the loader's menu-open payload. */
    public ContainerFusionTorus(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side constructor, bound to the live reactor. */
    public ContainerFusionTorus(int containerId, Inventory playerInventory, TileEntityFusionTorus torus) {
        this(containerId, playerInventory, torus, ContainerLevelAccess.create(torus.getLevel(), torus.getBlockPos()));
    }

    private ContainerFusionTorus(int containerId, Inventory playerInventory, @Nullable TileEntityFusionTorus torus,
                                 ContainerLevelAccess access) {
        super(ModMenus.FUSION_TORUS.get(), containerId);
        this.torus = torus;
        this.machine = torus != null ? torus : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(TileEntityFusionTorus.SLOT_BATTERY, 8, 82));
        this.addSlot(validated(TileEntityFusionTorus.SLOT_BLUEPRINT, 71, 81));
        this.addSlot(takeOnly(TileEntityFusionTorus.SLOT_OUTPUT, 130, 36));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 35, 162);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerFusionTorus.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityFusionTorus resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityFusionTorus reactor ? reactor : null;
    }

    public @Nullable TileEntityFusionTorus getTorus() {
        return torus;
    }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.fusion_torus.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.is(ModMachineItems.blueprints.get())) {
            if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (Library.isBattery(stack)) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
