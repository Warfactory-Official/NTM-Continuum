package com.hbm.items.gear;

import com.hbm.handler.JetpackHandler;
import com.hbm.items.armor.JetpackFueledBase;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Regular jetpack: simple upward momentum, five-tick fuel cadence. */
public class JetpackRegular extends JetpackFueledBase {

    public JetpackRegular(com.hbm.api.fluid.FluidType fuel, int maxFuel, Properties properties) {
        super(fuel, maxFuel, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Regular jetpack for simple upwards momentum."));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    protected void tickFlight(Level level, Player player, ItemStack stack) {
        if (getFuel(stack) <= 0 || !isActive(player)) return;
        JetpackHandler.resetFlightTime(player);
        player.fallDistance = 0;
        if (player.getDeltaMovement().y < 0.4D) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.1D, 0));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLAMETHROWER_SHOOT.get(),
                SoundSource.PLAYERS, 0.25F, 1.5F);
        if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_REGULAR);
        useUpFuel(player, stack, 5);
    }
}
