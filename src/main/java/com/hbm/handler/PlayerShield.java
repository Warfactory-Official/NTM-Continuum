package com.hbm.handler;

import com.hbm.extprop.HbmPlayerProps;
import dev.golem.core.hook.Hook;
import net.minecraft.world.entity.player.Player;

/** Player shield absorption, before armour-module damage handling. */
public final class PlayerShield {

    private PlayerShield() {}

    @FunctionalInterface
    public interface Absorber {
        float absorb(Player player, float amount);
    }

    /** Supplemental pools consume the damage left by the ordinary shield, in registration order. */
    public static final Hook<Absorber> AFTER_ABSORB = Hook.of(listeners -> (player, amount) -> {
        for (var absorber : listeners) amount = absorber.absorb(player, amount);
        return amount;
    });

    public static float absorb(Player player, float amount) {
        var props = HbmPlayerProps.getData(player);
        if (props.shield > 0F) {
            float reduction = Math.min(props.shield, amount);
            props.shield -= reduction;
            amount -= reduction;
        }
        props.lastDamage = player.tickCount;
        return AFTER_ABSORB.invoker().absorb(player, amount);
    }
}
