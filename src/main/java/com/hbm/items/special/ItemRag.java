package com.hbm.items.special;

import com.hbm.items.ModFinalConsumableItems;
import com.hbm.items.ModItems;
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

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.shrink(1); // 1.7 decrements even in creative
        player.getInventory().add(new ItemStack(ModItems.rag_piss.get()));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.rag.desc.0"));
        tooltip.add(Component.translatable("item.hbm.rag.desc.1"));
    }
}
