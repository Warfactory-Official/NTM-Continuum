package com.hbm.blocks.bomb;

import com.hbm.explosion.BlastInfo;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** A bomb block item whose tooltip states its blast. */
public class ItemNukeBlock extends CompatBlockItem {

    private final BlastInfo blast;

    public ItemNukeBlock(Block block, Item.Properties properties, BlastInfo blast) {
        super(block, properties);
        this.blast = blast;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        blast.addTo(tooltip);
    }
}
