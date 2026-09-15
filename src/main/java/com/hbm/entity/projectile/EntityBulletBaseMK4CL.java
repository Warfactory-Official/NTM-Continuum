package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.EntityChunkLoader;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.items.weapon.sedna.BulletConfig;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The original MK4 bullet with the original entity-backed chunk claim.  The claim follows the bullet in
 * a 3x3 envelope so a high-velocity shot cannot enter an unloaded chunk between server ticks.
 */
public class EntityBulletBaseMK4CL extends EntityBulletBaseMK4 implements IChunkLoader {

    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 1);

    public EntityBulletBaseMK4CL(EntityType<? extends EntityBulletBaseMK4CL> type, Level level) {
        super(type, level);
    }

    public EntityBulletBaseMK4CL(LivingEntity shooter, BulletConfig config, float baseDamage, float spread,
            double sideOffset, double heightOffset, double frontOffset) {
        this(ModEntities.BULLET_MK4_CL.get(), shooter.level());
        this.thrower = shooter;
        setBulletConfig(config);
        this.damage = baseDamage * config.damageMult;

        float yaw = shooter.getYRot();
        float pitch = shooter.getXRot();
        setYRot(yaw);
        setXRot(pitch);
        var offset = new net.minecraft.world.phys.Vec3(sideOffset, heightOffset, frontOffset)
                .xRot(-pitch * ((float) Math.PI / 180F))
                .yRot(-yaw * ((float) Math.PI / 180F));
        setPos(shooter.getX() + offset.x, shooter.getEyeY() + offset.y, shooter.getZ() + offset.z);

        double x = -Math.sin(yaw * Math.PI / 180D) * Math.cos(pitch * Math.PI / 180D);
        double z = Math.cos(yaw * Math.PI / 180D) * Math.cos(pitch * Math.PI / 180D);
        double y = -Math.sin(pitch * Math.PI / 180D);
        shoot(x, y, z, spread);
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) chunkLoader.tick();
        super.tick();
    }

    public void startChunkLoading() {
        chunkLoader.tick();
    }

    @Override public void initChunkLoading() { chunkLoader.initChunkLoading(); }
    @Override public void loadNeighboringChunks(int chunkX, int chunkZ) { chunkLoader.loadNeighboringChunks(chunkX, chunkZ); }
    @Override public void clearChunkLoader() { chunkLoader.clearChunkLoader(); }

    @Override
    public void remove(RemovalReason reason) {
        chunkLoader.clearChunkLoader();
        super.remove(reason);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        chunkLoader.markRestoredFromNbt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
    }
}
