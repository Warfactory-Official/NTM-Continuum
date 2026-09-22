package com.hbm.tileentity.machine;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Compact compressor tile: the old machine has two counter-rotating fans and no piston. */
public class TileEntityMachineCompressorCompact extends TileEntityMachineCompressorBase {

    public float fanSpin;
    public float prevFanSpin;

    public TileEntityMachineCompressorCompact(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_COMPRESSOR_COMPACT.get(), pos, state);
    }

    /** Old client tick: each running tick advances both fans by exactly 45 degrees. */
    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityMachineCompressorCompact te) {
        te.prevFanSpin = te.fanSpin;
        if (!te.isOn) return;

        te.fanSpin += 45F;
        if (te.fanSpin >= 360F) {
            te.prevFanSpin -= 360F;
            te.fanSpin -= 360F;
        }
    }

    /** Six y+1 fluid/power ports, verbatim from old {@code getConPos()}. */
    @Override
    public DirPos[] getConPos() {
        int meta = MachineCore.facingOrdinal(getBlockState(), 2);
        ForgeDirection dir = ForgeDirection.getOrientation(meta);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY() + 1, z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + rot.offsetX * 4, y, z + rot.offsetZ * 4, rot),
                new DirPos(x - rot.offsetX * 4, y, z - rot.offsetZ * 4, rot.getOpposite()),
                new DirPos(x + dir.offsetX * 2 - rot.offsetX, y,
                        z + dir.offsetZ * 2 - rot.offsetZ, dir),
                new DirPos(x + dir.offsetX * 2 + rot.offsetX, y,
                        z + dir.offsetZ * 2 + rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 2 - rot.offsetX, y,
                        z - dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y,
                        z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite())
        };
    }
}
