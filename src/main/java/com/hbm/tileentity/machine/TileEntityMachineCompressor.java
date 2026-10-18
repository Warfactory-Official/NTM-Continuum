package com.hbm.tileentity.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * Compressor ({@code machine_compressor}): 1:1 port of the 1.12.2 {@code TileEntityMachineCompressor}.
 * Adds the client-side pump/fan animation on top of {@link TileEntityMachineCompressorBase}: the fan
 * spins 15°/tick and the pump strokes down at a randomised speed / up at 0.05 per tick while running.
 *
 * <p>Not ported: the boltgun stroke sound at the bottom of each pump stroke.
 */
public class TileEntityMachineCompressor extends TileEntityMachineCompressorBase {

    public float fanSpin;
    public float prevFanSpin;
    public float piston;
    public float prevPiston;
    public boolean pistonDir;
    private float randSpeed = 0.1F;

    public TileEntityMachineCompressor(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_COMPRESSOR.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCompressor te) {
        te.prevFanSpin = te.fanSpin;
        te.prevPiston = te.piston;

        if (te.isOn) {
            te.fanSpin += 15;
            if (te.fanSpin >= 360) {
                te.prevFanSpin -= 360;
                te.fanSpin -= 360;
            }

            if (te.pistonDir) {
                te.piston -= te.randSpeed;
                if (te.piston <= 0) {
                    // old: the boltgun stroke sound plays here (sound not ported)
                    te.pistonDir = !te.pistonDir;
                }
            } else {
                te.piston += 0.05F;
                if (te.piston >= 1) {
                    te.randSpeed = 0.085F + level.getRandom().nextFloat() * 0.03F;
                    te.pistonDir = !te.pistonDir;
                }
            }

            te.piston = Mth.clamp(te.piston, 0F, 1F);
        }
    }

    protected PortDef[] cachedPorts;

    @Override
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            int meta = MachineCore.facingOrdinal(getBlockState(), 2);
            ForgeDirection dir = ForgeDirection.getOrientation(meta);
            ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + rot.offsetX, y, z + rot.offsetZ, rot),
                    PortDef.make(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX, y, z - dir.offsetZ, dir.getOpposite()),
            };
        }
        return cachedPorts;
    }
}
