package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** Block item descriptions for the fixed MK5 nuclear bomb family. */
public class ItemNukeBlock extends CompatBlockItem {

    public enum Type {
        FAT_MAN,
        N2,
        LITTLE_BOY,
        GADGET,
        TSAR,
        IVY_MIKE
    }

    private final Type type;

    public ItemNukeBlock(Block block, Item.Properties properties, Type type) {
        super(block, properties);
        this.type = type;
    }

    /** The configured radius printed by the old block-item description. */
    public static int baseRadius(Type type) {
        return switch (type) {
            case FAT_MAN -> BombConfig.manRadius;
            case N2 -> BombConfig.n2Radius;
            case LITTLE_BOY -> BombConfig.boyRadius;
            case GADGET -> BombConfig.gadgetRadius;
            case TSAR -> BombConfig.tsarRadius;
            case IVY_MIKE -> BombConfig.mikeRadius;
        };
    }

    /** Retains the old integer division in the fallout-radius expression. */
    public static int falloutRadius(int radius) {
        return radius * (1 + BombConfig.falloutRange / 100);
    }

    /** N2's old per-charge display value, also integer arithmetic. */
    public static int n2ChargeRadius() {
        return BombConfig.n2Radius / 12;
    }

    public static boolean hasFallout(Type type) {
        return type != Type.N2;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (type == Type.N2) {
            addHeading(tooltip, "trait.extremebomb", ChatFormatting.RED);
            addRadius(tooltip, baseRadius(type));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal(" ")
                    .append(Component.translatable("desc.chargeadds", n2ChargeRadius()))
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }

        ChatFormatting headingColor = type == Type.TSAR || type == Type.IVY_MIKE
                ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN;
        String heading = type == Type.TSAR || type == Type.IVY_MIKE
                ? "trait.thermobomb" : "trait.nuclearbomb";
        int radius = baseRadius(type);
        addHeading(tooltip, heading, headingColor);
        addRadius(tooltip, radius);
        if (hasFallout(type) && !BombConfig.disableNuclear) {
            addHeading(tooltip, "trait.fallout", ChatFormatting.DARK_GREEN);
            addRadius(tooltip, falloutRadius(radius));
        }
    }

    private static void addHeading(List<Component> tooltip, String key, ChatFormatting color) {
        tooltip.add(Component.literal("[").append(Component.translatable(key))
                .append(Component.literal("]")).withStyle(color));
    }

    private static void addRadius(List<Component> tooltip, int radius) {
        tooltip.add(Component.literal(" ").append(Component.translatable("desc.radius", radius))
                .withStyle(ChatFormatting.YELLOW));
    }
}
