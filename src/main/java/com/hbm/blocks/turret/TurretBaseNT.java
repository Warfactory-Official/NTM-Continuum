package com.hbm.blocks.turret;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import dev.golem.core.gui.Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 2x2 pad every turret stands on.
 *
 * <p>1.12.2 gives all four cells a block entity through {@code BlockDummyable}; here the three
 * non-core cells are folded {@link BlockTurretPad}s, so a turret costs one block entity rather than
 * four. The shape is one flat half-block for every cell, matching 1.12.2's single constant
 * {@code getBoundingBox}: no facing, cell or open-state term.
 */
public abstract class TurretBaseNT extends BlockMultiblockCore implements IEnergyConnectorBlock {

    static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);

    private static final int[] DIMENSIONS = {0, 0, 1, 0, 1, 0};

    protected TurretBaseNT(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /**
     * 1.12.2's {@code TileEntityTurretBaseNT.updateConnections} spells its direction reversed, so at
     * placement SOUTH the pad grows in -x/-z, and two of its eight plug positions land on the core
     * itself, which is what {@link #coreMask()} answers for.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        Direction rot = facing.getClockWise();
        Direction back = facing.getOpposite();
        visitor.cell(core.relative(rot), MASK_SOUTH | MASK_WEST);
        visitor.cell(core.relative(back), MASK_NORTH | MASK_EAST);
        visitor.cell(core.relative(rot).relative(back), MASK_NORTH | MASK_WEST);
    }

    @Override
    public int coreMask() {
        return MASK_SOUTH | MASK_EAST;
    }

    @Override
    protected BlockState cellStateFor(int localX, int localY, int localZ, Direction facing, int shapeId) {
        return ModTurretBlocks.turret_pad.get().defaultBlockState();
    }

    /** 1.12.2's {@code standardOpenBehavior} swallows a sneak-click instead of opening the menu. */
    @Override
    protected InteractionResult useAtCore(BlockState coreState, Level level, BlockPos core, Player player,
                                          BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu && player instanceof ServerPlayer serverPlayer) {
            Menus.openWithPos(serverPlayer, menu, core);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Power reaches the core through whichever pad cell the cable touches, so the check runs on the
     * block rather than a per-cell block entity; {@link BlockTurretPad} answers with the same call.
     */
    static boolean padConnects(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        if (!(world instanceof Level level) || dir == ForgeDirection.UNKNOWN) return false;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityTurretBaseNT turret
                && IEnergyConnectorBlock.isPort(turret.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return padConnects(world, pos, dir);
    }

    @Override
    protected void removedAt(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston && level.getBlockEntity(pos) instanceof TileEntityMachineBase machine) {
            Containers.dropContents(level, pos, machine);
        }
        super.removedAt(state, level, pos, movedByPiston);
    }

    /**
     * A gun turret may not be placed on top of another gun turret's half-block pad. 1.7 had no such rule (its space
     * check only reads the footprint), but the second turret then hangs half a block above the first, so the
     * placement and its highlight refuse it. Port-only restriction.
     */
    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        boolean[] onTurret = {isTurretPad(level.getBlockState(core.below()))};
        visitCells(core, facing, (cell, mask) -> {
            if (!onTurret[0] && isTurretPad(level.getBlockState(cell.below()))) onTurret[0] = true;
        });
        return !onTurret[0];
    }

    private static boolean isTurretPad(BlockState state) {
        return state.getBlock() instanceof TurretBaseNT || state.getBlock() instanceof BlockTurretPad;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {1, 0, 0, ForgeDirection.SOUTH.ordinal()},
                {0, 0, 0, ForgeDirection.SOUTH.ordinal()},
                {0, 0, -1, ForgeDirection.NORTH.ordinal()},
                {1, 0, -1, ForgeDirection.NORTH.ordinal()},
                {1, 0, 0, ForgeDirection.EAST.ordinal()},
                {1, 0, -1, ForgeDirection.EAST.ordinal()},
                {0, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 0, -1, ForgeDirection.WEST.ordinal()},
        }, dir);
    }
}
