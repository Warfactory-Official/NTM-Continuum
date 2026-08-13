package com.hbm.tileentity.machine;

import dev.golem.core.world.Compat;
import dev.golem.core.data.CompatBlockEntity;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/** Persistent accident aftermath: an initially burning, highly radioactive reactor wreck. */
public class TileEntityZirnoxDestroyed extends CompatBlockEntity {

    private static final double RADIATION_RANGE = 100.0D;
    private static final float BURNING_RADIATION = 500_000.0F;
    private static final float COOL_RADIATION = 75_000.0F;

    private boolean onFire = true;

    public TileEntityZirnoxDestroyed(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX_DESTROYED.get(), pos, state);
    }

    public boolean isOnFire() {
        return onFire;
    }

    public void extinguish() {
        if (!onFire) return;
        onFire = false;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityZirnoxDestroyed wreck) {
        float radiation = wreck.onFire ? BURNING_RADIATION : COOL_RADIATION;
        ContaminationUtil.radiate(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                RADIATION_RANGE, radiation, 0.0F, 0.0F, 0.0F, 0.0D);

        if (wreck.onFire) {
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(5.0D))) {
                if (living.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 25.0D) {
                    Compat.hurt(living, (ServerLevel) level, level.damageSources().onFire(), 2.0F);
                }
            }
            if (level.getGameTime() % 50L == 0L) {
                level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.0F + level.getRandom().nextFloat(),
                        0.3F + level.getRandom().nextFloat() * 0.7F);
            }
        }

        if (wreck.onFire && level.getRandom().nextInt(5_000) == 0) {
            wreck.extinguish();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityZirnoxDestroyed wreck) {
        // The renderer owns the legacy additive flame quads; no vanilla particle approximation here.
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("onFire", onFire);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        onFire = input.getBooleanOr("onFire", true);
    }
}
