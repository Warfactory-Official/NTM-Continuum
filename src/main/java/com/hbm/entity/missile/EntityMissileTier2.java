package com.hbm.entity.missile;

import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.entity.logic.EntityEMP;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * The "strong" family: double the yield of tier 1, on the larger airframe.
 */
public abstract class EntityMissileTier2 extends EntityMissileBaseNT {

    public EntityMissileTier2(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMissileTier2(EntityType<?> type, Level world, float x, float y, float z, int a, int b) {
        super(type, world, x, y, z, a, b);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(ModItems.plate_steel.get(), 10));
        list.add(new ItemStack(ModItems.plate_titanium.get(), 6));
        list.add(new ItemStack(ModItems.thruster_medium.get(), 1));
        return list;
    }

    public static class EntityMissileStrong extends EntityMissileTier2 {
        public EntityMissileStrong(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileStrong(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_STRONG.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { this.explodeStandard(30F, 32, false); }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_generic_medium.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_strong.get()); }
    }

    /** Fire in the crater, then a 25-block sweep that lights everything flammable around it. */
    public static class EntityMissileIncendiaryStrong extends EntityMissileTier2 {
        public EntityMissileIncendiaryStrong(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileIncendiaryStrong(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_INCENDIARY_STRONG.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) {
            this.explodeStandard(30F, 32, true);
            ExplosionChaos.flameDeath(level(), blockPosition(), 25);
        }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_incendiary_medium.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_incendiary_strong.get()); }
    }

    /** Twenty blasts down instead of fifteen, each half again as strong. */
    public static class EntityMissileBusterStrong extends EntityMissileTier2 {
        public EntityMissileBusterStrong(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileBusterStrong(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_BUSTER_STRONG.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) {
            for (int i = 0; i < 20; i++) {
                level().explode(this, getX(), getY() - i, getZ(), 7.5F, Level.ExplosionInteraction.TNT);
            }
            ExplosionLarge.spawnParticles(level(), getX(), getY(), getZ(), 8);
            ExplosionLarge.spawnShrapnels(level(), getX(), getY(), getZ(), 8);
            ExplosionLarge.spawnRubble(level(), getX(), getY(), getZ(), 8);
        }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_buster_medium.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_buster_strong.get()); }
    }

    /** Starts the reusable old EMP field on impact. */
    public static class EntityMissileEMPStrong extends EntityMissileTier2 {
        public EntityMissileEMPStrong(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileEMPStrong(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_EMP_STRONG.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) {
            EntityEMP emp = new EntityEMP(level());
            emp.setPos(getX(), getY(), getZ());
            level().addFreshEntity(emp);
        }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_generic_medium.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_emp_strong.get()); }
    }

    /** 1.7 EntityMissileClusterStrong: a 15-block blast scattering 50 submunitions. */
    public static class EntityMissileClusterStrong extends EntityMissileTier2 {
        public EntityMissileClusterStrong(EntityType<?> type, Level level) {
            super(type, level);
            this.isCluster = true;
        }
        public EntityMissileClusterStrong(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(com.hbm.entity.ModNtmEntities.MISSILE_CLUSTER_STRONG.get(), level, x, y, z, targetX, targetZ);
            this.isCluster = true;
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 15F, Level.ExplosionInteraction.TNT);
            com.hbm.explosion.ExplosionChaos.cluster(level(), getX(), getY(), getZ(), 50, getYRot(), getXRot(),
                    (float) Math.PI * 0.25F, (float) Math.PI * 0.25F, 1F);
        }

        @Override
        public void cluster() {
            this.onMissileImpact(null);
        }

        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_cluster_medium.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_cluster_strong.get()); }
    }
}
