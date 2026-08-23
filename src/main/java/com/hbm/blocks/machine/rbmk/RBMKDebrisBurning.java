package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.particle.helper.HbmEffectNT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKDebrisBurning extends RBMKDebrisBlock {
    protected static final int FLAME_CHANCE = 5;
    private static final int VENT_CHANCE = 10, QUENCH_CHANCE_SMOTHERED = 10, QUENCH_CHANCE_BARE = 100;

    public RBMKDebrisBurning(BlockBehaviour.Properties props) { super(props); }
    protected int tickRate(RandomSource rand) { return 100 + rand.nextInt(20); }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!(level instanceof ServerLevel server) || state.is(oldState.getBlock())) return;
        if (server.getRandom().nextInt(3) == 0) flame(server, pos);
        server.scheduleTick(pos, this, tickRate(server.getRandom()));
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(FLAME_CHANCE) == 0) {
            flame(level, pos);
            level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1F + rand.nextFloat(), rand.nextFloat() * .7F + .3F);
        }
        BlockPos side = pos.relative(Direction.values()[rand.nextInt(6)]);
        BlockState neighbour = level.getBlockState(side);
        if (rand.nextInt(VENT_CHANCE) == 0 && neighbour.isAir()) level.setBlockAndUpdate(side, ModNuclearBlocks.gas_meltdown.get().defaultBlockState());
        if (rand.nextInt(quenchChance(neighbour)) == 0) quench(level, pos);
        else level.scheduleTick(pos, this, tickRate(rand));
    }

    protected int quenchChance(BlockState neighbour) {
        return neighbour.is(ModBlocks.block_foam.get()) || neighbour.is(ModRBMKBlocks.foam_layer.get())
                || neighbour.is(ModHazardBlocks.block_boron.get()) ? QUENCH_CHANCE_SMOTHERED : QUENCH_CHANCE_BARE;
    }
    protected void quench(ServerLevel level, BlockPos pos) { level.setBlockAndUpdate(pos, ModRBMKBlocks.rbmk_debris.get().defaultBlockState()); }
    protected static void flame(ServerLevel level, BlockPos pos) {
        RandomSource rand = level.getRandom();
        HbmEffectNT.Ashes.send(level, pos.getX() + .25D + rand.nextDouble() * .5D, pos.getY() + 1.75D, pos.getZ() + .25D + rand.nextDouble() * .5D, null);
    }
}
