package com.hbm.items.gear;

import com.hbm.api.environment.Environment;
import com.hbm.handler.JetpackHandler;
import com.hbm.inventory.fluid.FluidType;
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

    public JetpackRegular(FluidType fuel, int maxFuel, Properties properties) {
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
        // hbm-space: thrust scaled up (never down) with gravity, and in free fall the jet pushes along the view
        // (com.hbm.api.environment.Environment.gravity, 1 on plain HBM: 1.7's +0.1 below 0.4)
        double gravity = Environment.gravity(player);
        if (gravity == 0D) {
            player.setDeltaMovement(player.getDeltaMovement().add(player.getViewVector(1.0F).scale(0.05D)));
        } else if (player.getDeltaMovement().y < 0.4D) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.1D * Math.max(gravity, 1D), 0));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLAMETHROWER_SHOOT.get(),
                SoundSource.PLAYERS, 0.25F, 1.5F);
        if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_REGULAR);
        useUpFuel(player, stack, 5);
    }
}
