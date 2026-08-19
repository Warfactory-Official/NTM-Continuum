package com.hbm.blocks.machine.struct;

import com.hbm.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * The declarative shape a {@link BlockStruct} core waits for before it transmutes into a finished
 * multiblock. This replaces the hand-rolled {@code cbr(block, meta, dx, dy, dz)} ladders in 1.12.2's
 * {@code TileEntityWatzStruct} / {@code TileEntityFusionTorusStruct}: the same coordinates, but stated
 * once so both the scan and the ghost preview read from them.
 *
 * <p>Two construction routes, matching the two shapes upstream actually uses:
 * <ul>
 *   <li>{@link #builder()} — explicit per-cell offsets (the Watz's four-armed cross).</li>
 *   <li>{@link Builder#mask} — a vertically mirrored stack of layer masks, where each layer is a
 *       square {@code int[][]} of component indices and {@code 0} means "leave empty" (the fusion
 *       torus's 15×15 layout, three authored layers mirrored into five).</li>
 * </ul>
 *
 * <p>Offsets are relative to the struct core block itself, and the core's own cell is never part of
 * the pattern — it is occupied by the struct block until the moment it becomes the multiblock core.
 */
public final class StructPattern {

    /**
     * One required block: where it sits relative to the struct core, which block must be there, and
     * the two textures the ghost preview draws it with. {@code top} and {@code side} are equal for a
     * plain cube and differ for a pillar-textured block (cooler, element), mirroring the
     * {@code renderSimpleBlockAt} / {@code renderPilarBlockAt} split in the old preview TESRs.
     */
    public record Cell(BlockPos offset, Supplier<Block> block, Identifier top, Identifier side) {}

    private final List<Cell> cells;
    private final AABB localBounds;

    private StructPattern(List<Cell> cells) {
        this.cells = Collections.unmodifiableList(cells);
        int minX = 0, minY = 0, minZ = 0, maxX = 1, maxY = 1, maxZ = 1;
        for (Cell c : cells) {
            BlockPos o = c.offset();
            minX = Math.min(minX, o.getX());
            minY = Math.min(minY, o.getY());
            minZ = Math.min(minZ, o.getZ());
            maxX = Math.max(maxX, o.getX() + 1);
            maxY = Math.max(maxY, o.getY() + 1);
            maxZ = Math.max(maxZ, o.getZ() + 1);
        }
        this.localBounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public List<Cell> cells() {
        return cells;
    }

    /** True when every cell of the pattern holds the block it asks for. */
    public boolean matches(BlockGetter level, BlockPos origin) {
        for (Cell cell : cells) {
            if (!isSatisfied(level, origin, cell)) return false;
        }
        return true;
    }

    public boolean isSatisfied(BlockGetter level, BlockPos origin, Cell cell) {
        return level.getBlockState(origin.offset(cell.offset())).getBlock() == cell.block().get();
    }

    /** World-space AABB covering the whole shape, for the preview renderer's culling box. */
    public AABB renderBounds(BlockPos origin) {
        return localBounds.move(origin.getX(), origin.getY(), origin.getZ());
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Standalone block texture, the form {@code CompatRenderTypes} wants (not an atlas sprite). */
    public static Identifier blockTexture(String name) {
        return dev.golem.core.Ids.of(Constants.MOD_ID, "textures/blocks/" + name + ".png");
    }

    public static final class Builder {

        private final List<Cell> cells = new ArrayList<>();

        /** A plain cube: one texture on every face. */
        public Builder add(int x, int y, int z, Supplier<Block> block, String texture) {
            Identifier tex = blockTexture(texture);
            cells.add(new Cell(new BlockPos(x, y, z), block, tex, tex));
            return this;
        }

        /** A pillar-textured cube (distinct top/bottom and side textures). */
        public Builder addPillar(int x, int y, int z, Supplier<Block> block, String top, String side) {
            cells.add(new Cell(new BlockPos(x, y, z), block, blockTexture(top), blockTexture(side)));
            return this;
        }

        /**
         * Adds a vertically mirrored stack of square layer masks. {@code layers[l][x][z]} indexes into
         * {@code palette} ({@code 0} = empty); layer {@code l} for height {@code y} is
         * {@code y > (height - 1) / 2 ? (height - 1) - y : y}, exactly the
         * {@code int l = iy > 2 ? 4 - iy : iy} fold in {@code MachineFusionTorus}. The mask is centred
         * on the core, so mask index {@code i} maps to offset {@code i - size / 2}.
         *
         * <p>The core's own cell ({@code 0, 0, 0}) is skipped — the struct block lives there.
         */
        public Builder mask(int[][][] layers, int height, PaletteEntry[] palette) {
            int size = layers[0].length;
            int half = size / 2;
            for (int y = 0; y < height; y++) {
                int l = y > (height - 1) / 2 ? (height - 1) - y : y;
                int[][] layer = layers[l];
                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < layer[x].length; z++) {
                        int index = layer[x][z];
                        if (index == 0) continue;
                        int ox = x - half;
                        int oz = z - half;
                        if (ox == 0 && y == 0 && oz == 0) continue; // the struct core's own cell
                        PaletteEntry entry = palette[index];
                        cells.add(new Cell(new BlockPos(ox, y, oz), entry.block(),
                                blockTexture(entry.top()), blockTexture(entry.side())));
                    }
                }
            }
            return this;
        }

        public StructPattern build() {
            return new StructPattern(new ArrayList<>(cells));
        }
    }

    /** A mask index's meaning: which block it is and how the preview draws it. */
    public record PaletteEntry(Supplier<Block> block, String top, String side) {
        public PaletteEntry(Supplier<Block> block, String texture) {
            this(block, texture, texture);
        }
    }
}
