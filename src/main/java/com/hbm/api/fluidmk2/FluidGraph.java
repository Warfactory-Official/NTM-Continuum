package com.hbm.api.fluidmk2;

import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The per-level duct topology: one {@code hbm:fluid_graph} save per dimension, all fluids in it. */
public final class FluidGraph {

    private FluidGraph() {}

    public static LevelNodeGraph<DuctData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, FluidGraphProvider.INSTANCE, "fluid_graph");
    }

    /** The fluid the cell at {@code posKey} carries, or null where there is no cell. */
    public static FluidType typeAt(LevelNodeGraph<DuctData> graph, long posKey) {
        DuctData data = graph.dataAt(posKey);
        return data == null ? null : data.type();
    }

    /** @see com.hbm.api.energymk2.PowerGraph#dropCells(BlockEntity, long...) */
    public static boolean dropCells(BlockEntity owner, long... cells) {
        if (!(owner.getLevel() instanceof ServerLevel server) || !LevelNodeGraph.blockWentWithIt(owner)) {
            return false;
        }
        LevelNodeGraph<DuctData> graph = get(server);
        for (long cell : cells) graph.removeNode(cell);
        return true;
    }

    public static boolean dropCells(BlockEntity owner, BlockPos cell) {
        return dropCells(owner, cell.asLong());
    }

    /**
     * Holds a machine's footprint as one conductor: the shape a tank in buffer mode has to take, where it
     * behaves as a length of duct so that two runs touching different ports are one network rather than
     * two the tank happens to sit between. Every port's inner cell is linked to the machine's own, which
     * is also where it declares itself as an endpoint.
     *
     * <p>Idempotent and called every tick: an existing cell is only retyped, and retyping to the same fluid
     * does nothing.
     *
     * @return the cells now held, to hand back to {@link #release}.
     */
    public static long[] hold(ServerLevel level, BlockEntity owner, FluidType type, DirPos[] ports) {
        LevelNodeGraph<DuctData> graph = get(level);
        DuctData data = DuctData.of(type);
        long[] cells = footprint(owner.getBlockPos(), ports);
        boolean minted = false;
        for (long cell : cells) {
            if (graph.containsCell(cell)) {
                graph.replaceNodeData(cell, data);
            } else {
                graph.addNode(cell, data, MachineCaps.FACE_MASK_ALL);
                minted = true;
            }
        }
        if (minted) for (int i = 1; i < cells.length; i++) graph.addRemoteLink(cells[0], cells[i]);
        EndpointRegistry.of(level).declare(owner.getBlockPos(), owner.getBlockPos(),
                MachineCaps.FACE_MASK_ALL, MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT);
        graph.setSelfEndpoint(cells[0], true);
        return cells;
    }

    /** Gives back what {@link #hold} took, for a machine that stopped conducting. */
    public static long[] release(ServerLevel level, long[] held) {
        if (held.length == 0) return held;
        LevelNodeGraph<DuctData> graph = get(level);
        for (long cell : held) graph.removeNode(cell);
        EndpointRegistry.of(level).withdraw(BlockPos.of(held[0]));
        return NONE;
    }

    public static final long[] NONE = new long[0];

    /** The machine's own cell first, as the anchor every port links to, then one cell per port. */
    private static long[] footprint(BlockPos core, DirPos[] ports) {
        LongArrayList cells = new LongArrayList(ports.length + 1);
        LongOpenHashSet seen = new LongOpenHashSet(ports.length + 1);
        cells.add(core.asLong());
        seen.add(core.asLong());
        for (DirPos port : ports) {
            BlockPos pos = port.getPos();
            long key = BlockPos.asLong(pos.getX() - port.getDir().offsetX, pos.getY() - port.getDir().offsetY,
                    pos.getZ() - port.getDir().offsetZ);
            if (seen.add(key)) cells.add(key);
        }
        return cells.toLongArray();
    }
}
