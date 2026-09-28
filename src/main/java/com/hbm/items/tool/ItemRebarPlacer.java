package com.hbm.items.tool;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.generic.BlockRebar;
import com.hbm.blocks.material.MaterialCompletionRegistrations;
import com.hbm.inventory.ModDungeonMenus;
import com.hbm.inventory.container.ContainerRebar;
import com.hbm.items.ModDungeonItems;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ItemRebarPlacer}. Sneak-right-click opens its one-slot menu, where a concrete block is set as the type
 * the rebar will cure into (a pattern slot: the stack stays in the inventory). Then two clicks on blocks span a
 * box - the first stores the cell in front of the clicked face, the second fills every replaceable cell of the box
 * with rebar (layer by layer from the bottom, as far as the rebar in the inventory reaches) and spends the rebar.
 * The stored corner is forgotten as soon as the placer is not held or holds no valid concrete. While a corner is
 * stored, the box and the "have / need" rebar count are previewed ({@code RebarPlacerPreview}).
 *
 * <p>Valid concrete (1.7 {@code acceptableConk}): concrete, reinforced concrete, smooth concrete, the concrete
 * pillar, the sixteen coloured and every extended concrete. A placer used before anything was loaded is loaded
 * with reinforced concrete, as 1.7 did.
 */
public class ItemRebarPlacer extends CompatItem {

    public ItemRebarPlacer(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code isValidConk}; the coloured concretes are one item each here, their colour a component. */
    public static boolean isValidConk(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModBlocks.concrete.get().asItem()
                || item == ModDecoContent.concrete_rebar.get().asItem()
                || item == ModBlocks.concrete_smooth.get().asItem()
                || item == ModDecoContent.concrete_pillar.get().asItem()
                || item == MaterialCompletionRegistrations.CONCRETE_COLORED.get().asItem()
                || item == MaterialCompletionRegistrations.CONCRETE_COLORED_EXT.get().asItem();
    }

    /** Every acceptable concrete as a display stack, in 1.7's order, for the menu's empty-slot tooltip. */
    public static List<ItemStack> acceptableConk() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(ModBlocks.concrete.get()));
        list.add(new ItemStack(ModDecoContent.concrete_rebar.get()));
        list.add(new ItemStack(ModBlocks.concrete_smooth.get()));
        list.add(new ItemStack(ModDecoContent.concrete_pillar.get()));
        for (int i = 0; i < 16; i++) list.add(MaterialCompletionRegistrations.concreteColoredItem(i));
        for (int i = 0; i < 8; i++) {
            ItemStack ext = new ItemStack(MaterialCompletionRegistrations.CONCRETE_COLORED_EXT.get());
            com.hbm.items.ModDataComponents.ITEM_VARIANT.set(ext, i);
            list.add(ext);
        }
        return list;
    }

    public static ItemStack loaded(ItemStack placer) {
        return ModDungeonItems.REBAR_CONCRETE.getOr(placer, ItemStack.EMPTY);
    }

    @Override
    protected int compatUseDuration(ItemStack stack, LivingEntity entity) {
        return 1;
    }

    // if the placer isn't equipped or no concrete is loaded, forget the cached position
    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot, boolean held) {
        if (!ModDungeonItems.REBAR_POS.has(stack)) return;
        ItemStack theConk = loaded(stack);
        if (!held || theConk.isEmpty() || !isValidConk(theConk)) ModDungeonItems.REBAR_POS.remove(stack);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            ItemStack held = player.getItemInHand(hand);
            Component title = ItemStacks.customName(held) != null ? held.getHoverName() : Component.translatable("container.rebar");
            Menus.openWithHand(serverPlayer, new SimpleMenuProvider((id, inventory, opener) ->
                    new ContainerRebar(ModDungeonMenus.REBAR.get(), id, inventory,
                            new InventoryRebar(opener, opener.getItemInHand(hand))), title), hand);
        }
        return InteractionResult.SUCCESS;
    }

    private static void chat(Player player, String message, ChatFormatting color) {
        player.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable("item.hbm.rebar_placer").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal(message).withStyle(color)));
    }

    /** 1.7 {@code InventoryUtil.countAStackMatches(player, rebar, true)}: rebar in the main inventory. */
    public static int countRebar(Player player) {
        Inventory inv = player.getInventory();
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModDungeonBlocks.rebar.get().asItem())) count += s.getCount();
        }
        return count;
    }

    private static void consumeRebar(Player player, int amount) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36 && amount > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(ModDungeonBlocks.rebar.get().asItem())) continue;
            int take = Math.min(amount, s.getCount());
            s.shrink(take);
            amount -= take;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        if (world.isClientSide() || player == null) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();

        if (!ModDungeonItems.REBAR_CONCRETE.has(stack)) {
            ModDungeonItems.REBAR_CONCRETE.set(stack, new ItemStack(ModDecoContent.concrete_rebar.get()));
        }
        ItemStack theConk = loaded(stack);

        if (theConk.isEmpty() || !isValidConk(theConk)) {
            chat(player, "No valid concrete type set!", ChatFormatting.RED);
            return InteractionResult.SUCCESS;
        }

        Direction dir = context.getClickedFace();
        BlockPos clicked = context.getClickedPos().relative(dir);

        if (!ModDungeonItems.REBAR_POS.has(stack)) {
            ModDungeonItems.REBAR_POS.set(stack, clicked);
        } else {
            int rebarLeft = countRebar(player);
            if (rebarLeft <= 0) {
                chat(player, "Out of rebar!", ChatFormatting.RED);
                ModDungeonItems.REBAR_POS.remove(stack);
                return InteractionResult.SUCCESS;
            }

            BlockPos pos = ModDungeonItems.REBAR_POS.getOr(stack, clicked);
            int minX = Math.min(pos.getX(), clicked.getX()), maxX = Math.max(pos.getX(), clicked.getX());
            int minY = Math.min(pos.getY(), clicked.getY()), maxY = Math.max(pos.getY(), clicked.getY());
            int minZ = Math.min(pos.getZ(), clicked.getZ()), maxZ = Math.max(pos.getZ(), clicked.getZ());

            int rebarUsed = 0;

            outer:
            for (int k = minY; k <= maxY; k++) {
                for (int j = minZ; j <= maxZ; j++) {
                    for (int i = minX; i <= maxX; i++) {
                        if (rebarLeft <= 0) break outer;
                        BlockPos at = new BlockPos(i, k, j);
                        if (world.getBlockState(at).canBeReplaced() && player.mayUseItemAt(at, dir, stack)) {
                            world.setBlock(at, ModDungeonBlocks.rebar.get().defaultBlockState(), 3);
                            if (world.getBlockEntity(at) instanceof BlockRebar.TileEntityRebar rebar) rebar.setup(theConk);
                            rebarUsed++;
                            rebarLeft--;
                        }
                    }
                }
            }

            consumeRebar(player, rebarUsed);
            chat(player, "Placed " + rebarUsed + " rebar!", ChatFormatting.GREEN);
            ModDungeonItems.REBAR_POS.remove(stack);
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code InventoryRebar}: the one pattern slot, written back to the placer on every change. */
    public static final class InventoryRebar extends SimpleContainer {

        private final Player player;
        public final ItemStack target;

        public InventoryRebar(Player player, ItemStack box) {
            super(1);
            this.player = player;
            this.target = box;
            ItemStack loaded = loaded(box);
            if (!loaded.isEmpty()) super.setItem(0, loaded.copy());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (player.level().isClientSide()) return;
            ItemStack slot = getItem(0);
            // 1.7 kept an empty slot list, which the next use reported as "No valid concrete type set!"; an empty
            // component cannot be stored, so an emptied placer falls back to reinforced concrete on its next use
            if (slot.isEmpty()) ModDungeonItems.REBAR_CONCRETE.remove(target);
            else ModDungeonItems.REBAR_CONCRETE.set(target, slot.copyWithCount(1));
        }

        @Override
        public boolean stillValid(Player player) {
            return player == this.player && (player.getMainHandItem() == target || player.getOffhandItem() == target);
        }
    }
}
