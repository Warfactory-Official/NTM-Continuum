package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.machine.TileEntityCharger;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code Charger} ("Charging Station"): a small wall-mounted box drawn entirely by {@code RenderCharger}.
 * 1.7 stored the facing as metadata 2..5 from the placer's yaw, so the front faces the player; the box hangs
 * on the opposite side of the cell, where it also takes its power ({@link TileEntityCharger}).
 */
public class Charger extends CompatBlock implements TickingBlock, ICapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final float F = 0.0625F;
    // 1.7 setBlockBoundsBasedOnState, by metadata 2 (north), 3 (south), 4 (west), 5 (east)
    private static final VoxelShape NORTH = Shapes.box(5 * F, 0.25F, 12 * F, 11 * F, 0.75F, 1F);
    private static final VoxelShape SOUTH = Shapes.box(5 * F, 0.25F, 0F, 11 * F, 0.75F, 4 * F);
    private static final VoxelShape WEST = Shapes.box(12 * F, 0.25F, 5 * F, 1F, 0.75F, 11 * F);
    private static final VoxelShape EAST = Shapes.box(0F, 0.25F, 5 * F, 4 * F, 0.75F, 11 * F);

    public Charger(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 1.7 {@code onBlockPlacedBy}: yaw quadrant 0/1/2/3 to metadata 2/5/3/4, i.e. the front faces the placer. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCharger(pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn().fe()
            .faces(TileEntityCharger.class, (be, side) -> be.canConnect(ForgeDirection.getOrientation(side)));

    @Override
    public MachineCaps caps() { return CAPS; }
}
