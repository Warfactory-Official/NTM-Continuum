package com.hbm.items.tool;

import com.hbm.items.ModDataComponents;
import com.hbm.Constants;
import com.hbm.inventory.ModGunSupportMenus;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuItemBox;
import dev.golem.core.data.ItemContents;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleMenuProvider;

/** The eight-slot ammunition container used directly by Sedna magazines. */
public class ItemAmmoBag extends CompatItem {
    public static final int SLOTS = 8;
    public static final MenuItemBox.Layout LAYOUT = new MenuItemBox.Layout(2, 4, 53, 18, 8, 82,
            140, 176, 168, 6, 0xFFFFFF, 70, Ids.of(Constants.MOD_ID, "textures/gui/gui_ammo_bag.png"));
    private static final int EMPTY_SLOT_ROOM = 64;
    private static final int BAR_WIDTH = 13;
    private final boolean infinite;

    public ItemAmmoBag(Properties properties) { this(properties, false); }
    public ItemAmmoBag(Properties properties, boolean infinite) {
        super(properties.stacksTo(1));
        this.infinite = infinite;
    }
    public boolean isInfinite() { return infinite; }

    /** Source-shaped alias used by magazine implementations and the bag menu. */
    public static InventoryAmmoBag contents(ItemStack bag) { return new InventoryAmmoBag(bag); }

    @Override
    protected int compatUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) { return 1; }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            dev.golem.core.gui.Menus.openWithHand(serverPlayer, new SimpleMenuProvider((id, inventory, opener) -> new MenuItemBox(
                    ModGunSupportMenus.AMMO_BAG.get(), id, inventory, LAYOUT,
                    new ItemStackContainer(opener, opener.getItemInHand(hand), SLOTS,
                            ModDataComponents.AMMO_BAG)), player.getItemInHand(hand).getHoverName()), hand);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !infinite && fill(stack) < 1F;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(BAR_WIDTH * fill(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(fill(stack) / 3.0F, 1.0F, 1.0F);
    }

    private static float fill(ItemStack bag) {
        InventoryAmmoBag contents = contents(bag);
        int room = 0;
        int rounds = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack held = contents.getStackInSlot(slot);
            if (held.isEmpty()) room += EMPTY_SLOT_ROOM;
            else {
                room += held.getMaxStackSize();
                rounds += held.getCount();
            }
        }
        return room == 0 ? 0F : (float) rounds / room;
    }

    /** Mutable fixed-slot view which writes every change back to the bag component. */
    public static final class InventoryAmmoBag {
        private final ItemStack box;
        private final List<ItemStack> stacks;

        public InventoryAmmoBag(ItemStack box) {
            this.box = box;
            this.stacks = new ArrayList<>(ModDataComponents.AMMO_BAG
                    .getOr(box, ItemContents.EMPTY).copies());
            while (stacks.size() < SLOTS) stacks.add(ItemStack.EMPTY);
            if (stacks.size() > SLOTS) stacks.subList(SLOTS, stacks.size()).clear();
        }
        public int getSlots() { return SLOTS; }
        public ItemStack getStackInSlot(int slot) { return stacks.get(slot); }
        public void setStackInSlot(int slot, ItemStack stack) {
            stacks.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            ModDataComponents.AMMO_BAG.set(box, new ItemContents(stacks));
        }
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack current = getStackInSlot(slot);
            if (current.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            int count = Math.min(amount, current.getCount());
            ItemStack result = current.copyWithCount(count);
            if (!simulate) {
                current.shrink(count);
                setStackInSlot(slot, current);
            }
            return result;
        }
    }
}
