package com.hbm.tileentity.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

public class TileEntityMachineCompressorCompact extends TileEntityMachineCompressorBase {

    public TileEntityMachineCompressorCompact(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_COMPRESSOR_COMPACT.get(), pos, state);
    }

    protected PortDef[] cachedPorts;
    @Override
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = TilePortShapes.condenser(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), ForgeDirection.getOrientation(MachineCore.facingOrdinal(getBlockState(), 2)));
        return cachedPorts;
    }
}
