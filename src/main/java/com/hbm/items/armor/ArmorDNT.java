package com.hbm.items.armor;

import com.hbm.handler.JetpackHandler;
import com.hbm.items.ModArmorItems;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.gear.ArmorFSBPowered;
import com.hbm.util.TickPhase;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** DNS armor, including sprint amplification, powered jetpack and glider assist. */
public class ArmorDNT extends ArmorFSBPowered {

    private static final String SPEED_MODIFIER = "hbm:dns_speed";

    public ArmorDNT(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                    long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("  + ").append(Component.translatable("armor.rocketBoots")));
        tooltip.add(Component.literal("  + ").append(Component.translatable("armor.fastFall")));
        tooltip.add(Component.literal("  + ").append(Component.translatable("armor.sprintBoost")));
    }

    /** Called once per player tick so removing DNS cannot leave its transient speed modifier behind. */
    public static void updateSprintSpeed(Player player) {
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
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
        boolean gliding = !thrusting && JetpackHandler.isBackpackEnabled(player)
                && !player.isShiftKeyDown() && !player.onGround();
        if (thrusting) {
            double y = player.getDeltaMovement().y;
            if (y < 0.6D) y += 0.2D;
            player.setDeltaMovement(player.getDeltaMovement().x, y, player.getDeltaMovement().z);
            player.fallDistance = 0;
            if (TickPhase.every(player, 4)) playThrustSound(level, player);
            if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_DNS);
        } else if (gliding) {
            player.fallDistance = 0;
            var motion = player.getDeltaMovement();
            double y = motion.y;
            if (y < -1D) y += 0.4D;
            else if (y < -0.1D) y += 0.2D;
            else if (y < 0D) y = 0D;
            motion = new net.minecraft.world.phys.Vec3(motion.x * 1.05D, y, motion.z * 1.05D);
            float forward = JetpackHandler.getForwardInput(player);
            if (forward != 0F) {
                var look = player.getViewVector(1.0F);
                motion = motion.add(look.x * 0.25D * forward, 0, look.z * 0.25D * forward);
            }
            player.setDeltaMovement(motion);
            if (TickPhase.every(player, 4)) playThrustSound(level, player);
            if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_DNS);
        }
        if (player.isShiftKeyDown() && !player.onGround()) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, -0.1D, 0));
        }
    }

    private static void playThrustSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                com.hbm.sound.ModSounds.IMMOLATOR_SHOOT.get(), net.minecraft.sounds.SoundSource.PLAYERS,
                0.125F, 1.5F);
    }

    @Override
    public void handleAttack(DamageEvents.DamageEvent event) {
        if (event.entity() instanceof Player player && ArmorFSB.hasFSBArmor(player)
                && !event.source().is(DamageTypeTags.IS_EXPLOSION)) event.cancel();
    }

    @Override
    public void handleHurt(DamageEvents.DamageEvent event) {
        if (!(event.entity() instanceof Player player) || !ArmorFSB.hasFSBArmor(player)) return;
        if (event.source().is(DamageTypeTags.IS_EXPLOSION)) event.setAmount(event.amount() * 0.001F);
        else event.setAmount(0F);
    }
}
