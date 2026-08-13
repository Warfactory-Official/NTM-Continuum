package com.hbm.items.tool;

import com.hbm.util.ContaminationUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Handheld dosimeter — a passive readout that prints the carrier's received RAD/s to chat on right-click
 * (no clicking sound). Ported from 1.12.2's {@code ItemDosimeter}.
 */
public class ItemDosimeter extends Item {

    public ItemDosimeter(Properties props) {
        super(props);
    }

    //? if >=26.1 {
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return hbm$use(level, player, hand);
    }
    //?} else {
    /*@Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, Player player, InteractionHand hand) {
        InteractionResult hbm$r = hbm$use(level, player, hand);
        net.minecraft.world.item.ItemStack hbm$held = player.getItemInHand(hand);
        return hbm$r == InteractionResult.SUCCESS
                ? net.minecraft.world.InteractionResultHolder.success(hbm$held)
                : hbm$r == InteractionResult.CONSUME
                ? net.minecraft.world.InteractionResultHolder.consume(hbm$held)
                : net.minecraft.world.InteractionResultHolder.pass(hbm$held);
    }
    *///?}

    private InteractionResult hbm$use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ContaminationUtil.printDosimeterData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
