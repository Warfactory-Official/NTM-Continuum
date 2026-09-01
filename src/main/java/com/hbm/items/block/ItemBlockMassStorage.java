package com.hbm.items.block;

import com.hbm.tileentity.machine.storage.MassStorageItemData;
import com.hbm.tileentity.machine.storage.MassStorageType;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

/** Block item for a mass-storage variant, including the old filter and stockpile tooltip. */
public class ItemBlockMassStorage extends CompatBlockItem {

    private final MassStorageType storageType;

    public ItemBlockMassStorage(Block block, Properties properties, MassStorageType storageType) {
        super(block, properties);
        this.storageType = storageType;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> lines, TooltipFlag flag) {
        ItemStack filter = MassStorageItemData.filter(stack);
        if (filter.isEmpty()) return;

        lines.add(filter.getHoverName().copy().withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal(String.format(Locale.US, "%,d / %,d",
                MassStorageItemData.stockpile(stack), storageType.capacity)));
    }
}
