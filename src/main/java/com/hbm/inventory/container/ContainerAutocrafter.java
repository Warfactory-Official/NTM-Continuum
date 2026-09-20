package com.hbm.inventory.container;

import com.hbm.lib.Library;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModProductionMenus;
import com.hbm.tileentity.machine.TileEntityMachineAutocrafter;

/**
 * 1.7 {@code ContainerAutocrafter}: nine pattern ghosts (44+18j, 22+18i), the template output (116,40), the
 * live 3x3 grid (44+18j, 86+18i), output (116,104), battery (17,99); player inventory at (8,158), 176x240.
 *
 * <p>Pattern clicks are handled as the old {@code slotClick} did: a right-click on a set ghost cycles its
 * match mode (or, on the template output, the chosen recipe); any other click writes the carried stack
 * into the ghost and re-matches the template.
 */
public final class ContainerAutocrafter extends CompatContainerMenu {

    public static final int MACHINE_SLOTS = 21;

    private final Container machine;
    private final @Nullable TileEntityMachineAutocrafter autocrafter;

    public ContainerAutocrafter(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerAutocrafter(int containerId, Inventory playerInventory, @Nullable TileEntityMachineAutocrafter autocrafter) {
        super(ModProductionMenus.AUTOCRAFTER.get(), containerId);
        this.autocrafter = autocrafter;
        this.machine = autocrafter != null ? autocrafter : new SimpleContainer(MACHINE_SLOTS);

        /* TEMPLATE */
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new SlotPattern(machine, j + i * 3, 44 + j * 18, 22 + i * 18));
            }
        }
        addSlot(new SlotPattern(machine, 9, 116, 40));

        /* RECIPE */
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new Slot(machine, 10 + j + i * 3, 44 + j * 18, 86 + i * 18));
            }
        }

        addSlot(new Slot(machine, 19, 116, 104));

        // Battery
        addSlot(new Slot(machine, 20, 17, 99));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 158);
    }

    private static @Nullable TileEntityMachineAutocrafter resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineAutocrafter autocrafter ? autocrafter : null;
    }

    public @Nullable TileEntityMachineAutocrafter getAutocrafter() { return autocrafter; }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        // L/R: PICKUP, M3: CLONE, SHIFT: QUICK_MOVE, DRAG: QUICK_CRAFT
        if (slotId < 0 || slotId > 9) return false;

        Slot slot = slots.get(slotId);
        ItemStack held = getCarried();

        // Don't allow any interaction for the template's output
        if (slotId == 9) {
            if (button == 1 && action == ClickAction.PICKUP && slot.hasItem() && autocrafter != null) {
                autocrafter.nextTemplate();
                broadcastChanges();
            }
            return true;
        }

        if (button == 1 && action == ClickAction.PICKUP && slot.hasItem()) {
            if (autocrafter != null) autocrafter.nextMode(slotId);
            return true;
        }

        machine.setItem(slotId, held.copy());
        slot.setChanged();
        if (autocrafter != null && autocrafter.getLevel() != null) {
            autocrafter.matcher.initPatternSmart(autocrafter.getLevel(), slot.getItem(), slotId);
            autocrafter.updateTemplateGrid();
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index <= 20 && index >= 10) {
                if (!moveItemStackTo(stack, 21, slots.size(), true)) return ItemStack.EMPTY;
            } else if (index > 20) {
                if (Library.isBattery(result)) {
                    if (!moveItemStackTo(stack, 20, 21, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }

    /** 1.7 SlotPattern: a filter slot that never takes or gives items. */
    public static class SlotPattern extends Slot {
        public SlotPattern(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}
