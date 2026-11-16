package com.hbm.items.food;

import java.util.List;
import com.hbm.inventory.gui.GuiUtil;
import dev.golem.core.world.CompatItem;
import dev.golem.core.hook.Hook;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
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

    /** Runs after ipecac or PTSD medication is consumed on the server. */
    public static final Hook<Consumer<Player>> EMETIC_USED = Hook.notifying();

    private final String id;

    public ItemLemon(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        // 1.7 resolveKeyArray: one key split on $; ipecac and PTSD keep their numbered lines
        Language language = Language.getInstance();
        String key = getDescriptionId() + ".desc";
        if (language.has(key)) tooltip.addAll(GuiUtil.keyArray(key));
        for (int i = 0; language.has(key + "." + i); i++) tooltip.add(Component.translatable(key + "." + i));
    }

    /** 1.7 ItemFood: the eat animation, sound and crumbs. */
    @Override
    protected UseAnimation compatUseAnimation(ItemStack stack) {
        return UseAnimation.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if ((id.equals("med_ipecac") || id.equals("med_ptsd"))
                && level instanceof ServerLevel && entity instanceof Player player) {
            for (var listener : EMETIC_USED) listener.accept(player);
        }
        // The old override returned a bowl after eating; this fallback also covers pre-26.1 targets where
        // Item.Properties has no use-time remainder component.
        return id.equals("loop_stew") ? Items.BOWL.getDefaultInstance() : result;
    }

}
