package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ItemNuclearWaste;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** RBMK recycling pellet; ITEM_VARIANT retains the old ten damage states. */
public class ItemRBMKPellet extends ItemNuclearWaste {
    public final String fullName;
    private boolean xenon = true;
    public ItemRBMKPellet(Properties properties, String fullName) { super(properties); this.fullName = fullName; }
    public static int rectify(int meta) { return Math.abs(meta) % 10; }
    public static boolean hasXenon(int meta) { return rectify(meta) >= 5; }
    public ItemRBMKPellet disableXenon() { xenon = false; return this; }
    public boolean isXenonEnabled() { return xenon; }
    public int getVariant(ItemStack stack) { return ModDataComponents.ITEM_VARIANT.getOr(stack, 0); }
    public ItemStack of(int meta) { ItemStack stack = new ItemStack(this); ModDataComponents.ITEM_VARIANT.set(stack, meta); return stack; }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal(fullName).withStyle(ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("trait.rbmk.pellet.recycling").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        int meta = rectify(getVariant(stack));
        switch (meta % 5) {
            case 0 -> tooltip.add(Component.translatable("trait.rbmk.pellet.brandNew").withStyle(ChatFormatting.GOLD));
            case 1 -> tooltip.add(Component.translatable("trait.rbmk.pellet.barelyDepleted").withStyle(ChatFormatting.YELLOW));
            case 2 -> tooltip.add(Component.translatable("trait.rbmk.pellet.moderatelyDepleted").withStyle(ChatFormatting.GREEN));
            case 3 -> tooltip.add(Component.translatable("trait.rbmk.pellet.highlyDepleted").withStyle(ChatFormatting.DARK_GREEN));
            case 4 -> tooltip.add(Component.translatable("trait.rbmk.pellet.fullyDepleted").withStyle(ChatFormatting.DARK_GRAY));
            default -> throw new IllegalStateException("Unexpected RBMK pellet depletion stage");
        }
        if (hasXenon(meta)) tooltip.add(Component.translatable("trait.rbmk.pellet.xenon").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
