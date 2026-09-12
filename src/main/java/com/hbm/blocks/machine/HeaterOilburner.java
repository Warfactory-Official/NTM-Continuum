package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
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

/**
 * Fluid Burner ({@code heater_oilburner}) — 1:1 port of the 1.12.2 {@code HeaterOilburner}: a 2-tall
 * 3×3 {@link BlockDummyable} (dims {@code {1,0,1,1,1,1}}, offset 1) with the 4 side cells and the
 * top-center marked as extras (fluid ports + the heat cell the consumer above polls through). Look
 * overlay shows setting, tank and stored heat. IToolable (screwdriver setting) is replaced by GUI
 * buttons — documented deviation.
 */
public class HeaterOilburner extends BlockDummyable implements IFluidConnectorBlockMK2, ILookOverlay {

    public HeaterOilburner(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeaterOilburner(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        x = x + dir.offsetX * o;
        z = z + dir.offsetZ * o;

        this.makeExtra(world, x + 1, y, z);
        this.makeExtra(world, x - 1, y, z);
        this.makeExtra(world, x, y, z + 1);
        this.makeExtra(world, x, y, z - 1);
        this.makeExtra(world, x, y + 1, z);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityHeaterOilburner te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityHeaterOilburner heater)) return null;

        List<String> text = new ArrayList<>();
        text.add("§a-> §r" + heater.setting + " mB/t");
        text.add(heater.tank.getTankType().getDisplayNameString() + ": " + String.format("%,d", heater.tank.getFill()) + "/" + String.format("%,d", heater.tank.getMaxFill()) + " mB");
        text.add(String.format("%,d", heater.heatEnergy) + " TU");
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
