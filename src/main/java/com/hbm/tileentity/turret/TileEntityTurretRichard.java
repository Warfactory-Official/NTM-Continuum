package com.hbm.tileentity.turret;

import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import dev.golem.core.data.Tags;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The missile launcher. Its rack holds seventeen rounds and takes five seconds to refill, so the
 * count is worth syncing on its own: the renderer draws one missile per loaded round.
 *
 * <p>Its rockets steer: each one leaves the tube locked onto whatever the turret was tracking.
 */
public class TileEntityTurretRichard extends TileEntityTurretBaseNT {

    private static final Identifier GUI = turretGui("richard");
    private static final int RACK_SIZE = 17;

    private static @Nullable List<BulletConfig> configs;

    public int loaded;

    private int timer;
    private int reload;
    private boolean rackPacket;

    public TileEntityTurretRichard(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_RICHARD.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) configs = List.of(XFactoryRocket.rocket_ml);
        return configs;
    }

    @Override
    public String getDefaultName() {
        return "container.turretRichard";
    }

    @Override
    public Identifier guiTexture() {
        return GUI;
    }

    @Override
    public int guiFontColor() {
        return 0x433B33;
    }

    @Override
    public double getTurretDepression() {
        return 25D;
    }

    @Override
    public double getTurretElevation() {
        return 25D;
    }

    @Override
    public double getBarrelLength() {
        return 1.25D;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getDetectorGrace() {
        return 8D;
    }

    @Override
    public double getDetectorRange() {
        return 64D;
    }

    @Override
    protected void tickServer() {
        super.tickServer();

        if (reload > 0) {
            reload--;
            if (reload == 0) loaded = RACK_SIZE;
        }

        if (loaded <= 0 && reload <= 0 && getFirstConfigLoaded() != null) reload = 100;
        if (getFirstConfigLoaded() == null) loaded = 0;

        rackPacket = true;
        networkPackNT();
        rackPacket = false;
    }

    @Override
    public void updateFiringTick() {
        if (reload > 0) return;

        timer++;
        if (timer <= 0 || timer % 10 != 0) return;

        BulletConfig conf = getFirstConfigLoaded();
        if (conf == null) {
            loaded = 0;
            return;
        }

        spawnBullet(conf);
        consumeAmmo(conf);
        level.playSound(null, worldPosition, ModSounds.TURRET_RICHARD_FIRE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        loaded--;
    }

    /** Overridden only to hand the round its lock, which the base's projectiles never carry. */
    @Override
    public void spawnBullet(BulletConfig bullet) {
        Vec3 tip = barrelTip();

        EntityBulletBaseMK4 projectile = new EntityBulletBaseMK4(level, bullet, getBaseDamage(), bullet.spread,
                worldYaw(), worldPitch());
        projectile.setPos(tip.x, tip.y, tip.z);
        projectile.lockonTarget = target;
        fire(projectile);
    }

    /** The rack count rides its own packet so the racks empty on screen as they are fired. */
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(rackPacket);
        if (rackPacket) buf.writeInt(loaded);
        else super.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) loaded = buf.readInt();
        else super.deserialize(buf);
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
    public float getBaseDamage() { return XFactoryTurret.TURRET_RICHARD_DAMAGE.get(); }
}
