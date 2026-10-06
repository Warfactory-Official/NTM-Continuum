package com.hbm.items.tool;

import com.hbm.blocks.ModDecoContent;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModNuclearMiscItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 1.7 {@code ItemMS}, the Brittle Spade: digging up {@code ntm_dirt} with it yields the three inert
 * U-238m2 easter-egg ingots (metadata 1, 2, 3; the port's {@code hs_elements}, {@code hs_arsenic}, {@code hs_vault}).
 */
public class ItemMS extends CompatItem {

    public ItemMS(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Lost but not forgotten"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!world.isClientSide() && world.getBlockState(pos).is(ModDecoContent.ntm_dirt.get())) {
            world.destroyBlock(pos, false);
            RandomSource rand = world.getRandom();

            // 1.7 ingot_u238m2 metadata 1..3, the hs_* items here
            for (net.minecraft.world.item.Item egg : new net.minecraft.world.item.Item[] {ModNuclearMiscItems.HS_ELEMENTS.get(),
                    ModNuclearMiscItems.HS_ARSENIC.get(), ModNuclearMiscItems.HS_VAULT.get()}) {
                ItemStack sta = new ItemStack(egg);
                float f = rand.nextFloat() * 0.8F + 0.1F;
                float f1 = rand.nextFloat() * 0.8F + 0.1F;
                float f2 = rand.nextFloat() * 0.8F + 0.1F;
                ItemEntity entityitem = new ItemEntity(world, pos.getX() + f, pos.getY() + f1, pos.getZ() + f2, sta);
                float f3 = 0.05F;
                entityitem.setDeltaMovement(rand.nextGaussian() * f3, rand.nextGaussian() * f3 + 0.2F,
                        rand.nextGaussian() * f3);
                world.addFreshEntity(entityitem);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
