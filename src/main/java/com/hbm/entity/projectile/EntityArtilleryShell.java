package com.hbm.entity.projectile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.entity.logic.EntityChunkLoader;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.items.weapon.ItemAmmoArty;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The 16-inch shell. Unlike the HIMARS rocket it does not steer: it is fired on a solved ballistic arc
 * and falls under gravity the whole way, which is why the gun aims from the muzzle rather than the
 * pivot.
 *
 * <p>It whistles shortly before it lands — the sound plays at the <em>target</em>, not at the shell,
 * so the people about to be hit hear it rather than the gunner. A cargo shell carries a stack that
 * survives the flight and can be taken back off it, which is also why that one variant sticks in the
 * ground instead of despawning.
 */
public class EntityArtilleryShell extends EntityThrowableNT implements IChunkLoader, IRadarDetectableNT {

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityArtilleryShell.class, EntityDataSerializers.INT);

    /** @see EntityArtilleryRocket#chunkLoader */
    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 1);

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean shouldWhistle;
    private boolean didWhistle;

    private ItemStack cargo = ItemStack.EMPTY;

    public EntityArtilleryShell(EntityType<? extends EntityArtilleryShell> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(TYPE, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public EntityArtilleryShell setType(int type) {
        this.entityData.set(TYPE, type);
        return this;
    }

    /** Renamed from upstream's {@code getType()}, which now clashes with {@code Entity.getType()}. */
    public int getShellType() {
        return this.entityData.get(TYPE);
    }

    public ItemAmmoArty.ArtilleryShellType getShell() {
        return ItemAmmoArty.byIndex(getShellType());
    }

    public double[] getTarget() {
        return new double[]{this.targetX, this.targetY, this.targetZ};
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public double getTargetHeight() {
        return this.targetY;
    }

    public void setWhistle(boolean whistle) {
        this.shouldWhistle = whistle;
    }

    public boolean getWhistle() {
        return this.shouldWhistle;
    }

    public boolean didWhistle() {
        return this.didWhistle;
    }

    public void setCargo(ItemStack stack) {
        this.cargo = stack;
    }

    @Override
    public void tick() {
        super.tick();

        if (isRemoved()) return;

        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5D, getZ(), 0D, 0.1D, 0D);
            return;
        }

        whistle();
        chunkLoader.tick();
        getShell().onUpdate(this);
    }

    /** Roughly eighteen ticks out, measured horizontally: the shell is still above, the sound is not. */
    private void whistle() {
        if (this.didWhistle || !this.shouldWhistle) return;

        Vec3 motion = getDeltaMovement();
        double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        double deltaX = getX() - this.targetX;
        double deltaZ = getZ() - this.targetZ;

        if (speed * 18D > Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)) {
            level().playSound(null, this.targetX, this.targetY, this.targetZ, ModSounds.MORTAR_WHISTLE.get(),
                    SoundSource.BLOCKS, 15.0F, 0.9F + this.random.nextFloat() * 0.2F);
            this.didWhistle = true;
        }
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (level().isClientSide()) return;
        // Shells do not chain-trigger off each other mid-flight, which a cluster salvo would otherwise do.
        if (hit.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) hit).getEntity() instanceof EntityArtilleryShell) {
            return;
        }
        getShell().onImpact(this, hit);
    }

    @Override
    public void initChunkLoading() {
        chunkLoader.initChunkLoading();
    }

    @Override
    public void loadNeighboringChunks(int chunkX, int chunkZ) {
        chunkLoader.loadNeighboringChunks(chunkX, chunkZ);
    }

    @Override
    public void clearChunkLoader() {
        chunkLoader.clearChunkLoader();
    }

    /** What a warhead calls instead of {@code discard()}: the claim has to go with the shell. */
    public void killAndClear() {
        discard();
        clearChunkLoader();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkLoader();
        super.remove(reason);
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    /** Real gravity per tick, which is what makes the gun's arc solvable in closed form. */
    @Override
    public double getGravityVelocity() {
        return 9.81D * 0.05D;
    }

    /** A cargo shell stays where it landed until somebody empties it. */
    @Override
    protected int groundDespawn() {
        return this.cargo.isEmpty() ? 1200 : 0;
    }

    /** Unlike other projectiles a shell can be caught, shot down, or opened where it stuck. */
    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected InteractionResult onInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!level().isClientSide()) {
            if (!this.cargo.isEmpty()) player.getInventory().add(this.cargo.copy());
            discard();
        }
        return InteractionResult.PASS;
    }

    @Override
    public String getRadarName() {
        return "entity.hbm.radar.artillery";
    }

    @Override
    public int getBlipLevel() {
        return ARTY;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanShells;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return params.scanShells;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("type", getShellType());
        output.putBoolean("shouldWhistle", this.shouldWhistle);
        output.putBoolean("didWhistle", this.didWhistle);
        output.putDouble("targetX", this.targetX);
        output.putDouble("targetY", this.targetY);
        output.putDouble("targetZ", this.targetZ);
        if (!this.cargo.isEmpty()) output.store("cargo", dev.golem.core.world.ItemStacks.optionalCodec(), this.cargo);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.entityData.set(TYPE, input.getIntOr("type", 0));
        this.shouldWhistle = input.getBooleanOr("shouldWhistle", false);
        this.didWhistle = input.getBooleanOr("didWhistle", false);
        this.targetX = input.getDoubleOr("targetX", 0D);
        this.targetY = input.getDoubleOr("targetY", 0D);
        this.targetZ = input.getDoubleOr("targetZ", 0D);
        setCargo(input.read("cargo", dev.golem.core.world.ItemStacks.optionalCodec()).orElse(ItemStack.EMPTY));
        chunkLoader.markRestoredFromNbt();
    }
}
