package com.hbm.tileentity;

import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.config.ServerConfig;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.PortNet;
import com.hbm.uninos.PortNode;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Each machine owns the nodes of its ports. Connections are made once, when the machine is placed or the port's type
 * changes; there are no per-tick subscriptions and no timeouts.
 */
public class TilePort {

    protected @Nullable PortNode node;
    protected @Nullable INetworkProvider type;
    public BlockPos[] positions;
    public DirPos[] connections;

    public boolean needsRebuild = false;
    protected boolean isEnabled = true;
    /** Usually a block entity, can be a delegate object too. */
    protected Object owner;
    /** The level the owner was last marked ported in for the current type. */
    private @Nullable Level markedIn;
    private @Nullable FluidType[] fluidTypes;
    private @Nullable TilePort[] fluidAliases;

    public TilePort setupOwner(Object owner) {
        this.owner = owner;
        this.markedIn = null;
        return this;
    }

    /** May change over the tile's lifetime; null and NONE disable this port. */
    public TilePort setupType(@Nullable INetworkProvider type) {
        if (this.type != type) {
            this.type = type;
            this.needsRebuild = true;
            this.markedIn = null;
        }
        return this;
    }

    /** One native port per exact resource of a loose kind, or the strict identifier resource alone. */
    public TilePort setupFluids(FluidTankNTM tank, @Nullable Level level) {
        if (owner instanceof BlockEntity blockEntity) tank.bindOwner(blockEntity);
        var types = tank.connectionTypes();
        if (types == fluidTypes) return this;
        if (fluidAliases != null) for (var alias : fluidAliases) alias.disableIfPresent(level);
        fluidTypes = types;
        setupType(FluidNetProvider.of(types[0]));
        fluidAliases = types.length <= 1 ? null : new TilePort[types.length - 1];
        if (fluidAliases != null) for (var index = 1; index < types.length; index++) {
            fluidAliases[index - 1] = new TilePort().setupOwner(owner).setupPositions(positions)
                    .setupConnections(connections).setupType(FluidNetProvider.of(types[index]));
            fluidAliases[index - 1].isEnabled = isEnabled;
            fluidAliases[index - 1].isEnabled = isEnabled;
        }
        return this;
    }

    public TilePort setupPort(PortDef def) {
        return this.setupPositions(def.portPositions).setupConnections(def.portConnections);
    }

    /** Several positions make one passthrough port group. */
    public TilePort setupPositions(BlockPos... pos) {
        this.positions = pos;
        this.needsRebuild = true;
        return this;
    }

    public TilePort setupConnections(DirPos... pos) {
        this.connections = pos;
        this.needsRebuild = true;
        return this;
    }

    public void update(Level world) {
        if (this.positions == null || this.positions.length == 0 || this.connections == null || this.connections.length == 0
                || this.owner == null) return;
        if (this.type != null && this.markedIn != world) {
            PortNodes.markPorted(world, owner, type);
            this.markedIn = world;
        }

        if (this.needsRebuild) {
            disableIfPresent(world);
            this.needsRebuild = false;
        }

        if (isEnabled()) {
            enableIfMissing(world);
        } else {
            disableIfPresent(world);
        }
        if (fluidAliases != null) for (var alias : fluidAliases) alias.update(world);
    }

    public void enableIfMissing(Level world) {
        if (this.node == null || this.node.expired) {
            this.node = PortNodes.getNode(world, positions[0], type);
        }
        if (this.node == null || this.node.expired) {
            this.createNode(world);
        }
    }

    public void disableIfPresent(Level world) {
        if (this.node != null) {
            PortNodes.destroyNode(world, node);
            this.node = null;
            if (type instanceof FluidNetProvider) for (var connection : connections) BlockFluidDuct.portChanged(world, connection.getPos());
        }
        if (fluidAliases != null) for (var alias : fluidAliases) alias.disableIfPresent(world);
    }

    protected void createNode(Level world) {
        this.node = new PortNode(type, positions, connections);
        PortNodes.createNode(world, this.node);
        if (type instanceof FluidNetProvider) for (DirPos con : connections) BlockFluidDuct.portChanged(world, con.getPos());
    }

    public void checkSubscribe(Level world) {
        if (fluidAliases != null) for (var alias : fluidAliases) alias.checkSubscribe(world);
        if (this.node == null) return;
        this.node.receivers.add(owner);
        if (ServerConfig.DEBUG_PORT_VIEW.get() && this.type != null && world instanceof ServerLevel server)
            for (DirPos pos : this.connections) this.type.spawnDebugParticles(server, pos, true);
    }

    public void checkProvide(Level world) {
        if (fluidAliases != null) for (var alias : fluidAliases) alias.checkProvide(world);
        if (this.node == null) return;
        this.node.providers.add(owner);
        if (ServerConfig.DEBUG_PORT_VIEW.get() && this.type != null && world instanceof ServerLevel server)
            for (DirPos pos : this.connections) this.type.spawnDebugParticles(server, pos, false);
    }

    public void forceRebuild() {
        this.needsRebuild = true;
    }

    public void enable() {
        if (!isEnabled) { isEnabled = true; forceRebuild(); }
        if (fluidAliases != null) for (var alias : fluidAliases) alias.enable();
    }
    public void disable() {
        if (isEnabled) { isEnabled = false; forceRebuild(); }
        if (fluidAliases != null) for (var alias : fluidAliases) alias.disable();
    }

    /** Active when switched on and the type is neither null nor the NONE fluid. */
    protected boolean isEnabled() {
        if (!isEnabled) return false;
        if (type == null) return false;
        return !(type instanceof FluidNetProvider fluid && fluid.type == Fluids.NONE);
    }

    /** One PortDef as several ports with identical positions and connections: passthrough fluid ports. */
    public static TilePort[] oneToMany(Object owner, int portCount, PortDef def) {
        TilePort[] ports = new TilePort[portCount];
        for (int i = 0; i < portCount; i++) {
            ports[i] = new TilePort().setupOwner(owner).setupPositions(def.portPositions).setupConnections(def.portConnections);
        }
        return ports;
    }

    /** One port per PortDef: separate power ports. */
    public static TilePort[] manyToMany(Object owner, PortDef... defs) {
        TilePort[] ports = new TilePort[defs.length];
        for (int i = 0; i < defs.length; i++) {
            ports[i] = new TilePort().setupOwner(owner).setupPositions(defs[i].portPositions).setupConnections(defs[i].portConnections);
        }
        return ports;
    }

    /** The net of the last round, or null while the port has none. */
    public @Nullable PortNet<?> getNetwork() {
        if (this.node == null || this.node.expired) return null;
        return this.node.net;
    }

    public static class PortDef {
        public BlockPos[] portPositions;
        public DirPos[] portConnections;

        public PortDef atPos(BlockPos... pos) {
            this.portPositions = pos;
            return this;
        }

        public PortDef withCon(DirPos... pos) {
            this.portConnections = pos;
            return this;
        }

        public static PortDef make(int x, int y, int z, ForgeDirection... dirs) {
            DirPos[] cons = new DirPos[dirs.length];
            PortDef def = new PortDef().atPos(new BlockPos(x, y, z));
            for (int i = 0; i < cons.length; i++) {
                cons[i] = new DirPos(x + dirs[i].offsetX, y + dirs[i].offsetY, z + dirs[i].offsetZ, dirs[i]);
            }
            def.withCon(cons);
            return def;
        }

        public static PortDef make(BlockPos pos, ForgeDirection... dirs) {
            return make(pos.getX(), pos.getY(), pos.getZ(), dirs);
        }

        /** Every connection of these ports, for a block asked whether a face is a port. */
        public static DirPos[] connections(PortDef... defs) {
            return combine(defs).portConnections;
        }

        public static PortDef combine(PortDef... defs) {
            PortDef single = new PortDef();
            List<BlockPos> pos = new ArrayList<>();
            List<DirPos> con = new ArrayList<>();
            for (PortDef def : defs) {
                for (BlockPos p : def.portPositions) pos.add(p);
                for (DirPos c : def.portConnections) con.add(c);
            }
            single.atPos(pos.toArray(new BlockPos[0])).withCon(con.toArray(new DirPos[0]));
            return single;
        }
    }
}
