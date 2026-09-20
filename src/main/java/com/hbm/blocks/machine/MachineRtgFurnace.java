package com.hbm.blocks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityRtgFurnace;

/** 1.7 {@code MachineRtgFurnace} ({@code machine_rtg_furnace_off}/{@code _on}, deprecated): the RTG-pellet furnace. */
public final class MachineRtgFurnace extends BlockRtgFurnaceBase {

    public MachineRtgFurnace(Properties properties) { super(properties); }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRtgFurnace(pos, state);
    }

    /** 1.7 {@code randomDisplayTick}: the vanilla furnace smoke+flame at the front face while lit. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) return;

        float f = pos.getX() + 0.5F;
        float f1 = pos.getY() + 0.25F + rand.nextFloat() * 6.0F / 16.0F;
        float f2 = pos.getZ() + 0.5F;
        float f3 = 0.52F;
        float f4 = rand.nextFloat() * 0.6F - 0.3F;
        rand.nextFloat();
        rand.nextFloat();

        switch (state.getValue(FACING)) {
            case WEST -> {
                level.addParticle(ParticleTypes.SMOKE, f - f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.FLAME, f - f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
            }
            case EAST -> {
                level.addParticle(ParticleTypes.SMOKE, f + f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.FLAME, f + f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
            }
            case NORTH -> {
                level.addParticle(ParticleTypes.SMOKE, f + f4, f1, f2 - f3, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.FLAME, f + f4, f1, f2 - f3, 0.0D, 0.0D, 0.0D);
            }
            case SOUTH -> {
                level.addParticle(ParticleTypes.SMOKE, f + f4, f1, f2 + f3, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.FLAME, f + f4, f1, f2 + f3, 0.0D, 0.0D, 0.0D);
            }
            default -> { }
        }
    }
}
