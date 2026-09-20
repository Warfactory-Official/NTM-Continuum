package com.hbm.blocks.network;

import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import com.hbm.tileentity.network.TileEntityConnector;
import com.hbm.tileentity.network.TileEntityPylonBase;

/**
 * {@code red_connector}: the wall-mounted single-wire connector. {@link #FACING} is 1.7's metadata -- the
 * side of the clicked block, so the wall it hangs on is the opposite direction. A dye recolours its wire.
 */
public class ConnectorRedWire extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.values()) SHAPES.put(facing, shapeFor(facing, false));
    }

    /** 5..11 core, stretched to the wall face; the super connector stretches out to the far face too. */
    protected static VoxelShape shapeFor(Direction facing, boolean through) {
        Direction wall = facing.getOpposite();
        double min = 5, max = 11;
        double minX = wall == Direction.WEST || (through && wall == Direction.EAST) ? 0 : min;
        double maxX = wall == Direction.EAST || (through && wall == Direction.WEST) ? 16 : max;
        double minY = wall == Direction.DOWN || (through && wall == Direction.UP) ? 0 : min;
        double maxY = wall == Direction.UP || (through && wall == Direction.DOWN) ? 16 : max;
        double minZ = wall == Direction.NORTH || (through && wall == Direction.SOUTH) ? 0 : min;
        double maxZ = wall == Direction.SOUTH || (through && wall == Direction.NORTH) ? 16 : max;
        return Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public ConnectorRedWire(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityConnector(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) {
            return PylonDyes.colorFromDye(stack) != 0 ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.getBlockEntity(pos) instanceof TileEntityPylonBase pylon && pylon.setColor(stack)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
