package com.hbm.tileentity.turret;

import com.hbm.config.WeaponConfig;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Methusalem", the abandoned CIWS. It needs neither switch, power nor ammunition, tracks anything
 * alive, and lands its bursts half as often as a maintained Goalkeeper.
 */
public class TileEntityTurretHowardDamaged extends TileEntityTurretHoward {

    public TileEntityTurretHowardDamaged(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_HOWARD_DAMAGED.get(), pos, state);
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
    public double getDetectorRange() {
        return 16D;
    }

    @Override
    public double getDetectorGrace() {
        return 5D;
    }

    @Override
    public boolean hasThermalVision() {
        return false;
    }

    @Override
    public boolean entityAcceptableTarget(Entity e) {
        if (e instanceof Player player && player.isCreative()) return false;
        if (e.isSpectator()) return false;
        return e instanceof LivingEntity && e.isAlive();
    }

    @Override
    public void updateFiringTick() {
        timer++;
        if (tPos == null || timer % 4 != 0) return;

        level.playSound(null, worldPosition, ModSounds.TURRET_HOWARD_FIRE.get(), SoundSource.BLOCKS, 4.0F,
                0.7F + level.getRandom().nextFloat() * 0.3F);

        if (level.getRandom().nextInt(100) + 1 <= WeaponConfig.ciwsHitrate * 0.5D && target != null) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(target, ModDamageSources.shrapnel(level),
                    2F + level.getRandom().nextInt(2));
        }

        muzzleFlash(barrelTip().add(alongBarrel(0, 0.25, 0)), 1.5F, 1);
    }
}
