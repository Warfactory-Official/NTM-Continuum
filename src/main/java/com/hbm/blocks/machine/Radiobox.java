package com.hbm.blocks.machine;

import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityRadiobox;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code Radiobox} (Rosenberg Pest Control Box): a wall-mounted {@code ModelRadio} whose lever a right-click
 * throws (1.7 metadata +4, {@link #ON}) with the reactor-start clunk. Switched on it kills every hostile mob in
 * range for 25,000 HE a pulse - free once a spark battery is plugged in, which comes back out when the box is
 * broken. It has no GUI: 1.7's {@code provideGUI} returned null and nothing ever opened one.
 */
public class Radiobox extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ON = BlockStateProperties.ENABLED;

    private static final VoxelShape WEST = Block.box(11, 1, 4, 16, 15, 12);
    private static final VoxelShape NORTH = Block.box(4, 1, 11, 12, 15, 16);
    private static final VoxelShape EAST = Block.box(0, 1, 4, 5, 15, 12);
    private static final VoxelShape SOUTH = Block.box(4, 1, 0, 12, 15, 5);

    public Radiobox(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ON, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ON);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadiobox(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** 1.7 metadata 2/5/3/4 for yaw quadrants 0..3: facing back at the placer. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case WEST -> WEST;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            default -> NORTH;
        };
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (held.getItem() != ModItems.battery_spark.get() || player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof TileEntityRadiobox box) || box.infinite) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        held.shrink(1);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.UPGRADE_PLUG.get(),
                SoundSource.BLOCKS, 1.5F, 1.0F);
        box.infinite = true;
        box.setChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        boolean on = state.getValue(ON);
        level.setBlock(pos, state.setValue(ON, !on), 2);
        // 1.7 played it at the block's corner, 0.85 pitch when switching off
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), ModSounds.REACTOR_START.get(), SoundSource.BLOCKS,
                1.0F, on ? 0.85F : 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Structure rotation and mirroring turn the placed facing, as vanilla's directional blocks do. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
