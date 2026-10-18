package com.hbm.module.portmanager;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.uninos.networkproviders.PowerNetProvider;
import net.minecraft.world.level.Level;

/** Power ports with a fixed shape, one separate port per PortDef. A new {@code PortDef[]} instance rebuilds them. */
public class ModulePortManPower {

    protected TileEntityLoadedBase owner;
    protected PortDef[] portReference;

    public TilePort[] powerPorts;
    public boolean input = true;
    public boolean output = true;

    public ModulePortManPower(TileEntityLoadedBase owner) {
        this.owner = owner;
    }

    public ModulePortManPower onlyInput() {
        this.input = true;
        this.output = false;
        return this;
    }

    public ModulePortManPower onlyOutput() {
        this.input = false;
        this.output = true;
        return this;
    }

    public void destroy() {
        if (powerPorts != null) for (TilePort port : powerPorts) port.disableIfPresent(owner.getLevel());
    }

    public void update(PortDef[] ports) {

        Level world = owner.getLevel();

        if (portReference != ports) {
            portReference = ports;

            if (this.powerPorts != null) for (TilePort port : this.powerPorts) {
                if (port != null) port.disableIfPresent(world);
            }

            this.powerPorts = TilePort.manyToMany(owner, ports);

            for (TilePort port : this.powerPorts) {
                port.setupType(PowerNetProvider.THE_PROVIDER);
            }
        }

        if (this.powerPorts != null) {
            for (TilePort port : this.powerPorts) {
                if (port == null) continue;
                port.update(world);
                // a machine is almost never both
                if (input && owner instanceof IEnergyReceiverMK2) port.checkSubscribe(world);
                if (output && owner instanceof IEnergyProviderMK2) port.checkProvide(world);
            }
        }
    }
}
