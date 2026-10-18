package com.hbm.tileentity.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;

/** Large deuterium tower variant with the legacy 50,000/5,000 mB tanks and eight ports. */
public class TileEntityDeuteriumTower extends TileEntityDeuteriumExtractor {

    public TileEntityDeuteriumTower(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MACHINE_DEUTERIUM_TOWER.get(), pos, state);
    }

    /** For a subclass with its own block-entity type (NTM: Space's atmosphere tower extends this one). */
    protected TileEntityDeuteriumTower(BlockEntityType<?> type, BlockPos pos,
                                       BlockState state) {
        super(type, pos, state, 50_000, 5_000, 100_000);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityDeuteriumTower tower) {
        TileEntityDeuteriumExtractor.serverTick(level, pos, state, tower);
    }

    @Override
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x - dir.offsetX, y, z - dir.offsetZ, dir.getOpposite(), rot.getOpposite()),
                    PortDef.make(x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ, dir.getOpposite(), rot),
                    PortDef.make(x, y, z, rot.getOpposite(), dir),
                    PortDef.make(x + rot.offsetX, y, z + rot.offsetZ, dir, rot),
            };
        }
        return cachedPorts;
    }

    @Override
    public String getDefaultName() {
        return "block.hbm.machine_deuterium_tower";
    }
}
