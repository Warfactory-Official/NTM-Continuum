package com.hbm.tileentity.machine.soyuz;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.soyuz.ModSoyuzBlocks;
import com.hbm.blocks.machine.soyuz.SoyuzLauncher;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Watches for a launch complex to be built around it: the thirteen-by-thirteen apron and its two outriggers
 * in launcher plate, four concrete legs under them, and a service tower of scaffolding forty-six blocks
 * tall beside a shorter mast. When it all checks out the scaffolding is taken back down — it was there to
 * build with, not to keep — and the launcher forms in its place.
 *
 * <p>This one does not go through {@code StructPattern} as the Watz segment does: the shape is around five
 * thousand cells, the preview that pattern drives would draw a ghost for every one of them, and upstream
 * has no preview here either. The scan is upstream's, box for box.
 */
public class TileEntitySoyuzStruct extends TileEntityLoadedBase {

    private int age;

    public TileEntitySoyuzStruct(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRUCT_SOYUZ_CORE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntitySoyuzStruct struct) {
        struct.age++;
        if (struct.age < 20) return;
        struct.age = 0;

        if (!struct.isComplete(level, pos)) return;

        struct.clearScaffolding(level, pos);
        struct.raiseLauncher(level, pos);
    }

    public boolean isComplete(Level level, BlockPos pos) {
        Block plate = ModBlocks.struct_launcher.get();

        if (!isAll(level, pos, -6, 3, -6, 6, 4, 6, plate)) return false;
        if (!isAll(level, pos, -1, 3, -8, 1, 4, -7, plate)) return false;
        if (!isAll(level, pos, -2, 3, 7, 2, 4, 9, plate)) return false;
        if (!isAll(level, pos, -2, 51, 5, 2, 51, 9, plate)) return false;
        if (!isAll(level, pos, -1, 38, -8, 1, 38, -6, plate)) return false;

        if (!isConcrete(level, pos, 3, 0, 3, 6, 2, 6)) return false;
        if (!isConcrete(level, pos, -6, 0, 3, -3, 2, 6)) return false;
        if (!isConcrete(level, pos, -6, 0, -6, -3, 2, -3)) return false;
        if (!isConcrete(level, pos, 3, 0, -6, 6, 2, -3)) return false;
        if (!isConcrete(level, pos, -1, 0, -8, 1, 2, -6)) return false;
        if (!isConcrete(level, pos, -2, 0, 5, 2, 2, 9)) return false;

        Block scaffold = ModBlocks.struct_scaffold.get();
        if (!isAll(level, pos, -1, 5, 6, 1, 50, 8, scaffold)) return false;
        return isAll(level, pos, 0, 5, -7, 0, 37, -7, scaffold);
    }

    /** The scaffolding was the means, not the machine; it comes down once the shape is confirmed. */
    private void clearScaffolding(Level level, BlockPos pos) {
        clear(level, pos, -2, 51, 5, 2, 51, 9);
        clear(level, pos, -1, 38, -8, 1, 38, -6);
        clear(level, pos, -2, 0, 5, 2, 2, 9);
        clear(level, pos, -1, 5, 6, 1, 50, 8);
        clear(level, pos, 0, 5, -7, 0, 37, -7);
    }

    private void raiseLauncher(Level level, BlockPos pos) {
        SoyuzLauncher launcher = ModSoyuzBlocks.soyuz_launcher.get();
        BlockPos core = pos.above(SoyuzLauncher.HEIGHT);

        BlockMultiblockCore.withoutTeardown(() -> {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            MultiblockHandlerXR.placeAssembled(level, core, launcher, Direction.EAST);
        });
    }

    private static boolean isAll(Level level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1,
                                 Block block) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (!level.getBlockState(origin.offset(x, y, z)).is(block)) return false;
                }
            }
        }
        return true;
    }

    /** The legs take either concrete, which is what upstream allows. */
    private static boolean isConcrete(Level level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    BlockState state = level.getBlockState(origin.offset(x, y, z));
                    if (!state.is(ModBlocks.concrete.get()) && !state.is(ModBlocks.concrete_smooth.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void clear(Level level, BlockPos origin, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
