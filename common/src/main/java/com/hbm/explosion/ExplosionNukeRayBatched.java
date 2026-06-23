package com.hbm.explosion;

import com.hbm.interfaces.IExplosionRay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ExplosionNukeRayBatched implements IExplosionRay {

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static final int CLEAR_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final HashMap<ChunkPos, BitSet> perChunk = new HashMap<>();
    private final List<ChunkPos> orderedChunks = new ArrayList<>();
    private final CoordComparator comparator = new CoordComparator();
    private boolean isContained = true;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final Level world;

    private final int strength;
    private final int radius;

    private final int minY;
    private final int maxY;

    private final int gspNumMax;
    private int gspNum;
    private double gspX;
    private double gspY;

    private boolean isAusf3Complete = false;
    private int rayCheckInterval;
    @SuppressWarnings("unused")
    private UUID detonator;

    public ExplosionNukeRayBatched(Level world, int x, int y, int z, int strength, int radius) {
        this.world = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.radius = radius;
        this.minY = world.getMinY();
        this.maxY = world.getMaxY();

        this.gspNumMax = (int) (2.5 * Math.PI * Math.pow(this.strength, 2));
        this.gspNum = 1;

        this.gspX = Math.PI;
        this.gspY = 0.0;
        this.rayCheckInterval = Math.max(1, 10000 / radius);
    }

    private void generateGspUp() {
        if (this.gspNum < this.gspNumMax) {
            int k = this.gspNum + 1;
            double hk = -1.0 + 2.0 * (k - 1.0) / (this.gspNumMax - 1.0);
            this.gspX = Math.acos(hk);

            double prev_lon = this.gspY;
            double lon = prev_lon + 3.6 / Math.sqrt(this.gspNumMax) / Math.sqrt(1.0 - hk * hk);
            this.gspY = lon % (Math.PI * 2);
        } else {
            this.gspX = 0.0;
            this.gspY = 0.0;
        }
        this.gspNum++;
    }

    private Vec3 getSpherical2cartesian() {
        double dx = Math.sin(this.gspX) * Math.cos(this.gspY);
        double dy = Math.sin(this.gspX) * Math.sin(this.gspY);
        double dz = Math.cos(this.gspX);
        return new Vec3(dx, dy, dz);
    }

    private ChunkPos chunk;

    public void addPos(int x, int y, int z) {
        chunk = new ChunkPos(x >> 4, z >> 4);
        BitSet hitPositions = perChunk.computeIfAbsent(chunk, k -> new BitSet((maxY - minY + 1) << 8));
        hitPositions.set(((maxY - y) << 8) + ((x - chunk.getMinBlockX()) << 4) + (z - chunk.getMinBlockZ()));
    }

    private void cacheChunksTick(int time) {
        MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long raysProcessed = 0;
        long start = System.currentTimeMillis();

        BlockState blockState;
        int iX, iY, iZ;
        int rad;
        float rayStrength;
        Vec3 vec;

        while (this.gspNumMax >= this.gspNum) {
            vec = this.getSpherical2cartesian();

            rad = this.radius;
            rayStrength = strength * 0.3F;

            for (int r = 0; r < rad + 1; r++) {

                iY = (int) Math.floor(posY + (vec.y * r));

                if (iY < minY || iY > maxY) {
                    isContained = false;
                    break;
                }

                iX = (int) Math.floor(posX + (vec.x * r));
                iZ = (int) Math.floor(posZ + (vec.z * r));

                pos.set(iX, iY, iZ);
                blockState = world.getBlockState(pos);
                if (blockState.getBlock().getExplosionResistance() >= 2_000_000)
                    break;

                rayStrength -= (float) (Math.pow(getNukeResistance(blockState) + 1, 3 * ((float) (r = (r > 0) ? r : 1)) / ((float) rad)) - 1);

                if (rayStrength > 0) {
                    if (!blockState.isAir()) {

                        addPos(iX, iY, iZ);
                    }
                    if (r >= rad) {
                        isContained = false;
                    }
                } else {
                    break;
                }
            }

            this.generateGspUp();
            raysProcessed++;
            if (raysProcessed % rayCheckInterval == 0 && System.currentTimeMillis() + 1 > start + time) {
                return;
            }
        }
        orderedChunks.addAll(perChunk.keySet());
        orderedChunks.sort(comparator);

        isAusf3Complete = true;
    }

    public static float getNukeResistance(BlockState blockState) {
        if (blockState.liquid()) {
            return 0.1F;
        }

        return blockState.getBlock().getExplosionResistance();
    }

    @Override
    public void update(int processTimeMs) {
        if (isAusf3Complete)
            destructionTick(processTimeMs);
        else
            cacheChunksTick(processTimeMs);
    }

    @Override
    public void cancel() {
        isAusf3Complete = true;
        orderedChunks.clear();
        perChunk.clear();
    }

    @Override
    public boolean isComplete() {
        return isAusf3Complete && perChunk.isEmpty();
    }

    @Override
    public boolean isContained() {
        return isContained;
    }

    @Override
    public void setDetonator(UUID detonator) {
        this.detonator = detonator;
    }

    public class CoordComparator implements Comparator<ChunkPos> {

        @Override
        public int compare(ChunkPos o1, ChunkPos o2) {
            int chunkX = ExplosionNukeRayBatched.this.posX >> 4;
            int chunkZ = ExplosionNukeRayBatched.this.posZ >> 4;

            int diff1 = Math.abs((chunkX - (o1.getMinBlockX() >> 4))) + Math.abs((chunkZ - (o1.getMinBlockZ() >> 4)));
            int diff2 = Math.abs((chunkX - (o2.getMinBlockX() >> 4))) + Math.abs((chunkZ - (o2.getMinBlockZ() >> 4)));

            return Integer.compare(diff1, diff2);
        }
    }

    public void destructionTick(int time) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + time) {
            if (perChunk.isEmpty()) return;
            processChunkBlocks(start, time);
        }
    }

    private BitSet hitArray;
    private ChunkPos destChunk;
    private boolean needsNewHitArray = true;
    private int index = 0;

    public void processChunkBlocks(long start, int time) {
        if (this.perChunk.isEmpty()) return;
        if (needsNewHitArray) {
            destChunk = orderedChunks.get(0);
            hitArray = perChunk.get(destChunk);
            index = hitArray.nextSetBit(0);
            needsNewHitArray = false;
        }

        int chunkX = destChunk.getMinBlockX();
        int chunkZ = destChunk.getMinBlockZ();

        MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int blocksRemoved = 0;
        while (index > -1) {
            pos.set(((index >> 4) & 15) + chunkX, maxY - (index >> 8), (index & 15) + chunkZ);

            world.setBlock(pos, AIR, CLEAR_FLAGS);
            index = hitArray.nextSetBit(index + 1);
            blocksRemoved++;
            if (blocksRemoved % 256 == 0 && System.currentTimeMillis() + 1 > start + time) {
                break;
            }
        }

        if (index < 0) {
            perChunk.remove(destChunk);
            orderedChunks.remove(0);
            needsNewHitArray = true;
        }
    }
}
