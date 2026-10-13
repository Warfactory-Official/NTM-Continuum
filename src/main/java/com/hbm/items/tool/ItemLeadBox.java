package com.hbm.items.tool;

import com.hbm.inventory.ModUtilityToolMenus;
import com.hbm.inventory.container.ContainerLeadBox;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.block.ItemBlockStorageCrate;
import dev.golem.core.data.ItemContents;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemLeadBox}, the Containment Box: a 20-slot box (one item per slot) opened by right-click. What
 * it holds is kept on the stack ({@code hbm:containment_box} in place of 1.7's NBT item list), and its
 * radiation reaches the carrier only as the square root of the contents' total (1.7
 * {@code HazardTransformerRadiationContainer}, registered by {@link ModUtilityToolItems}). Storage crates may
 * not go inside.
 */
public class ItemLeadBox extends CompatItem {

    public static final int SIZE = 20;

    public ItemLeadBox(Properties properties) {
        super(properties);
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 1;
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            Component title = ItemStacks.customName(held) != null
                    ? held.getHoverName() : Component.translatable("container.leadBox");
            Menus.openWithHand(serverPlayer, new SimpleMenuProvider((id, inventory, opener) ->
                    new ContainerLeadBox(ModUtilityToolMenus.LEAD_BOX.get(), id, inventory,
                            new InventoryLeadBox(opener, opener.getItemInHand(hand))), title), hand);
        }
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code ItemStackUtil.readStacksFromNBT(box, 20)}: null for a box that never held anything. */
    public static ItemStack[] readStacks(ItemStack box) {
        if (!ModUtilityToolItems.CONTAINMENT.has(box)) return null;
        List<ItemStack> copies = ModUtilityToolItems.CONTAINMENT.getOr(box, ItemContents.EMPTY).copies();
        ItemStack[] stacks = new ItemStack[SIZE];
        for (int i = 0; i < SIZE; i++) {
            stacks[i] = i < copies.size() && !copies.get(i).isEmpty() ? copies.get(i) : null;
        }
        return stacks;
    }

    /** 1.7 {@code InventoryLeadBox}: the open box's slots, written back to the stack on every change. */
    public static final class InventoryLeadBox extends SimpleContainer {

        private final Player player;
        public final ItemStack target;

        public InventoryLeadBox(Player player, ItemStack box) {
            super(SIZE);
            this.player = player;
            this.target = box;
            ItemStack[] fromNBT = readStacks(box);
            if (fromNBT != null) {
                for (int i = 0; i < SIZE; i++) if (fromNBT[i] != null) super.setItem(i, fromNBT[i]);
            }
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !(stack.getItem() instanceof ItemBlockStorageCrate);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (player.level().isClientSide()) return;
            List<ItemStack> list = new ArrayList<>(SIZE);
            for (int i = 0; i < SIZE; i++) list.add(getItem(i).copy());
            ModUtilityToolItems.CONTAINMENT.set(target, new ItemContents(list));
        }

        @Override
        public boolean stillValid(Player player) {
            return player == this.player && (player.getMainHandItem() == target || player.getOffhandItem() == target);
        }
    }
}
