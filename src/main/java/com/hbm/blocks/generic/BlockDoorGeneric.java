package com.hbm.blocks.generic;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.IBomb;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import com.hbm.tileentity.DoorDecl;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.hbm.util.DoorRotation;

/**
 * Port of 1.7.10 {@code BlockDoorGeneric}: a folded multiblock whose geometry, timing and sounds are
 * one {@link DoorDecl}. The 1.7 "extra" metadata a moving door stamped on the cells it had cleared is
 * the {@link #OPEN} property here; a cell's outline and collision come from
 * {@link DoorDecl#getBlockBound} in the door's own frame, so an open door is walkable exactly where
 * the original was.
 */
public class BlockDoorGeneric extends BlockMultiblockMachine implements IBomb, IToolable {

    /** 1.7's {@code hasExtra(meta)}: this cell has been cleared by the moving door. */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public final DoorDecl type;
    private final ConcurrentHashMap<Long, VoxelShape> doorShapes = new ConcurrentHashMap<>();

    public BlockDoorGeneric(Properties properties, DoorDecl type) {
        super(properties);
        this.type = type;
        registerDefaultState(defaultBlockState().setValue(OPEN, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override
    protected BlockState cellStateFor(int localX, int localY, int localZ, Direction facing, int shapeId) {
        return super.cellStateFor(localX, localY, localZ, facing, shapeId).setValue(OPEN, Boolean.FALSE);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDoorGeneric(pos, state);
    }

    @Override public int[] getDimensions() { return type.getDimensions(); }
    @Override public int getOffset() { return type.getBlockOffset(); }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        if (type.getExtraDimensions() != null) for (int[] dims : type.getExtraDimensions()) {
            if (!MultiblockHandlerXR.checkSpace(level, core, dims, clicked, facing)) return false;
        }
        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        if (type.getExtraDimensions() != null) for (int[] dims : type.getExtraDimensions()) {
            MultiblockHandlerXR.visitBox(core, dims, facing, visitor);
        }
    }

    // --- the 1.7 extra flag ---

    public void makeExtra(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this || !state.getValue(CELL) || state.getValue(OPEN)) return;
        level.setBlock(pos, state.setValue(OPEN, Boolean.TRUE), 3);
    }

    public void removeExtra(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != this || !state.getValue(CELL) || !state.getValue(OPEN)) return;
        level.setBlock(pos, state.setValue(OPEN, Boolean.FALSE), 3);
    }

    public boolean hasExtra(BlockState state) {
        return state.getBlock() == this && state.getValue(CELL) && state.getValue(OPEN);
    }

    // --- interaction ---

    private @Nullable TileEntityDoorGeneric doorAt(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos core = coreOf(level, pos, state);
        if (core == null) return null;
        return level.getBlockEntity(core) instanceof TileEntityDoorGeneric door ? door : null;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        TileEntityDoorGeneric door = doorAt(level, pos, level.getBlockState(pos));
        if (door != null) {
            DoorDecl decl = door.getDoorType();
            if (decl == null || !decl.remoteControllable()) return BombReturnCode.ERROR_INCOMPATIBLE;
            if (door.tryToggle(null)) {
                return BombReturnCode.TRIGGERED;
            }
        }
        return BombReturnCode.ERROR_INCOMPATIBLE;
    }

    @Override
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof TileEntityDoorGeneric door) {
            return door.tryToggle(player) ? InteractionResult.CONSUME : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || !player.isShiftKeyDown()) return false;

        TileEntityDoorGeneric door = doorAt(level, pos, level.getBlockState(pos));
        if (door == null || door.getDoorType() == null || !door.getDoorType().hasSkins()) return false;
        if (level.isClientSide()) return true;
        door.cycleSkinIndex();
        door.networkPackNT();
        return true;
    }

    // --- redstone ---

    @Override public boolean wantsNeighborUpdates() { return true; }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (level.getBlockEntity(core) instanceof TileEntityDoorGeneric door) {
            door.updateRedstonePower(cell);
        }
    }

    // --- geometry ---

    // 1.7's getBlocksMovement override (open cell = passable) is what vanilla's default
    // isPathfindable already answers from the collision shape below, whose signature differs per era.

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return doorShape(state, level, pos, false);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return doorShape(state, level, pos, true);
    }

    /**
     * 1.7 {@code BlockDoorGeneric#getBoundingBox}: rotate the cell's offset from the core into the
     * door's frame (with the original's extra quarter turn), ask the declaration for that cell's box,
     * and turn the box back out by the facing. A cell that cannot find its core is a full cube.
     */
    private VoxelShape doorShape(BlockState state, BlockGetter level, BlockPos pos, boolean forCollision) {
        boolean open;
        BlockPos core;
        BlockState coreState;
        if (state.getValue(CELL)) {
            open = state.getValue(OPEN);
            core = MultiblockSurface.coreOfCell(level, pos);
            if (core == null) return Shapes.block();
            coreState = level.getBlockState(core);
            if (MultiblockSurface.foldedCore(coreState) != this) return Shapes.block();
        } else {
            // The core's own state carries the facing: vanilla bakes every state's shape cache against an
            // EmptyBlockGetter at registry freeze, where a world lookup would answer air and have no FACING.
            core = pos;
            coreState = state;
            open = level.getBlockEntity(pos) instanceof TileEntityDoorGeneric door && door.state != TileEntityDoorGeneric.STATE_CLOSED;
        }
        ForgeDirection dir = ForgeDirection.getOrientation(coreState.getValue(FACING));
        int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
        long key = (dx & 0xFFL) | (dy & 0xFFL) << 8 | (dz & 0xFFL) << 16 | (long) dir.ordinal() << 24
                | (open ? 1L << 32 : 0L) | (forCollision ? 1L << 33 : 0L);
        VoxelShape cached = doorShapes.get(key);
        if (cached != null) return cached;
        VoxelShape shape = Shapes.create(cellBox(dx, dy, dz, dir, open, forCollision));
        doorShapes.put(key, shape);
        return shape;
    }

    /** The cell's box in its own block space, normalised the way the original did before colliding. */
    public AABB cellBox(int dx, int dy, int dz, ForgeDirection dir, boolean open, boolean forCollision) {
        BlockPos local = DoorRotation.getBlockRotation(dir).add(DoorRotation.COUNTERCLOCKWISE_90).rotate(dx, dy, dz);
        AABB box = type.getBlockBound(local.getX(), local.getY(), local.getZ(), open, forCollision);
        AABB turned = switch (dir) {
            case NORTH -> new AABB(1 - box.minX, box.minY, 1 - box.minZ, 1 - box.maxX, box.maxY, 1 - box.maxZ);
            case WEST -> new AABB(1 - box.minZ, box.minY, box.minX, 1 - box.maxZ, box.maxY, box.maxX);
            case SOUTH -> new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            case EAST -> new AABB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
            default -> new AABB(0, 0, 0, 1, 1, 1);
        };
        // AABB's constructor already orders min/max; this is the original's explicit normalisation.
        return new AABB(Math.min(turned.minX, turned.maxX), Math.min(turned.minY, turned.maxY), Math.min(turned.minZ, turned.maxZ),
                Math.max(turned.minX, turned.maxX), Math.max(turned.minY, turned.maxY), Math.max(turned.minZ, turned.maxZ));
    }
}
