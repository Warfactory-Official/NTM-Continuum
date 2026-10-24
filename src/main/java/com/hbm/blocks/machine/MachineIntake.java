package com.hbm.blocks.machine;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.ForgeDirection;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** The legacy two-by-two footprint air intake; registry wiring supplies its BlockEntityType. */
public class MachineIntake extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2,
        ILookOverlay, ICapabilityBlock {

    private final Supplier<? extends BlockEntityType<TileEntityMachineIntake>> type;

    public MachineIntake(Properties properties,
                         Supplier<? extends BlockEntityType<TileEntityMachineIntake>> type) {
        super(properties);
        this.type = type;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineIntake(type.get(), pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 1, 0, 1, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineIntake intake)) return null;

        List<String> lines = List.of(
                (intake.isMotorRunning() ? "§a" : "§c") + "Power: "
                        + Library.getShortNumber(intake.power) + "HE",
                "§c<- §r" + intake.compair.getTankType().getDisplayNameString() + ": "
                        + intake.compair.getFill() + "/" + intake.compair.getMaxFill() + "mB");
        return new ILookOverlay.Data(Component.translatable("block.hbm.machine_intake").getString(),
                0xffff00, 0x404000, lines);
    }

    @Override
    public int coreMask() {
        return MASK_EAST | MASK_SOUTH;
    }

    /** Every cell of the 2x2 pad is an anchor; what 1.7 left unsaid is which faces each one offers. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        Direction rot = facing.getClockWise();
        BlockPos back = core.relative(facing.getOpposite());
        visitor.cell(back, MASK_NORTH | MASK_EAST);
        visitor.cell(core.relative(rot), MASK_SOUTH | MASK_WEST);
        visitor.cell(back.relative(rot), MASK_NORTH | MASK_WEST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        passiveEverywhere(core, facing, visitor, PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
    }

    @Override
    public boolean acceptsFluid(FluidType type) {
        return type == Fluids.AIR;
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

    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn().fluidOut().fe()
            .faces(BlockEntity.class, (be, side) -> side.getAxis().isHorizontal())
            .fluidTypes(BlockEntity.class, (be, fluid, side) -> (fluid == null || fluid == com.hbm.inventory.fluid.Fluids.AIR) && (side == null || side.getAxis().isHorizontal()));

    @Override
    public MachineCaps caps() { return CAPS; }
}
