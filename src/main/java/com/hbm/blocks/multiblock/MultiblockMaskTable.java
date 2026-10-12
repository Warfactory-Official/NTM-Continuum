package com.hbm.blocks.multiblock;

import com.hbm.capability.CapRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * A machine class's connection geometry, baked once into flat {@code byte[]} planes over its footprint's
 * local bounding box. The active plane is the {@code getConPos} subscription a net delivers on; the passive
 * plane is the accept surface a direct asker finds. A cell absent from the table is not part of the
 * footprint.
 */
public final class MultiblockMaskTable {

    /** The queried offset is outside this machine's footprint entirely. Not a legal mask value. */
    public static final int NOT_A_CELL = -1;

    /** The owner's own {@code MachineCaps.declaredBits()}, re-applied by the shared cell block's providers. */
    public final int declaredCaps;
    /** The active plane role-blind: the union over {@link #byRole}. Every asker with no role in hand reads it. */
    private final byte[] masks;
    /** Passive planes, indexed by {@code domain - 1}; see {@link BlockMultiblockCore#PASSIVE_POWER_IN}. */
    private final byte[][] passive;
    /**
     * Active planes per {@link CapRole}, or null where every role carries the same faces — which is every
     * machine that never emits a role-qualified cell, so they allocate nothing and read {@link #masks}.
     */
    private final byte[] @Nullable [] byRole;
    private final int minX, minY, minZ, sizeX, sizeY, sizeZ;

    private MultiblockMaskTable(byte[] masks, byte[][] passive, byte[] @Nullable [] byRole,
                                int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ, int declaredCaps) {
        this.masks = masks;
        this.passive = passive;
        this.byRole = byRole;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.declaredCaps = declaredCaps;
    }

    /**
     * A geometry statement to bake. Stated apart from the block because a {@link BlockMultiblockCore} can
     * only exist while block registration is open, and the bake-time refusals below have to be reachable
     * after it closes; {@code maskAtOwner} is the class a refusal reflects on.
     */
    public record Geometry(Class<?> maskAtOwner, CellSource cells, PassiveCellSource passiveCells,
                           int coreMask, int passiveCoreMask, int passiveCoreDomains) {}

    @FunctionalInterface public interface CellSource {
        void visit(BlockPos core, Direction facing, BlockMultiblockCore.CellVisitor visitor);
    }

    @FunctionalInterface public interface PassiveCellSource {
        void visit(BlockPos core, Direction facing, BlockMultiblockCore.PassiveCellVisitor visitor);
    }

    public static MultiblockMaskTable bake(BlockMultiblockCore block, int declaredCaps) {
        return bake(new Geometry(block.getClass(), block::visitCells, block::visitPassiveCells, block.coreMask(),
                block.passiveCoreMask(), block.passiveCoreDomains(declaredCaps)), declaredCaps);
    }

    /** Run the geometry statement twice — once to size the box, once to fill it. */
    public static MultiblockMaskTable bake(Geometry geometry, int declaredCaps) {
        Bounds bounds = new Bounds();
        geometry.cells().visit(BlockPos.ZERO, Direction.SOUTH, bounds);
        bounds.include(0, 0, 0);

        int sizeX = bounds.maxX - bounds.minX + 1;
        int sizeY = bounds.maxY - bounds.minY + 1;
        int sizeZ = bounds.maxZ - bounds.minZ + 1;
        byte[] masks = new byte[sizeX * sizeY * sizeZ];
        Arrays.fill(masks, (byte) NOT_A_CELL);

        byte[][] passive = new byte[BlockMultiblockCore.PASSIVE_DOMAINS][];
        for (int d = 0; d < passive.length; d++) {
            passive[d] = new byte[masks.length];
            Arrays.fill(passive[d], (byte) NOT_A_CELL);
        }
        byte[][] byRole = new byte[CapRole.COUNT][];
        for (int r = 0; r < byRole.length; r++) {
            byRole[r] = new byte[masks.length];
            Arrays.fill(byRole[r], (byte) NOT_A_CELL);
        }

        MultiblockMaskTable table = new MultiblockMaskTable(masks, passive, byRole, bounds.minX, bounds.minY,
                bounds.minZ, sizeX, sizeY, sizeZ, declaredCaps);
        geometry.cells().visit(BlockPos.ZERO, Direction.SOUTH, new BlockMultiblockCore.CellVisitor() {
            @Override public void cell(BlockPos pos, int mask) {
                table.put(pos.getX(), pos.getY(), pos.getZ(), mask, BlockMultiblockCore.ROLE_ALL);
            }

            @Override public void cell(BlockPos pos, int mask, int roles) {
                table.put(pos.getX(), pos.getY(), pos.getZ(), mask, roles);
            }
        });
        table.put(0, 0, 0, geometry.coreMask(), BlockMultiblockCore.ROLE_ALL);
        for (int i = 0; i < masks.length; i++) {
            if (masks[i] == (byte) NOT_A_CELL) continue;
            for (byte[] plane : passive) plane[i] = 0;
        }
        geometry.passiveCells().visit(BlockPos.ZERO, Direction.SOUTH, (pos, faces, domains) ->
                table.putPassive(pos.getX(), pos.getY(), pos.getZ(), faces, domains));
        table.putPassive(0, 0, 0, geometry.passiveCoreMask(), geometry.passiveCoreDomains());

        for (byte[] plane : byRole) {
            if (!Arrays.equals(plane, masks)) {
                assertAnswersFromTheTable(geometry.maskAtOwner());
                return table;
            }
        }
        return new MultiblockMaskTable(masks, passive, null, bounds.minX, bounds.minY, bounds.minZ,
                sizeX, sizeY, sizeZ, declaredCaps);
    }

    /**
     * Role planes are read straight off the table, so a machine that also answers {@code maskAt}
     * analytically would keep its override for the role-blind ask and lose it for every role-qualified
     * one — two geometries, green build. Whichever a machine needs, it may not need both.
     */
    private static void assertAnswersFromTheTable(Class<?> maskAtOwner) {
        for (Class<?>[] signature : new Class<?>[][]{
                {int.class, int.class, int.class},
                {int.class, int.class, int.class, Direction.class}}) {
            Class<?> owner;
            try {
                owner = maskAtOwner.getMethod("maskAt", signature).getDeclaringClass();
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
            if (owner != BlockMultiblockCore.class) {
                throw new IllegalStateException(maskAtOwner.getName() + " emits role-qualified cells and takes"
                        + " maskAt/" + signature.length + " from " + owner.getName()
                        + "; the role planes bypass the override");
            }
        }
    }

    /**
     * The distance from the core to the furthest corner of the footprint. Read off the baked bounds rather
     * than {@code getDimensions}, which several machines understate badly -- the fracking tower declares a
     * radius of 3 and erects a derrick 24 tall -- because the table holds every cell the machine actually
     * places.
     */
    public double cornerRadius() {
        double x = Math.max(Math.abs(minX), Math.abs(minX + sizeX - 1));
        double y = Math.max(Math.abs(minY), Math.abs(minY + sizeY - 1));
        double z = Math.max(Math.abs(minZ), Math.abs(minZ + sizeZ - 1));
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static int localX(int dx, int dy, int dz, Direction facing) {
        return switch (facing) { case WEST -> dz; case NORTH -> -dx; case EAST -> -dz; default -> dx; };
    }

    public static int localY(int dx, int dy, int dz, Direction facing) {
        return dy;
    }

    public static int localZ(int dx, int dy, int dz, Direction facing) {
        return switch (facing) { case WEST -> -dx; case NORTH -> -dz; case EAST -> dx; default -> dz; };
    }

    public static int worldX(int lx, int ly, int lz, Direction facing) {
        return switch (facing) { case WEST -> -lz; case NORTH -> -lx; case EAST -> lz; default -> lx; };
    }

    public static int worldZ(int lx, int ly, int lz, Direction facing) {
        return switch (facing) { case WEST -> lx; case NORTH -> -lz; case EAST -> -lx; default -> lz; };
    }

    /** A world direction expressed in the local frame. Vertical faces are fixed points of every rotation. */
    public static Direction toLocal(Direction side, Direction facing) {
        if (side.getAxis() == Direction.Axis.Y) return side;
        return switch (facing) {
            case WEST -> side.getCounterClockWise();
            case NORTH -> side.getOpposite();
            case EAST -> side.getClockWise();
            default -> side;
        };
    }

    /** Does the owner declare every capability in {@code bits}? */
    public boolean declares(int bits) {
        return (declaredCaps & bits) == bits;
    }

    /**
     * Add {@code mask} to the named roles' planes; roles not named keep what they had, so two emissions
     * on one cell compose, for the same role too: 1.7 subscribed every {@code getConPos} point on its own, so a
     * cell next to two points connects on both faces, and a later {@code MASK_NONE} box fill over a port cell
     * closes nothing. {@link #masks} follows as the union — no other writer touches it.
     */
    private void put(int lx, int ly, int lz, int mask, int roles) {
        if (mask < 0 || mask > 0b111111) {
            throw new IllegalStateException("multiblock mask table: mask " + mask + " at local " + lx + ","
                    + ly + "," + lz + " is not a 6-bit face set");
        }
        if (roles == 0 || (roles & ~BlockMultiblockCore.ROLE_ALL) != 0) {
            throw new IllegalStateException("multiblock mask table: roles 0b" + Integer.toBinaryString(roles)
                    + " at local " + lx + "," + ly + "," + lz + " is not a non-empty subset of ROLE_ALL");
        }
        int index = index(lx, ly, lz);
        int union = 0;
        for (CapRole role : CapRole.VALUES) {
            byte[] plane = byRole[role.planeIndex];
            if ((roles & role.bit) != 0) {
                plane[index] = plane[index] == (byte) NOT_A_CELL ? (byte) mask : (byte) (plane[index] | mask);
            }
            else if (plane[index] == (byte) NOT_A_CELL) plane[index] = 0;
            union |= plane[index];
        }
        masks[index] = (byte) union;
    }

    /**
     * OR {@code faces} into every named domain's passive plane; a passive emission outside the active
     * geometry is a transcription error.
     */
    private void putPassive(int lx, int ly, int lz, int faces, int domains) {
        if (domains == 0 || faces == 0) return;
        if (faces < 0 || faces > 0b111111) {
            throw new IllegalStateException("multiblock mask table: passive faces " + faces + " at local "
                    + lx + "," + ly + "," + lz + " is not a 6-bit face set");
        }
        if (maskAtLocal(lx, ly, lz) == NOT_A_CELL) {
            throw new IllegalStateException("multiblock mask table: passive emission at local " + lx + ","
                    + ly + "," + lz + " is not a cell of the footprint");
        }
        int index = index(lx, ly, lz);
        for (int d = 0; d < passive.length; d++) {
            if ((domains & (1 << d)) != 0) passive[d][index] |= (byte) faces;
        }
    }

    private int index(int lx, int ly, int lz) {
        return ((ly - minY) * sizeZ + (lz - minZ)) * sizeX + (lx - minX);
    }

    private boolean outside(int lx, int ly, int lz) {
        return lx < minX || ly < minY || lz < minZ
                || lx >= minX + sizeX || ly >= minY + sizeY || lz >= minZ + sizeZ;
    }

    public int maskAtLocal(int lx, int ly, int lz) {
        return outside(lx, ly, lz) ? NOT_A_CELL : masks[index(lx, ly, lz)];
    }

    /** The active mask one role sees. Null planes mean every role sees {@link #maskAtLocal}. */
    public int maskAtLocal(int lx, int ly, int lz, CapRole role) {
        if (byRole == null) return maskAtLocal(lx, ly, lz);
        return outside(lx, ly, lz) ? NOT_A_CELL : byRole[role.planeIndex][index(lx, ly, lz)];
    }

    /** Does this machine state its active geometry per role? Machines that do are table-driven by construction. */
    public boolean hasRolePlanes() {
        return byRole != null;
    }

    /** The union of the passive face masks over {@code domains}, or {@link #NOT_A_CELL}. */
    public int passiveMaskAtLocal(int lx, int ly, int lz, int domains) {
        int named = domains & ((1 << passive.length) - 1);
        if (named == 0) return 0;
        if (outside(lx, ly, lz)) return NOT_A_CELL;
        int index = index(lx, ly, lz);
        int mask = 0;
        for (int d = 0; d < passive.length; d++) {
            if ((named & (1 << d)) != 0) mask |= passive[d][index];
        }
        return mask;
    }

    public void forEachLocalCell(LocalCellConsumer consumer) {
        for (int ly = minY; ly < minY + sizeY; ly++) for (int lz = minZ; lz < minZ + sizeZ; lz++)
            for (int lx = minX; lx < minX + sizeX; lx++) {
                if (masks[index(lx, ly, lz)] != (byte) NOT_A_CELL) consumer.accept(lx, ly, lz);
            }
    }

    @FunctionalInterface public interface LocalCellConsumer { void accept(int lx, int ly, int lz); }

    private static final class Bounds implements BlockMultiblockCore.CellVisitor {
        private int minX, minY, minZ, maxX, maxY, maxZ;

        @Override public void cell(BlockPos pos, int mask) { include(pos.getX(), pos.getY(), pos.getZ()); }

        void include(int x, int y, int z) {
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        }
    }
}
