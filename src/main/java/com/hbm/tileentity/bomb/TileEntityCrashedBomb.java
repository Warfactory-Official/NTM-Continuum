package com.hbm.tileentity.bomb;

import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import dev.golem.core.data.CompatBlockEntity;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.hbm.blocks.bomb.BlockCrashedBomb;
import com.hbm.tileentity.ModExplosivesBlockEntities;

/** 1.7 {@code TileEntityCrashedBomb}: every other tick a dud irradiates whatever stands near it, by kind. */
public class TileEntityCrashedBomb extends CompatBlockEntity {

    public TileEntityCrashedBomb(BlockPos pos, BlockState state) {
        super(ModExplosivesBlockEntities.CRASHED_BOMB.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCrashedBomb dud) {
        if (level.isClientSide() || level.getGameTime() % 2 != 0) return;
        if (!(state.getBlock() instanceof BlockCrashedBomb bomb)) return;

        switch (bomb.type) {
            case BALEFIRE -> dud.affectEntities((entity, intensity) ->
                    ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, 1F * intensity), 15D);
            case NUKE -> dud.affectEntities((entity, intensity) ->
                    ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, 0.25F * intensity), 10D);
            case SALTED -> dud.affectEntities((entity, intensity) ->
                    ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, 0.5F * intensity), 10D);
            case CONVENTIONAL -> { }
        }
    }

    public void affectEntities(BiConsumer<LivingEntity, Float> effect, double range) {
        double cx = worldPosition.getX() + 0.5, cy = worldPosition.getY() + 0.5, cz = worldPosition.getZ() + 0.5;
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(cx, cy, cz, cx, cy, cz).inflate(range, range, range));
        for (LivingEntity entity : list) {
            double dist = Math.sqrt(worldPosition.distToCenterSqr(entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ()));
            if (dist > range) continue;
            float intensity = (float) (1D - dist / range);
            effect.accept(entity, intensity);
        }
    }
}
