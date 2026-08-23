package com.hbm.handler.neutron;

import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** One queued cardinal neutron-flux stream. */
public abstract class NeutronStream {
    public NeutronNode origin;
    public double fluxQuantity, fluxRatio;
    public NeutronType type = NeutronType.DUMMY;
    public Vec3 vector;
    public NeutronStream(NeutronNode origin, Vec3 vector) { this.origin = origin; this.vector = vector; }
    public NeutronStream(NeutronNode origin, Vec3 vector, double flux, double ratio, NeutronType type) {
        this(origin, vector); this.fluxQuantity = flux; this.fluxRatio = ratio; this.type = type;
        NeutronNodeWorld.getOrAddWorld(origin.tile.getLevel()).addStream(this);
    }
    public Iterator<BlockPos> getBlocks(int range) {
        BlockPos originPos = origin.tile.getBlockPos();
        return new Iterator<>() { int i = 1;
            @Override public boolean hasNext() { return i <= range; }
            @Override public BlockPos next() { int x = (int) Math.floor(.5D + vector.x * i); int z = (int) Math.floor(.5D + vector.z * i++); return new BlockPos(originPos.getX() + x, originPos.getY(), originPos.getZ() + z); }
        };
    }
    public abstract void runStreamInteraction(Level level, StreamWorld streamWorld);
    public enum NeutronType { DUMMY, RBMK, PILE }
}
