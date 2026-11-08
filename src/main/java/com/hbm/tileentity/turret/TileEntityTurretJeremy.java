package com.hbm.tileentity.turret;

import net.minecraft.world.phys.Vec3;
import com.hbm.particle.helper.CasingCreator;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The 240 mm gun. It fires once every two seconds and works the breech for a second afterwards, which
 * is why the reload is a countdown rather than a state.
 */
public class TileEntityTurretJeremy extends TileEntityTurretBaseNT {

    private static final Identifier GUI = turretGui("cannon");

    private static @Nullable List<BulletConfig> configs;

    private int timer;
    private int reload;

    public TileEntityTurretJeremy(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_JEREMY.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) {
            configs = List.of(XFactoryTurret.shell_normal, XFactoryTurret.shell_explosive,
                    XFactoryTurret.shell_ap, XFactoryTurret.shell_du, XFactoryTurret.shell_w9);
        }
        return configs;
    }

    @Override
    public String getDefaultName() {
        return "container.turretJeremy";
    }

    @Override
    public Identifier guiTexture() {
        return GUI;
    }

    @Override
    public int guiFontColor() {
        return 0x373737;
    }

    @Override
    public double getDetectorGrace() {
        return 16D;
    }

    @Override
    public double getTurretDepression() {
        return 45D;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getBarrelLength() {
        return 4.25D;
    }

    @Override
    public double getDetectorRange() {
        return 80D;
    }

    @Override
    protected void tickServer() {
        if (reload > 0) reload--;
        if (reload == 1) {
            level.playSound(null, worldPosition, ModSounds.TURRET_JEREMY_RELOAD.get(), SoundSource.BLOCKS,
                    2.0F, 1.0F);
        }

        super.tickServer();
    }

    @Override
    public void updateFiringTick() {
        timer++;
        if (timer % 40 != 0) return;

        BulletConfig conf = getFirstConfigLoaded();
        if (conf == null) return;

        cachedCasingConfig = conf.casing;
        spawnBullet(conf);
        consumeAmmo(conf);
        level.playSound(null, worldPosition, ModSounds.TURRET_JEREMY_FIRE.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
        reload = 20;
        muzzleFlash(barrelTip(), 0F, 5);
    }

    @Override
    public float getBaseDamage() { return XFactoryTurret.TURRET_JEREMY_DAMAGE.get(); }

    @Override
    protected void spawnCasing() {
        if (cachedCasingConfig == null) return;
        Vec3 spawn = getCasingSpawnPos();
        CasingCreator.composeEffect(level, spawn.x, spawn.y, spawn.z, (float) Math.toDegrees(worldYaw()),
                (float) -Math.toDegrees(worldPitch()), -0.2, -0.2, 0, 0.01, -5, 0, cachedCasingConfig.getName(),
                true, 100, 0.5, 20);
        cachedCasingConfig = null;
    }

    @Override
    protected Vec3 getCasingSpawnPos() {
        return getTurretPos().add(alongBarrel(-2, 0, 0));
    }

    @Override
    public boolean usesCasings() {
        return true;
    }

    @Override
    public int casingDelay() {
        return 22;
    }
}
