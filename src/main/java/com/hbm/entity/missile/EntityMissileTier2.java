package com.hbm.entity.missile;

import com.hbm.explosion.ExplosionChaos;
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
 * The "strong" family: double the yield of tier 1, on the larger airframe.
 *
 * <p>Not ported yet: {@code EntityMissileClusterStrong} (needs {@code EntityRocket}) and
 * {@code EntityMissileEMPStrong} (needs {@code EntityEMP}) — IMPLEMENTATION.md §20.
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
}
