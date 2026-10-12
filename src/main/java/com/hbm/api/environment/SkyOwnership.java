package com.hbm.api.environment;

import dev.golem.core.hook.Hook;
import net.minecraft.world.level.Level;

/**
 * Which dimensions' skies another mod draws itself, so HBM's own sky layers stay off them. HBM draws 1.7's overworld
 * sky objects (the chainloader's lode star, digamma star and Bobmazon satellite, and the impact sky that replaces the sun,
 * moon and stars while Tom's dust or fire lasts: {@code client/render/world/NTMSkyboxes}). An addon that gives the
 * overworld a sky of its own (NTM: Space's Kerbin sky; hbm-space drew none of HBM's objects there) claims it here, and
 * HBM then draws nothing on it. Unclaimed, HBM draws as 1.7 did. Only the drawing is affected: Tom's sky darkening and
 * fog colour ({@code ImpactSky}) still apply.
 */
public final class SkyOwnership {

    private SkyOwnership() {}

    /** A listener's answer: whether it draws {@code level}'s sky itself. */
    @FunctionalInterface
    public interface Owner {
        boolean ownsSky(Level level);
    }

    /** Sky owners; any one that owns a level's sky takes it from HBM. Register at mod construction (either side). */
    public static final Hook<Owner> OWNERS = Hook.of(listeners -> level -> {
        for (Owner owner : listeners) if (owner.ownsSky(level)) return true;
        return false;
    });

    /** Whether another mod draws {@code level}'s sky, so HBM's sky layers skip it. */
    public static boolean claimed(Level level) {
        return OWNERS.invoker().ownsSky(level);
    }
}
