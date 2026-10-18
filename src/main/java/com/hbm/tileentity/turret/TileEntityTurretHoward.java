package com.hbm.tileentity.turret;

import dev.golem.core.data.Tags;

import com.hbm.config.WeaponConfig;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Goalkeeper CIWS. It does not fire projectiles at all: a belt round loads one hundred bursts and
 * each burst rolls a flat hit chance against whatever it is tracking, which is 1.12.2's way of making a
 * point-defence gun that can actually catch a missile.
 */
public class TileEntityTurretHoward extends TileEntityTurretRadarCommandBase {

    private static final Identifier GUI = turretGui("howard");
    private static @Nullable List<BulletConfig> configs;

    public float spin;
    public float lastSpin;
    protected int loaded;
    protected int timer;

    public TileEntityTurretHoward(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TURRET_HOWARD.get(), pos, state);
    }

    protected TileEntityTurretHoward(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) configs = List.of(XFactoryTurret.dgk_normal);
        return configs;
    }

    @Override
    public String getDefaultName() {
        return "container.turretHoward";
    }

    @Override
    public Identifier guiTexture() {
        return GUI;
    }

    @Override
    public int guiFontColor() {
        return 0x04141C;
    }

    @Override
    public double getHeightOffset() {
        return 2.25D;
    }

    @Override
    public double getDetectorGrace() {
        return 3D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 12D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 8D;
    }

    @Override
    public double getTurretElevation() {
        return 90D;
    }

    @Override
    public double getTurretDepression() {
        return 50D;
    }

    @Override
    public double getDetectorRange() {
        return targetPlayers ? 48D : 300D;
    }

    @Override
    public double getBarrelLength() {
        return 3.25D;
    }

    @Override
    public long getMaxPower() {
        return 50000;
    }

    @Override
    public long getConsumption() {
        return 500;
    }

    @Override
    protected void tickServer() {
        if (loaded <= 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                consumeAmmo(conf);
                level.playSound(null, worldPosition, ModSounds.TURRET_HOWARD_RELOAD.get(), SoundSource.BLOCKS,
                        4.0F, 1F);
                loaded = 100;
            }
        }

        super.tickServer();
    }

    @Override
    protected void tickClient() {
        lastSpin = spin;
        if (tPos != null) spin += 45;

        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        super.tickClient();
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (loaded <= 0 || tPos == null) return;

        level.playSound(null, worldPosition, ModSounds.TURRET_HOWARD_FIRE.get(), SoundSource.BLOCKS, 4.0F,
                0.9F + level.getRandom().nextFloat() * 0.3F);
        level.playSound(null, worldPosition, ModSounds.TURRET_HOWARD_FIRE.get(), SoundSource.BLOCKS, 4.0F,
                1F + level.getRandom().nextFloat() * 0.3F);

        if (timer % 2 != 0) return;

        loaded--;

        if (level.getRandom().nextInt(100) + 1 <= WeaponConfig.ciwsHitrate && target != null) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(target, ModDamageSources.shrapnel(level),
                    getBaseDamage() + level.getRandom().nextInt(2));
        }

        Vec3 tip = barrelTip();
        Vec3 offset = alongBarrel(0, 0.25, 0);
        muzzleFlash(tip.add(offset), 1.5F, 1);
        muzzleFlash(tip.subtract(offset), 1.5F, 1);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("loaded", loaded);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        loaded = input.getIntOr("loaded", 0);
    }

    @Override
    public float getBaseDamage() { return XFactoryTurret.TURRET_HOWARD_DAMAGE.get(); }
}
