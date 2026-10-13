package com.hbm.entity.logic;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.entity.ModEntities;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;

/** Long-lived HBM-energy EMP field, ported from the old reusable {@code EntityEMP}. */
public class EntityEMP extends CompatEntity implements IChunkLoader {

    private static final int RADIUS = 100;
    private static final int LIFE = 10 * 60 * 20;

    private List<BlockPos> machines;
    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 1);

    public EntityEMP(EntityType<?> type, Level level) { super(type, level); }

    public EntityEMP(Level level) { this(ModEntities.ENTITY_EMP.get(), level); }

    @Override
    protected void defineData(SynchedData data) {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;

        chunkLoader.tick();
        if (machines == null) allocate();
        else shock();

        if (tickCount > LIFE) discard();
    }

    private void allocate() {
        machines = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return;

        int centerX = (int) getX();
        int centerY = (int) getY();
        int centerZ = (int) getZ();
        int minChunkX = Math.floorDiv(centerX - RADIUS, 16);
        int maxChunkX = Math.floorDiv(centerX + RADIUS, 16);
        int minChunkZ = Math.floorDiv(centerZ - RADIUS, 16);
        int maxChunkZ = Math.floorDiv(centerZ + RADIUS, 16);

        // Enumerate resident block entities rather than asking the level for every block. The old call
        // was harmless when a chunk was already present, but modern getBlockEntity can generate an absent
        // chunk; the EMP's own 3x3 ticket must not turn a radius-100 scan into synchronous loads.
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = server.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    int x = pos.getX() - centerX;
                    int y = pos.getY() - centerY;
                    int z = pos.getZ() - centerZ;
                    if (x * x + y * y + z * z <= RADIUS * RADIUS) add(pos);
                }
            }
        }
    }

    private void add(BlockPos pos) {
        if (findBlockEntity(pos) instanceof IEnergyReceiverMK2) machines.add(pos);
    }

    private void shock() {
        for (BlockPos pos : machines) {
            if (findBlockEntity(pos) instanceof IEnergyReceiverMK2 receiver) receiver.setPower(0);
        }
    }

    private BlockEntity findBlockEntity(BlockPos pos) {
        if (!(level() instanceof ServerLevel server)) return null;
        LevelChunk chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }

    @Override public void initChunkLoading() { chunkLoader.initChunkLoading(); }
    @Override public void loadNeighboringChunks(int chunkX, int chunkZ) { chunkLoader.loadNeighboringChunks(chunkX, chunkZ); }
    @Override public void clearChunkLoader() { chunkLoader.clearChunkLoader(); }

    @Override
    public void remove(RemovalReason reason) {
        chunkLoader.clearChunkLoader();
        super.remove(reason);
    }

    @Override
    protected void loadData(DataIn input) { chunkLoader.markRestoredFromNbt(); }

    @Override
    protected void saveData(DataOut output) {}
}
