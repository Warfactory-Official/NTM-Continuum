package com.hbm.entity.missile;

import com.hbm.explosion.ExplosionChaos;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;

/**
 * The "huge" family: the last tier before the warhead goes nuclear, and the first that needs the big
 * launch pad and kerosene/LOX.
 *
 * <p>{@code EntityMissileShuttle} lives in its own file. There is no endo/exo/carrier missile: upstream
 * retired those items (they have no entity class in 1.7.10 either).
 */
public abstract class EntityMissileTier3 extends EntityMissileBaseNT {

    public EntityMissileTier3(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMissileTier3(EntityType<?> type, Level world, float x, float y, float z, int a, int b) {
        super(type, world, x, y, z, a, b);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(ModItems.plate_steel.get(), 16));
        list.add(new ItemStack(ModItems.plate_titanium.get(), 10));
        list.add(new ItemStack(ModItems.thruster_large.get(), 1));
        return list;
    }

    /**
     * Four nozzles, upstream's exactly.
     *
     * <p>The offset vector is built by yawing onto the heading, pitching, and yawing straight back —
     * which leaves a vector of length ½ perpendicular to the body — and the four trails are that
     * vector and three shuffles of its components. The shuffles are not a clean 90° rotation (the
     * third and fourth reuse {@code z} where {@code y} would be expected); that is upstream's, and it
     * is kept, because what it produces is the flared four-engine plume players know.
     */
    @Override
    protected void spawnContrail() {
        Vec3 thrust = rotateYaw(rotatePitch(rotateYaw(new Vec3(0, 0, 0.5), contrailYawRadians()),
                contrailPitchRadians()), -contrailYawRadians());

        this.spawnContrailWithOffset(thrust.x, thrust.y, thrust.z);
        this.spawnContrailWithOffset(-thrust.z, thrust.y, thrust.x);
        this.spawnContrailWithOffset(-thrust.x, -thrust.z, -thrust.z);
        this.spawnContrailWithOffset(thrust.z, -thrust.z, -thrust.x);
    }

    public static class EntityMissileBurst extends EntityMissileTier3 {
        public EntityMissileBurst(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileBurst(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_BURST.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { this.explodeStandard(50F, 48, false); com.hbm.particle.helper.ExplosionCreator.composeEffectLarge(level(), getX(), getY(), getZ()); }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_generic_large.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_burst.get()); }
    }

    /**
     * The largest conventional warhead: a 50-power blast that leaves fire, then <em>two</em> sweeps —
     * a 10-block one that ignites everything regardless of material, inside a 25-block one that
     * ignites what will burn.
     */
    public static class EntityMissileInferno extends EntityMissileTier3 {
        public EntityMissileInferno(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileInferno(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_INFERNO.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) {
            this.explodeStandard(50F, 48, true);
            com.hbm.particle.helper.ExplosionCreator.composeEffectLarge(level(), getX(), getY(), getZ());
            // 1.7 Tier3:76-77, (int)-truncated centre
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                ExplosionChaos.igniteAllBlocks(server, (int) getX(), (int) getY(), (int) getZ(), 10);
                ExplosionChaos.igniteFlammableBlocks(server, (int) getX(), (int) getY(), (int) getZ(), 25);
            }
        }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_incendiary_large.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_inferno.get()); }
    }

    /** 1.7 EntityMissileRain: a 25-block blast scattering 100 submunitions. */
    public static class EntityMissileRain extends EntityMissileTier3 {
        public EntityMissileRain(EntityType<?> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }
        public EntityMissileRain(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(com.hbm.entity.ModNtmEntities.MISSILE_RAIN.get(), level, x, y, z, targetX, targetZ);
            this.isCluster = true;
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 25F, Level.ExplosionInteraction.TNT);
            com.hbm.explosion.ExplosionChaos.cluster(level(), getX(), getY(), getZ(), 100, getYRot(), getXRot(),
                    (float) Math.PI * 0.25F, (float) Math.PI * 0.25F, 1F);
        }

        @Override
        public void cluster() {
            this.onMissileImpact(null);
        }

        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_cluster_large.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_rain.get()); }
    }

    /**
     * 1.7 EntityMissileDrill: bores a 30-deep shaft of eroding blasts straight down.
     */
    public static class EntityMissileDrill extends EntityMissileTier3 {
        public EntityMissileDrill(EntityType<?> type, Level level) { super(type, level); }
        public EntityMissileDrill(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(com.hbm.entity.ModNtmEntities.MISSILE_DRILL.get(), level, x, y, z, targetX, targetZ);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            // 1.7: 30 stacked ExplosionNT(10F), each with ExAttrib.ERRODE (concrete -> gravel, at 60%).
            for (int i = 0; i < 30; i++) {
                com.hbm.explosion.ExplosionNT.explodeErode(level(), this, getX(), getY() - i, getZ(), 10F);
            }
            com.hbm.explosion.ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 25);
            com.hbm.explosion.ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 12);
            com.hbm.explosion.ExplosionLarge.jolt(level(), getX(), getY(), getZ(), 10, 50, 1);
        }

        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_buster_large.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_drill.get()); }
    }
}
