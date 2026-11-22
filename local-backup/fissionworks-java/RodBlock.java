package fissionworks;

import fissionworks.sim.Reactor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Control rod column. LEVEL (8 = fully in, 0 = fully out) and GROUP are only for looks;
 * the real values live in the reactor. A wrench cycles the rod's colour group.
 */
public class RodBlock extends PileBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);
    public static final IntegerProperty GROUP = IntegerProperty.create("group", 0, Reactor.GROUPS - 1);

    public RodBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 8).setValue(GROUP, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LEVEL, GROUP);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!Wrench.isWrench(stack) || !state.getValue(FORMED)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            BlockPos panel = Hall.panelFor(level, pos, null);
            int group = panel != null && level.getBlockEntity(panel) instanceof ReactorBE be ? be.cycleGroupAt(pos) : -1;
            if (group >= 0) {
                player.displayClientMessage(Component.translatable("message.fissionworks.group",
                        Component.translatable("group.fissionworks." + group)), true);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }
}
