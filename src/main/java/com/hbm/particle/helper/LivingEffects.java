package com.hbm.particle.helper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;

/**
 * Server-side senders for original-hbm (1.7.10)'s per-entity {@code sweat} and {@code vomit} bursts
 * ({@code EntityEffectHandler}, {@code HbmLivingProps.setDigamma}, {@code EntityPigeon}). 1.7 sent an
 * {@code AuxParticlePacketNT} at (0, 0, 0) to everyone within {@code range} of the entity; the client reads the
 * entity id from the tag. Server-safe: no client types.
 */
public final class LivingEffects {

    private LivingEffects() {}

    /** 1.7 {@code sweat}: {@code count} dust particles of {@code block} over the entity's box. */
    public static void sweat(LivingEntity entity, int count, Block block, double range) {
        CompoundTag data = new CompoundTag();
        data.putInt("count", count);
        data.putInt("block", BuiltInRegistries.BLOCK.getId(block)); // runtime id, packet only
        data.putInt("entity", entity.getId());
        HbmEffectNT.Sweat.send(entity.level(), entity.getX(), entity.getY(), entity.getZ(), data, range);
    }

    /** 1.7 {@code vomit} within 25 blocks: mode {@code normal}, {@code blood} or {@code smoke}. */
    public static void vomit(LivingEntity entity, String mode, int count) {
        CompoundTag data = new CompoundTag();
        data.putString("mode", mode);
        data.putInt("count", count);
        data.putInt("entity", entity.getId());
        HbmEffectNT.Vomit.send(entity.level(), entity.getX(), entity.getY(), entity.getZ(), data, 25D);
    }
}
