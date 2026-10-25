package com.hbm.items.armor;

import com.hbm.api.environment.Environment;
import com.hbm.handler.JetpackHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Vectorized jetpack: capped look-direction acceleration, three-tick fuel cadence. */
public class JetpackVectorized extends JetpackFueledBase {

    public JetpackVectorized(FluidType fuel, int maxFuel, Properties properties) {
        super(fuel, maxFuel, properties);
    }

    @Override
    public void addInformation(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.literal("High-mobility jetpack."));
        tooltip.add(Component.literal("Higher fuel consumption."));
        super.addInformation(stack, tooltip);
    }

    @Override
    protected void tickFlight(Level level, Player player, ItemStack stack) {
        if (getFuel(stack) <= 0 || !isActive(player)) return;
        JetpackHandler.resetFlightTime(player);
        Vec3 motion = player.getDeltaMovement();
        // 1.7 lifts by 0.1 below 0.4 before the vectored push (missing from the port until now); hbm-space shrinks the
        // lift (never grows it) with gravity (com.hbm.api.environment.Environment.gravity)
        if (motion.y < 0.4D) {
            motion = motion.add(0, 0.1D * Math.min(Environment.gravity(player), 1D), 0);
            player.setDeltaMovement(motion);
        }
        if (motion.length() < 2D) {
            Vec3 look = player.getViewVector(1.0F);
            player.setDeltaMovement(motion.add(look.scale(0.1D)));
            if (look.y > 0) player.fallDistance = 0;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLAMETHROWER_SHOOT.get(),
                SoundSource.PLAYERS, 0.25F, 1.5F);
        if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_VECTOR);
        useUpFuel(player, stack, 3);
    }
}
