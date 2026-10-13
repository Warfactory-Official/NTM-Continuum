package com.hbm.tileentity.turret;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The 9 mm sentry: the only turret that fits in one block, so it has no pad, no plugs and takes its
 * power from below alone.
 */
public class TileEntityTurretSentry extends TileEntityTurretBaseNT {

    private static final Identifier GUI = turretGui("sentry");

    private static @Nullable List<BulletConfig> configs;

    public double barrelLeftPos;
    public double lastBarrelLeftPos;
    public double barrelRightPos;
    public double lastBarrelRightPos;

    protected boolean didJustShootLeft;
    protected boolean retractingLeft;
    protected boolean didJustShootRight;
    protected boolean retractingRight;

    protected boolean shotSide;
    protected int timer;

    public TileEntityTurretSentry(BlockPos pos, BlockState state) {
        this(ModBlockEntities.TURRET_SENTRY.get(), pos, state);
    }

    protected TileEntityTurretSentry(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean hasConnectorPlugs() {
        return false;
    }

    @Override
    public DirPos[] getConPos() {
        return new DirPos[]{new DirPos(worldPosition.below(), Library.NEG_Y)};
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir == ForgeDirection.DOWN;
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) {
            configs = List.of(XFactory9mm.p9_sp, XFactory9mm.p9_fmj, XFactory9mm.p9_jhp, XFactory9mm.p9_ap);
        }
        return configs;
    }

    @Override
    public String getDefaultName() {
        return "container.turretSentry";
    }

    @Override
    public Identifier guiTexture() {
        return GUI;
    }

    @Override
    public double getTurretDepression() {
        return 20D;
    }

    @Override
    public double getTurretElevation() {
        return 20D;
    }

    @Override
    public double getDetectorRange() {
        return 24D;
    }

    @Override
    public double getDetectorGrace() {
        return 2D;
    }

    @Override
    public long getMaxPower() {
        return 1_000;
    }

    @Override
    public long getConsumption() {
        return 5;
    }

    @Override
    public double getBarrelLength() {
        return 1.25D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    @Override
    public boolean hasThermalVision() {
        return false;
    }

    @Override
    public Vec3 getHorizontalOffset() {
        return new Vec3(0.5, 0, 0.5);
    }

    @Override
    protected void tickClient() {
        lastBarrelLeftPos = barrelLeftPos;
        lastBarrelRightPos = barrelRightPos;

        float retractSpeed = 0.5F;
        float pushSpeed = 0.25F;

        if (retractingLeft) {
            barrelLeftPos += retractSpeed;
            if (barrelLeftPos >= 1) retractingLeft = false;
        } else {
            barrelLeftPos -= pushSpeed;
            if (barrelLeftPos < 0) barrelLeftPos = 0;
        }

        if (retractingRight) {
            barrelRightPos += retractSpeed;
            if (barrelRightPos >= 1) retractingRight = false;
        } else {
            barrelRightPos -= pushSpeed;
            if (barrelRightPos < 0) barrelRightPos = 0;
        }

        super.tickClient();
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer % 10 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null) {
                spawnBullet(conf, 5F);
                consumeAmmo(conf);
                level.playSound(null, worldPosition, ModSounds.TURRET_SENTRY_FIRE.get(), SoundSource.BLOCKS,
                        2.0F, 1.0F);

                Vec3 side = toWorldDirection(new Vec3(0.125 * (shotSide ? 1 : -1), 0, 0).yRot((float) -rotationYaw));
                muzzleFlash(barrelTip().add(side.x, 0, side.z), 1F, 1);

                if (shotSide) didJustShootLeft = true;
                else didJustShootRight = true;
                shotSide = !shotSide;
            }
        }
    }

    @Override
    protected void seekNewTarget() {
        Entity lastTarget = target;
        super.seekNewTarget();

        if (lastTarget != target && target != null) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    ModSounds.TURRET_SENTRY_LOCKON.get(), SoundSource.BLOCKS, 2.0F, 1.5F);
        }
    }

    /**
     * A shot is sent as an edge, and the client turns it into a retraction it plays out on its own.
     * The edge is cleared by the tick that raised it rather than by this method, because serialisation
     * runs only when someone watches: a machine nobody is watching does not build a payload at all, and a
     * flag consumed only on write would survive until the next viewer and fire a muzzle flash for a
     * shot taken minutes earlier.
     */
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(didJustShootLeft);
        buf.writeBoolean(didJustShootRight);
    }

    @Override
    protected void tickServer() {
        super.tickServer();
        didJustShootLeft = false;
        didJustShootRight = false;
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        if (buf.readBoolean()) retractingLeft = true;
        if (buf.readBoolean()) retractingRight = true;
    }
}
