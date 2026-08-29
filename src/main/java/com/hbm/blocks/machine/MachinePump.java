package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachinePumpElectric;
import com.hbm.tileentity.machine.TileEntityMachinePumpSteam;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The shared 3x3x5 groundwater-pump multiblock. */
public class MachinePump extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    private final boolean steam;

    public MachinePump(Properties properties) {
        this(properties, false);
    }

    public MachinePump(Properties properties, boolean steam) {
        super(properties);
        this.steam = steam;
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return steam ? new TileEntityMachinePumpSteam(pos, state) : new TileEntityMachinePumpElectric(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        // The four meta>=6 cells are the pump's horizontal connection ports.
        makeExtra(level, core.getX() + 1, core.getY(), core.getZ());
        makeExtra(level, core.getX() - 1, core.getY(), core.getZ());
        makeExtra(level, core.getX(), core.getY(), core.getZ() + 1);
        makeExtra(level, core.getX(), core.getY(), core.getZ() - 1);
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection dir) {
        if (steam) return false;
        return findCoreTE(level, pos) instanceof TileEntityMachinePumpElectric pump
                && IEnergyConnectorBlock.isPort(pump.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter level, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        if (steam) {
            return (type == Fluids.STEAM || type == Fluids.SPENTSTEAM || type == Fluids.WATER)
                    && findCoreTE(level, pos) instanceof TileEntityMachinePumpSteam pump
                    && IEnergyConnectorBlock.isPort(pump.getConPos(), pos, dir);
        }
        return type == Fluids.WATER
                && findCoreTE(level, pos) instanceof TileEntityMachinePumpElectric pump
                && IEnergyConnectorBlock.isPort(pump.getConPos(), pos, dir);
    }

    @Override
    public void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos) {
        List<String> text = new ArrayList<>();
        if (steam) {
            if (!(findCoreTE(level, pos) instanceof TileEntityMachinePumpSteam pump)) return;
            text.add("§a-> §r" + pump.steam.getTankType().getDisplayNameString() + ": "
                    + String.format(Locale.US, "%,d", pump.steam.getFill()) + " / "
                    + String.format(Locale.US, "%,d", pump.steam.getMaxFill()) + "mB");
            text.add("§c<- §r" + pump.lps.getTankType().getDisplayNameString() + ": "
                    + String.format(Locale.US, "%,d", pump.lps.getFill()) + " / "
                    + String.format(Locale.US, "%,d", pump.lps.getMaxFill()) + "mB");
            text.add("§c<- §r" + pump.water.getTankType().getDisplayNameString() + ": "
                    + String.format(Locale.US, "%,d", pump.water.getFill()) + " / "
                    + String.format(Locale.US, "%,d", pump.water.getMaxFill()) + "mB");
            if (pump.getBlockPos().getY() > TileEntityMachinePumpSteam.GROUND_HEIGHT) {
                text.add("&[" + warningColor() + "&]! ! ! ALTITUDE ! ! !");
            }
            if (!pump.onGround) text.add("&[" + warningColor() + "&]! ! ! NO VALID GROUND ! ! !");
        } else {
            if (!(findCoreTE(level, pos) instanceof TileEntityMachinePumpElectric pump)) return;
            text.add("§a-> §r" + String.format(Locale.US, "%,d", pump.power) + " / "
                    + String.format(Locale.US, "%,d", pump.getMaxPower()) + "HE");
            text.add("§c<- §r" + pump.water.getTankType().getDisplayNameString() + ": "
                    + String.format(Locale.US, "%,d", pump.water.getFill()) + " / "
                    + String.format(Locale.US, "%,d", pump.water.getMaxFill()) + "mB");
            if (pump.getBlockPos().getY() > TileEntityMachinePumpElectric.GROUND_HEIGHT) {
                text.add("&[" + warningColor() + "&]! ! ! ALTITUDE ! ! !");
            }
            if (!pump.onGround) text.add("&[" + warningColor() + "&]! ! ! NO VALID GROUND ! ! !");
        }
        ILookOverlay.printGeneric(graphics, getName().getString(), 0xffff00, 0x404000, text);
    }

    private static int warningColor() {
        return System.currentTimeMillis() % 1000 < 500 ? 0xff0000 : 0xffff00;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (steam) {
            return level.isClientSide()
                    ? createTickerHelper(type, ModBlockEntities.MACHINE_PUMP_STEAM.get(),
                            TileEntityMachinePumpSteam::clientTick)
                    : createTickerHelper(type, ModBlockEntities.MACHINE_PUMP_STEAM.get(),
                            TileEntityMachinePumpSteam::serverTick);
        }
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_PUMP_ELECTRIC.get(),
                        TileEntityMachinePumpElectric::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_PUMP_ELECTRIC.get(),
                        TileEntityMachinePumpElectric::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
