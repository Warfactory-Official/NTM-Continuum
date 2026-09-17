package com.hbm.entity.logic;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

public abstract class EntityExplosionChunkloading extends CompatEntity {

    private final LongOpenHashSet forcedChunks = new LongOpenHashSet();
    private boolean released = false;
    /**
     * Whether this entity currently holds its anchors. Reset on load so a reloaded entity re-anchors once.
     */
    private boolean anchored;

    protected EntityExplosionChunkloading(EntityType<? extends EntityExplosionChunkloading> type, Level level) {
        super(type, level);
    }

    /**
     * Claim a (2r+1)^2 chunk square with a PER-ENTITY anchor ticket, not setChunkForced - a global boolean with no owner, so the first finisher cleared everyone's claim (measured: fallout never ticking).
     */

    protected void forceArea(int chunkRadius) {
        if (released || !(level() instanceof ServerLevel server)) return;
        int cx = blockPosition().getX() >> 4;
        int cz = blockPosition().getZ() >> 4;
        for (int x = cx - chunkRadius; x <= cx + chunkRadius; x++) {
            for (int z = cz - chunkRadius; z <= cz + chunkRadius; z++) {
                long key = ChunkPos.pack(x, z);
                if (forcedChunks.add(key)) ChunkUtil.addAnchor(server, key);
            }
        }
        anchored = true;
    }

    /**
     * Re-anchor after a world reload, and ONLY then - the old body re-forced every tick, which was harmless for a boolean and would be a ticket leak here.
     */
    protected void ensureForced() {
        if (released || anchored || forcedChunks.isEmpty() || !(level() instanceof ServerLevel server)) return;
        for (long key : forcedChunks) ChunkUtil.addAnchor(server, key);
        anchored = true;
    }

    public void clearChunkLoader() {
        if (released || forcedChunks.isEmpty() || !(level() instanceof ServerLevel server)) return;
        released = true;
        if (anchored) for (long key : forcedChunks) ChunkUtil.releaseAnchor(server, key);
        anchored = false;
        forcedChunks.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkLoader();
        super.remove(reason);
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    @Override
    protected void loadData(DataIn input) {
        int[] packed = input.getIntArray("forcedChunks").orElse(null);
        if (packed != null)
            for (int i = 0; i + 1 < packed.length; i += 2) forcedChunks.add(ChunkPos.pack(packed[i], packed[i + 1]));
    }

    @Override
    protected void saveData(DataOut output) {
        if (forcedChunks.isEmpty()) return;
        int[] packed = new int[forcedChunks.size() * 2];
        int i = 0;
        for (long key : forcedChunks) {
            packed[i++] = ChunkPos.getX(key);
            packed[i++] = ChunkPos.getZ(key);
        }
        output.putIntArray("forcedChunks", packed);
    }
}
