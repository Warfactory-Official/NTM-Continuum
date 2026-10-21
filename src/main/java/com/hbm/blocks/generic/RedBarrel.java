package com.hbm.blocks.generic;

import dev.golem.core.world.Explosions;
import com.hbm.blocks.bomb.BlockTaint;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code RedBarrel}: the four effect drums. {@code red_barrel} and {@code pink_barrel} are
 * flammable and go off as a 2.5-strength fire explosion; {@code lox_barrel} pops weakly and freezes
 * everything living within 7 blocks in ice (the 1.7 {@code ExplosionThermo.freezer}, ported here);
 * {@code taint_barrel} pops weakly and seeds taint on 100 random solid blocks within ±4.
 */
public class RedBarrel extends BlockBarrelDeco {

    public enum Kind { RED, PINK, LOX, TAINT }

    private final Kind effect;

    public RedBarrel(Properties properties, String texture, int kindId, Kind effect) {
        super(properties, texture, kindId, effect == Kind.RED || effect == Kind.PINK);
        this.effect = effect;
    }

    @Override
    public void detonate(Level level, double x, double y, double z, @Nullable EntityPrimedExplosive primed) {
        int ix = Mth.floor(x), iy = Mth.floor(y), iz = Mth.floor(z);
        switch (effect) {
            case RED, PINK -> Explosions.explode(level, primed, x, y, z, 2.5F, true, Level.ExplosionInteraction.TNT);
            case LOX -> {
                Explosions.explode(level, primed, x, y, z, 1F, false, Level.ExplosionInteraction.NONE);
                freezer(level, ix, iy, iz, 7);
            }
            case TAINT -> {
                Explosions.explode(level, primed, x, y, z, 1F, false, Level.ExplosionInteraction.NONE);
                RandomSource rand = level.getRandom();
                for (int i = 0; i < 100; i++) {
                    int a = rand.nextInt(9) - 4 + ix;
                    int b = rand.nextInt(9) - 4 + iy;
                    int c = rand.nextInt(9) - 4 + iz;
                    BlockPos at = new BlockPos(a, b, c);
                    BlockState block = level.getBlockState(at);
                    if (block.isCollisionShapeFullBlock(level, at) && !block.isAir()) {
                        level.setBlock(at, ModBombBlocks.taint.get().defaultBlockState()
                                .setValue(BlockTaint.TAINTAGE, rand.nextInt(3) + 4), 2);
                    }
                }
            }
        }
    }

    /**
     * The 1.7 {@code ExplosionThermo.freezer}: every living thing (cats excepted) within {@code strength}
     * blocks is entombed in a 3×3×3-ish shell of ice at its feet and weakened/slowed/fatigued.
     */
    public static void freezer(Level level, int x, int y, int z, int strength) {
        double wat = strength;
        AABB box = new AABB(x - wat - 1.0D, y - wat - 1.0D, z - wat - 1.0D, x + wat + 1.0D, y + wat + 1.0D, z + wat + 1.0D);
        for (Entity entity : level.getEntities((Entity) null, box)) {
            double d4 = Math.sqrt(entity.distanceToSqr(x, y, z)) / (strength * 2.0D);
            if (d4 > 1.0D) continue;
            double d5 = entity.getX() - x;
            double d6 = entity.getY() + entity.getEyeHeight() - y;
            double d7 = entity.getZ() - z;
            double d9 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
            if (d9 < wat && !(entity instanceof Ocelot) && !(entity instanceof Cat) && entity instanceof LivingEntity living) {
                for (int a = (int) entity.getX() - 2; a < (int) entity.getX() + 1; a++) {
                    for (int b = (int) entity.getY(); b < (int) entity.getY() + 3; b++) {
                        for (int c = (int) entity.getZ() - 1; c < (int) entity.getZ() + 2; c++) {
                            level.setBlock(new BlockPos(a, b, c), Blocks.ICE.defaultBlockState(), 3);
                        }
                    }
                }
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2 * 60 * 20, 4));
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 90 * 20, 2));
                living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 3 * 60 * 20, 2));
            }
        }
    }

    /** For the renderer: which block a barrel drop is (used nowhere else). */
    public static boolean isFlammable(Block block) {
        return block instanceof RedBarrel barrel && barrel.flammable;
    }
}
