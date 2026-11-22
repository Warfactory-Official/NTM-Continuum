package fissionworks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Every hall block. FORMED is set by the wrench on the panel; breaking any formed block unforms the hall. */
public class PileBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public PileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && state.getValue(FORMED) && !state.is(newState.getBlock())) {
            Hall.unform(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
