package com.hbm.items.armor;

import com.hbm.handler.ArmorModEvents;
import com.hbm.items.ModArmorSetItems;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.GolemArmorMaterial;
import dev.golem.core.world.Sounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ArmorTrenchmaster}: the red-room loot suit.
 *
 * <p>Besides the FSB bonus it (a) never wears ({@code setMaxDamage(0)}, registered unbreakable), (b) makes
 * the wearer immune to his own explosions, (c) shrugs off one attack in three with a break "plink", and
 * (d) is read by the gun code: {@link #isTrenchMaster} doubles reload ticking and halves reload animation
 * time, and with the Ace of Spades ({@link #hasAoS}) makes a round cost nothing one time in three.
 *
 * <p>Upstream's tooltip still carries the commented-out "Faster Reload" line; only "More Ammo" is shown.
 */
public class ArmorTrenchmaster extends ArmorFSB {

    public ArmorTrenchmaster(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties);
    }

    @Override
    public void addInformation(ItemStack stack, List<Component> tooltip) {
        super.addInformation(stack, tooltip);
        tooltip.add(Component.literal("  ").append(Component.translatable("armor.moreAmmo"))
                .withStyle(ChatFormatting.RED));
    }

    @Override
    public void handleHurt(DamageEvents.DamageEvent event) {
        if (!(event.entity() instanceof Player player) || !ArmorFSB.hasFSBArmor(player)) return;
        // 1.7 source.getSourceOfDamage() == player on an explosion: the wearer's own blast. A modern
        // explosion names its culprit as the causing entity and the exploding thing as the direct one.
        if (event.source().is(DamageTypeTags.IS_EXPLOSION)
                && (event.source().getEntity() == player || event.source().getDirectEntity() == player)) {
            event.setAmount(0F);
        }
    }

    @Override
    public void handleAttack(DamageEvents.DamageEvent event) {
        if (!(event.entity() instanceof Player player) || !ArmorFSB.hasFSBArmor(player)) return;
        if (player.getRandom().nextInt(3) == 0) {
            ArmorModEvents.plink(player, Sounds.minecraft("entity.item.break"), 0.5F,
                    1.0F + player.getRandom().nextFloat() * 0.5F);
            event.cancel();
        }
    }

    /** 1.7 {@code isTrenchMaster}: the trenchmaster chestplate as part of a complete, working set. */
    public static boolean isTrenchMaster(Player player) {
        if (player == null) return false;
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModArmorSetItems.trenchmaster_plate.get())
                && ArmorFSB.hasFSBArmor(player);
    }

    /** 1.7 {@code hasAoS}: an Ace of Spades in the helmet's helmet-only module slot. */
    public static boolean hasAoS(Player player) {
        return ItemModCard.hasAoS(player);
    }
}
