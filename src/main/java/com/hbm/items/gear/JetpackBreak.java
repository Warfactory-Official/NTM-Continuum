package com.hbm.items.gear;

import com.hbm.api.environment.Environment;
import com.hbm.handler.JetpackHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.armor.JetpackFueledBase;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Regular jetpack with the old automatic airborne hover assist. */
public class JetpackBreak extends JetpackFueledBase {

    public JetpackBreak(FluidType fuel, int maxFuel, Properties properties) {
        super(fuel, maxFuel, properties);
    }

    @Override
    public void addInformation(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.literal("Regular jetpack that will automatically hover mid-air."));
        tooltip.add(Component.literal("Sneaking will stop hover mode."));
        tooltip.add(Component.literal("Hover mode will consume less fuel and increase air-mobility."));
        super.addInformation(stack, tooltip);
    }

    @Override
    protected void tickFlight(Level level, Player player, ItemStack stack) {
        if (getFuel(stack) <= 0) return;
        JetpackHandler.resetFlightTime(player);
        // hbm-space: thrust scaled up (never down) with gravity, the jet pushing along the view in free fall, and no
        // hovering without gravity (com.hbm.api.environment.Environment.gravity, 1 on plain HBM)
        double gravity = Environment.gravity(player);
        if (isActive(player)) {
            player.fallDistance = 0;
            if (gravity == 0D) {
                player.setDeltaMovement(player.getDeltaMovement().add(player.getViewVector(1.0F).scale(0.05D)));
            } else if (player.getDeltaMovement().y < 0.4D) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, 0.1D * Math.max(gravity, 1D), 0));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLAMETHROWER_SHOOT.get(),
                    SoundSource.PLAYERS, 0.25F, 1.5F);
            if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_REGULAR);
            useUpFuel(player, stack, 5);
        } else if (JetpackHandler.isBackpackEnabled(player) && !player.isShiftKeyDown() && !player.onGround() && gravity > 0D) {
            player.fallDistance = 0;
            double thrust = Math.max(gravity, 1D);
            double y = player.getDeltaMovement().y;
            if (y < -1D * thrust) y += 0.2D * thrust;
            else if (y < -0.1D * thrust) y += 0.1D * thrust;
            else if (y < 0D) y = 0D;
            player.setDeltaMovement(player.getDeltaMovement().x, y, player.getDeltaMovement().z);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLAMETHROWER_SHOOT.get(),
                    SoundSource.PLAYERS, 0.25F, 1.5F);
            if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_REGULAR);
            useUpFuel(player, stack, 10);
        }
    }
}
