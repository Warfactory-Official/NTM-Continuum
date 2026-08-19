package com.hbm.blocks.generic;

import com.hbm.handler.ArmorUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Radioactive reaction sludge — what a Watz power plant becomes when its mud volume overflows.
 *
 * <p>Upstream this is a flowing {@code BlockFluidClassic}. The port has no vanilla flowing fluids at
 * all (HBM fluids live in their own network and never became {@code FlowingFluid}s), so this is a
 * <b>static</b> block: it keeps the parts a player actually experiences — you get stuck in it like a
 * cobweb, and it poisons you unless you are wearing a hazmat suit — and drops the spreading. Recorded
 * in IMPLEMENTATION.md §20; revisit when the port gains flowing fluids.
 */
public class BlockMud extends dev.golem.core.world.CompatBlock {

    /** Damage per contact tick, matching upstream's mud poisoning. */
    private static final float DAMAGE = 8F;

    public BlockMud(Properties properties) {
        super(properties);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // The old block called setInWeb(); 26.1 spells the same effect as a movement multiplier.
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));

        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (ArmorUtil.checkForHazmat(living)) return;
        living.hurt(level.damageSources().magic(), DAMAGE);
    }
}
