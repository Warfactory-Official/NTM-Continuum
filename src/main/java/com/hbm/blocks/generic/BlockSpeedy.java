package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code BlockSpeedy} (asphalt): a player walking on it has their horizontal motion multiplied by
 * {@code speed} every tick, client side only — the old {@code IStepTickReceiver.onPlayerStep}. The
 * stair and slab forms ({@code BlockSpeedyStairs}, the asphalt entry of the concrete multi-slab) are
 * nested here.
 */
public class BlockSpeedy extends Block {

    public final double speed;

    public BlockSpeedy(Properties properties, double speed) {
        super(properties);
        this.speed = speed;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        boost(level, entity, speed);
    }

    /** The old {@code onPlayerStep}: only the client, only while the player is steering. */
    public static void boost(Level level, Entity entity, double speed) {
        if (!level.isClientSide()) return;
        if (!(entity instanceof Player player)) return;
        if (player.zza != 0 || player.xxa != 0) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * speed, motion.y, motion.z * speed);
        }
    }

    /** 1.7 {@code BlockSpeedyStairs}. */
    public static class Stairs extends HbmStairBlock {
        public final double speed;

        public Stairs(BlockState baseState, Properties properties, double speed) {
            super(baseState, properties);
            this.speed = speed;
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            boost(level, entity, speed);
        }
    }

    /** The asphalt slab of the old {@code concrete_slab} multi-slab, which boosted like the block (x1.5). */
    public static class Slab extends SlabBlock {
        public Slab(Properties properties) {
            super(properties);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            boost(level, entity, 1.5);
        }
    }
}
