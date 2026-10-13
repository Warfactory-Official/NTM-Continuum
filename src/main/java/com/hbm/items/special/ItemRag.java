package com.hbm.items.special;

import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModItems;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemRag} (cloth): dropped into water it becomes damp cloth, stack size kept; right-clicked it is
 * consumed for a piss-soaked rag.
 */
public class ItemRag extends CompatItem {

    public ItemRag(Properties properties) {
        super(properties);
    }

    /**
     * 1.7 {@code onEntityItemUpdate}, driven by the loaders' item-entity tick through
     * {@link com.hbm.handler.ExplosivesEntityHooks}. 1.7 tested the block material at the floored entity
     * position; water material is the water fluid here.
     */
    public static void tickDropped(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (!stack.is(ModFinalConsumableItems.rag.get())) return;
        Level level = entity.level();
        BlockPos pos = BlockPos.containing(Mth.floor(entity.getX()), Mth.floor(entity.getY()), Mth.floor(entity.getZ()));
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            entity.setItem(new ItemStack(ModItems.rag_damp.get(), stack.getCount()));
        }
    }

    /** What a used rag becomes instead of {@code rag_piss}; HBM has no listener (1.7: always {@code rag_piss}). */
    @FunctionalInterface
    public interface Use {
        /** The stack the player gets for a used rag, or empty to let the next listener (finally {@code rag_piss}) decide. */
        ItemStack product(Player player);
    }

    /**
     * Listeners in phase order; the first non-empty product wins. NTM: Space gives {@code rag_blood} above 500 RAD.
     */
    public static final Hook<Use> USE = Hook.notifying();

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.shrink(1); // 1.7 decrements even in creative
        player.getInventory().add(product(player));
        return InteractionResult.SUCCESS;
    }

    /** The stack a used rag gives {@code player}: the first {@link #USE} answer, else {@code rag_piss} (1.7). */
    public static ItemStack product(Player player) {
        for (Use listener : USE) {
            ItemStack out = listener.product(player);
            if (out != null && !out.isEmpty()) return out;
        }
        return new ItemStack(ModItems.rag_piss.get());
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.rag.desc.0"));
        tooltip.add(Component.translatable("item.hbm.rag.desc.1"));
    }
}
