package com.hbm.compat.happyghast;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;

/**
 * Fabric's entry to the happy-ghast Fat Man ({@link HbmHappyGhast}): a {@code main} entrypoint the build puts
 * <b>before</b> HBM's {@code FabricInit} in {@code fabric.mod.json}, only on nodes that set {@code feature.happy_ghast}.
 * Fabric registers everything in {@code FabricInit}'s one flush, so the entity type and the GameTest bodies must exist
 * by then; the 26.1 GameTest hook then finds this suite with every other.
 */
public final class HbmHappyGhastFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        HbmHappyGhast.init();
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            InteractionResult result = HbmHappyGhast.tryRig(player, level, hand, hit.getBlockPos());
            return result != null ? result : InteractionResult.PASS;
        });
    }
}
