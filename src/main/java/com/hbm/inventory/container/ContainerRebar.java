package com.hbm.inventory.container;

import com.hbm.items.tool.ItemRebarPlacer.InventoryRebar;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ItemRebarPlacer.ContainerRebar}: the placer's one pattern slot at (53, 36), the player inventory at
 * (8, 100) and the hotbar at y 158. Clicking the pattern slot copies the carried stack into it (or clears it) and
 * leaves the carried stack alone; shift-clicking moves nothing; the open placer cannot be number-key swapped.
 */
public class ContainerRebar extends CompatContainerMenu {

    private final InventoryRebar rebar;

    public ContainerRebar(MenuType<?> type, int containerId, Inventory invPlayer, InventoryRebar rebar) {
        super(type, containerId);
        this.rebar = rebar;

        addSlot(new Slot(rebar, 0, 53, 36) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 100 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(invPlayer, i, 8 + i * 18, 158));
        }
    }

    /** Client side: Core's extended menu payload names the hand holding the placer. */
    public ContainerRebar(MenuType<?> type, int containerId, Inventory invPlayer, InteractionHand hand) {
        this(type, containerId, invPlayer, new InventoryRebar(invPlayer.player, invPlayer.player.getItemInHand(hand)));
    }

    public InventoryRebar inventory() {
        return rebar;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        int current = Compat.selectedSlot(player);
        // prevents the player from moving around the currently open placer
        if (action == ClickAction.SWAP && button == current) return true;
        // 1.7 also refused index currentItem + 47, copied from the 20-slot lead box; with one pattern slot that index
        // is past the hotbar and never matched, so the open placer itself can still be picked up here too
        if (slotId != 0) return false;

        // 1.7: the pattern slot takes a copy of whatever is carried, the carried stack stays in hand
        Slot slot = getSlot(0);
        ItemStack held = getCarried();
        slot.set(held.isEmpty() ? ItemStack.EMPTY : held.copy());
        rebar.setChanged();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return rebar.stillValid(player);
    }
}
