package com.hbm.tileentity.machine.fusion;

import com.hbm.capability.MachineCaps;
import com.hbm.uninos.graph.EndpointRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The fusion beams: a klystron to a torus, and a torus to whatever it powers.
 *
 * <p>The 1.7 plasma and klystron nets did no distribution: both had an empty {@code update()}. All they
 * did was answer "is there a machine at the coordinate my beam lands on", which the nodespace did by
 * giving every machine a node and linking two whose coordinates coincided. That is a per-tick graph scan
 * for what is one map lookup.
 *
 * <p>It is a map lookup rather than a block lookup because the coordinate a beam lands on need not hold
 * a block: the torus claims positions seven out from its core, a klystron four out from its own, and
 * the two meet in the air between them. So a machine <b>claims</b> its end of the beam and a provider
 * <b>resolves</b> the other, through the same per-level table a duct uses to find a machine without
 * touching the chunk.
 */
public final class FusionLink {

    private FusionLink() {}

    /**
     * Claims {@code at} for {@code owner} on {@code domains}. Idempotent and called from the machine's
     * tick, like 1.7's node creation. The table evicts on chunk unload, which stands in for 1.7's node
     * {@code expired} flag.
     */
    public static void claim(@Nullable Level level, BlockEntity owner, BlockPos at, int domains) {
        if (!(level instanceof ServerLevel server)) return;
        EndpointRegistry.ofBeams(server).declare(at, owner.getBlockPos(), MachineCaps.FACE_MASK_ALL, domains);
    }

    /** The machine that claimed {@code at} on any of {@code domains}, or null where the beam lands on nothing. */
    public static @Nullable BlockEntity at(@Nullable Level level, BlockPos at, int domains) {
        if (!(level instanceof ServerLevel server)) return null;
        return EndpointRegistry.ofBeams(server).resolve(server, at.asLong(), null, domains);
    }

    /** Gives back every claim this machine made. Call from {@code setRemoved} on the server. */
    public static void release(@Nullable Level level, BlockPos... claimed) {
        if (!(level instanceof ServerLevel server)) return;
        for (BlockPos at : claimed) if (at != null) EndpointRegistry.ofBeams(server).withdraw(at);
    }
}
