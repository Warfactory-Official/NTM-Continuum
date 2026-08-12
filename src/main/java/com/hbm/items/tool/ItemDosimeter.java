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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ContaminationUtil.printDosimeterData(player);
        }
        return InteractionResult.SUCCESS;
    }
}
