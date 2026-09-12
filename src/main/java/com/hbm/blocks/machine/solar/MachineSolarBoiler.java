package com.hbm.blocks.machine.solar;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.solar.TileEntitySolarBoiler;
import com.hbm.tileentity.machine.solar.ModSolarBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The three-high solar boiler dummyable, with only its legacy top and bottom fluid ports. */
public class MachineSolarBoiler extends BlockDummyable implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineSolarBoiler(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySolarBoiler(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
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
        makeExtra(level, x, y + 2, z);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        makeExtra(level, core.getX(), core.getY() + 2, core.getZ());
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos cell = new BlockPos(x, y, z);
        return findCoreTE(world, cell) instanceof TileEntitySolarBoiler boiler
                && IEnergyConnectorBlock.isPort(boiler.getConPos(), cell, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntitySolarBoiler boiler)) return null;

        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", boiler.heat) + " TU");
        text.add("§a-> §r" + String.format("%,d", boiler.heatInput) + " TU/t");
        if (boiler.tanks[0].getTankType() != com.hbm.inventory.fluid.Fluids.NONE) {
            text.add("§a-> §r" + boiler.tanks[0].getTankType().getDisplayNameString() + ": "
                    + boiler.tanks[0].getFill() + "/" + boiler.tanks[0].getMaxFill() + "mB");
        }
        if (boiler.tanks[1].getTankType() != com.hbm.inventory.fluid.Fluids.NONE) {
            text.add("§c<- §r" + boiler.tanks[1].getTankType().getDisplayNameString() + ": "
                    + boiler.tanks[1].getFill() + "/" + boiler.tanks[1].getMaxFill() + "mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
