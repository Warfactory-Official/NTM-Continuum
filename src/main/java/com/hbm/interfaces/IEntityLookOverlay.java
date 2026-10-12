package com.hbm.interfaces;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * 1.7's {@code ILookOverlay} on an entity: what is shown beside the crosshair while the player looks at it (1.7
 * {@code ModEventHandlerClient} asked {@code mop.entityHit instanceof ILookOverlay}; the port's dispatcher had only the
 * block half). No HBM entity implements it; NTM: Space's rideable rocket does (ADDON-API-INVENTORY C-20).
 */
public interface IEntityLookOverlay {

    /** The readout for {@code player} looking at this entity, or null for none. Client side. */
    ILookOverlay.@Nullable Data getLookOverlay(Player player);

    /** The readout of {@code entity}, if it has one. */
    static ILookOverlay.@Nullable Data of(@Nullable Entity entity, Player player) {
        return entity instanceof IEntityLookOverlay overlay ? overlay.getLookOverlay(player) : null;
    }
}
