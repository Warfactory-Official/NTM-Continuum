package com.hbm.entity.particle;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.explosion.ExplosionChaos;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 {@code entity/particle/EntityChlorineFX}: the chlorine cloud - drifts, eats armour and poisons whatever is not sealed.
 *
 * <p>1.7 built this on {@code EntityModFX}, its fake-particle-as-entity base. It stays an entity here
 * because the tick carries server-side gameplay - see {@link EntityOrangeFX}, the same shape. The purely
 * cosmetic members of that 1.7 family are real particles in 26.1 instead.
 */
public class EntityChlorineFX extends CompatEntity {

    private int particleAge;
    private int maxAge;

    public EntityChlorineFX(EntityType<? extends EntityChlorineFX> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public EntityChlorineFX(Level level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        this(ModNtmEntities.CHLORINE_FX.get(), level);
        setPos(x, y, z);
        setDeltaMovement(ModFXMotion.spawn(motionX, motionY, motionZ));
    }

    public int particleAge() {
        return particleAge;
    }

    public int maxAge() {
        return maxAge;
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    @Override
    protected void saveData(DataOut output) {
    }

    @Override
    protected void loadData(DataIn input) {
        discard(); // 1.7 EntityFX never persisted
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    public void tick() {
        xo = getX();
        yo = getY();
        zo = getZ();
        super.tick();

        if (maxAge < 700) maxAge = random.nextInt(101) + 700;

        if (random.nextInt(50) == 0) ExplosionChaos.poison(level(), getX(), getY(), getZ(), 2D);

        if (++particleAge >= maxAge) {
            discard();
            return;
        }

        double motionX = getDeltaMovement().x * 0.76D;
        double motionY = getDeltaMovement().y * 0.76D;
        double motionZ = getDeltaMovement().z * 0.76D;

        if (onGround()) {
            motionX *= 0.7D;
            motionZ *= 0.7D;
        }

        // 1.7: rain drags the cloud down where the sky is visible
        if (level().isRaining() && level().canSeeSky(blockPosition())) motionY -= 0.01D;

        setDeltaMovement(motionX, motionY, motionZ);

        // 1.7 steps the move in four sub-steps and dies on the first solid block it meets
        for (int i = 0; i < 4; i++) {
            setPos(getX() + motionX / 4D, getY() + motionY / 4D, getZ() + motionZ / 4D);

            BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ()); // 1.7: cast truncation
            if (!Compat.isSolidRender(level().getBlockState(pos))) continue;

            if (!level().isClientSide() && random.nextInt(5) != 0) discard();

            setPos(getX() - motionX / 4D, getY() - motionY / 4D, getZ() - motionZ / 4D);
            setDeltaMovement(0D, 0D, 0D);
            motionX = motionY = motionZ = 0D;
        }
    }
}
