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
import net.minecraft.world.phys.Vec3;

/** Vectorized jetpack: capped look-direction acceleration, three-tick fuel cadence. */
public class JetpackVectorized extends JetpackFueledBase {

    public JetpackVectorized(com.hbm.inventory.fluid.FluidType fuel, int maxFuel, Properties properties) {
        super(fuel, maxFuel, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("High-mobility jetpack."));
        tooltip.add(Component.literal("Higher fuel consumption."));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    protected void tickFlight(Level level, Player player, ItemStack stack) {
        if (getFuel(stack) <= 0 || !isActive(player)) return;
        JetpackHandler.resetFlightTime(player);
        Vec3 motion = player.getDeltaMovement();
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
