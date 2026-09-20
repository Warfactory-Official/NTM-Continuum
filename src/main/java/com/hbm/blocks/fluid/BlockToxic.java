package com.hbm.blocks.fluid;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code ToxicBlock} ({@code toxic_block}, "Stereotypical Green Ooze"): webs and irradiates
 * (1 rad per contact tick, creative-type contamination) whatever is inside; touching another liquid
 * slakes it into {@code sellafield_slaked}. Also what {@link SchrabidicBlock} does with a stronger
 * web check, so the two share this class.
 */
public class BlockToxic extends BlockStaticFluid {

    private final boolean web;

    public BlockToxic(Properties properties, boolean web) {
        super(properties);
        this.web = web;
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (web) setInWeb(entity, state);
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 1.0F);
        }
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level.isClientSide()) return;
        for (Direction dir : Direction.values()) {
            if (isForeignLiquid(level, pos.relative(dir))) {
                level.setBlock(pos, ModFalloutBlocks.sellafield_slaked.get().defaultBlockState(), 3);
                return;
            }
        }
    }
}
