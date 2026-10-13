package com.hbm.items.tool;

import com.hbm.items.ModDataComponents;
import com.hbm.Constants;
import com.hbm.inventory.ModGunSupportMenus;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.container.MenuItemBox;
import dev.golem.core.Ids;
import dev.golem.core.data.Tags;
import dev.golem.core.data.ItemContents;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleMenuProvider;

/** Fifteen-slot spent-casing container, including the old fractional accumulation behavior. */
public class ItemCasingBag extends CompatItem {

    public static final int SLOTS = 15;
    public static final MenuItemBox.Layout LAYOUT = new MenuItemBox.Layout(3, 5, 44, 18, 8, 100,
            158, 176, 186, 6, 0xFFFFFF, 88, Ids.of(Constants.MOD_ID, "textures/gui/gui_casing_bag.png"));

    public ItemCasingBag(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static InventoryCasingBag contents(ItemStack bag) { return new InventoryCasingBag(bag); }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) { return 1; }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Menus.openWithHand(serverPlayer, new SimpleMenuProvider((id, inventory, opener) -> new MenuItemBox(
                    ModGunSupportMenus.CASING_BAG.get(), id, inventory, LAYOUT,
                    new ItemStackContainer(opener, opener.getItemInHand(hand), SLOTS,
                            ModDataComponents.CASING_BAG)), player.getItemInHand(hand).getHoverName()), hand);
        }
        return InteractionResult.SUCCESS;
    }

    /** Adds a fractional casing and materializes whole casings into the fifteen slots. */
    public static boolean pushCasing(ItemStack bag, ItemStack casing, float amount) {
        if (!(bag.getItem() instanceof ItemCasingBag) || casing.isEmpty() || amount <= 0) return false;
        // 1.7 keyed the fraction on the casing's name and metadata; the item id carries both here (ntm-next)
        String key = Ids.keyId(casing.getItem().builtInRegistryHolder().key()).toString();
        var progress = ModDataComponents.CASING_PROGRESS.getOr(bag, new CompoundTag());
        float current = Tags.getFloatOr(progress, key, 0F);
        boolean accepted = false;
        if (current < 1F) {
            current += amount;
            accepted = true;
        }
        InventoryCasingBag inventory = new InventoryCasingBag(bag);
        while (current >= 1F) {
            ItemStack one = casing.copyWithCount(1);
            if (!inventory.insert(one)) break;
            current -= 1F;
            accepted = true;
        }
        progress.putFloat(key, current);
        ModDataComponents.CASING_PROGRESS.set(bag, progress);
        return accepted;
    }

    public static final class InventoryCasingBag {
        private final ItemStack box;
        private final List<ItemStack> stacks;

        public InventoryCasingBag(ItemStack box) {
            this.box = box;
            this.stacks = new ArrayList<>(ModDataComponents.CASING_BAG
                    .getOr(box, ItemContents.EMPTY).copies());
            while (stacks.size() < SLOTS) stacks.add(ItemStack.EMPTY);
            if (stacks.size() > SLOTS) stacks.subList(SLOTS, stacks.size()).clear();
        }

        public int getSlots() { return SLOTS; }
        public ItemStack getStackInSlot(int slot) { return stacks.get(slot); }

        public void setStackInSlot(int slot, ItemStack stack) {
            stacks.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            ModDataComponents.CASING_BAG.set(box, new ItemContents(stacks));
        }

        private boolean insert(ItemStack input) {
            for (int i = 0; i < SLOTS; i++) {
                ItemStack slot = stacks.get(i);
                if (!slot.isEmpty() && ItemStacks.sameItemAndData(slot, input)
                        && slot.getCount() < slot.getMaxStackSize()) {
                    slot.grow(1);
                    ModDataComponents.CASING_BAG.set(box, new ItemContents(stacks));
                    return true;
                }
            }
            for (int i = 0; i < SLOTS; i++) if (stacks.get(i).isEmpty()) {
                stacks.set(i, input.copy());
                ModDataComponents.CASING_BAG.set(box, new ItemContents(stacks));
                return true;
            }
            return false;
        }
    }
}
