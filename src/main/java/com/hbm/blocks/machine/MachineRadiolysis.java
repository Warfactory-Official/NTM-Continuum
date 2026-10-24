package com.hbm.blocks.machine;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.handler.BossSpawnHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.machine.TileEntityMachineRadiolysis;

/**
 * The RTG + radiolysis chamber: a 3x3x3 folded footprint (1.7 dims {@code {2,0,1,1,1,1}}, offset 0) whose
 * four cardinal ground cells are the {@code makeExtra} inventory/power/fluid ports.
 */
public final class MachineRadiolysis extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder, ICapabilityBlock {

    public MachineRadiolysis(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRadiolysis(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 0; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineRadiolysis te ? te : null;
    }

    /** The four cardinal neighbours of the core, each open on its outward face. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            visitor.cell(core.relative(side), maskOf(MultiblockMaskTable.toLocal(side, facing)));
        }
    }

    /** 1.7 {@code TileEntityProxyCombo(true, true, true)}: inventory, power and fluid on every port. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            visitor.passiveCell(core.relative(side), MASK_ALL, domains);
        }
    }

    private static int maskOf(Direction local) {
        return switch (local) {
            case NORTH -> MASK_NORTH;
            case SOUTH -> MASK_SOUTH;
            case WEST -> MASK_WEST;
            case EAST -> MASK_EAST;
            case UP -> MASK_UP;
            case DOWN -> MASK_DOWN;
        };
    }

    /** 1.7 {@code onBlockActivated}: {@code BossSpawnHandler.markFBI} on every non-sneaking use. */
    @Override
    protected InteractionResult useAtCore(BlockState state,
            Level level, BlockPos core, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide() && !player.isShiftKeyDown()) BossSpawnHandler.markFBI(player);
        return super.useAtCore(state, level, core, player, hit);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {1, 0, 0, ForgeDirection.EAST.ordinal()},
                {-1, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {0, 0, -1, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }

    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerOut().fluidIn().fluidOut().items().itemsAtCells().fe()
            .fluidFaces(BlockEntity.class, (be, side) -> side != Direction.DOWN);

    @Override
    public MachineCaps caps() { return CAPS; }
}
