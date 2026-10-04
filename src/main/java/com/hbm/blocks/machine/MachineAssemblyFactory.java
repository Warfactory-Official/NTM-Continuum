package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.assemblyfactory.AssemblyFactoryPorts;
import com.hbm.assemblyfactory.AssemblyFactoryRegistrations;
import com.hbm.assemblyfactory.AssemblyFactorySidedContainer;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The legacy 5x3 Assembly Factory: four independent Assembly Machine lanes. */
public final class MachineAssemblyFactory extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder, ILookOverlay {

    public MachineAssemblyFactory(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAssemblyFactory(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }

    /** Ordinary dummies have no proxy inventory; promoted dummies retain the old accessor-relative view. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(CELL) && !MultiblockSurface.carriesPassive(level, pos, PASSIVE_ITEMS)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineAssemblyFactory factory)) return null;
        return core.equals(pos) ? factory : new AssemblyFactorySidedContainer(factory, pos);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineAssemblyFactory factory)) return null;

        for (DirPos port : factory.getCoolPos()) {
            if (port.getPos().equals(pos.relative(port.getDir().toDirection()))) {
                return new ILookOverlay.Data(getName().getString(), 0xFFFF00, 0x404000, List.of(
                        "§a-> §r" + factory.water.getTankType().getDisplayNameString(),
                        "§c<- §r" + factory.lps.getTankType().getDisplayNameString()));
            }
        }

        DirPos[] io = factory.getIOPos();
        for (int i = 0; i < io.length; i++) {
            DirPos port = io[i];
            if (port.getPos().equals(pos.relative(port.getDir().toDirection()))) {
                return new ILookOverlay.Data(getName().getString(), 0xFFFF00, 0x404000,
                        List.of("§e-> §rRecipe field [" + (i + 1) + "]"));
            }
        }
        return null;
    }

    /**
     * The 5x5 ring at ground level (the upper strips are passive proxies only). Every cell on the ring's ends is open
     * outward; the two odd cells on each end are the coolant anchors, and the two on each flank are
     * the recipe-field proxies, which 1.7 subscribes on as well as reading items through.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dz = -2; dz <= 2; dz += 2) {
                if (dx == 0 && dz == 0) continue;
                visitor.cell(core.offset(dx, 0, dz), outwardMask(dx, dz, 2));
            }
        }

        Direction rot = facing.getClockWise();
        for (int side = -1; side <= 1; side += 2) {
            visitor.cell(core.relative(facing, 2).relative(rot, side), MASK_SOUTH);
            visitor.cell(core.relative(facing, -2).relative(rot, side), MASK_NORTH);
            visitor.cell(core.relative(rot, 2).relative(facing, side), MASK_WEST);
            visitor.cell(core.relative(rot, -2).relative(facing, side), MASK_EAST);
        }
        // 1.7.10: the two upper strips are proxy extras only (passive plane below); unlike the chemical
        // factory, the assembly factory subscribes on no y+3 port.
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, domains);
                }
            }
        }
        Direction rot = facing.getClockWise();
        for (int along = -2; along <= 2; along++) {
            visitor.passiveCell(core.relative(facing, along).relative(rot, 2).above(2), MASK_ALL, domains);
            visitor.passiveCell(core.relative(facing, along).relative(rot, -2).above(2), MASK_ALL, domains);
        }
    }

    /**
     * The four coolant anchors take the factory's own water and spent-steam channels and nothing else.
     * A per-cell commodity gate, which the geometry cannot carry -- {@code acceptsFluid} answers for the
     * whole block.
     */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos cell = new BlockPos(x, y, z);
        if (!super.canConnect(type, world, x, y, z, dir)) return false;
        return type == com.hbm.inventory.fluid.Fluids.WATER
                || type == com.hbm.inventory.fluid.Fluids.SPENTSTEAM
                || !coolantAnchor(world, cell);
    }

    private static boolean coolantAnchor(BlockGetter world, BlockPos cell) {
        BlockPos core = MultiblockSurface.coreOfAny(world, cell);
        if (core == null || cell.getY() != core.getY()) return false;
        Direction facing = world.getBlockState(core).getValue(FACING);
        int dx = cell.getX() - core.getX(), dz = cell.getZ() - core.getZ();
        return Math.abs(MultiblockMaskTable.localX(dx, 0, dz, facing)) == 1
                && Math.abs(MultiblockMaskTable.localZ(dx, 0, dz, facing)) == 2;
    }
}
