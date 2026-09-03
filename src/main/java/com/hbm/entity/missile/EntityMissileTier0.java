package com.hbm.entity.missile;

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

/** The tier-0 missile family. Only warheads with a ported payload are launchable. */
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
}
