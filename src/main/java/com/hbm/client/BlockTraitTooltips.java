package com.hbm.client;

import com.hbm.Constants;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.lib.HbmBlockTags;
import dev.golem.core.client.Tooltips;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** 1.12.2's block lines: "[Radiation Shielding]" and, above 50, the blast resistance (1.7 setResistance x 0.6). */
public final class BlockTraitTooltips {

    private BlockTraitTooltips() {}

    public static void register() {
        Tooltips.onItemTooltip(BlockTraitTooltips::addLines);
    }

    private static void addLines(ItemStack stack, List<Component> lines) {
        if (!(stack.getItem() instanceof BlockItem item)) return;
        Block block = item.getBlock();
        if (block instanceof IRadResistantBlock shield && shield.isRadShielding()
                || block.defaultBlockState().is(HbmBlockTags.RADIATION_RESISTANT)) {
            lines.add(Component.literal("[").append(Component.translatable("trait.radshield")).append("]")
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        float resistance = block.getExplosionResistance();
        if (resistance > 50F && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Constants.MOD_ID)) {
            lines.add(Component.translatable("trait.blastres", String.valueOf(resistance)).withStyle(ChatFormatting.GOLD));
        }
    }
}
