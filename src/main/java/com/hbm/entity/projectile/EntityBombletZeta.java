package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 EntityBombletZeta. Its manual movement intentionally does not reuse EntityThrowableNT.
 */
public class EntityBombletZeta extends CompatEntity {

    /**
     * What a bomblet does when it lands: 1.7's bomblet {@code type} 0, 1, 2 and 4, as a registry of records keyed by
     * the name the bomblet saves (1.7 saved the number). An addon's bomber may drop HBM's kinds or register its own.
     */
    public record Kind(String name, java.util.function.Consumer<EntityBombletZeta> impact) {}

    public static final com.hbm.api.registry.NamedRegistry<Kind> KINDS =
            new com.hbm.api.registry.NamedRegistry<>("bomblet kind", Kind::name);

    /** 1.7 type 0: a small HE blast. */
    public static final Kind HE = KINDS.register(new Kind("he", zeta -> zeta.blast(false, 3.5F, 1.25F)));
    /** 1.7 type 1: the same blast setting fires. */
    public static final Kind INCENDIARY = KINDS.register(new Kind("incendiary", zeta -> zeta.blast(true, 5F, 1.75F)));
    /** 1.7 type 2: a chlorine mist. */
    public static final Kind CHLORINE = KINDS.register(new Kind("chlorine", EntityBombletZeta::chlorine));
    /** 1.7 type 4: an MK5 blast, 1-in-100 balefire flash. */
    public static final Kind NUKE = KINDS.register(new Kind("nuke", EntityBombletZeta::nuke));

    private Kind kind = HE;

    // 26.2: Entity.setXRot clamps to +-90; 1.7:99 writes rotationPitch raw and a steep dive needs -180..-90.
    public float renderPitch;
    public float renderPitchO;

    public EntityBombletZeta(EntityType<? extends EntityBombletZeta> type, Level level) {
        super(type, level);
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    @Override
    protected void loadData(DataIn input) {
        // by name; a pre-2026-10-08 numeric type falls back to HE with one WARN
        if (input.getIntOr("type", Integer.MIN_VALUE) != Integer.MIN_VALUE) com.hbm.util.SavedIds.warnNumeric("bomblet kind", "type");
        Kind saved = KINDS.get(input.getStringOr("kind", HE.name()));
        kind = saved != null ? saved : HE;
    }

    @Override
    protected void saveData(DataOut output) {
        output.putString("kind", kind.name());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    // 26.2: hurt() final; not damageable in flight
    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        // 26.2: Entity.baseTick dropped the prev-rotation bookkeeping 1.7 Entity.onEntityUpdate:404-405 ran
        renderPitchO = renderPitch;
        yRotO = getYRot();
        xo = getX();
        yo = getY();
        zo = getZ();
        super.tick();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.x * 0.99D, motion.y - 0.05D, motion.z * 0.99D);
        rotation();
        BlockPos block = new BlockPos((int) getX(), (int) getY(), (int) getZ()); // 1.7 cast truncation, not floor
        if (level().getBlockState(block).isAir() || level().isClientSide()) return;
        kind.impact().accept(this);
        discard();
    }

    private void blast(boolean fire, float sfxSize, float sfxStrength) {
        ExplosionVNT vnt = new ExplosionVNT(level(), getX() + 0.5F, getY() + 1.5F, getZ() + 0.5F, 4F, this); // exploder: see MachineExplosionDamage
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(fire ? new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()) : new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 100));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(15, sfxSize, sfxStrength));
        vnt.explode();
    }

    private void chlorine() {
        level().playSound(null, getX() + 0.5D, getY() + 0.5D, getZ() + 0.5D, SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS,
                5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
        EntityMist mist = new EntityMist(level()).setType(Fluids.CHLORINE).setArea(15, 7.5F);
        mist.setPos(getX() - getDeltaMovement().x, getY() - getDeltaMovement().y, getZ() - getDeltaMovement().z);
        level().addFreshEntity(mist);
    }

    private void nuke() {
        level().addFreshEntity(EntityNukeExplosionMK5.statFac(level(),
                (int) (BombConfig.fatmanRadius * 1.5F), getX(), getY(), getZ()));
        if (level() instanceof ServerLevel server) {
            com.hbm.particle.helper.EntityEffects.muke(server, getX(), getY() + 0.5D, getZ(), false, random.nextInt(100) == 0, 250);
        }
        level().playSound(null, getX(), getY(), getZ(), ModSounds.MUKE_EXPLOSION.get(), SoundSource.BLOCKS,
                15F, 1F);
    }

    public void rotation() {
        Vec3 motion = getDeltaMovement();
        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180D / Math.PI);
        renderPitch = (float) (Math.atan2(motion.y, horizontal) * 180D / Math.PI) - 90F;
        while (renderPitch - renderPitchO < -180F) renderPitchO -= 360F;
        while (renderPitch - renderPitchO >= 180F) renderPitchO += 360F;
        while (yaw - yRotO < -180F) yRotO -= 360F;
        while (yaw - yRotO >= 180F) yRotO += 360F;
        setYRot(yaw);
    }
}
