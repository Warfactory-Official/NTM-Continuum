package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityCondenserPowered;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The six-port, powered 3x3x3 steam condenser multiblock. */
public class MachineCondenserPowered extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineCondenserPowered(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCondenserPowered(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 3, 3};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);

        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, x + rot.offsetX * 3, y + 1, z + rot.offsetZ * 3);
        makeExtra(level, x - rot.offsetX * 3, y + 1, z - rot.offsetZ * 3);
        makeExtra(level, x + dir.offsetX + rot.offsetX, y + 1, z + dir.offsetZ + rot.offsetZ);
        makeExtra(level, x + dir.offsetX - rot.offsetX, y + 1, z + dir.offsetZ - rot.offsetZ);
        makeExtra(level, x - dir.offsetX + rot.offsetX, y + 1, z - dir.offsetZ + rot.offsetZ);
        makeExtra(level, x - dir.offsetX - rot.offsetX, y + 1, z - dir.offsetZ - rot.offsetZ);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityCondenserPowered condenser
                && IEnergyConnectorBlock.isPort(condenser.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        if (type != Fluids.SPENTSTEAM && type != Fluids.WATER) return false;
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(world, pos) instanceof TileEntityCondenserPowered condenser
                && IEnergyConnectorBlock.isPort(condenser.getConPos(), pos, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityCondenserPowered condenser)) return null;

        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(condenser.power) + "HE / "
                + Library.getShortNumber(condenser.getMaxPower()) + "HE");
        text.add("§a-> §r" + tankLine(condenser.input));
        text.add("§c<- §r" + tankLine(condenser.output));
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    private static String tankLine(com.hbm.inventory.fluid.tank.FluidTankNTM tank) {
        return tank.getTankType().getDisplayNameString() + ": "
                + String.format(Locale.US, "%,d", tank.getFill()) + "/"
                + String.format(Locale.US, "%,d", tank.getMaxFill()) + "mB";
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_CONDENSER_POWERED.get(), TileEntityCondenserPowered::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_CONDENSER_POWERED.get(), TileEntityCondenserPowered::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
