package com.hbm.entity.logic;

import com.hbm.entity.ModSatelliteEntities;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.lib.ModDamageSources.DamageClass;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code EntityOrbitalLaser}: the precision laser's strike. {@link #explode()} runs once, when the satellite
 * fires, before the entity is spawned; the entity itself only lives five ticks so the beam can be drawn
 * ({@code RenderOrbitalLaser}). Nothing is saved.
 */
public class EntityOrbitalLaser extends CompatEntity {

    public static final int maxAge = 5;

    public EntityOrbitalLaser(Level level) {
        this(ModSatelliteEntities.ORBITAL_LASER.get(), level);
    }

    public EntityOrbitalLaser(EntityType<? extends EntityOrbitalLaser> type, Level level) {
        super(type, level);
        // 1.7 ignoreFrustumCheck lives in the renderer (culled() == false)
    }

    @Override
    protected void defineData(SynchedData builder) { }

    @Override
    protected void loadData(DataIn input) { }

    @Override
    protected void saveData(DataOut output) { }

    // 1.7 leaves Entity's default attackEntityFrom (immune)
    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        // 1.7 onUpdate never calls super: no motion, no fire, no water; the level advances tickCount
        if (this.tickCount >= maxAge && !level().isClientSide()) this.discard();
    }

    public void explode() {
        ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), 5F);
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 1_000F).setupPiercing(50F, 0.5F)
                .setDamageClass(DamageClass.LASER));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
        vnt.explode();
    }

    // 1.7 getBrightnessForRender 15728880 / isInRangeToRenderDist < 25000: the renderer draws full-bright and unculled
}
