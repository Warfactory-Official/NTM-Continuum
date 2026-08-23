package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModHazardBlocks;

import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.util.ContaminationUtil;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;

/** Fuel-bearing wreckage: radiation and fire persist while boron burns the fuel down. */
public class RBMKDebrisRadiating extends RBMKDebrisBurning {
    public static final IntegerProperty BURN = IntegerProperty.create("burn", 0, 15);
    private static final double RANGE = 100D, BURN_RANGE = 5D;
    private static final float RADS = 1_000_000F, BURN_DAMAGE = 100F;

    public RBMKDebrisRadiating(BlockBehaviour.Properties props) { super(props); registerDefaultState(stateDefinition.any().setValue(BURN, 0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BURN); }
    @Override protected int tickRate(RandomSource rand) { return 20 + rand.nextInt(20); }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        radiate(level, pos);
        if (rand.nextInt(FLAME_CHANCE) == 0) {
            flame(level, pos);
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1F + rand.nextFloat(), rand.nextFloat() * .7F + .3F);
        }
        BlockPos side = pos.relative(Direction.values()[rand.nextInt(6)]);
        BlockState neighbour = level.getBlockState(side);
        if (rand.nextInt(10) == 0 && neighbour.isAir()) level.setBlockAndUpdate(side, ModNuclearBlocks.gas_meltdown.get().defaultBlockState());
        if (rand.nextInt(quenchChance(neighbour)) == 0) {
            int burn = state.getValue(BURN);
            if (burn < 15) {
                level.setBlock(pos, state.setValue(BURN, burn + 1), Block.UPDATE_CLIENTS);
                level.scheduleTick(pos, this, tickRate(rand));
            } else quench(level, pos);
        } else level.scheduleTick(pos, this, tickRate(rand));
    }

    @Override protected int quenchChance(BlockState neighbour) { return neighbour.is(ModHazardBlocks.block_boron.get()) ? 25 : 1000; }
    @Override protected void quench(ServerLevel level, BlockPos pos) { level.setBlockAndUpdate(pos, ModRBMKBlocks.rbmk_debris_burning.get().defaultBlockState()); }

    private static void radiate(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + .5D, y = pos.getY() + .5D, z = pos.getZ() + .5D;
        ContaminationUtil.radiate(level, x, y, z, RANGE, RADS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(x, y, z, x, y, z).inflate(BURN_RANGE))) {
            if (entity.distanceToSqr(x, y, z) < BURN_RANGE * BURN_RANGE) Compat.hurt(entity, level, level.damageSources().inFire(), BURN_DAMAGE);
        }
    }
}
