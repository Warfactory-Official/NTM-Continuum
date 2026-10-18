package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.client.ClientEffects;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.tileentity.machine.TileEntityMachineSatDock;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code EntitySatellitePod}, the cargo drop pod: falls from y=300 onto the cargo pad that called it, brakes
 * over the last 25 blocks, unloads after five seconds and lifts off again. Cargo that does not fit the pad, or a pod
 * that missed it, spills onto the ground. Any hit of 5 or more destroys it.
 */
public class EntitySatellitePod extends EntityThrowableInterp implements IRadarDetectableNT {

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(EntitySatellitePod.class, EntityDataSerializers.INT);
    public static final int STATE_LEGS_UP = 0;
    public static final int STATE_LEGS_DOWN = 1;

    public ItemStack[] slots = new ItemStack[0];

    /** Ticks since landing; unloads at 100. */
    public int timer = 0;
    /** Y of the pad that called the pod, for braking in time. */
    public int callerYPos;
    public double speed = 0.75D;

    public float legs = 0F;
    public float prevLegs = 0F;
    public static final float LEG_SPEED = 1F / 20F;

    public EntitySatellitePod(Level level) {
        this(ModNtmEntities.MINER_LANDER.get(), level);
    }

    public EntitySatellitePod(EntityType<? extends EntitySatellitePod> type, Level level) {
        super(type, level);
    }

    public EntitySatellitePod setup(int caller, ItemStack... cargo) {
        this.callerYPos = caller;
        this.slots = cargo;
        return this;
    }

    @Override
    protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(STATE, STATE_LEGS_UP);
    }

    public boolean doesDeployLegs() {
        return entityData.get(STATE) == STATE_LEGS_DOWN;
    }

    public void setDeployLegs(boolean deploy) {
        entityData.set(STATE, deploy ? STATE_LEGS_DOWN : STATE_LEGS_UP);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        if (amount >= 5F && level() instanceof ServerLevel && !isRemoved()) {
            discard();

            ExplosionVNT xnt = new ExplosionVNT(level(), getX(), getY() + 1.5, getZ(), 15F);
            xnt.setEntityProcessor(new EntityProcessorCrossSmooth(1D, 50));
            xnt.setPlayerProcessor(new PlayerProcessorStandard());
            xnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            xnt.explode();

            Vec3 motion = getDeltaMovement();
            if (motion.y != 0) {
                ExplosionLarge.spawnShrapnelShower(level(), getX(), getY() + 1.5, getZ(), motion.x, motion.y, motion.z, 15, 0.25);
            }
        }
        return true;
    }

    @Override
    public void tick() {

        if (!level().isClientSide()) {

            if (this.isLanding()) {

                if (this.timer > 0) {
                    this.timer++;
                    setPos(getX(), Math.ceil(getY()), getZ()); // 1.7: "hack hack hackedy hack"
                    if (this.timer >= 100) this.unloadItems();

                } else if (this.onGround()) {
                    this.speed = 0D;
                    this.timer = 1;
                    this.setDeployLegs(true);

                } else {
                    if (getY() < this.callerYPos + 17 && !this.doesDeployLegs()) this.setDeployLegs(true);
                    if (getY() < this.callerYPos + 25) this.speed -= 0.01;
                    this.speed = Mth.clamp(this.speed, 0.025D, 0.75D);
                }

                setDeltaMovement(0, -this.speed, 0);

            } else {

                setOnGround(false);

                this.speed += 0.01;
                if (this.speed >= 0.2) this.setDeployLegs(false);
                this.speed = Mth.clamp(this.speed, 0D, 2D);
                setDeltaMovement(0, this.speed, 0);

                if (getY() > 300) discard();
            }

        } else {

            this.prevLegs = this.legs;
            this.legs += doesDeployLegs() ? LEG_SPEED : -LEG_SPEED;
            this.legs = Mth.clamp(this.legs, 0F, 1F);

            double motionY = getDeltaMovement().y;
            if (this.legs > 0 && motionY < 0 || motionY > 0) {
                ClientEffects.spawnGasFlame(level(), getX(), getY() + 0.5, getZ(), 0, this.speed - 1, 0,
                        ClientEffects.GAS_FLAME_DEFAULT_SCALE);
            }
        }

        if (!isRemoved()) super.tick();
    }

    /** Fills the cargo pad under the pod; whatever does not fit is spilled, and the pod leaves empty. */
    public void unloadItems() {
        BlockPos below = BlockPos.containing(getX(), getY() - 0.5, getZ());

        if (level().getBlockEntity(below) instanceof TileEntityMachineSatDock dock && dock.getBlockPos().equals(below)) {
            for (int i = 0; i < this.slots.length; i++) {
                if (slots[i] == null || slots[i].isEmpty()) continue;
                slots[i] = dock.addCargo(slots[i]);
            }
        }

        if (level() instanceof ServerLevel server) {
            for (ItemStack stack : this.slots) {
                if (stack == null || stack.isEmpty()) continue;
                spawnAtLocation(server, stack, 0.25F);
            }
        }

        this.slots = new ItemStack[0];
    }

    public boolean isLanding() {
        return this.slots.length > 0;
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 at = hit.getLocation();
            setPos(at.x, at.y, at.z);
            setOnGround(true);

            if (this.speed < -0.02 && level() instanceof ServerLevel server) {
                this.onHurt(server.damageSources().generic(), 10F);
            }
        }
    }

    @Override public boolean doesImpactEntities() { return false; }
    @Override protected float getAirDrag() { return 1F; }
    @Override protected float getWaterDrag() { return 1F; }
    @Override public double getGravityVelocity() { return 0D; }

    @Override public String getRadarName() { return "radar.target.dropship"; }
    @Override public int getBlipLevel() { return IRadarDetectableNT.SPECIAL; }
    @Override public boolean canBeSeenBy(Object radar) { return !(radar instanceof TileEntityTurretBaseNT); }
    @Override public boolean paramsApplicable(RadarScanParams params) { return params.scanMissiles; }
    @Override public boolean suppliesRedstone(RadarScanParams params) { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        entityData.set(STATE, input.getIntOr("state", STATE_LEGS_UP));
        timer = input.getIntOr("timer", 0);
        callerYPos = input.getIntOr("callerYPos", 0);
        speed = input.getDoubleOr("speed", 0.75D);
        slots = input.read("items", ItemStacks.optionalCodec().listOf()).orElse(List.of()).toArray(new ItemStack[0]);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("state", entityData.get(STATE));
        output.putInt("timer", timer);
        output.putInt("callerYPos", callerYPos);
        output.putDouble("speed", speed);
        List<ItemStack> items = new ArrayList<>(slots.length);
        for (ItemStack stack : slots) items.add(stack == null ? ItemStack.EMPTY : stack);
        output.store("items", ItemStacks.optionalCodec().listOf(), items);
    }
}
