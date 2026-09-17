package com.hbm.entity.particle;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionNukeGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * 1.7 EntityOrangeFX - the physically falling Agent Orange cloudlet.
 */
public class EntityOrangeFX extends CompatEntity {

    private int particleAge;
    private int maxAge;

    public EntityOrangeFX(EntityType<? extends EntityOrangeFX> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public EntityOrangeFX(Level level, double x, double y, double z, double motionX, double motionY, double motionZ) {
        this(ModNtmEntities.AGENT_ORANGE.get(), level);
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
        discard();
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
        if (maxAge < 900) maxAge = random.nextInt(301) + 900;
        if (!level().isClientSide() && random.nextInt(50) == 0)
            ExplosionChaos.poison(level(), getX(), getY(), getZ(), 2D);
        if (++particleAge >= maxAge) {
            discard();
            return;
        }
        double motionX = getDeltaMovement().x * 0.86D;
        double motionY = getDeltaMovement().y * 0.86D - 0.1D;
        double motionZ = getDeltaMovement().z * 0.86D;
        setDeltaMovement(motionX, motionY, motionZ);
        for (int i = 0; i < 4; i++) {
            setPos(getX() + motionX / 4D, getY() + motionY / 4D, getZ() + motionZ / 4D);
            BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ()); // 1.7: cast truncation
            if (level().getBlockState(pos).isAir()) continue;
            discard();
            for (int x = -1; x < 2; x++)
                for (int y = -1; y < 2; y++)
                    for (int z = -1; z < 2; z++) {
                        BlockPos affected = pos.offset(x, y, z);
                        if (level().getBlockState(affected).is(Blocks.GRASS_BLOCK)) {
                            level().setBlock(affected, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                        } else {
                            ExplosionNukeGeneric.solinium(level(), affected);
                        }
                    }
        }
    }
}
