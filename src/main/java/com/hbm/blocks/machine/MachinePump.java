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

/** The electric groundwater pump. The steam variant remains intentionally deferred. */
public class MachinePump extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachinePump(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachinePumpElectric(pos, state);
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
        return findCoreTE(level, pos) instanceof TileEntityMachinePumpElectric pump
                && IEnergyConnectorBlock.isPort(pump.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter level, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return type == Fluids.WATER
                && findCoreTE(level, pos) instanceof TileEntityMachinePumpElectric pump
                && IEnergyConnectorBlock.isPort(pump.getConPos(), pos, dir);
    }

    @Override
    public void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos) {
        if (!(findCoreTE(level, pos) instanceof TileEntityMachinePumpElectric pump)) return;

        List<String> text = new ArrayList<>();
        text.add("§a-> §r" + String.format(Locale.US, "%,d", pump.power) + " / "
                + String.format(Locale.US, "%,d", pump.getMaxPower()) + "HE");
        text.add("§c<- §r" + pump.water.getTankType().getDisplayNameString() + ": "
                + String.format(Locale.US, "%,d", pump.water.getFill()) + " / "
                + String.format(Locale.US, "%,d", pump.water.getMaxFill()) + "mB");
        if (pump.getBlockPos().getY() > TileEntityMachinePumpElectric.GROUND_HEIGHT) {
            text.add("&[" + warningColor() + "&]! ! ! ALTITUDE ! ! !");
        }
        if (!pump.onGround) text.add("&[" + warningColor() + "&]! ! ! NO VALID GROUND ! ! !");
        ILookOverlay.printGeneric(graphics, getName().getString(), 0xffff00, 0x404000, text);
    }

    private static int warningColor() {
        return System.currentTimeMillis() % 1000 < 500 ? 0xff0000 : 0xffff00;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
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
