package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.tileentity.machine.TileEntityMachineGiantTank;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1.7 {@code TileEntityBarrel}'s {@code ntm_fluid_tank}, inherited there by the Orbus, the big-ass tank and the
 * BAT-9000. The port's two giant tanks share {@link TileEntityMachineGiantTank} instead of extending the barrel.
 */
public final class BarrelPeripheral extends FluidTankPeripheral<BlockEntity> {

    public BarrelPeripheral(TileEntityBarrel machine) {
        super(machine, machine.tank);
    }

    public BarrelPeripheral(TileEntityMachineGiantTank machine) {
        super(machine, machine.tankNew);
    }
}
//?}
