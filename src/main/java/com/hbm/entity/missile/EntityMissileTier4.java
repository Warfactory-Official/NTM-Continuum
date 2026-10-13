package com.hbm.entity.missile;

import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.config.BombConfig;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ModItems;
import com.hbm.items.ModMissileItems;
import dev.golem.core.data.SynchedData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Atlas family: nuclear warheads, launched from the silo.
 *
 * <p>Each of these spawns the same two entities the Fat Man does, an {@link EntityNukeExplosionMK5}
 * crater and an {@link EntityNukeTorex} mushroom cloud, differing only in radius and fallout.
 *
 * <p>{@link #ROT_IDX} is the pad's facing, pushed to the client by the launch pad at spawn. Its only
 * consumer upstream is the contrail, which is not ported, so it is carried and not read; the launch pad
 * sets it, and a future contrail needs it.
 */
public abstract class EntityMissileTier4 extends EntityMissileBaseNT {

    public static final EntityDataAccessor<Byte> ROT_IDX =
            SynchedEntityData.defineId(EntityMissileTier4.class, EntityDataSerializers.BYTE);

    public EntityMissileTier4(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityMissileTier4(EntityType<?> type, Level world, float x, float y, float z, int a, int b) {
        super(type, world, x, y, z, a, b);
    }

    @Override
    protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(ROT_IDX, (byte) 5);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(ModItems.plate_titanium.get(), 16));
        list.add(new ItemStack(ModItems.plate_steel.get(), 20));
        list.add(new ItemStack(ModItems.plate_aluminium.get(), 12));
        list.add(new ItemStack(ModItems.thruster_large.get(), 1));
        return list;
    }

    /** Crater + cloud at {@code radius}, with {@code falloutAdd} extra fallout and no radiation gate. */
    protected void detonateNuclear(int radius, int falloutAdd, boolean rad) {
        EntityNukeExplosionMK5 mk5 = rad
                ? EntityNukeExplosionMK5.statFac(level(), radius, getX(), getY(), getZ())
                : EntityNukeExplosionMK5.statFacNoRad(level(), radius, getX(), getY(), getZ());
        if (falloutAdd > 0) mk5.moreFallout(falloutAdd);
        if (thrower != null) mk5.setDetonator(thrower);
        if (level().addFreshEntity(mk5)) mk5.startChunkLoading();

        if (BombConfig.enableNukeClouds) {
            EntityNukeTorex torex = EntityNukeTorex.statFac(level(), getX(), getY(), getZ(), radius);
            if (level().addFreshEntity(torex)) torex.startChunkLoading();
        }
    }

    /**
     * Three trails: one to the side, one down the axis, one to the other side.
     *
     * <p>{@link #ROT_IDX}, the pad's facing, pushed over at spawn, pre-rotates the side offset so
     * the boosters sit where the model has them however the pad was placed. This is that byte's only
     * consumer, upstream and here.
     */
    @Override
    protected void spawnContrail() {
        Vec3 thrust = new Vec3(0, 0, 1);
        switch (this.entityData.get(ROT_IDX)) {
            case 2 -> thrust = rotateYaw(thrust, -Math.PI / 2D);
            case 4 -> thrust = rotateYaw(thrust, -Math.PI);
            case 3 -> thrust = rotateYaw(thrust, -Math.PI / 2D * 3D);
            default -> { }
        }
        thrust = rotateYaw(rotatePitch(rotateYaw(thrust, contrailYawRadians()), contrailPitchRadians()),
                -contrailYawRadians());

        this.spawnContrailWithOffset(thrust.x, thrust.y, thrust.z);
        this.spawnContrailWithOffset(0, 0, 0);
        this.spawnContrailWithOffset(-thrust.x, -thrust.z, -thrust.z);
    }

    public static class EntityMissileNuclear extends EntityMissileTier4 {
        public EntityMissileNuclear(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileNuclear(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_NUCLEAR.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { detonateNuclear(BombConfig.missileRadius, 0, true); }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_nuclear.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_nuclear.get()); }
    }

    /** MIRV: double radius. Upstream never actually splits it, the "cluster" is the yield. */
    public static class EntityMissileMirv extends EntityMissileTier4 {
        public EntityMissileMirv(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileMirv(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_MIRV.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { detonateNuclear(BombConfig.missileRadius * 2, 0, true); }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_mirv.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_nuclear_cluster.get()); }
    }

    /** Doomsday: MIRV yield plus a hundred extra fallout, the salted warhead. */
    public static class EntityMissileDoomsday extends EntityMissileTier4 {
        public EntityMissileDoomsday(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileDoomsday(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_DOOMSDAY.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { detonateNuclear(BombConfig.missileRadius * 2, 100, true); }
        @Override public @Nullable List<ItemStack> getDebris() { return null; }
        @Override public @Nullable ItemStack getDebrisRareDrop() { return null; }
        @Override public String getRadarName() { return "radar.target.doomsday"; }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_doomsday.get()); }
    }

    /** The rusted one found in the world: same fallout, half the radius, and it cannot be launched. */
    public static class EntityMissileDoomsdayRusted extends EntityMissileDoomsday {
        public EntityMissileDoomsdayRusted(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileDoomsdayRusted(Level world, float x, float y, float z, int a, int b) {
            super(world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { detonateNuclear(BombConfig.missileRadius, 100, true); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_doomsday_rusted.get()); }
    }

    /** N2 mine warhead on a missile: {@code n2Radius / 12 · 5}, and no radiation. */
    public static class EntityMissileN2 extends EntityMissileTier4 {
        public EntityMissileN2(EntityType<?> type, Level world) { super(type, world); }
        public EntityMissileN2(Level world, float x, float y, float z, int a, int b) {
            super(ModMissileEntities.MISSILE_N2.get(), world, x, y, z, a, b);
        }
        @Override public void onMissileImpact(HitResult hit) { detonateNuclear(BombConfig.n2Radius / 12 * 5, 0, false); }
        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_n2.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_n2.get()); }
    }

    /** 1.7 EntityMissileVolcano: a large blast that seeds a volcano core in a 3x3x3 of volcanic lava. */
    public static class EntityMissileVolcano extends EntityMissileTier4 {
        public EntityMissileVolcano(EntityType<?> type, Level level) { super(type, level); }
        public EntityMissileVolcano(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(ModNtmEntities.MISSILE_VOLCANO.get(), level, x, y, z, targetX, targetZ);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            ExplosionLarge.explode(level(), getX(), getY(), getZ(), 10.0F, true, true, true);

            // 1.7 then fills a 3x3x3 with volcanic lava (plain setBlock, meta 0, flags 3) and puts the volcano core
            // (meta 0 = the bare, static-active "volcano_core") in the centre cell. Positions use floor(pos + offset).
            BlockState lava =
                    ModFluidNetBlocks.volcanic_lava_block.get().defaultBlockState();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        level().setBlock(new BlockPos(
                                (int) Math.floor(getX() + x), (int) Math.floor(getY() + y), (int) Math.floor(getZ() + z)),
                                lava, Block.UPDATE_ALL);
                    }
                }
            }
            level().setBlock(new BlockPos(
                    (int) Math.floor(getX()), (int) Math.floor(getY()), (int) Math.floor(getZ())),
                    ModTerrainBlocks.volcanoCore("volcano_core").defaultBlockState(),
                    Block.UPDATE_ALL);
        }

        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(ModItems.warhead_volcano.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(ModMissileItems.missile_volcano.get()); }
    }
}
