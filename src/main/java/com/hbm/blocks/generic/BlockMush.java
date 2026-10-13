package com.hbm.blocks.generic;

import com.hbm.blocks.ModFalloutBlocks;
import dev.golem.core.world.CompatBonemealableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.config.TerrainConfig;

/**
 * 1.7 {@code BlockMush} ({@code mush}): the glowing mushroom of the nuclear wasteland. Stays on any block
 * with a solid top (Forge's {@code canSustainPlant} for a Cave plant) or on the waste blocks, but only
 * spreads onto the waste set ({@code canMushGrowHere}). On its random tick (one in 25, at most two other
 * mushrooms in range) it spreads to nearby waste ground and turns the dead grass under it into glowing
 * mycelium when the config allows. Bonemeal (40%) grows it into the huge mushroom.
 */
public class BlockMush extends BlockTerrainPlant implements CompatBonemealableBlock {

    /** 1.7 bounds: {@code 0.3..0.7} horizontally, {@code 0.4} high. */
    private static final VoxelShape SHAPE = Block.box(4.8, 0, 4.8, 11.2, 6.4, 11.2);

    public BlockMush(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 1.7 {@code canMushGrowHere}: the waste-terrain set. */
    public static boolean canMushGrowOn(BlockState below) {
        Block b = below.getBlock();
        return b == ModFalloutBlocks.waste_earth.get() || b == ModFalloutBlocks.waste_mycelium.get()
                || b == ModFalloutBlocks.waste_trinitite.get() || b == ModFalloutBlocks.waste_trinitite_red.get()
                || b == ModTerrainBlocks.block_waste.get() || b == ModTerrainBlocks.block_waste_painted.get()
                || b == ModTerrainBlocks.block_waste_vitrified.get();
    }

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY()) return false;
        // 1.7 canBlockStay: Forge's canSustainPlant for EnumPlantType.Cave is isSideSolid(UP), which is any solid top, OR the waste set
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        return ground.isFaceSturdy(level, below, Direction.UP) || canMushGrowOn(ground);
    }

    // Growth

    @Override
    public boolean canBonemeal(LevelReader level, BlockPos pos, BlockState state) {
        return canStay(state, level, pos);
    }

    /** 40% */
    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.4D;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        growHuge(level, pos);
    }

    /** 1.7 {@code growHuge}: clear the mushroom and stamp the {@code HugeMush} feature. */
    public void growHuge(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        HugeMush.generate(level, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (!canStay(state, level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
            return;
        }

        if (TerrainConfig.enableMycelium && level.getBlockState(pos.below()).is(ModFalloutBlocks.waste_earth.get())
                && rand.nextInt(5) == 0) {
            level.setBlockAndUpdate(pos.below(), ModFalloutBlocks.waste_mycelium.get().defaultBlockState());
        }

        if (rand.nextInt(25) == 0) {
            int range = 4;
            int maxShroom = 3;
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();

            // 1.7 scans x±4, y±4, z±1 (its loop variables were crossed; ported as written)
            for (int ix = x - range; ix <= x + range; ++ix) {
                for (int iy = y - range; iy <= y + range; ++iy) {
                    for (int iz = z - 1; iz <= z + 1; ++iz) {
                        if (level.getBlockState(new BlockPos(ix, iz, iy)).is(this)) {
                            --maxShroom;
                            if (maxShroom <= 0) return;
                        }
                    }
                }
            }

            int ix = x + rand.nextInt(5) - 2;
            int iy = z + rand.nextInt(2) - rand.nextInt(2);
            int iz = y + rand.nextInt(5) - 2;

            for (int l1 = 0; l1 < 4; ++l1) {
                BlockPos at = new BlockPos(ix, iy, iz);
                if (level.getBlockState(at).isAir() && canMushGrowOn(level.getBlockState(at.below()))) {
                    x = ix;
                    z = iy;
                    y = iz;
                }
                ix = x + rand.nextInt(5) - 2;
                iy = z + rand.nextInt(2) - rand.nextInt(2);
                iz = y + rand.nextInt(5) - 2;
            }

            BlockPos at = new BlockPos(ix, iy, iz);
            if (level.getBlockState(at).isAir() && canMushGrowOn(level.getBlockState(at.below()))) {
                level.setBlock(at, defaultBlockState(), 2);
            }
        }
    }

    /** Placement of the huge glowing mushroom ({@code com.hbm.world.feature.HugeMush}, unchanged). */
    public static final class HugeMush {
        private HugeMush() {}

        public static void generate(Level world, BlockPos origin) {
            int x = origin.getX(), y = origin.getY(), z = origin.getZ();
            BlockState skin = ModTerrainBlocks.mush_block.get().defaultBlockState();
            BlockState stem = ModTerrainBlocks.mush_block_stem.get().defaultBlockState();

            for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) world.setBlockAndUpdate(new BlockPos(x + i, y, z + j), skin);
            for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) world.setBlockAndUpdate(new BlockPos(x + i, y + 3, z + j), skin);
            for (int i = -2; i < 3; i++) for (int j = -2; j < 3; j++) world.setBlockAndUpdate(new BlockPos(x + i, y + 5, z + j), skin);
            for (int i = -4; i < 5; i++) for (int j = -4; j < 5; j++) for (int k = 0; k < 3; k++) {
                world.setBlockAndUpdate(new BlockPos(x + i, y + 6 + k, z + j), skin);
            }
            for (int i = -3; i < 4; i++) for (int j = -3; j < 4; j++) world.setBlockAndUpdate(new BlockPos(x + i, y + 9, z + j), skin);
            for (int i = -1; i < 2; i++) for (int j = -1; j < 2; j++) world.setBlockAndUpdate(new BlockPos(x + i, y + 10, z + j), skin);
            for (int i = 0; i < 8; i++) world.setBlockAndUpdate(new BlockPos(x, y + i, z), stem);
        }
    }
}
