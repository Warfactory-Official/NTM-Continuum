package com.hbm.blocks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityDiFurnaceRTG;

/** 1.7 {@code MachineDiFurnaceRTG} ({@code machine_difurnace_rtg_off}/{@code _on}, deprecated): the nuclear blast furnace. */
public final class MachineDiFurnaceRTG extends BlockRtgFurnaceBase {

    public MachineDiFurnaceRTG(Properties properties) { super(properties); }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDiFurnaceRTG(pos, state);
    }

    /**
     * 1.7 {@code onBlockAdded}. The LIT flip is a state change of the same block, where 1.7's
     * {@code updateBlockState} restored the old metadata right after the swap, so only a fresh placement
     * runs the solid-neighbour correction.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) correctDefaultDirection(state, level, pos);
    }

    /**
     * 1.7 {@code onBlockPlacedBy} runs after {@code onBlockAdded}, so a player's facing wins over the
     * solid-neighbour correction; {@code setPlacedBy} runs after {@code onPlace} here for the same order.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer == null || level.isClientSide()) return;
        Direction facing = placer.getDirection().getOpposite();
        if (facing != state.getValue(FACING)) level.setBlock(pos, state.setValue(FACING, facing), 2);
    }

    /** 1.7 {@code randomDisplayTick}: a front flame and a smoke puff on the top face while lit. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!state.getValue(LIT)) return;

        float f = pos.getX() + 0.5F;
        float f1 = pos.getY() + 0.25F + rand.nextFloat() * 6.0F / 16.0F;
        float f2 = pos.getZ() + 0.5F;
        float f3 = 0.52F;
        float f4 = rand.nextFloat() * 0.5F - 0.25F;
        float f5 = rand.nextFloat() * 0.75F + 0.125F;
        float f6 = rand.nextFloat() * 0.75F + 0.125F;

        switch (state.getValue(FACING)) {
            case WEST -> {
                level.addParticle(ParticleTypes.FLAME, f - f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + f5, pos.getY() + 1, pos.getZ() + f6, 0.0D, 0.0D, 0.0D);
            }
            case EAST -> {
                level.addParticle(ParticleTypes.FLAME, f + f3, f1, f2 + f4, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + f5, pos.getY() + 1, pos.getZ() + f6, 0.0D, 0.0D, 0.0D);
            }
            case NORTH -> {
                level.addParticle(ParticleTypes.FLAME, f + f4, f1, f2 - f3, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + f5, pos.getY() + 1, pos.getZ() + f6, 0.0D, 0.0D, 0.0D);
            }
            case SOUTH -> {
                level.addParticle(ParticleTypes.FLAME, f + f4, f1, f2 + f3, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + f5, pos.getY() + 1, pos.getZ() + f6, 0.0D, 0.0D, 0.0D);
            }
            default -> { }
        }
    }
}
