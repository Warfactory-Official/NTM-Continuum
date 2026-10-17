package com.hbm.items.armor;

import com.hbm.handler.JetpackHandler;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** BJ chest variant with the original powered jetpack and sneaking glider behavior. */
public class ArmorBJJetpack extends ArmorBJ {

    public ArmorBJJetpack(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                          long maxPower, long chargeRate, long consumption, long drain) {
        super(material, slot, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    public void addInformation(ItemStack stack, List<Component> tooltip) {
        super.addInformation(stack, tooltip);
        tooltip.add(Component.literal("  + ").append(Component.translatable("armor.electricJetpack"))
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  + ").append(Component.translatable("armor.glider"))
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        super.armorTick(level, player, stack);
        if (!ArmorFSB.hasFSBArmor(player)) return;
        JetpackHandler.resetFlightTime(player);

        if (JetpackHandler.isJetpackActive(player)) {
            double y = player.getDeltaMovement().y;
            if (y < 0.4D) y += 0.1D;
            player.setDeltaMovement(player.getDeltaMovement().x, y, player.getDeltaMovement().z);
            player.fallDistance = 0;
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.IMMOLATOR_SHOOT.get(), SoundSource.PLAYERS, 0.125F, 1.5F);
            if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_BJ);
        } else if (player.isShiftKeyDown() && !player.onGround()) {
            var motion = player.getDeltaMovement();
            if (motion.y < -0.08D) {
                double amount = motion.y * -0.4D;
                var look = player.getViewVector(1.0F);
                player.setDeltaMovement(motion.add(0, amount, 0).add(look.scale(amount)));
            }
        }
    }
}
