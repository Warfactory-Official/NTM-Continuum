package com.hbm.blocks.generic;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** High-dose radon in ancient tombs, ported from {@code BlockGasRadonTomb}. */
public class BlockGasRadonTomb extends BlockGasBase {
    public BlockGasRadonTomb(Properties properties) {
        super(properties);
        cloudColor(0.1F, 0.3F, 0.1F);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !GeneralConfig.enableRadon || !(entity instanceof LivingEntity living)) return;
        if (ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD, ArmorRegistry.HazardClass.PARTICLE_FINE)) {
            ArmorUtil.damageGasMaskFilter(living, 4);
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.CREATIVE, 5.0F);
        } else {
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.RAD_BYPASS, 5.0F);
            // The old radaway/radx effects have no ported equivalent in 26.1.
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!GeneralConfig.enableRadon) {
            level.removeBlock(pos, false);
            return;
        }

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (random.nextInt(10) == 0) {
            if (below.is(Blocks.GRASS_BLOCK)) {
                level.setBlock(belowPos, random.nextInt(5) == 0
                        ? Blocks.COARSE_DIRT.defaultBlockState()
                        : ModFalloutBlocks.waste_earth.get().defaultBlockState(), 3);
            } else if ((below.is(BlockTags.LEAVES) || below.is(BlockTags.FLOWERS)
                    || below.is(BlockTags.SAPLINGS) || below.is(Blocks.VINE))
                    && below.getCollisionShape(level, belowPos).isEmpty()) {
                level.removeBlock(belowPos, false);
            }
        }
        if (random.nextInt(600) == 0) {
            level.removeBlock(pos, false);
            return;
        }

        Direction first = random.nextInt(3) == 0 ? Direction.UP : Direction.DOWN;
        drift(level, state, pos, random, first, randomHorizontal(random));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Ancient scrap schedules this gas cell after the source has already been replaced.
        for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) for (int z = -2; z <= 2; z++) {
            int manhattan = Math.abs(x + y + z); // Deliberately odd: this is the old formula.
            if (manhattan > 0 && manhattan < 5) {
                BlockPos target = pos.offset(x, y, z);
                if (isAirLike(level.getBlockState(target)))
                    level.setBlock(target, state, 3);
            }
        }
    }
}
