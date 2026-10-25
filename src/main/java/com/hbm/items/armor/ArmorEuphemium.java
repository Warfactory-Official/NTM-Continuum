package com.hbm.items.armor;

import com.hbm.items.ModArmorSetItems;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Effects;
import dev.golem.core.world.GolemArmorMaterial;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ArmorEuphemium}: the creative-only god suit.
 *
 * <p>Upstream is a bare {@code ItemArmor}, not an {@code ArmorFSB}; it extends {@link ArmorFSB} here only
 * because the port's per-piece armour tick ({@code EntityEffectHandler.handleArmor}) dispatches through that
 * class; the old {@code onArmorTick} hook does not exist. No FSB configuration is declared, so the base
 * class contributes no effects, sounds or tooltip bonus lines.
 *
 * <p>Behaviour matches upstream, including its oddities: every worn piece runs the check, so a full
 * set re-applies the four level-128 effects four times a tick (harmless), and the fall clamp only kicks in
 * past -0.25 blocks/tick. Upstream's no-op {@code setDamage} is the unbreakable property the items are
 * registered with; the items sit in no creative tab ({@code setCreativeTab(null)}), reachable through the
 * euphemium kit.
 */
public class ArmorEuphemium extends ArmorFSB {

    public ArmorEuphemium(GolemArmorMaterial material, ArmorSlot slot, Properties properties) {
        super(material, slot, properties);
    }

    /** 1.7 {@code ArmorUtil.checkArmor} over the four exact euphemium items. */
    public static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModArmorSetItems.euphemium_helmet.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModArmorSetItems.euphemium_plate.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModArmorSetItems.euphemium_legs.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModArmorSetItems.euphemium_boots.get());
    }

    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        if (!hasFullSet(player)) return;
        // PotionEffect(id, 5, 127, true): ambient, and 1.7 always showed the icon.
        player.addEffect(Effects.instance(MobEffects.REGENERATION, 5, 127, true, true, true));
        player.addEffect(Effects.instance(Effects.resistance(), 5, 127, true, true, true));
        player.addEffect(Effects.instance(MobEffects.FIRE_RESISTANCE, 5, 127, true, true, true));
        // field_76443_y is 1.7's saturation.
        player.addEffect(Effects.instance(MobEffects.SATURATION, 5, 127, true, true, true));

        var motion = player.getDeltaMovement();
        if (motion.y < -0.25D) {
            player.setDeltaMovement(motion.x, -0.25D, motion.z);
            player.fallDistance = 0;
        }
    }
}
