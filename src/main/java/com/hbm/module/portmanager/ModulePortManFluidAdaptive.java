package com.hbm.module.portmanager;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import net.minecraft.world.level.Level;

/**
 * A fluid port group with a fixed shape: one passthrough port per tank over all of the shape's positions, typed by
 * its tank. A new {@code PortDef[]} instance rebuilds every port.
 */
public class ModulePortManFluidAdaptive {

    protected TileEntityLoadedBase owner;
    protected PortDef[] portReference;

    public TilePort[] inPorts;
    public FluidTankNTM[] inTanks;
    public boolean[] inEnabled;
    public TilePort[] outPorts;
    public FluidTankNTM[] outTanks;
    public boolean[] outEnabled;

    public ModulePortManFluidAdaptive(TileEntityLoadedBase owner) {
        this.owner = owner;
    }

    public void destroy() {
        if (inPorts != null) for (TilePort port : inPorts) if (port != null) port.disableIfPresent(owner.getLevel());
        if (outPorts != null) for (TilePort port : outPorts) if (port != null) port.disableIfPresent(owner.getLevel());
    }

    public ModulePortManFluidAdaptive setInputTanks(FluidTankNTM... tanks) {
        inPorts = new TilePort[tanks.length];
        inTanks = tanks;
        inEnabled = new boolean[tanks.length];
        for (int i = 0; i < inEnabled.length; i++) inEnabled[i] = true;
        return this;
    }

    public void toggleInput(FluidTankNTM input, boolean enabled) {
        for (int i = 0; i < inEnabled.length; i++) {
            if (inTanks[i] == input) {
                inEnabled[i] = enabled;
                return;
            }
        }
    }

    public ModulePortManFluidAdaptive setOutputTanks(FluidTankNTM... tanks) {
        outPorts = new TilePort[tanks.length];
        outTanks = tanks;
        outEnabled = new boolean[tanks.length];
        for (int i = 0; i < outEnabled.length; i++) outEnabled[i] = true;
        return this;
    }

    public void toggleOutput(FluidTankNTM output, boolean enabled) {
        for (int i = 0; i < outEnabled.length; i++) {
            if (outTanks[i] == output) {
                outEnabled[i] = enabled;
                return;
            }
        }
    }

    public void update(PortDef[] ports) {

        Level world = owner.getLevel();

        if (portReference != ports) {
            portReference = ports;

            PortDef unified = PortDef.combine(ports);
            if (inPorts != null) {
                for (int i = 0; i < inPorts.length; i++) {
                    TilePort port = inPorts[i];
                    if (port != null) port.disableIfPresent(world);
                    inPorts[i] = new TilePort().setupOwner(owner)
                            .setupPositions(unified.portPositions).setupConnections(unified.portConnections);
                }
            }
            if (outPorts != null) {
                for (int i = 0; i < outPorts.length; i++) {
                    TilePort port = outPorts[i];
                    if (port != null) port.disableIfPresent(world);
                    outPorts[i] = new TilePort().setupOwner(owner)
                            .setupPositions(unified.portPositions).setupConnections(unified.portConnections);
                }
            }
        }

        if (inPorts != null) {
            for (int i = 0; i < inPorts.length; i++) {
                TilePort port = inPorts[i];
                if (port == null) continue;
                port.setupFluids(inTanks[i], world);
                if (inEnabled[i]) port.enable(); else port.disable();
                port.update(world);
                port.checkSubscribe(world);
            }
        }

        if (outPorts != null) {
            for (int i = 0; i < outPorts.length; i++) {
                TilePort port = outPorts[i];
                if (port == null) continue;
                port.setupFluids(outTanks[i], world);
                if (outEnabled[i]) port.enable(); else port.disable();
                port.update(world);
                port.checkProvide(world);
            }
        }
    }
}
