package com.hbm.tileentity.turret;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The wrecked sentry found in worldgen: always on, never needs power or ammo, and shoots at anything
 * alive. Only one of its two barrels actually fires; the other just makes noise, which is 1.12.2's
 * behaviour and reads as the damage.
 */
public class TileEntityTurretSentryDamaged extends TileEntityTurretSentry {

    public TileEntityTurretSentryDamaged(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_SENTRY_DAMAGED.get(), pos, state);
    }

    @Override
    public boolean hasPower() {
        return true;
    }

    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public double getTurretYawSpeed() {
        return 3D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 2D;
    }

    @Override
    public boolean entityAcceptableTarget(Entity e) {
        if (e instanceof Player player && player.isCreative()) return false;
        if (e.isSpectator()) return false;
        return e instanceof LivingEntity && e.isAlive();
    }

    /** The old damaged block drops neither itself nor the contents of its otherwise-unused inventory. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    public void updateFiringTick() {
        timer++;
        if (timer % 10 != 0) return;

        BulletConfig conf = XFactory9mm.p9_fmj;
        if (conf == null) return;

        if (shotSide) {
            level.playSound(null, worldPosition, ModSounds.TURRET_SENTRY_FIRE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
            spawnBullet(conf, 5F);
            Vec3 side = toWorldDirection(new Vec3(0.125, 0, 0).yRot((float) -rotationYaw));
            muzzleFlash(barrelTip().add(side.x, 0, side.z), 1F, 1);
            didJustShootLeft = true;
        } else {
            level.playSound(null, worldPosition, ModSounds.TURRET_SENTRY_FIRE.get(), SoundSource.BLOCKS, 2.0F, 0.75F);
            muzzleFlash(getTurretPos(), 1F, 1);
            didJustShootRight = true;
        }

        shotSide = !shotSide;
    }
}
