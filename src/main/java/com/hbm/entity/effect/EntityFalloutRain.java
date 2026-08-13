package com.hbm.entity.effect;

import dev.golem.core.data.CompatEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.config.BombConfig;
import com.hbm.config.FalloutConfig;
import com.hbm.config.FalloutConfig.FalloutEntry;
import com.hbm.mixin.FireBlockAccessor;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * The fallout sweep spawned when a nuclear crater completes, ported from 1.12.2's
 * {@code EntityFalloutRain}: it walks every chunk the fallout disc touches and "stomps" each column —
 * depositing the radioactive {@code fallout} ash layer (chance peaking at 70% of the scale),
 * petrifying/killing organics, converting terrain into the sellafite shade rings
 * ({@link FalloutConfig}), starting fires, and dropping now-unsupported soft blocks as falling blocks.
 * Invisible logic entity like {@code EntityNukeExplosionMK5} (NoopRenderer).
 *
 * <p><b>Threading (mirrors the fork):</b> the old entity scanned chunks off-thread on a ForkJoin pool
 * and applied block writes on the server thread within a {@code BombConfig.mk5} ms budget per tick.
 * This port does the same: {@link #tick} resolves each chunk <em>on the server thread</em> (sync
 * load), hands the {@link LevelChunk} to the common pool where {@link #scanChunk} computes the
 * column stomps using <b>chunk-confined reads only</b> (workers must never call {@code Level} methods
 * — the server chunk map is main-thread-guarded), and the resulting ordered write lists are applied
 * back on the server thread under the budget. A fully single-threaded version processed 1–3 chunks
 * per tick and took minutes over a Fat Man's disc; the fork finished in tens of seconds.
 *
 * <p>Deferred vs the fork: the crater biome rewrite ({@code getBiomeChange}) and the post-fallout
 * thunderstorm.
 *
 * <p>The chunk lists survive save/reload via NBT ({@code chunks}/{@code outerChunks} int-pair arrays);
 * chunks leave the lists only once their writes are fully applied, so an interrupted sweep resumes
 * (in-flight scans are simply redone).
 */
public class EntityFalloutRain extends CompatEntity {

    private static final int MAX_SOLID_DEPTH = 3;
    private static final int MIN_ANGLE_STEPS = 18;
    private static final int SPOKE_STEP_BLOCKS = 8;

    private int scale = 1;
    /** Chunks fully inside the disc, then the boundary ring (whose columns are clamped to the radius). */
    private LongArrayList chunksToProcess = new LongArrayList();
    private LongArrayList outerChunksToProcess = new LongArrayList();
    private boolean gathered = false;

    /** Dispatched-but-not-yet-applied chunks (server thread only; subset of the lists above). */
    private final LongOpenHashSet inFlight = new LongOpenHashSet();
    /** Scan results awaiting application, produced by pool workers. */
    private final ConcurrentLinkedQueue<ChunkScan> completedScans = new ConcurrentLinkedQueue<>();
    /** The scan currently being applied (may span ticks if the budget runs out mid-chunk). */
    private ChunkScan applying;

    public EntityFalloutRain(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void setScale(int scale) {
        this.scale = Math.max(scale, 1);
    }

    public int getScale() {
        return scale;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synced data — server-driven and invisible (the old SCALE DataParameter fed a renderer we don't have).
    }

    @Override
    //? if >=26.1 {
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
    //?} else {
    /*public boolean hurt(DamageSource source, float damage) {
    *///?}
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        if (!gathered && chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()) {
            gatherChunks();
        }
        gathered = true;

        // Force the lazy transform table to build on the server thread — its unsynchronized first
        // build must not race between pool workers.
        FalloutConfig.entries();

        dispatchScans(server);
        applyScans(server);

        if (chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()
                && inFlight.isEmpty() && completedScans.isEmpty() && applying == null) {
            discard();
        }
    }

    /** Keeps up to the pool's parallelism in chunk scans in flight. Chunk resolution stays main-thread. */
    private void dispatchScans(ServerLevel server) {
        int maxInFlight = ForkJoinPool.commonPool().getParallelism();
        while (inFlight.size() < maxInFlight) {
            boolean outer = false;
            long packed = nextUndispatched(chunksToProcess);
            if (packed == Long.MIN_VALUE) {
                packed = nextUndispatched(outerChunksToProcess);
                outer = true;
            }
            if (packed == Long.MIN_VALUE) return;

            LevelChunk chunk = server.getChunk(ChunkPos.getX(packed), ChunkPos.getZ(packed)); // sync load
            inFlight.add(packed);

            long chunkPos = packed;
            boolean clamp = outer;
            double ex = getX(), ez = getZ();
            int sc = getScale();
            ForkJoinPool.commonPool().execute(() -> {
                ChunkScan scan = new ChunkScan(chunkPos);
                scanChunk(scan, chunk, clamp, ex, ez, sc);
                completedScans.add(scan);
            });
        }
    }

    /** Last-to-first like the old removeLong(size-1) processing order, skipping in-flight chunks. */
    private long nextUndispatched(LongArrayList list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            long packed = list.getLong(i);
            if (!inFlight.contains(packed)) return packed;
        }
        return Long.MIN_VALUE;
    }

    /** Applies completed scans' block writes on the server thread within the old mk5 ms budget. */
    private void applyScans(ServerLevel server) {
        long deadline = System.nanoTime() + BombConfig.mk5 * 1_000_000L;
        while (true) {
            if (applying == null) applying = completedScans.poll();
            if (applying == null) return;

            if (!applyWrites(server, applying, deadline)) return; // budget exhausted mid-chunk

            long packed = applying.chunkPos;
            inFlight.remove(packed);
            chunksToProcess.rem(packed);
            outerChunksToProcess.rem(packed);
            applying = null;
        }
    }

    /** One chunk's ordered writes. {@code state == null} marks a falling-block spawn (re-validated at apply). */
    private static final class ChunkScan {
        final long chunkPos;
        final List<BlockWrite> writes = new ArrayList<>();
        int appliedUpTo = 0;

        ChunkScan(long chunkPos) {
            this.chunkPos = chunkPos;
        }
    }

    private record BlockWrite(long pos, BlockState state) {}

    private boolean applyWrites(ServerLevel server, ChunkScan scan, long deadline) {
        List<BlockWrite> writes = scan.writes;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        float stonebrickRes = Blocks.STONE_BRICKS.getExplosionResistance();

        for (int i = scan.appliedUpTo; i < writes.size(); i++) {
            if ((i & 15) == 0 && System.nanoTime() >= deadline) {
                scan.appliedUpTo = i;
                return false;
            }
            BlockWrite write = writes.get(i);
            pos.set(BlockPos.getX(write.pos()), BlockPos.getY(write.pos()), BlockPos.getZ(write.pos()));
            if (write.state() != null) {
                server.setBlock(pos, write.state(), Block.UPDATE_ALL);
            } else {
                // Falling-block spawn: re-read at apply time (the scan ran against pre-write state).
                BlockState now = server.getBlockState(pos);
                if (!now.isAir()) {
                    float hardness = now.getDestroySpeed(server, pos);
                    if (hardness >= 0.0F && hardness <= stonebrickRes) {
                        FallingBlockEntity falling = FallingBlockEntity.fall(server, pos.immutable(), now);
                        falling.dropItem = false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * The old {@code gatherChunks}: the boundary ring is sampled at {@code max(18, 20·scale/32)}
     * angles; the interior along the same spokes every 8 blocks. Dedup'd into inner/outer lists.
     */
    private void gatherChunks() {
        int radius = getScale();
        int angleSteps = Math.max(MIN_ANGLE_STEPS, 20 * radius / 32);

        LongOpenHashSet outer = new LongOpenHashSet();
        LongOpenHashSet inner = new LongOpenHashSet();

        double px = getX();
        double pz = getZ();

        for (int step = 0; step <= angleSteps; step++) {
            double theta = step * (2.0 * Math.PI) / angleSteps;
            double cos = Math.cos(theta);
            double sin = Math.sin(theta);

            int cx = ((int) Math.floor(px + radius * cos)) >> 4;
            int cz = ((int) Math.floor(pz - radius * sin)) >> 4;
            outer.add(ChunkPos.pack(cx, cz));

            for (int d = 0; d <= radius; d += SPOKE_STEP_BLOCKS) {
                int ix = ((int) Math.floor(px + d * cos)) >> 4;
                int iz = ((int) Math.floor(pz - d * sin)) >> 4;
                long packed = ChunkPos.pack(ix, iz);
                if (!outer.contains(packed)) inner.add(packed);
            }
        }
        inner.removeAll(outer);

        chunksToProcess = new LongArrayList(inner);
        outerChunksToProcess = new LongArrayList(outer);
    }

    // ------------------------------------------------------------------ worker side (pool threads)

    /**
     * Scans one chunk into an ordered write list. Runs on a pool thread: every read is confined to the
     * given {@link LevelChunk} (block states, the chunk-local heightmap, chunk-as-BlockGetter shape
     * queries) — never {@code Level}/{@code ServerLevel}, whose chunk access is main-thread-guarded.
     * Decisions are made against pre-write state, exactly like the fork's off-thread scan.
     */
    private void scanChunk(ChunkScan scan, LevelChunk chunk, boolean clampToRadius, double ex, double ez, int scale) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        RandomSource rand = RandomSource.create();

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double distance = Math.hypot(x - ex, z - ez);
                if (clampToRadius && distance > scale) continue;
                double percent = distance * 100.0 / scale;
                stompColumn(scan, chunk, x, z, percent, rand);
            }
        }
    }

    /**
     * The old {@code stompColumn}: walk the column top-down, transforming at most
     * {@link #MAX_SOLID_DEPTH} solid blocks, depositing the fallout layer / fire on the way.
     * Emits writes instead of applying them.
     */
    private void stompColumn(ChunkScan scan, LevelChunk chunk, int x, int z, double distPercent, RandomSource rand) {
        int solidDepth = 0;
        int bottomY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        float stonebrickRes = Blocks.STONE_BRICKS.getExplosionResistance();
        List<FalloutEntry> entries = FalloutConfig.entries();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos posUp = new BlockPos.MutableBlockPos();

        // Start at the surface, not the sky top (chunk-local heightmap; +1 = Level.getHeight parity).
        int top = Math.min(maxY, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1);
        for (int y = top; y >= bottomY; y--) {
            if (solidDepth >= MAX_SOLID_DEPTH) return;

            pos.set(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            Block block = state.getBlock();
            if (state.isAir() || block == ModFalloutBlocks.fallout.get()) continue;

            // fallout ash layer on the surface — chance peaks (0.1) at 70% of the scale
            BlockState stateUp = null;
            int upY = y + 1;
            if (solidDepth == 0 && upY <= maxY) {
                stateUp = chunk.getBlockState(posUp.set(x, upY, z));
                boolean airOrReplaceable = stateUp.isAir() || (stateUp.canBeReplaced() && !stateUp.liquid());
                // BlockFallout.canSurvive replicated chunk-locally: it only inspects the support block
                // below (= this column's current block): no ice, and leaves or a sturdy up face.
                boolean supports = !(state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE))
                        && (state.is(BlockTags.LEAVES) || state.isFaceSturdy(chunk, pos, Direction.UP));
                if (airOrReplaceable && supports) {
                    double d = distPercent / 100.0;
                    double chance = 0.1 - Math.pow(d - 0.7, 2.0);
                    if (chance >= rand.nextDouble()) {
                        scan.writes.add(new BlockWrite(posUp.asLong(), ModFalloutBlocks.fallout.get().defaultBlockState()));
                    }
                }
            }

            // fires on flammables inside 65%
            if (distPercent < 65 && ((FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(state) > 0 && upY <= maxY) {
                if (stateUp == null) stateUp = chunk.getBlockState(posUp.set(x, upY, z));
                if (stateUp.isAir() && rand.nextInt(5) == 0) {
                    scan.writes.add(new BlockWrite(BlockPos.asLong(x, upY, z), Blocks.FIRE.defaultBlockState()));
                }
            }

            // the transform table — first matching entry wins
            boolean transformed = false;
            for (FalloutEntry entry : entries) {
                BlockState result = entry.eval(y, state, distPercent, rand, bottomY);
                if (result != null) {
                    scan.writes.add(new BlockWrite(pos.asLong(), result));
                    if (entry.isSolid()) solidDepth++;
                    transformed = true;
                    break;
                }
            }

            // untransformed soft blocks over air collapse as falling blocks (no drops)
            if (!transformed && distPercent < 65 && y > bottomY) {
                BlockState below = chunk.getBlockState(posUp.set(x, y - 1, z));
                if (below.isAir()) {
                    float hardnessHere = state.getDestroySpeed(chunk, pos);
                    if (hardnessHere >= 0.0F && hardnessHere <= stonebrickRes) {
                        for (int i = 0; i <= solidDepth; i++) {
                            int yy = y + i;
                            if (yy > maxY) break;
                            BlockPos p = new BlockPos(x, yy, z);
                            BlockState sAt = chunk.getBlockState(p);
                            if (sAt.isAir()) continue;
                            float h = sAt.getDestroySpeed(chunk, p);
                            if (h >= 0.0F && h <= stonebrickRes) {
                                scan.writes.add(new BlockWrite(p.asLong(), null)); // falling-block marker
                            }
                        }
                    }
                }
            }

            if (!transformed && state.isCollisionShapeFullBlock(chunk, pos)) solidDepth++;
        }
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void loadData(DataIn input) {
        setScale(input.getIntOr("scale", 1));
        chunksToProcess = fromPairs(input.getIntArray("chunks").orElse(new int[0]));
        outerChunksToProcess = fromPairs(input.getIntArray("outerChunks").orElse(new int[0]));
        gathered = input.getBooleanOr("gathered", !chunksToProcess.isEmpty() || !outerChunksToProcess.isEmpty());
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("scale", scale);
        output.putIntArray("chunks", toPairs(chunksToProcess));
        output.putIntArray("outerChunks", toPairs(outerChunksToProcess));
        output.putBoolean("gathered", gathered);
    }

    private static LongArrayList fromPairs(int[] data) {
        LongArrayList list = new LongArrayList(data.length / 2);
        for (int i = 0; i + 1 < data.length; i += 2) list.add(ChunkPos.pack(data[i], data[i + 1]));
        return list;
    }

    private static int[] toPairs(LongArrayList coords) {
        int[] data = new int[coords.size() * 2];
        int i = 0;
        for (LongIterator it = coords.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            data[i++] = ChunkPos.getX(packed);
            data[i++] = ChunkPos.getZ(packed);
        }
        return data;
    }
}
