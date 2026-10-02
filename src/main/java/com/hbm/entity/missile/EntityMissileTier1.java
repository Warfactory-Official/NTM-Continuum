package com.hbm.entity.missile;

import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * The V2 family: the first missiles a player can build, all on the small launch pad.
 */
public abstract class EntityMissileTier1 extends EntityMissileBaseNT {

    public EntityMissileTier1(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMissileTier1(EntityType<?> type, Level world, float x, float y, float z, int a, int b) {
        super(type, world, x, y, z, a, b);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(ModItems.plate_titanium.get(), 4));
        list.add(new ItemStack(ModItems.thruster_small.get(), 1));
        return list;
    }

    @Override
    protected float getContrailScale() {
        return 0.5F;
    }

    /** Plain high explosive. The reference warhead every other one is measured against. */
    public static class EntityMissileGeneric extends EntityMissileTier1 {
        public EntityMissileGeneric(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileGeneric(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_GENERIC.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { this.explodeStandard(15F, 24, false); }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_generic_small.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_generic.get()); }
    }

    /**
     * A decoy: a trivial warhead on a body that reports itself to radar as a tier 4. It exists to make
     * an ABM battery waste an interceptor, which is why its blip level lies about its tier.
     */
    public static class EntityMissileDecoy extends EntityMissileTier1 {
        public EntityMissileDecoy(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileDecoy(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_DECOY.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) {
            level().explode(this, getX(), getY(), getZ(), 4F, Level.ExplosionInteraction.NONE);
        }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.ingot_steel.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_decoy.get()); }
        @Override public String getRadarName() { return "radar.target.tier4"; }
        @Override public int getBlipLevel() { return com.hbm.api.entity.IRadarDetectableNT.TIER4; }
    }

    /** The same blast as the generic, but it leaves the crater burning. */
    public static class EntityMissileIncendiary extends EntityMissileTier1 {
        public EntityMissileIncendiary(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileIncendiary(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_INCENDIARY.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { this.explodeStandard(15F, 24, true); }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_incendiary_small.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_incendiary.get()); }
    }

    /**
     * Bunker buster: fifteen stacked vanilla blasts, one per block <em>downwards</em>. It digs rather
     * than spreads, which is the entire point — a surface warhead cannot reach a buried base.
     */
    public static class EntityMissileBunkerBuster extends EntityMissileTier1 {
        public EntityMissileBunkerBuster(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileBunkerBuster(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_BUSTER.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) {
            for (int i = 0; i < 15; i++) {
                level().explode(this, getX(), getY() - i, getZ(), 5F, Level.ExplosionInteraction.TNT);
            }
            ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 5);
            ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 5);
            ExplosionLarge.spawnRubble(level(), getX(), getY(), getZ(), 5);
        }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_buster_small.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_buster.get()); }
    }

    /** 1.7 EntityMissileCluster: a 5-block blast scattering 25 submunitions; also the mid-air cluster split. */
    public static class EntityMissileCluster extends EntityMissileTier1 {
        public EntityMissileCluster(EntityType<?> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }
        public EntityMissileCluster(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(com.hbm.entity.ModNtmEntities.MISSILE_CLUSTER.get(), level, x, y, z, targetX, targetZ);
            this.isCluster = true;
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 5F, Level.ExplosionInteraction.TNT);
            com.hbm.explosion.ExplosionChaos.cluster(level(), getX(), getY(), getZ(), 25, getYRot(), getXRot(),
                    (float) Math.PI * 0.25F, (float) Math.PI * 0.25F, 1F);
        }

        @Override
        public void cluster() {
            this.onMissileImpact(null);
        }

        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_cluster_small.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_cluster.get()); }
    }
}
