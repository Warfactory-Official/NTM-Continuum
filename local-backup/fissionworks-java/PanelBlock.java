package fissionworks;

import fissionworks.client.ClientHooks;
import fissionworks.sim.Reactor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Front-centre block of the hall. Wrench forms it or reads it out; an empty hand opens the console. */
public class PanelBlock extends PileBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public PanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorBE(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return type == FissionWorks.REACTOR_BE.get()
                ? (lv, pos, st, be) -> ReactorBE.tick(lv, pos, st, (ReactorBE) be) : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!Wrench.isWrench(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!state.getValue(FORMED)) {
            Component error = Hall.tryForm(level, pos, state.getValue(FACING));
            if (error == null) {
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 0.6f);
            }
            player.displayClientMessage(error != null ? error : Component.translatable("message.fissionworks.formed"), true);
        } else if (level.getBlockEntity(pos) instanceof ReactorBE be && be.reactor() != null) {
            Reactor r = be.reactor();
            player.displayClientMessage(Component.translatable("message.fissionworks.status",
                    Math.round(r.power()) + "%", r.count(Reactor.Kind.FUEL), r.count(Reactor.Kind.ROD)), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide && state.getValue(FORMED)) {
            ClientHooks.open(pos);
        } else if (!level.isClientSide && !state.getValue(FORMED)) {
            player.displayClientMessage(Component.translatable("message.fissionworks.not_formed"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
