package com.hbm.blocks.machine;

import com.hbm.entity.logic.EntityNukeExplosionMK3;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code MachineFieldDisturber} (High Energy Field Jammer): every ten ticks it renews its claim in
 * {@link EntityNukeExplosionMK3#at}, which the FLEIJA/solinium blasts check to fizzle nearby, valid for the next
 * 100 ticks. It needs no power and has no creative tab in 1.7.
 */
public class MachineFieldDisturber extends CompatBlock {

    public static final int TICK_RATE = 10;

    public MachineFieldDisturber(Properties properties) {
        super(properties);
    }

    public int tickRate() {
        return TICK_RATE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, tickRate());
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.scheduleTick(pos, this, tickRate());
        EntityNukeExplosionMK3.at.put(new EntityNukeExplosionMK3.ATEntry(level.dimension(), pos.getX(), pos.getY(), pos.getZ()),
                level.getGameTime() + 100);
    }
}
