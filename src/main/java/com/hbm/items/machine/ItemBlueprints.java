package com.hbm.items.machine;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMachineItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A machine template ("blueprint") — inserting it into a machine unlocks the recipes in its
 * {@link #pool}. Fixed templates keep their pool on the item, while the legacy dynamic item carries it in
 * {@link com.hbm.items.ModDataComponents#BLUEPRINT_POOL}; {@link #grabPool} reads either form.
 */
public class ItemBlueprints extends CompatItem implements com.hbm.items.ModelState {

    public final String pool;

    public ItemBlueprints(Properties properties, String pool) {
        super(properties);
        this.pool = pool;
    }

    /** The recipe pool a blueprint stack unlocks, or {@code null} if it isn't a blueprint. */
    @Nullable
    public static String grabPool(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBlueprints bp)) return null;
        return bp.pool != null ? bp.pool : ModDataComponents.blueprintPool(stack);
    }

    /** Makes the dynamic blueprint produced by a blueprint folder. */
    public static ItemStack make(String pool) {
        ItemStack stack = new ItemStack(ModMachineItems.blueprints.get());
        ModDataComponents.blueprintPool(stack, pool);
        return stack;
    }

    /** {@code hbm:model_state}: the sprite of the pool's kind (discover 1, secret 2, 528 3), from the typed pool name. */
    @Override
    public int modelState(ItemStack stack) {
        String pool = grabPool(stack);
        if (pool == null) return 0;
        return pool.startsWith(GenericRecipes.POOL_PREFIX_DISCOVER) ? 1
                : pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET) ? 2
                : pool.startsWith(GenericRecipes.POOL_PREFIX_528) ? 3 : 0;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        return tryCopy(level, player, hand);
    }

    /** Executes the shared copy action without exposing vanilla's version-dependent use return type. */
    public InteractionResult tryCopy(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String poolName = grabPool(stack);
        if (level.isClientSide() || poolName == null
                || poolName.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!player.isCreative() && !consumePaper(player)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        player.swing(hand);
        ItemStack copy = stack.copy();
        copy.setCount(1);

        if (player.isCreative()) {
            player.drop(copy, false);
        } else if (stack.getCount() < stack.getMaxStackSize()) {
            stack.grow(1);
        } else if (!player.getInventory().add(copy)) {
            player.drop(copy, false);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean consumePaper(Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.PAPER)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(getPoolTooltip(stack));
    }

    /** Server-safe tooltip data shared by the item tooltip and focused GameTests. */
    public static List<Component> getPoolTooltip(ItemStack stack) {
        String poolName = grabPool(stack);
        if (poolName == null) return List.of();

        List<String> pool = GenericRecipes.blueprintPools.get(poolName);
        if (pool == null || pool.isEmpty()) return List.of();

        List<Component> tooltip = new ArrayList<>();
        if (poolName.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) {
            tooltip.add(Component.literal("Cannot be copied!").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Right-click to copy (requires paper)")
                    .withStyle(ChatFormatting.YELLOW));
        }
        for (String name : pool) {
            GenericRecipe recipe = GenericRecipes.pooledBlueprints.get(name);
            if (recipe != null) tooltip.add(Component.literal(recipe.getLocalizedName()));
        }
        return tooltip;
    }
}
