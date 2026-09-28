package com.hbm.items.special;

import com.hbm.main.MainRegistry;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemPolaroid}: the per-launch "fate" picture. Anywhere in the inventory it holds Resistance III
 * on a player below 10 health; its tooltip and icon ({@code polaroid_<id>}, chosen by the item model's
 * {@code hbm:polaroid} property) follow {@link MainRegistry#polaroidID}, which client and server each roll on
 * their own exactly as 1.7's two proxies did.
 */
public class ItemPolaroid extends CompatItem {

    public ItemPolaroid(Properties properties) {
        super(properties);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot,
                                       boolean held) {
        if (entity instanceof Player player && player.getHealth() < 10F) {
            Effects.add(player, Effects.resistance(), 10, 2);
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Fate chosen"));
        list.add(Component.literal(""));
        switch (MainRegistry.polaroidID) {
            case 1 -> list.add(Component.literal("..."));
            case 2 -> list.add(Component.literal("Clear as glass."));
            case 3 -> list.add(Component.literal("'M"));
            case 4 -> list.add(Component.literal("It's about time."));
            case 5 -> list.add(Component.literal("If you stare long into the abyss, the abyss stares back."));
            case 6 -> list.add(Component.literal("public Party celebration = new Party();"));
            case 7 -> list.add(Component.literal("V urnerq lbh yvxr EBG13!"));
            case 8 -> list.add(Component.literal("11011100"));
            case 9 -> list.add(Component.literal("Vg'f nobhg gvzr."));
            case 10 -> list.add(Component.literal("Schrabidium dislikes the breeding reactor."));
            case 11 -> list.add(Component.literal("yss stares back.6public Party cel"));
            case 12 -> list.add(Component.literal("Red streaks."));
            case 13 -> list.add(Component.literal("Q1"));
            case 14 -> list.add(Component.literal("Q4"));
            case 15 -> list.add(Component.literal("Q3"));
            case 16 -> list.add(Component.literal("Q2"));
            case 17 -> list.add(Component.literal("Two friends before christmas."));
            case 18 -> {
                list.add(Component.literal("Duchess of the boxcars."));
                list.add(Component.literal(""));
                list.add(Component.literal("\"P.S.: Thirty-one.\""));
                list.add(Component.literal("\"Huh, what does thirty-one mean?\""));
            }
            default -> { }
        }
    }
}
