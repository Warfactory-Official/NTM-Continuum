package com.hbm.tileentity.machine.fusion;

import dev.golem.core.graph.Domains;
import dev.golem.core.graph.EndpointRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The fusion beams: a klystron to a torus, and a torus to whatever it powers.
 *
 * <p>These were the last two things on UNINOS, and they were never distribution — both nets had an
 * empty {@code update()}. All they ever did was answer "is there a machine at the coordinate my beam
 * lands on", which the nodespace did by giving every machine a node and linking two whose coordinates
 * coincided. A whole per-tick graph scan for a question that is one map lookup.
 *
 * <p>It is a map lookup rather than a block lookup because the coordinate a beam lands on need not hold
 * a block: the torus claims positions seven out from its core, a klystron four out from its own, and
 * the two meet in the air between them. So a machine <b>claims</b> its end of the beam and a provider
 * <b>resolves</b> the other, through the same per-level table a duct uses to find a machine without
 * touching the chunk.
 */
public final class FusionLink {

    public static final int PLASMA_IN = Domains.FIRST_FREE;
    public static final int PLASMA_OUT = Domains.FIRST_FREE << 1;
    public static final int KLYSTRON_IN = Domains.FIRST_FREE << 2;
    public static final int KLYSTRON_OUT = Domains.FIRST_FREE << 3;

    private FusionLink() {}

    /**
     * Claims {@code at} for {@code owner} on {@code domains}. Idempotent and called from the machine's
     * tick, exactly as the node creation it replaces was; the table evicts on chunk unload, which is
     * what the node's {@code expired} flag used to mean.
     */
    public static void claim(@Nullable Level level, BlockEntity owner, BlockPos at, int domains) {
        if (!(level instanceof ServerLevel server)) return;
        EndpointRegistry.ofBeams(server).declare(at, owner.getBlockPos(), Domains.FACES_ALL, domains);
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
