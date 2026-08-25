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
 * <p>Not ported yet: {@code EntityMissileRain} (cluster → {@code EntityRocket}),
 * {@code EntityMissileDrill} (needs {@code ExplosionNT}'s ERRODE mode and {@code ExplosionLarge.jolt}),
 * {@code EntityMissileEndo}/{@code Exo} (need {@code ExplosionThermo}) and {@code EntityMissileShuttle}
 * (the Soyuz/satellite system) — IMPLEMENTATION.md §20.
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
        @Override public void onMissileImpact(HitResult hit) { this.explodeStandard(50F, 48, false); }
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
            ExplosionChaos.burn(level(), blockPosition(), 10);
            ExplosionChaos.flameDeath(level(), blockPosition(), 25);
        }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_incendiary_large.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_inferno.get()); }
    }
}
