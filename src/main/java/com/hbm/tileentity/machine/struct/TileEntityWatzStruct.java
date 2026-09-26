package com.hbm.tileentity.machine.struct;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.struct.StructPattern;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import dev.golem.core.multiblock.MultiblockCore;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Watches for a Watz power plant segment to be built around it. The shape is upstream's
 * {@code TileEntityWatzStruct} scan, coordinate for coordinate: a three-tall cross of reaction
 * chambers and supercoolers wrapped in riveted stability casing.
 *
 * <p>Note the casing must be the <b>riveted</b> variant — upstream checked meta 1, which in this port
 * is {@code watz_casing_tooled}, the output of hitting plain casing with the boltgun.
 */
public class TileEntityWatzStruct extends TileEntityStructBase {

    private static final StructPattern PATTERN = buildPattern();

    public TileEntityWatzStruct(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRUCT_WATZ_CORE.get(), pos, state);
    }

    @Override public StructPattern pattern() { return PATTERN; }

    @Override protected MultiblockCore target() { return ModMachineBlocks.watz.get(); }

    private static StructPattern buildPattern() {
        StructPattern.Builder b = StructPattern.builder();

        // The two supercooler blocks stacked directly on the core.
        b.addPillar(0, 1, 0, ModBlocks.watz_cooler, "watz_cooler_top", "watz_cooler_side");
        b.addPillar(0, 2, 0, ModBlocks.watz_cooler, "watz_cooler_top", "watz_cooler_side");

        int[][] elements = {{1, 0}, {2, 0}, {0, 1}, {0, 2}, {-1, 0}, {-2, 0}, {0, -1}, {0, -2},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        int[][] coolers = {{2, 1}, {2, -1}, {1, 2}, {-1, 2}, {-2, 1}, {-2, -1}, {1, -2}, {-1, -2}};

        for (int y = 0; y < 3; y++) {
            for (int[] c : elements) {
                b.addPillar(c[0], y, c[1], ModBlocks.watz_element, "watz_element_top", "watz_element_side");
            }
            for (int[] c : coolers) {
                b.addPillar(c[0], y, c[1], ModBlocks.watz_cooler, "watz_cooler_top", "watz_cooler_side");
            }
            for (int j = -1; j < 2; j++) {
                casing(b, 3, y, j);
                casing(b, j, y, 3);
                casing(b, -3, y, j);
                casing(b, j, y, -3);
            }
            casing(b, 2, y, 2);
            casing(b, 2, y, -2);
            casing(b, -2, y, 2);
            casing(b, -2, y, -2);
        }
        return b.build();
    }

    private static void casing(StructPattern.Builder b, int x, int y, int z) {
        b.add(x, y, z, ModMachineBlocks.watz_casing_tooled, "watz_casing_tooled");
    }
}
