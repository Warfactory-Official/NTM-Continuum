package com.hbm.items.block;

import com.hbm.tileentity.machine.storage.CrateType;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;


/**
 * Block item for the storage crates. Adds the "{@code X/Y Slots used (Z%)}" tooltip the 1.12.2
 * {@code BlockStorageCrate.addInformation} showed, reading the stored inventory from the item's
 * {@code minecraft:container} component (which the crate carries its contents in — see
 * {@link com.hbm.tileentity.machine.storage.TileEntityCrate}). Colour-coded like the original:
 * green &lt;25 %, yellow in the middle, red ≥75 %.
 */
public class ItemBlockStorageCrate extends CompatBlockItem {

    private final CrateType crateType;

    public ItemBlockStorageCrate(Block block, Properties properties, CrateType crateType) {
        super(block, properties);
        this.crateType = crateType;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         java.util.List<Component> lines, TooltipFlag flag) {

        int total = crateType.slots;
        int used = contents(stack);

        float percent = Math.round(used * 1000.0F / total) / 10.0F; // one decimal, like Library.roundFloat(..,1)
        ChatFormatting color = percent >= 75 ? ChatFormatting.RED
                : percent < 25 ? ChatFormatting.GREEN : ChatFormatting.YELLOW;

        lines.add(Component.literal(used + "/" + total + " Slots used (" + percent + "%)").withStyle(color));
    }

    private int contents(ItemStack stack) {
        return Compat.containerItemCount(stack, crateType.slots);
    }
}
