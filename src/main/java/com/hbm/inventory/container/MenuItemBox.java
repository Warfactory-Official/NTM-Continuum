package com.hbm.inventory.container;

import com.hbm.inventory.ItemStackContainer;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemCasingBag;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.data.ItemContents;
import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The source ntm-next item-backed box menu, narrowed to HBM's component stores. */
public final class MenuItemBox extends CompatContainerMenu {

    public record Layout(int rows, int columns, int slotX, int slotY, int inventoryX, int inventoryY,
                         int hotbarY, int width, int height, int titleY, int titleColor,
                         int inventoryLabelY, net.minecraft.resources.Identifier texture) {
        public int slots() { return rows * columns; }
    }

    private final Layout layout;
    private final ItemStackContainer stackContainer;

    public MenuItemBox(MenuType<?> type, int containerId, Inventory playerInventory, Layout layout,
                       ItemStackContainer container) {
        this(type, containerId, playerInventory, layout, container, container);
    }

    private MenuItemBox(MenuType<?> type, int containerId, Inventory playerInventory, Layout layout,
                        Container container, ItemStackContainer stackContainer) {
        super(type, containerId);
        checkContainerSize(container, layout.slots());
        this.layout = layout;
        this.stackContainer = stackContainer;

        for (int row = 0; row < layout.rows(); row++) {
            for (int col = 0; col < layout.columns(); col++) {
                int slot = col + row * layout.columns();
                addSlot(new Slot(container, slot, layout.slotX() + col * 18, layout.slotY() + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return MenuItemBox.this.accepts(stack);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        layout.inventoryX() + col * 18, layout.inventoryY() + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, layout.inventoryX() + col * 18, layout.hotbarY()));
        }
    }

    /** Client-side constructor; Core's extended menu payload identifies the owning hand. */
    public MenuItemBox(MenuType<?> type, int containerId, Inventory playerInventory, Layout layout,
                       GolemComponent<ItemContents> component,
                       net.minecraft.world.InteractionHand hand) {
        this(type, containerId, playerInventory, layout,
                new ItemStackContainer(playerInventory.player, playerInventory.player.getItemInHand(hand),
                        layout.slots(), component));
    }

    public Layout layout() {
        return layout;
    }

    private boolean accepts(ItemStack stack) {
        // The casing bag is filled by spent-casing recovery, never by player insertion.
        if (layout.equals(ItemCasingBag.LAYOUT)) return false;
        return dev.golem.core.world.ItemStacks.hasOnlyVariantAndDamage(stack, ModDataComponents.ITEM_VARIANT);
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (stackContainer == null) return false;
        ItemStack target = stackContainer.target();
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId).getItem() == target) return true;
        return action == ClickAction.SWAP && (button >= 0 && button < 9 || button == 40)
                && player.getInventory().getItem(button) == target;
    }

    @Override
    public boolean stillValid(Player player) {
        return stackContainer == null || stackContainer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (stackContainer != null) stackContainer.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)
                || stackContainer != null && slot.getItem() == stackContainer.target()) return ItemStack.EMPTY;

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        int boxSlots = layout.slots();
        boolean moved = index < boxSlots
                ? moveItemStackTo(source, boxSlots, slots.size(), true)
                : accepts(source) && moveItemStackTo(source, 0, boxSlots, false);
        if (!moved) return ItemStack.EMPTY;

        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return original;
    }
}
