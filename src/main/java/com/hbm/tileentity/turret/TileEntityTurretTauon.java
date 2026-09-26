package com.hbm.tileentity.turret;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.world.Compat;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The XVL1456 prototype. It spawns no projectile: the beam is instantaneous, so the turret damages
 * whatever it holds and tells the client to draw a line for three ticks.
 */
public class TileEntityTurretTauon extends TileEntityTurretBaseNT {

    private static final Identifier GUI = turretGui("tau");

    private static @Nullable List<BulletConfig> configs;

    public int beam;
    public float spin;
    public float lastSpin;
    public double lastDist;

    private int timer;
    private boolean shot;

    public TileEntityTurretTauon(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_TAUON.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) configs = List.of(XFactoryAccelerator.tau_uranium);
        return configs;
    }

    @Override
    public String getDefaultName() {
        return "container.turretTauon";
    }

    @Override
    public Identifier guiTexture() {
        return GUI;
    }

    @Override
    public int guiFontColor() {
        return 0x121514;
    }

    @Override
    public double getDetectorGrace() {
        return 3D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 9D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 6D;
    }

    @Override
    public double getTurretElevation() {
        return 35D;
    }

    @Override
    public double getTurretDepression() {
        return 35D;
    }

    @Override
    public double getDetectorRange() {
        return 128D;
    }

    @Override
    public double getBarrelLength() {
        return 2.0D - 0.0625D;
    }

    @Override
    public long getMaxPower() {
        return 100000;
    }

    @Override
    public long getConsumption() {
        return 1000;
    }

    @Override
    protected void tickClient() {
        if (tPos != null) lastDist = tPos.subtract(getTurretPos()).length();

        if (beam > 0) beam--;

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
        if (timer % 5 != 0) return;

        BulletConfig conf = getFirstConfigLoaded();
        if (conf == null || target == null || !(level instanceof ServerLevel server)) return;

        Compat.hurt(target, server, ModDamageSources.electricity(level),
                30F + level.getRandom().nextInt(11));
        consumeAmmo(conf);
        level.playSound(null, worldPosition, ModSounds.WEAPON_TAU_SHOOT.get(), SoundSource.BLOCKS, 4.0F,
                0.9F + level.getRandom().nextFloat() * 0.3F);

        shot = true;
        syncState();
        shot = false;
    }

    /**
     * A shot is its own one-bit packet: 1.12.2 sends it beside the ordinary state sync so the beam is
     * drawn on the tick it was fired rather than whenever the next full update lands.
     */
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(shot);
        if (!shot) super.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) beam = 3;
        else super.deserialize(buf);
    }
}
