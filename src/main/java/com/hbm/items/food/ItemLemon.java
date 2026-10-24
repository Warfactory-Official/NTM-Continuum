package com.hbm.items.food;

import java.util.List;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

/** The plain 1.12.2 {@code ItemLemon} food family. */
public class ItemLemon extends CompatItem {

    private final String id;

    public ItemLemon(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        switch (id) {
            case "lemon" -> tooltip.add(Component.literal("Eh, good enough."));
            case "med_ipecac", "med_ptsd" -> {
                Language language = Language.getInstance();
                String base = getDescriptionId() + ".desc.";
                for (int i = 0; language.has(base + i); i++) tooltip.add(Component.translatable(base + i));
            }
            case "med_schizophrenia" -> {
                tooltip.add(Component.literal("Makes the voices go away. Just for a while."));
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("..."));
                tooltip.add(Component.literal("Better not take it."));
            }
            case "loops" -> tooltip.add(Component.literal("Br\u00f8ther, may I have some l\u00f6\u00f6ps?"));
            case "loop_stew" -> tooltip.add(Component.literal("A very, very healthy breakfast."));
            case "twinkie" -> tooltip.add(Component.literal("Expired 600 years ago!"));
            case "pudding" -> {
                tooltip.add(Component.literal("What if he did?"));
                tooltip.add(Component.literal("What if he didn't?"));
                tooltip.add(Component.literal("What if the world was made of pudding?"));
            }
            case "ingot_semtex" -> {
                tooltip.add(Component.literal("Semtex H Plastic Explosive"));
                tooltip.add(Component.literal("Performant explosive for many applications."));
                tooltip.add(Component.literal("Edible"));
            }
            case "marshmallow" -> tooltip.add(Component.literal("Gets grilled in the heat of burning nuclear failure"));
            case "marshmallow_roasted" -> tooltip.add(Component.literal("Hmmm... tastes a bit metallic"));
            default -> {
            }
        }
    }

    /** 1.7 ItemFood: the eat animation, sound and crumbs. */
    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        // The old override returned a bowl after eating; this fallback also covers pre-26.1 targets where
        // Item.Properties has no use-time remainder component.
        return id.equals("loop_stew") ? Items.BOWL.getDefaultInstance() : result;
    }

}
