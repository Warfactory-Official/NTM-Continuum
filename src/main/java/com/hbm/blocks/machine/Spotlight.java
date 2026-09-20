package com.hbm.blocks.machine;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import com.hbm.blocks.generic.ReinforcedLamp;
import com.hbm.blocks.generic.SpotlightBeams;
import com.hbm.client.render.DecoModelRenderer;
import com.hbm.tileentity.deco.TileEntityDecoModel;

/**
 * 1.7 {@code Spotlight}: the cage lamp, fluorescent tube and halogen floodlight fixtures. Two blocks per
 * fixture (on/off) like the lamps, but with <b>inverted</b> redstone — they are lit by default and a
 * signal switches them off. The old metadata {@code side << 1 | broken} is {@link #FACING} (the face the
 * fixture points away from) and {@link #BROKEN} (a structure-generated fixture that needs a click to
 * replace its bulb). The lit block lays a {@link SpotlightBeam} of {@code beamLength} along its facing.
 *
 * <p>The OBJ body is drawn by {@code DecoModelRenderer} through the shared {@code TileEntityDecoModel}.
 */
public class Spotlight extends CompatBlock implements ISpotlight, EntityBlock {

    public enum LightType { INCANDESCENT, FLUORESCENT, HALOGEN }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    // I'd be extending the ReinforcedLamp class if it wasn't for the inverted behaviour of these specific lights
    // I want these blocks to be eminently useful, so removing the need for redstone by default is desired,
    // these act more like redstone torches, in that applying a signal turns them off
    public final boolean isOn;
    public final int beamLength;
    public final LightType type;
    private final Supplier<Block> off;
    private final Supplier<Block> on;

    public Spotlight(Properties properties, int beamLength, LightType type, boolean isOn, Supplier<Block> off, Supplier<Block> on) {
        super(properties);
        this.beamLength = beamLength;
        this.type = type;
        this.isOn = isOn;
        this.off = off;
        this.on = on;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BROKEN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDecoModel(pos, state);
    }

    /** Old {@code getPartName}: which OBJ group draws this fixture. */
    public String getPartName(int connectionCount) {
        return switch (type) {
            case HALOGEN -> "FloodLamp";
            default -> "CageLamp";
        };
    }

    // Returns an xyz (half-)size for a given object type
    private float[] getBounds() {
        return switch (type) {
            case FLUORESCENT -> new float[]{0.5F, 0.5F, 0.1F};
            case HALOGEN -> new float[]{0.35F, 0.25F, 0.2F};
            default -> new float[]{0.25F, 0.2F, 0.15F};
        };
    }

    private float[] swizzleBounds(Direction dir) {
        float[] bounds = getBounds();
        return switch (dir) {
            case EAST, WEST -> new float[]{bounds[2], bounds[1], bounds[0]};
            case UP, DOWN -> new float[]{bounds[1], bounds[2], bounds[0]};
            default -> bounds;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction dir = state.getValue(FACING);
        float[] bounds = swizzleBounds(dir);
        float[] offset = new float[]{0.5F - dir.getStepX() * (0.5F - bounds[0]), 0.5F - dir.getStepY() * (0.5F - bounds[1]),
                0.5F - dir.getStepZ() * (0.5F - bounds[2])};
        return Shapes.box(offset[0] - bounds[0], offset[1] - bounds[1], offset[2] - bounds[2],
                offset[0] + bounds[0], offset[1] + bounds[1], offset[2] + bounds[2]);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    // BlockSlab doesn't actually properly return isSideSolid,
    // probably because MOJANK thought this would only ever be used for torches,
    // which can't be placed on ceilings...
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction dir = state.getValue(FACING);
        BlockPos support = pos.relative(dir.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, dir);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (oldState.is(this) || level.isClientSide()) return;
        if (updatePower(state, level, pos)) return;
        updateBeam(state, level, pos);
    }

    private boolean updatePower(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(BROKEN)) return false;
        boolean isPowered = level.hasNeighborSignal(pos);
        if (isOn && isPowered) {
            level.scheduleTick(pos, this, 4);
            return true;
        } else if (!isOn && !isPowered) {
            level.setBlock(pos, copyTo(on.get(), state), 2);
            return true;
        }
        return false;
    }

    private static BlockState copyTo(Block block, BlockState from) {
        return block.defaultBlockState().setValue(FACING, from.getValue(FACING)).setValue(BROKEN, from.getValue(BROKEN));
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level.isClientSide()) return;
        SpotlightBeams.unpropagateBeam(level, pos, state.getValue(FACING));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isOn && level.hasNeighborSignal(pos)) {
            level.setBlock(pos, copyTo(off.get(), state), 2);
        }
    }

    // Repropagate the beam if we've become unblocked
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (neighbourBlock instanceof SpotlightBeam) return;
        if (neighbourBlock == net.minecraft.world.level.block.Blocks.AIR) return;
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        if (updatePower(state, level, pos)) return;
        updateBeam(state, level, pos);
    }

    private void updateBeam(BlockState state, Level level, BlockPos pos) {
        if (!isOn) return;
        SpotlightBeams.propagateBeam(level, pos, state.getValue(FACING), beamLength, SpotlightBeams.META_YELLOW);
    }

    // Replace bulbs on broken lights with a click
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(BROKEN)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) repair(level, pos);
        return InteractionResult.SUCCESS;
    }

    private void repair(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof Spotlight) || !state.getValue(BROKEN)) return;
        level.setBlock(pos, copyTo(on.get(), state).setValue(BROKEN, false), 2);
        for (Direction dir : Direction.values()) {
            BlockPos other = pos.relative(dir);
            if (level.getBlockState(other).getBlock() == this) repair(level, other);
        }
    }

    @Override
    public int getBeamLength() {
        return beamLength;
    }

    @Override
    public boolean lights(BlockState state, Direction dir) {
        return isOn && state.getValue(FACING) == dir;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
