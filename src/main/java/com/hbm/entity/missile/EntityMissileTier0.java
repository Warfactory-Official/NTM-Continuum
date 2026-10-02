package com.hbm.entity.missile;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/** The tier-0 missile family: micro, test, taint, schrabidium, black hole and EMP. */
public abstract class EntityMissileTier0 extends EntityMissileBaseNT {

    protected EntityMissileTier0(EntityType<?> type, Level level) { super(type, level); }

    protected EntityMissileTier0(EntityType<?> type, Level level, float x, float y, float z,
                                 int targetX, int targetZ) {
        super(type, level, x, y, z, targetX, targetZ);
    }

    @Override
    public List<ItemStack> getDebris() {
        List<ItemStack> debris = new ArrayList<>();
        debris.add(new ItemStack(ModAutogenItems.get(MaterialShapes.WIRE, Mats.MAT_ALUMINIUM), 4));
        debris.add(new ItemStack(ModItems.plate_titanium.get(), 4));
        debris.add(new ItemStack(ModAutogenItems.get(MaterialShapes.SHELL, Mats.MAT_ALUMINIUM), 2));
        debris.add(new ItemStack(ModItems.ducttape.get()));
        return debris;
    }

    @Override
    protected float getContrailScale() { return 0.5F; }

    public static class EntityMissileMicro extends EntityMissileTier0 {
        public EntityMissileMicro(EntityType<?> type, Level level) { super(type, level); }

        public EntityMissileMicro(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(ModMissileEntities.MISSILE_MICRO.get(), level, x, y, z, targetX, targetZ);
        }

        @Override
        public void onMissileImpact(HitResult hit) {
            if (!level().isClientSide()) {
                ExplosionNukeSmall.explode(level(), getX(), getY() + 0.5D, getZ(), ExplosionNukeSmall.PARAMS_HIGH);
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModAmmoItems.ammo_standard_nuke_high.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(com.hbm.items.ModMissileItems.missile_micro.get());
        }
    }

    public static class EntityMissileTaint extends EntityMissileTier0 {
        public EntityMissileTaint(EntityType<?> type, Level level) { super(type, level); }

        public EntityMissileTaint(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(ModMissileEntities.MISSILE_TAINT.get(), level, x, y, z, targetX, targetZ);
        }

        @Override
        public void onMissileImpact(HitResult hit) {
            if (level().isClientSide()) return;

            level().explode(this, getX(), getY(), getZ(), 10.0F, Level.ExplosionInteraction.TNT);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int i = 0; i < 100; i++) {
                int x = random.nextInt(11) + (int) getX() - 5;
                int y = random.nextInt(11) + (int) getY() - 5;
                int z = random.nextInt(11) + (int) getZ() - 5;
                pos.set(x, y, z);
                BlockState state = level().getBlockState(pos);
                if (state.isCollisionShapeFullBlock(level(), pos) && !state.isAir()) {
                    level().setBlock(pos, ModBombBlocks.taint.get().defaultBlockState(), 2);
                }
            }
        }

        @Override
        public ItemStack getDebrisRareDrop() {
            return new ItemStack(ModItems.powder_spark_mix.get());
        }

        @Override
        public ItemStack getMissileItemForInfo() {
            return new ItemStack(com.hbm.items.ModMissileItems.missile_taint.get());
        }
    }

    /** 1.7 EntityMissileTest: chars the ground into sellafite in a 50-block sphere (ntm-next copy). */
    public static class EntityMissileTest extends EntityMissileTier0 {
        public EntityMissileTest(EntityType<?> type, Level level) { super(type, level); }
        public EntityMissileTest(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(com.hbm.entity.ModNtmEntities.MISSILE_TEST.get(), level, x, y, z, targetX, targetZ);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            int x = (int) Math.floor(getX());
            int y = (int) Math.floor(getY());
            int z = (int) Math.floor(getZ());
            int range = 50;

            for (int iX = -range; iX <= range; iX++) {
                for (int iY = -range; iY <= range; iY++) {
                    for (int iZ = -range; iZ <= range; iZ++) {
                        double dist = Math.sqrt(iX * iX + iY * iY + iZ * iZ);
                        if (dist > range) continue;
                        BlockPos pos = new BlockPos(x + iX, y + iY, z + iZ);
                        BlockState state = level().getBlockState(pos);
                        boolean slaked = state.getBlock() == com.hbm.blocks.ModFalloutBlocks.sellafield_slaked.get();
                        // 1.7: getBlockMetadata on sellafield_slaked == SHADE (0 otherwise)
                        int shade = state.hasProperty(com.hbm.blocks.generic.BlockSellafieldSlaked.SHADE)
                                ? state.getValue(com.hbm.blocks.generic.BlockSellafieldSlaked.SHADE) : 0;
                        // 1.7 clamps to meta 12; this port's slaked block carries ten shades, so the top is 9.
                        int charMeta = Math.min((int) Mth.clamp(12 - (dist / range) * (dist / range) * 13, 0, 12), 9);

                        // 1.7: isNormalCube -> isSolidRender
                        if (dev.golem.core.world.Compat.isSolidRender(state)) {
                            if (!slaked || shade < charMeta) {
                                level().setBlock(pos, com.hbm.blocks.ModFalloutBlocks.sellafield_slaked.get().defaultBlockState()
                                        .setValue(com.hbm.blocks.generic.BlockSellafieldSlaked.SHADE, charMeta), 3);
                            }
                        } else {
                            level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        @Override public ItemStack getDebrisRareDrop() { return ItemStack.EMPTY; } // 1.7: null rare drop
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(com.hbm.items.ModMissileItems.missile_test.get()); }
    }

    /** 1.7 EntityMissileSchrabidium: an anti-schrabidium fleija blast of {@code BombConfig.aSchrabRadius}. */
    public static class EntityMissileSchrabidium extends EntityMissileTier0 {
        public EntityMissileSchrabidium(EntityType<?> type, Level level) { super(type, level); }
        public EntityMissileSchrabidium(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(com.hbm.entity.ModNtmEntities.MISSILE_SCHRABIDIUM.get(), level, x, y, z, targetX, targetZ);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            int radius = com.hbm.config.BombConfig.aSchrabRadius;

            com.hbm.entity.logic.EntityNukeExplosionMK3 ex =
                    com.hbm.entity.logic.EntityNukeExplosionMK3.statFacFleija(level(), getX(), getY(), getZ(), radius);
            if (!ex.isRemoved()) { // 1.7: !ex.isDead
                level().addFreshEntity(ex); // 1.7: WorldUtil.loadAndSpawnEntityInWorld(ex)
                level().addFreshEntity(
                        com.hbm.entity.effect.EntityCloudFleija.statFac(level(), radius, getX(), getY(), getZ()));
            }
        }

        @Override public ItemStack getDebrisRareDrop() { return ItemStack.EMPTY; } // 1.7: null rare drop
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(com.hbm.items.ModMissileItems.missile_schrabidium.get()); }
    }

    /** 1.7 EntityMissileBHole: a small blast that leaves a 1.5-size black hole. */
    public static class EntityMissileBHole extends EntityMissileTier0 {
        public EntityMissileBHole(EntityType<?> type, Level level) { super(type, level); }
        public EntityMissileBHole(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(com.hbm.entity.ModNtmEntities.MISSILE_BHOLE.get(), level, x, y, z, targetX, targetZ);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            level().explode(this, getX(), getY(), getZ(), 1.5F, Level.ExplosionInteraction.TNT);

            com.hbm.entity.effect.EntityBlackHole hole = new com.hbm.entity.effect.EntityBlackHole(
                    com.hbm.entity.ModEntities.BLACK_HOLE.get(), level(), 1.5F);
            hole.setPos(getX(), getY(), getZ());
            level().addFreshEntity(hole);
        }

        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(com.hbm.items.ModSingularityItems.black_hole.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(com.hbm.items.ModMissileItems.missile_bhole.get()); }
    }

    /** 1.7 EntityMissileEMP: a 50-block EMP drain plus the expanding EMP wave. */
    public static class EntityMissileEMP extends EntityMissileTier0 {
        public EntityMissileEMP(EntityType<?> type, Level level) { super(type, level); }
        public EntityMissileEMP(Level level, float x, float y, float z, int targetX, int targetZ) {
            super(com.hbm.entity.ModNtmEntities.MISSILE_EMP.get(), level, x, y, z, targetX, targetZ);
        }

        @Override
        public void onMissileImpact(HitResult mop) {
            com.hbm.explosion.ExplosionNukeGeneric.empBlast(level(),
                    new BlockPos((int) getX(), (int) getY(), (int) getZ()), 50);

            com.hbm.entity.effect.EntityEMPBlast wave = new com.hbm.entity.effect.EntityEMPBlast(
                    com.hbm.entity.ModNtmEntities.EMP_BLAST.get(), level());
            wave.setPos(getX(), getY(), getZ());
            wave.setMaxAge(50);
            level().addFreshEntity(wave);
        }

        @Override public ItemStack getDebrisRareDrop() { return new ItemStack(com.hbm.blocks.ModBombs2Blocks.emp_bomb.get()); }
        @Override public ItemStack getMissileItemForInfo() { return new ItemStack(com.hbm.items.ModMissileItems.missile_emp.get()); }
    }
}
