package com.hbm.blocks.fluid;

import com.hbm.lib.ModDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The 1.7 {@code GenericFluidBlock} with {@code setDamage(acid, 5)} ({@code sulfuric_acid_block}):
 * dropped items are held still and dissolved (0.5 acid damage every 20 ticks, a cloud every 5);
 * anything else has its fall slowed and takes 5 acid damage per contact tick; everything fizzes.
 * (1.7 also awarded an achievement for dissolving a slime ball; there is no achievement system here.)
 */
public class BlockSulfuricAcid extends BlockStaticFluid {

    private static final float DAMAGE = 5F;

    public BlockSulfuricAcid(Properties properties) {
        super(properties);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity item) {
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity.tickCount % 20 == 0 && !level.isClientSide()) {
                entity.hurt(ModDamageSources.source(level, ModDamageSources.ACID), DAMAGE * 0.1F);
                if (entity.isRemoved() && item.getItem().is(Items.SLIME_BALL)) {
                    // 1.7 achSulfuric: every player in the dissolved item's box grown by 10
                    if (level instanceof net.minecraft.server.level.ServerLevel server) {
                        com.hbm.advancement.AwardRegions.nearby(server, entity.getBoundingBox().inflate(10D),
                                player -> com.hbm.advancement.HbmCriteria.DISSOLVED.fire(player, item.getItem()));
                    }
                }
            }
            if (entity.tickCount % 5 == 0) {
                level.addParticle(ParticleTypes.CLOUD, entity.getX(), entity.getY(), entity.getZ(), 0.0, 0.0, 0.0);
            }
        } else {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y < -0.2) entity.setDeltaMovement(motion.x, motion.y * 0.5, motion.z);
            if (!level.isClientSide()) entity.hurt(ModDamageSources.source(level, ModDamageSources.ACID), DAMAGE);
        }
        if (entity.tickCount % 5 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1F);
        }
    }
}
