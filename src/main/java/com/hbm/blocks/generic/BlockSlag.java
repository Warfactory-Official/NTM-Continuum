package com.hbm.blocks.generic;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Blast slag. 1.12.2 keeps the intact and cracked variants as two metadata values on one block; here
 * they fold onto {@link #CRACKED}.
 */
public class BlockSlag extends dev.golem.core.world.CompatBlock {

    public static final BooleanProperty CRACKED = BooleanProperty.create("cracked");

    public BlockSlag(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRACKED, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(CRACKED);
    }
}
