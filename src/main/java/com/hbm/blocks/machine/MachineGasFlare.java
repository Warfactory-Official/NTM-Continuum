package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Flare Stack block — 1:1 port of the 1.12.2 {@code MachineGasFlare}: a 12-tall 3×3 {@link BlockDummyable}
 * (dims {@code {11,0,1,1,1,1}}, offset 1) with the 4 base side cells marked as extras (the fluid/power
 * ports on {@code getConPos}).
 */
public class MachineGasFlare extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineGasFlare(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineGasFlare(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{11, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o);
        this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o);
        this.makeExtra(world, x + dir.offsetX * o, y, z + dir.offsetZ * o + 1);
        this.makeExtra(world, x + dir.offsetX * o, y, z + dir.offsetZ * o - 1);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineGasFlare te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineGasFlare te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

}
