package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.chemicalfactory.ChemicalFactoryRegistrations;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The legacy 5x3 chemical factory: four chemical-plant lanes around one shared controller. */
public class MachineChemicalFactory extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder, ILookOverlay {

    public MachineChemicalFactory(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineChemicalFactory(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(CELL) && !MultiblockSurface.carriesPassive(level, pos, PASSIVE_ITEMS)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineChemicalFactory factory
                ? factory : null;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineChemicalFactory factory)) return null;
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
     * The 5x5 ring at ground level and the two upper strips. Every cell on the ring's ends is open
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
        for (int along = -2; along <= 2; along++) {
            visitor.cell(core.relative(facing, along).relative(rot, 2).above(2), MASK_UP);
            visitor.cell(core.relative(facing, along).relative(rot, -2).above(2), MASK_UP);
        }
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
}
