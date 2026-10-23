package com.hbm.inventory.container;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
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

/** Pos-synchronised menu for the plasma forge: battery, blueprint, booster, twelve inputs and one output. */
public class ContainerMachinePlasmaForge extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 16;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityFusionPlasmaForge forge;

    public ContainerMachinePlasmaForge(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachinePlasmaForge(int containerId, Inventory playerInventory, TileEntityFusionPlasmaForge forge) {
        this(containerId, playerInventory, forge,
                ContainerLevelAccess.create(forge.getLevel(), forge.getBlockPos()));
    }

    private ContainerMachinePlasmaForge(int containerId, Inventory playerInventory,
                                        @Nullable TileEntityFusionPlasmaForge forge, ContainerLevelAccess access) {
        super(ModMenus.FUSION_PLASMA_FORGE.get(), containerId);
        this.forge = forge;
        this.machine = forge != null ? forge : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        this.addSlot(validated(TileEntityFusionPlasmaForge.SLOT_BATTERY, 152, 82));
        this.addSlot(nonRetarded(TileEntityFusionPlasmaForge.SLOT_BLUEPRINT, 35, 81));
        this.addSlot(nonRetarded(TileEntityFusionPlasmaForge.SLOT_BOOSTER, 98, 116));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 4; col++) {
            this.addSlot(validated(TileEntityFusionPlasmaForge.FIRST_INPUT + row * 4 + col,
                    8 + col * 18, 18 + row * 18));
        }
        this.addSlot(new PlasmaForgeOutputSlot(playerInventory.player, this.machine,
                TileEntityFusionPlasmaForge.SLOT_OUTPUT, 116, 36));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 162);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return canPlaceInMachine(index, stack);
            }
        };
    }

    private Slot nonRetarded(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return canPlaceInMachine(index, stack);
            }

            @Override public int getMaxStackSize() {
                return Math.max(super.getMaxStackSize(), hasItem() ? getItem().getCount() : 1);
            }
        };
    }

    private boolean canPlaceInMachine(int index, ItemStack stack) {
        if (index == TileEntityFusionPlasmaForge.SLOT_BATTERY) return Library.isBattery(stack);
        if (index == TileEntityFusionPlasmaForge.SLOT_BLUEPRINT) {
            return !stack.isEmpty() && stack.getItem() instanceof ItemBlueprints;
        }
        return machine.canPlaceItem(index, stack);
    }

    private static @Nullable TileEntityFusionPlasmaForge resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityFusionPlasmaForge forge ? forge : null;
    }

    public @Nullable TileEntityFusionPlasmaForge getForge() { return forge; }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player, ModMachineBlocks.fusion_plasma_forge.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack source = slot.getItem();
        ItemStack result = source.copy();
        int originalCount = source.getCount();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(source, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
        } else if (Library.isBattery(source)) {
            if (!moveItemStackTo(source, TileEntityFusionPlasmaForge.SLOT_BATTERY,
                    TileEntityFusionPlasmaForge.SLOT_BATTERY + 1, false)) return ItemStack.EMPTY;
        } else if (source.getItem() instanceof ItemBlueprints) {
            if (!moveItemStackTo(source, TileEntityFusionPlasmaForge.SLOT_BLUEPRINT,
                    TileEntityFusionPlasmaForge.SLOT_BLUEPRINT + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(source, TileEntityFusionPlasmaForge.SLOT_BOOSTER,
                TileEntityFusionPlasmaForge.SLOT_OUTPUT, false)) {
            return ItemStack.EMPTY;
        }

        if (source.getCount() == originalCount) return ItemStack.EMPTY;
        int movedCount = originalCount - source.getCount();
        if (index == TileEntityFusionPlasmaForge.SLOT_OUTPUT) {
            slot.onQuickCraft(source, result);
        }
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        slot.onTake(player, result.copyWithCount(movedCount));
        return result;
    }

    /** Modern equivalent of the legacy SlotCraftingOutput. */
    private static final class PlasmaForgeOutputSlot extends Slot {
        private final Player player;
        private int craftBuffer;

        private PlasmaForgeOutputSlot(Player player, Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.player = player;
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }

        @Override public ItemStack remove(int amount) {
            if (hasItem()) craftBuffer += Math.min(amount, getItem().getCount());
            return super.remove(amount);
        }

        @Override public void onTake(Player player, ItemStack stack) {
            onCrafting(stack);
            super.onTake(player, stack);
        }

        @Override protected void onQuickCraft(ItemStack stack, int amount) {
            craftBuffer += amount;
        }

        private void onCrafting(ItemStack stack) {
            if (craftBuffer <= 0) return;
            ItemStacks.onCraftedBy(stack, player, craftBuffer);
            craftBuffer = 0;
        }
    }
}
