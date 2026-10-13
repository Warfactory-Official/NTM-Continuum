package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.hbm.blocks.ModDecoContent;

/**
 * 1.7 {@code BlockUberConcrete}: über concrete rots. Each random tick has a {@code 1/(damage+1)} chance to
 * advance {@link #DAMAGE}; at 15 the block crumbles into "moldy debris", dropped straight down if the
 * block below is air, otherwise toppled as a falling block off a random open side, otherwise left in
 * place. Damage stages 10-11, 12-13, 14 and 15 show progressively mouldier textures.
 */
public class BlockUberConcrete extends Block {

    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, 15);

    public BlockUberConcrete(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DAMAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGE);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int meta = state.getValue(DAMAGE);
        if (rand.nextInt(meta + 1) > 0) return;
        if (meta < 15) {
            level.setBlock(pos, state.setValue(DAMAGE, meta + 1), 3);
            return;
        }
        Block broken = ModDecoContent.concrete_super_broken.get();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        if (level.getBlockState(pos.below()).isAir()) {
            level.setBlock(pos, broken.defaultBlockState(), 3);
            return;
        }
        List<Direction> sides = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));
        Collections.shuffle(sides);
        for (Direction dir : sides) {
            BlockPos side = pos.relative(dir);
            if (level.getBlockState(side).isAir() && level.getBlockState(side.below()).isAir()) {
                FallingBlockEntity debris = FallingBlockEntity.fall(level, side, broken.defaultBlockState());
                debris.time = 2;
                debris.dropItem = false;
                debris.setHurtsEntities(2.0F, 40);
                return;
            }
        }
        level.setBlock(pos, broken.defaultBlockState(), 3);
    }
}
