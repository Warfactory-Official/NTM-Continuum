package com.hbm.items.armor;

import com.hbm.api.environment.Environment;
import com.hbm.handler.JetpackHandler;
import com.hbm.handler.ArmorModEvents;
import com.hbm.items.ModArmorItems;
import com.hbm.sound.ModSounds;
import com.hbm.lib.Library;
import com.hbm.util.TickPhase;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Sounds;
import dev.golem.core.hook.Hook;
import java.util.function.Predicate;
import net.minecraft.world.damagesource.DamageSource;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** DNS armor, including sprint amplification, powered jetpack and glider assist. */
public class ArmorDNT extends ArmorFSBPowered {

    private static final String SPEED_MODIFIER = "hbm:dns_speed";

    /** Damage that the suit cancels silently; addons may identify their own environmental sources. */
    public static final Hook<Predicate<DamageSource>> SILENT_ATTACK = Hook.of(listeners -> source -> {
        for (var predicate : listeners) if (predicate.test(source)) return true;
        return false;
    });

    public ArmorDNT(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                    long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }

    // 1.7 replaces the full-set lines here: always shown, one effect per line.
    @Override
    public void addInformation(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.literal("Charge: " + Library.getShortNumber(getCharge(stack)) + " / "
                + Library.getShortNumber(getMaxCharge(stack))));
        tooltip.add(Component.translatable("armor.fullSetBonus").withStyle(ChatFormatting.GOLD));
        for (MobEffectInstance effect : effects) tooltip.add(line(ChatFormatting.AQUA, effect.getDescriptionId()));
        tooltip.add(line(ChatFormatting.RED, "armor.vats"));
        tooltip.add(line(ChatFormatting.RED, "armor.thermal"));
        tooltip.add(line(ChatFormatting.RED, "armor.hardLanding"));
        tooltip.add(line(ChatFormatting.AQUA, "armor.rocketBoots"));
        tooltip.add(line(ChatFormatting.AQUA, "armor.fastFall"));
        tooltip.add(line(ChatFormatting.AQUA, "armor.sprintBoost"));
    }

    private static Component line(ChatFormatting colour, String key) {
        return Component.literal("  ").append(Component.translatable(key)).withStyle(colour);
    }

    /** Called once per player tick so removing DNS cannot leave its transient speed modifier behind. */
    public static void updateSprintSpeed(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() == ModArmorItems.dns_plate.get() && player.isSprinting()) {
            Attributes.replaceTransient(player.getAttribute(
                    net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), SPEED_MODIFIER, 0.25D);
        } else {
            Attributes.removeTransient(player.getAttribute(
                    net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), SPEED_MODIFIER);
        }
    }

    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        super.armorTick(level, player, stack);
        if (this != ModArmorItems.dns_plate.get() || !ArmorFSB.hasFSBArmor(player)) return;
        JetpackHandler.resetFlightTime(player);

        boolean thrusting = JetpackHandler.isJetpackActive(player);
        if (thrusting) {
            double y = player.getDeltaMovement().y;
            if (y < 0.6D) y += 0.2D;
            player.setDeltaMovement(player.getDeltaMovement().x, y, player.getDeltaMovement().z);
            player.fallDistance = 0;
            if (TickPhase.every(player, 4)) playThrustSound(level, player);
            if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_DNS);
        } else {
            boolean backpack = JetpackHandler.isBackpackEnabled(player);
            float forward = JetpackHandler.getForwardInput(player);
            float strafe = JetpackHandler.getStrafeInput(player);
            float gravity = (float) Environment.gravity(player);
            player.setDeltaMovement(SuitFlight.dnsGlide(player, player.getDeltaMovement(), backpack,
                    forward, strafe, gravity));
            if (SuitFlight.shows(player.isShiftKeyDown(), player.onGround(), backpack,
                    forward != 0F || strafe != 0F, player.isSprinting(), gravity)) {
                if (TickPhase.every(player, 4)) playThrustSound(level, player);
                if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_DNS);
            }
        }
    }

    private static void playThrustSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.IMMOLATOR_SHOOT.get(), SoundSource.PLAYERS,
                0.125F, 1.5F);
    }

    @Override
    public void handleAttack(DamageEvents.DamageEvent event) {
        if (event.entity() instanceof Player player && ArmorFSB.hasFSBArmor(player)
                && !event.source().is(DamageTypeTags.IS_EXPLOSION)) {
            if (event.amount() > 0F && !SILENT_ATTACK.invoker().test(event.source())) {
                ArmorModEvents.plink(player, Sounds.minecraft("entity.item.break"), 0.5F,
                        1F + player.getRandom().nextFloat() * 0.5F);
            }
            event.cancel();
        }
    }

    @Override
    public void handleHurt(DamageEvents.DamageEvent event) {
        if (!(event.entity() instanceof Player player) || !ArmorFSB.hasFSBArmor(player)) return;
        if (event.source().is(DamageTypeTags.IS_EXPLOSION)) event.setAmount(event.amount() * 0.001F);
        else event.setAmount(0F);
    }
}
