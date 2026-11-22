package fissionworks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Fuel column. GLOW (0-4) follows its flux and sets its light. */
public class FuelBlock extends PileBlock {
    public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 4);

    public FuelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(GLOW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GLOW);
    }
}
