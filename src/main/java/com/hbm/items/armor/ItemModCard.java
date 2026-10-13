package com.hbm.items.armor;

import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModArmorModItems;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Sounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModCard}: the Ace of Spades (33% chance a gun does not consume ammo, read by
 * {@code IMagazine.shouldUseUpTrenchie}) and the Queen of Spades (33% chance to tank a hit outright).
 * Upstream hid both from the creative tab; they came from the red room and the pedestal only.
 */
public class ItemModCard extends ItemArmorMod {

    private final boolean queen;

    public ItemModCard(boolean queen, Item.Properties properties) {
        super(ArmorModHandler.helmet_only, true, true, false, false, properties);
        this.queen = queen;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (!queen) {
            tooltip.add(coloured(ChatFormatting.RED, "Top of the line!"));
            tooltip.add(coloured(ChatFormatting.RED, "Guns now have a 33% chance to not consume ammo."));
        } else {
            tooltip.add(coloured(ChatFormatting.RED, "Power!"));
            tooltip.add(coloured(ChatFormatting.RED, "Adds a 33% chance to tank damage with no cap."));
        }
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        // Upstream's card line has no two-space indent, unlike every other module.
        tooltip.add(stack.getHoverName().copy().withStyle(ChatFormatting.RED));
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        if (queen && event.entity().getRandom().nextInt(3) == 0 && event.entity() instanceof Player player) {
            ArmorModEvents.plink(player, Sounds.minecraft("entity.item.break"), 0.5F,
                    1.0F + player.getRandom().nextFloat() * 0.5F);
            event.setAmount(0);
            event.cancel();
        }
    }

    /** 1.7 {@code ArmorTrenchmaster.hasAoS}: an Ace of Spades in the helmet's helmet-only slot. */
    public static boolean hasAoS(Player player) {
        if (player == null) return false;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return false;
        ItemStack mod = ArmorModHandler.pryMod(helmet, ArmorModHandler.helmet_only);
        return mod.getItem() == ModArmorModItems.card_aos.get();
    }
}
