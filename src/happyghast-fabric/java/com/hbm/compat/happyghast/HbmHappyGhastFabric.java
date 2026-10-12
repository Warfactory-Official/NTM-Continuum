package com.hbm.compat.happyghast;

import dev.golem.core.platform.ModEntry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

/**
 * Fabric's entry to the happy-ghast Fat Man ({@link HbmHappyGhast}): a {@code golemcore:entry} the build puts
 * <b>before</b> HBM's {@code FabricInit} in {@code fabric.mod.json}, only on nodes that set {@code feature.happy_ghast}.
 * Golem Core fills the registries once, after every entry's declarations, so the entity type and the GameTest bodies
 * declared here exist by then; the 26.1 GameTest hook then finds this suite with every other.
 */
public final class HbmHappyGhastFabric implements ModEntry {

    @Override
    public void declare() {
        HbmHappyGhast.init();
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            InteractionResult result = HbmHappyGhast.tryRig(player, level, hand, hit.getBlockPos());
            return result != null ? result : InteractionResult.PASS;
        });
    }
}
