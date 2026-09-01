package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** The legacy two-by-two footprint air intake; registry wiring supplies its BlockEntityType. */
public class MachineIntake extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2,
        ILookOverlay {

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

    /** A cable or duct may connect only at one of the eight real horizontal anchors. */
    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection direction) {
        return findCoreTE(level, pos) instanceof TileEntityMachineIntake intake
                && IEnergyConnectorBlock.isPort(intake.getConPos(), pos, direction);
    }

    @Override
    public boolean canConnect(FluidType fluid, BlockGetter level, int x, int y, int z, ForgeDirection direction) {
        if (fluid != Fluids.AIR) return false;
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(level, pos) instanceof TileEntityMachineIntake intake
                && IEnergyConnectorBlock.isPort(intake.getConPos(), pos, direction);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineIntake intake)) return null;

        List<String> lines = List.of(
                (intake.isMotorRunning() ? "§a" : "§c") + "Power: "
                        + com.hbm.lib.Library.getShortNumber(intake.power) + "HE",
                "§c<- §r" + intake.compair.getTankType().getDisplayNameString() + ": "
                        + intake.compair.getFill() + "/" + intake.compair.getMaxFill() + "mB");
        return new ILookOverlay.Data(Component.translatable("block.hbm.machine_intake").getString(),
                0xffff00, 0x404000, lines);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> actualType) {
        return level.isClientSide()
                ? createTickerHelper(actualType, type.get(), TileEntityMachineIntake::clientTick)
                : createTickerHelper(actualType, type.get(), TileEntityMachineIntake::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
