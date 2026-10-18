package com.hbm.tileentity.turret;

import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The .50 BMG gatling. Its barrels spin up while it holds a target and coast back down when it loses
 * one, which is the only reason it keeps a client tick of its own.
 *
 * <p>The ejector 1.12.2 hands its spent brass to has no counterpart yet: {@code BulletConfig} carries
 * no casing in this port, so nothing is thrown.
 */
public class TileEntityTurretChekhov extends TileEntityTurretBaseNT {

    private static @Nullable List<BulletConfig> configs;

    public float spin;
    public float lastSpin;
    protected int timer;
    private float accel;
    private boolean manual;

    public TileEntityTurretChekhov(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TURRET_CHEKHOV.get(), pos, state);
    }

    protected TileEntityTurretChekhov(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) {
            configs = List.of(XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp,
                    XFactory50.bmg50_ap, XFactory50.bmg50_du);
        }
        return configs;
    }

    @Override
    public String getDefaultName() {
        return "container.turretChekhov";
    }

    @Override
    public int guiFontColor() {
        return 0x373737;
    }

    @Override
    public double getTurretElevation() {
        return 45D;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getBarrelLength() {
        return 3.5D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    public int getDelay() {
        return 2;
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer > 20 && timer % getDelay() == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                spawnBullet(conf);
                consumeAmmo(conf);
                level.playSound(null, worldPosition, ModSounds.TURRET_CHEKHOV_FIRE.get(), SoundSource.BLOCKS,
                        2.0F, 1.0F);
                muzzleFlash(barrelTip(), 1.5F, 1);
            }
        }
    }

    @Override
    protected void tickServer() {
        super.tickServer();

        if (tPos == null && !manual) {
            timer--;
            if (timer > 20) timer = 20;
            if (timer < 0) timer = 0;
        }
        manual = false;
    }

    @Override
    protected void tickClient() {
        super.tickClient();

        if (tPos != null || manual) accel = Math.min(45F, accel += 2);
        else accel = Math.max(0F, accel -= 2);

        manual = false;

        lastSpin = spin;
        spin += accel;

        if (spin >= 360F) {
            spin -= 360F;
            lastSpin -= 360F;
        }
    }

    @Override
    public void manualSetup() {
        manual = true;
    }

    @Override
    public float getBaseDamage() { return XFactoryTurret.TURRET_CHEKHOV_DAMAGE.get(); }
}
