package com.hbm.blocks.generic;

import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.ArmorUtil;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Short-lived radioactive gas released by a burning ZIRNOX wreck. */
public class BlockGasMeltdown extends BlockGasBase {

    public BlockGasMeltdown(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.removeBlock(pos, false);
            return;
        }
        BlockPos target = pos.relative(random.nextInt(5) == 0 ? Direction.UP
                : Direction.Plane.HORIZONTAL.getRandomDirection(random));
        if (level.getBlockState(target).isAir()) {
            level.setBlock(target, state, 2);
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        boolean filtered = ArmorRegistry.hasProtection(living, EquipmentSlot.HEAD,
                ArmorRegistry.HazardClass.PARTICLE_FINE);
        if (filtered) {
            ArmorUtil.damageGasMaskFilter(living, 1);
        } else {
            HbmLivingProps.incrementAsbestos(living, 1);
        }
        ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                filtered ? ContaminationUtil.ContaminationType.CREATIVE
                        : ContaminationUtil.ContaminationType.RAD_BYPASS, 0.5F);
        Effects.addRegistered(living, ModEffects.RADIATION.get(), 60 * 20, 2);
    }
}
