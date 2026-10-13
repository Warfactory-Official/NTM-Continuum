package com.hbm.items.tool;

import com.hbm.inventory.ModMeleeKitMenus;
import com.hbm.inventory.container.ContainerToolBox;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.ModelState;
import dev.golem.core.data.ItemContents;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ItemToolBox}: a 24-slot (three rows of eight) box. Right-click rotates hotbar rows through it
 * (the hotbar minus the box itself goes into the box, the lowest filled row comes out onto the hotbar);
 * sneak-right-click opens it. While open the icon shows its {@code kit_toolbox_empty} lid ({@code isOpen},
 * here {@code hbm:toolbox_open}, ntm-next's TOOLBOX_OPEN). Contents ride {@code hbm:toolbox}, in place of 1.7's NBT item list, and are
 * ejected with a warning when their compressed size passes 6 kB exactly as {@code ItemInventory.checkNBT} did.
 */
public class ItemToolBox extends CompatItem implements ModelState {

    public static final int SIZE = 24;

    public ItemToolBox(Properties properties) {
        super(properties);
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 1;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Click with the toolbox to swap hotbars in/out of the toolbox."));
        list.add(Component.literal("Shift-click with the toolbox to open the toolbox."));
    }

    /** 1.7 {@code ItemStackUtil.readStacksFromNBT(box, 24)}: null when the box never held anything. */
    public static ItemStack[] readStacks(ItemStack box) {
        if (!ModMeleeKitItems.TOOLBOX.has(box)) return null;
        List<ItemStack> copies = ModMeleeKitItems.TOOLBOX.getOr(box, ItemContents.EMPTY).copies();
        ItemStack[] stacks = new ItemStack[SIZE];
        for (int i = 0; i < SIZE; i++) {
            stacks[i] = i < copies.size() && !copies.get(i).isEmpty() ? copies.get(i) : null;
        }
        return stacks;
    }

    public static void writeStacks(ItemStack box, ItemStack[] stacks) {
        List<ItemStack> list = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) list.add(stacks[i] == null ? ItemStack.EMPTY : stacks[i]);
        ModMeleeKitItems.TOOLBOX.set(box, new ItemContents(list));
    }

    /** Finds active rows in the toolbox (rows with items inside them). */
    public static List<Integer> getActiveRows(ItemStack box) {
        ItemStack[] stacks = readStacks(box);
        if (stacks == null) return new ArrayList<>();
        List<Integer> activeRows = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int slot = 0; slot < 8; slot++) {
                if (stacks[row * 8 + slot] != null) {
                    activeRows.add(row);
                    break;
                }
            }
        }
        return activeRows;
    }

    /** 1.7 "This function genuinely hurts my soul, but it works...", transcribed. */
    public static void moveRows(ItemStack box, Player player) {
        Inventory inv = player.getInventory();
        int current = Compat.selectedSlot(player);

        // Move from hotbar into array in preparation for boxing.
        ItemStack[] endingHotBar = new ItemStack[9];
        ItemStack[] stacksToTransferToBox = new ItemStack[8];

        boolean hasToolbox = false;
        int extraToolboxes = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inv.getItem(i);

            if (!slot.isEmpty() && slot.getItem() instanceof ItemToolBox && i != current) {
                extraToolboxes++;
                player.drop(slot, true);
                inv.setItem(i, ItemStack.EMPTY);
            } else if (i == current) {
                hasToolbox = true;
                endingHotBar[i] = slot;
            } else {
                stacksToTransferToBox[i - (hasToolbox ? 1 : 0)] = slot.isEmpty() ? null : slot;
            }
        }

        if (extraToolboxes > 0) {
            player.sendSystemMessage(Component.literal(extraToolboxes == 1 ? "You can't toolbox a toolbox... "
                    : "You can't toolbox a toolbox... (x" + extraToolboxes + ")").withStyle(ChatFormatting.RED));
        }

        // Move stacks around inside the box, mostly shifts rows to other rows and shifts the top row to the hotbar.
        ItemStack[] stacks = readStacks(box);
        ItemStack[] endingStacks = new ItemStack[SIZE];

        int lowestActiveIndex = Integer.MAX_VALUE;
        int lowestInactiveIndex = Integer.MAX_VALUE;

        if (stacks != null) {
            List<Integer> activeRows = getActiveRows(box);

            for (int i = 0; i < 3; i++) {
                if (activeRows.contains(i)) lowestActiveIndex = Math.min(i, lowestActiveIndex);
                else lowestInactiveIndex = Math.min(i, lowestInactiveIndex);
            }

            if (lowestInactiveIndex > 2) lowestInactiveIndex = 2; // no inactive rows: the last row
            else lowestInactiveIndex = Math.max(0, lowestInactiveIndex - 1);

            for (Integer activeRowIndex : activeRows) {
                int activeIndex = 8 * activeRowIndex;

                if (activeRowIndex == lowestActiveIndex) { // items to "flow" to the hotbar
                    hasToolbox = false;
                    for (int i = 0; i < 9; i++) {
                        if (i == current) {
                            hasToolbox = true;
                            continue;
                        }
                        endingHotBar[i] = stacks[activeIndex + i - (hasToolbox ? 1 : 0)];
                    }
                    continue;
                }

                int targetIndex = 8 * (activeRowIndex - 1);
                System.arraycopy(stacks, activeIndex, endingStacks, targetIndex, 8);
            }
        }

        if (stacks == null) lowestInactiveIndex = 0; // 1.7's fix for a null list leaving MAX_VALUE

        System.arraycopy(stacksToTransferToBox, 0, endingStacks, lowestInactiveIndex * 8, 8);

        for (int i = 0; i < endingHotBar.length; i++) {
            inv.setItem(i, endingHotBar[i] == null ? ItemStack.EMPTY : endingHotBar[i]);
        }

        ModMeleeKitItems.TOOLBOX.remove(box);
        writeStacks(box, endingStacks);
        checkSize(box, player, endingStacks);
    }

    /** 1.7 {@code ItemInventory.checkNBT}: over 6 kB compressed, eject everything in 10..30 piles and empty the box. */
    public static boolean checkSize(ItemStack box, Player player, ItemStack[] slots) {
        if (ItemStacks.compressedSize(box, player.level().registryAccess()) <= 6000) return false;

        player.sendSystemMessage(Component.literal("Warning: Container NBT exceeds 6kB, contents will be ejected!")
                .withStyle(ChatFormatting.RED));
        RandomSource random = player.getRandom();
        for (ItemStack itemstack : slots) {
            if (itemstack == null || itemstack.isEmpty()) continue;
            ItemStack left = itemstack.copy();
            float f = random.nextFloat() * 0.8F + 0.1F;
            float f1 = random.nextFloat() * 0.8F + 0.1F;
            float f2 = random.nextFloat() * 0.8F + 0.1F;
            while (left.getCount() > 0) {
                int j1 = Math.min(random.nextInt(21) + 10, left.getCount());
                ItemEntity entityitem = new ItemEntity(player.level(), player.getX() + f, player.getY() + f1,
                        player.getZ() + f2, left.split(j1));
                float f3 = 0.05F;
                entityitem.setDeltaMovement(random.nextGaussian() * f3 + player.getDeltaMovement().x,
                        random.nextGaussian() * f3 + 0.2F + player.getDeltaMovement().y,
                        random.nextGaussian() * f3 + player.getDeltaMovement().z);
                player.level().addFreshEntity(entityitem);
            }
        }
        ModMeleeKitItems.TOOLBOX.remove(box);
        return true;
    }

    /** {@code hbm:model_state}: 1 draws the open lid (1.7 metadata 1), from {@code hbm:toolbox_open}. */
    @Override
    public int modelState(ItemStack stack) {
        return ModDataComponents.TOOLBOX_OPEN.getOr(stack, false) ? 1 : 0;
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player instanceof ServerPlayer serverPlayer) {
            if (!player.isShiftKeyDown()) {
                moveRows(stack, player);
                player.inventoryMenu.broadcastChanges();
            } else {
                ModDataComponents.TOOLBOX_OPEN.set(stack, true); // 1.7 isOpen (metadata 1)
                Component title = ItemStacks.customName(stack) != null ? stack.getHoverName()
                        : Component.translatable("container.toolBox");
                Menus.openWithHand(serverPlayer, new SimpleMenuProvider((id, inventory, opener) ->
                        new ContainerToolBox(ModMeleeKitMenus.TOOLBOX.get(), id, inventory,
                                new InventoryToolBox(opener, opener.getItemInHand(hand))), title), hand);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code ItemToolBox.InventoryToolBox}: the open box's 24 slots, written back on every change. */
    public static final class InventoryToolBox extends SimpleContainer {

        private final Player player;
        public final ItemStack target;

        public InventoryToolBox(Player player, ItemStack box) {
            super(SIZE);
            this.player = player;
            this.target = box;
            ItemStack[] fromNBT = readStacks(box);
            if (fromNBT != null) {
                for (int i = 0; i < SIZE; i++) if (fromNBT[i] != null) super.setItem(i, fromNBT[i]);
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return !(stack.getItem() instanceof ItemToolBox);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (player.level().isClientSide()) return;
            ItemStack[] slots = new ItemStack[SIZE];
            for (int i = 0; i < SIZE; i++) slots[i] = getItem(i).isEmpty() ? null : getItem(i);
            writeStacks(target, slots);
            if (checkSize(target, player, slots)) {
                for (int i = 0; i < SIZE; i++) super.setItem(i, ItemStack.EMPTY);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return player == this.player && (player.getMainHandItem() == target || player.getOffhandItem() == target);
        }

        /** 1.7 {@code closeInventory}: the lid closes. */
        public void close() {
            ModDataComponents.TOOLBOX_OPEN.remove(target);
        }
    }
}
