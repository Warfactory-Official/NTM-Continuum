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
import net.minecraft.world.phys.Vec3;

/** High-powered vectorized jetpack: the five-speed-cap, one-tick fuel tier. */
public class JetpackBooster extends JetpackFueledBase {

    public JetpackBooster(FluidType fuel, int maxFuel, Properties properties) {
        super(fuel, maxFuel, properties);
    }

    @Override
    public void addInformation(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.literal("High-powered vectorized jetpack."));
        tooltip.add(Component.literal("Highly increased fuel consumption."));
        super.addInformation(stack, tooltip);
    }

    @Override
    protected void tickFlight(Level level, Player player, ItemStack stack) {
        if (getFuel(stack) <= 0 || !isActive(player)) return;
        Vec3 motion = player.getDeltaMovement();
        // hbm-space: the lift shrinks (never grows) with gravity (com.hbm.api.environment.Environment.gravity)
        if (motion.y < 0.6D) motion = motion.add(0, 0.1D * Math.min(Environment.gravity(player), 1D), 0);
        Vec3 look = player.getViewVector(1.0F);
        if (motion.length() < 5D) {
            motion = motion.add(look.scale(0.25D));
            if (look.y > 0) player.fallDistance = 0;
        }
        player.setDeltaMovement(motion);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLAMETHROWER_SHOOT.get(),
                SoundSource.PLAYERS, 0.25F, 1.0F);
        if (!level.isClientSide()) JetpackHandler.sendExhaust(player, JetpackHandler.EXHAUST_VECTOR);
        useUpFuel(player, stack, 1);
    }
}
