package com.hbm.blocks.multiblock;

import com.hbm.capability.CapRole;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ICopiable;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Folded multiblock core. Cells carry no core pointer; ownership is persisted before their placement is
 * considered complete, and removal consults that record so neighbouring structures cannot be consumed.
 *
 * <p>A machine states its whole connection geometry once, in {@link #visitCells}: each cell with the faces
 * it exposes and, where the two differ, the roles those faces answer for. That statement is baked into
 * {@link MultiblockMaskTable}, so asking whether a face is open is an array index rather than a scan of a
 * position list held by the block entity.
 */
public abstract class BlockMultiblockCore extends CompatBlock implements TickingBlock, ICopiable {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** No face. Distinct from {@link MultiblockMaskTable#NOT_A_CELL}. */
    public static final int MASK_NONE = 0;
    public static final int MASK_DOWN = 1, MASK_UP = 1 << 1, MASK_NORTH = 1 << 2;
    public static final int MASK_SOUTH = 1 << 3, MASK_WEST = 1 << 4, MASK_EAST = 1 << 5;
    /** NORTH|SOUTH|WEST|EAST — {@code Direction.ordinal()} 2..5. */
    public static final int MASK_HORIZONTAL = 0b111100;
    public static final int MASK_ALL = 0b111111;

    /**
     * The roles a {@link CellVisitor#cell(BlockPos, int, int)} emission speaks for — the whole vocabulary of
     * that argument, so no call site reaches into {@link MachineCaps} for it.
     */
    public static final int ROLE_POWER_IN = MachineCaps.POWER_IN;
    public static final int ROLE_POWER_OUT = MachineCaps.POWER_OUT;
    public static final int ROLE_FLUID_IN = MachineCaps.FLUID_IN;
    public static final int ROLE_FLUID_OUT = MachineCaps.FLUID_OUT;
    public static final int ROLE_POWER = ROLE_POWER_IN | ROLE_POWER_OUT;
    public static final int ROLE_FLUID = ROLE_FLUID_IN | ROLE_FLUID_OUT;
    public static final int ROLE_ALL = ROLE_POWER | ROLE_FLUID;

    /**
     * The passive plane's receive domains. 1.7's {@code TileEntityProxyCombo} implements no provider
     * interface, so a machine's provider role is reachable only on the active plane.
     */
    public static final int PASSIVE_NONE = 0;
    public static final int PASSIVE_POWER_IN = 1;
    public static final int PASSIVE_FLUID_IN = 1 << 1;
    /** The proxy's inventory surface — an independent flag, not a third receive domain. */
    public static final int PASSIVE_ITEMS = 1 << 2;
    public static final int PASSIVE_ANY = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
    public static final int PASSIVE_DOMAINS = 3;

    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);
    /**
     * Per-cell collision and outline boxes in the core's own frame (1.7 {@code BlockDummyable#bounding});
     * the base answers the whole silhouette at every cell of the footprint.
     */
    protected final List<AABB> bounding = new ArrayList<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, VoxelShape> shapeCache =
            new java.util.concurrent.ConcurrentHashMap<>();
    private @Nullable MultiblockMaskTable table;

    protected BlockMultiblockCore(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static boolean isBusy() { return BUSY.get(); }

    /** Suppresses removal-triggered teardown while a batch replaces folded cells. */
    public static void withoutTeardown(Runnable action) {
        boolean previous = BUSY.get();
        BUSY.set(true);
        try {
            action.run();
        } finally {
            BUSY.set(previous);
        }
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public abstract int[] getDimensions();
    public abstract int getOffset();
    public int getHeightOffset() { return 0; }
    public Direction getDirModified(Direction direction) { return direction; }

    /**
     * The yaw the placer turns through to cycle all four facings once. 1.7's large pylon halves it, so its
     * facing advances every 45 degrees and a full turn cycles the four twice.
     */
    public float placementYawSpan() { return 360.0F; }

    /**
     * Every cell this machine occupies (never the core), with the faces that cell exposes. Must be a
     * function of the core's blockstate alone, or teardown can orphan cells. The position handed to the
     * visitor is mutable and reused; a caller that keeps it takes a copy.
     */
    /** Public spelling of {@link #visitCells} for the falling multiblock's structure query. */
    public final void visitPlacedCells(BlockPos core, Direction facing, CellVisitor visitor) {
        visitCells(core, facing, visitor);
    }

    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, getDimensions(), facing, visitor);
    }

    /**
     * The cell set a teardown enumerates — a superset of {@link #visitCells} is allowed where the footprint
     * is a function of live configuration.
     */
    protected void visitTeardownCells(BlockPos core, Direction facing, CellVisitor visitor) {
        visitCells(core, facing, visitor);
    }

    public int coreMask() { return MASK_NONE; }

    /**
     * The faces a cell on the edge of a square footprint turns outward. What 1.7 left unsaid: {@code
     * makeExtra} marked a ring cell a port and recorded no face, because the face was recovered later by
     * scanning {@code getConPos}.
     */
    protected static int outwardMask(int dx, int dz, int radius) {
        int mask = MASK_NONE;
        if (dx == -radius) mask |= MASK_WEST;
        if (dx == radius) mask |= MASK_EAST;
        if (dz == -radius) mask |= MASK_NORTH;
        if (dz == radius) mask |= MASK_SOUTH;
        return mask;
    }

    /**
     * Every cell where 1.7 stamps a fluid- or power-carrying proxy, and the faces that proxy accepts on;
     * read the stamp's arguments — only {@code .power()} and {@code .fluid()} name a domain.
     */
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {}

    protected final void passiveEverywhere(BlockPos core, Direction facing, PassiveCellVisitor visitor, int domains) {
        visitCells(core, facing, (pos, mask) -> visitor.passiveCell(pos, MASK_ALL, domains));
    }

    /**
     * The core cell's own passive mask, in the local frame — {@link #MASK_ALL} for every folded machine:
     * the core holds the real machine tile, reachable by a bare fetch on any face.
     */
    public int passiveCoreMask() { return MASK_ALL; }

    /**
     * Which domains {@link #passiveCoreMask} applies to — receive only: no 1.7 asker ever pulls, so a
     * provider answer at a passive face would be a port invention.
     */
    public int passiveCoreDomains(int declaredCaps) {
        return ((declaredCaps & MachineCaps.POWER_IN) != 0 ? PASSIVE_POWER_IN : PASSIVE_NONE)
                | ((declaredCaps & MachineCaps.FLUID_IN) != 0 ? PASSIVE_FLUID_IN : PASSIVE_NONE)
                | ((declaredCaps & (MachineCaps.ITEMS | MachineCaps.ITEMS_AT_CELLS)) != 0
                ? PASSIVE_ITEMS : PASSIVE_NONE);
    }

    /**
     * The face mask at a local offset, or {@link MultiblockMaskTable#NOT_A_CELL}. Override for a footprint
     * the packed table cannot size — an RBMK column answers analytically.
     */
    public int maskAt(int lx, int ly, int lz) { return maskTable().maskAtLocal(lx, ly, lz); }

    /**
     * The active mask at a local offset for a core facing this way — a narrow licence for a machine whose
     * 1.7 code names world directions that do not rotate with the machine.
     */
    public int maskAt(int lx, int ly, int lz, Direction facing) { return maskAt(lx, ly, lz); }

    /**
     * The active mask one role sees. Falls through to the role-blind {@link #maskAt} unless the machine
     * actually emitted role-qualified cells.
     */
    public int maskAt(int lx, int ly, int lz, Direction facing, @Nullable CapRole role) {
        MultiblockMaskTable baked = maskTable();
        if (role == null || !baked.hasRolePlanes()) return maskAt(lx, ly, lz, facing);
        return baked.maskAtLocal(lx, ly, lz, role);
    }

    /** {@link #maskAt}'s passive counterpart, over a set of receive domains. Override alongside it. */
    public int passiveMaskAt(int lx, int ly, int lz, int domains) {
        return maskTable().passiveMaskAtLocal(lx, ly, lz, domains);
    }

    /**
     * Does this machine present a heat source at the cell? Heat is a raw {@code instanceof} in 1.7 and
     * cannot ride either mask plane.
     */
    public boolean heatSourceAt(int lx, int ly, int lz) { return false; }

    /**
     * Whether a heat-source owner answers at {@code pos}. 1.7 reaches the core through its own tile and a
     * cell only through a {@code heatSource()} proxy, so a cell this machine does not flag vends nothing
     * rather than the core's store.
     */
    public static boolean vendsHeatAt(BlockEntity owner, BlockPos pos) {
        BlockPos core = owner.getBlockPos();
        if (pos.equals(core)) return true;
        BlockState state = owner.getBlockState();
        if (!(state.getBlock() instanceof BlockMultiblockCore block)) return false;
        Direction facing = state.getValue(FACING);
        int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
        return block.heatSourceAt(MultiblockMaskTable.localX(dx, dy, dz, facing), dy,
                MultiblockMaskTable.localZ(dx, dy, dz, facing));
    }

    /**
     * Baked on first query rather than at a registration hook: the declaration a machine's caps carry is
     * final once the block exists, and nothing can ask before then.
     */
    public final MultiblockMaskTable maskTable() {
        MultiblockMaskTable baked = table;
        if (baked == null) {
            baked = MultiblockMaskTable.bake(this,
                    this instanceof ICapabilityBlock declaring ? declaring.caps().declaredBits() : 0);
            table = baked;
        }
        return baked;
    }

    public boolean isCellState(BlockState state) { return false; }

    public final boolean isCellOf(BlockState state) {
        return state.getBlock() instanceof BlockMultiblockCell || state.getBlock() == this && isCellState(state);
    }

    /** The family spans this machine's core and every cell block, so the test cannot be identity alone. */
    public boolean isSameMultiblock(Block other) {
        return other == this || other instanceof BlockMultiblockCell;
    }

    public final boolean claimsCell(BlockPos core, BlockPos cell, Direction facing) {
        int dx = cell.getX() - core.getX(), dy = cell.getY() - core.getY(), dz = cell.getZ() - core.getZ();
        return maskAt(MultiblockMaskTable.localX(dx, dy, dz, facing), dy,
                MultiblockMaskTable.localZ(dx, dy, dz, facing)) != MultiblockMaskTable.NOT_A_CELL;
    }

    public boolean isOpenAt(Direction facing, int dx, int dy, int dz, Direction side) {
        return isOpenAt(facing, dx, dy, dz, side, PASSIVE_NONE, null);
    }

    public boolean isOpenAt(Direction facing, int dx, int dy, int dz, Direction side, int passiveDomains) {
        return isOpenAt(facing, dx, dy, dz, side, passiveDomains, null);
    }

    /** {@link #isOpenAt} for one role; a null role is the role-blind ask and reads the union. */
    public boolean isOpenAt(Direction facing, int dx, int dy, int dz, Direction side, int passiveDomains,
                            @Nullable CapRole role) {
        int lx = MultiblockMaskTable.localX(dx, dy, dz, facing);
        int lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
        int mask = maskAt(lx, dy, lz, facing, role);
        if (mask == MultiblockMaskTable.NOT_A_CELL) return false;
        if (passiveDomains != PASSIVE_NONE) {
            int passive = passiveMaskAt(lx, dy, lz, passiveDomains);
            if (passive != MultiblockMaskTable.NOT_A_CELL) mask |= passive;
        }
        return (mask & (1 << MultiblockMaskTable.toLocal(side, facing).ordinal())) != 0;
    }

    /**
     * Does the cell carry a passive proxy in any of {@code domains}, on any face at all? The side-blind
     * ask: a hopper resolves the container before it knows which face it is pulling from, and 1.7's
     * {@code meta >= extra} was the same question asked of the cell alone.
     */
    public boolean carriesPassiveAt(Direction facing, int dx, int dy, int dz, int domains) {
        int mask = passiveMaskAt(MultiblockMaskTable.localX(dx, dy, dz, facing), dy,
                MultiblockMaskTable.localZ(dx, dy, dz, facing), domains);
        return mask != MultiblockMaskTable.NOT_A_CELL && mask != MASK_NONE;
    }

    /**
     * {@link #isOpenAt} over the passive plane alone — no active union; the item route's question, and the
     * union would be wrong for it: 1.7 has no item mechanism on the {@code getConPos} plane at all.
     */
    public boolean isPassiveOnlyOpenAt(Direction facing, int dx, int dy, int dz, Direction side, int passiveDomains) {
        int lx = MultiblockMaskTable.localX(dx, dy, dz, facing);
        int lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
        int mask = passiveMaskAt(lx, dy, lz, passiveDomains);
        if (mask == MultiblockMaskTable.NOT_A_CELL) return false;
        return (mask & (1 << MultiblockMaskTable.toLocal(side, facing).ordinal())) != 0;
    }

    /** Is any of {@code roles} open on this face? The union a caller holding a commodity but not a direction wants. */
    public boolean isOpenForAny(Direction facing, int dx, int dy, int dz, Direction side, int roles) {
        for (CapRole role : CapRole.VALUES) {
            if ((roles & role.bit) != 0 && isOpenAt(facing, dx, dy, dz, side, PASSIVE_NONE, role)) return true;
        }
        return false;
    }

    /**
     * The cable answer, inherited rather than written per machine: a subclass that declares
     * {@link com.hbm.api.energymk2.IEnergyConnectorBlock} satisfies it from the baked table and states its
     * ports in one place. Reads the power roles only, so a fluid port does not answer for a cable.
     */
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return openForConnector(world, pos, dir, ROLE_POWER);
    }

    /**
     * Which fluids this machine's ducts may carry. 1.7 gates the type at the same call site as the face --
     * the large turbine takes only a coolant or spent steam, the intake only air -- so a folded machine
     * that inherits its geometry still has to say this, or it accepts every fluid at a port that refused
     * all but one.
     */
    public boolean acceptsFluid(com.hbm.inventory.fluid.FluidType type) {
        return true;
    }

    /** The duct answer; see {@link #canConnect(BlockGetter, BlockPos, ForgeDirection)}. */
    public boolean canConnect(com.hbm.inventory.fluid.FluidType type, BlockGetter world, int x, int y, int z,
                              ForgeDirection dir) {
        return acceptsFluid(type) && openForConnector(world, new BlockPos(x, y, z), dir, ROLE_FLUID);
    }

    private boolean openForConnector(BlockGetter world, BlockPos pos, ForgeDirection dir, int roles) {
        if (!(world instanceof Level level)) return false;
        BlockState state = level.getBlockState(pos);
        BlockPos core = MultiblockSurface.foldedCore(state) != null ? pos : MultiblockSurface.coreOfCell(level, pos);
        if (core == null) return false;
        BlockState coreState = level.getBlockState(core);
        BlockMultiblockCore block = MultiblockSurface.foldedCore(coreState);
        return block != null && block.isOpenForAny(coreState.getValue(FACING), pos.getX() - core.getX(),
                pos.getY() - core.getY(), pos.getZ() - core.getZ(), dir.toDirection(), roles);
    }

    /** This cell's world-frame geometry, or null for a plain full cube. */
    public @Nullable VoxelShape cellShape(int lx, int ly, int lz, Direction facing, boolean open, boolean forCollision) {
        if (bounding.isEmpty()) return null;
        return MultiblockHandlerXR.boundingSilhouette(bounding,
                -MultiblockMaskTable.worldX(lx, ly, lz, facing), -ly,
                -MultiblockMaskTable.worldZ(lx, ly, lz, facing));
    }

    /**
     * {@link #cellShape} unions and optimises a fresh {@link VoxelShape} per call, and shape queries run on
     * the meshing threads; the answer is a function of the local offset and the facing alone, so it is
     * memoised on those.
     */
    protected final VoxelShape cachedCellShape(int lx, int ly, int lz, Direction facing, boolean forCollision) {
        if (bounding.isEmpty()) return Shapes.block();
        long key = (lx & 0xFFFFL) | (ly & 0xFFFFL) << 16 | (lz & 0xFFFFL) << 32
                | (long) facing.ordinal() << 48 | (forCollision ? 1L << 56 : 0L);
        return shapeCache.computeIfAbsent(key, ignored -> {
            VoxelShape shape = cellShape(lx, ly, lz, facing, false, forCollision);
            return shape == null ? Shapes.block() : shape;
        });
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return cachedCellShape(0, 0, 0, state.getValue(FACING), false);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return cachedCellShape(0, 0, 0, state.getValue(FACING), true);
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(placer instanceof Player player)) return;
        withoutTeardown(() -> level.removeBlock(pos, false));
        Direction facing = getDirModified(switch (Mth.floor(player.getYRot() * 4.0F / placementYawSpan() + .5D) & 3) {
            case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; case 3 -> Direction.WEST; default -> Direction.NORTH;
        });
        BlockPos placed = pos.above(getHeightOffset());
        BlockPos core = placed.relative(facing, -getOffset());
        if (!checkRequirement(level, core, facing, placed)) {
            ItemStack refund = dev.golem.core.world.ItemStacks.copyWithCount(stack, 1);
            if (!player.getAbilities().instabuild && !player.getInventory().add(refund)) player.drop(refund, false);
            return;
        }
        if (level.isClientSide()) return;
        withoutTeardown(() -> {
            level.setBlock(core, defaultBlockState().setValue(FACING, facing), 3);
            fillSpace(level, core, facing);
        });
        MultiblockHandlerXR.refreshSpace(level, core, getDimensions(), facing);
        BlockEntity placedCore = level.getBlockEntity(core);
        if (placedCore != null) {
            dev.golem.core.world.ItemStacks.applyComponents(placedCore, stack);
            placedCore.setChanged();
        }
    }

    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return MultiblockHandlerXR.checkSpace(level, core, getDimensions(), clicked, facing);
    }

    /** The state written for one cell. */
    protected abstract BlockState cellStateFor(int localX, int localY, int localZ, Direction facing, int shapeId);

    public final void fillSpace(Level level, BlockPos core, Direction facing) {
        CoreIndexScratch scratch = CoreIndexScratch.borrow();
        try {
            visitCells(core, facing, (pos, mask) -> {
                int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
                int lx = MultiblockMaskTable.localX(dx, dy, dz, facing), lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
                level.setBlock(pos, cellStateFor(lx, dy, lz, facing, 0), 3);
                if (isCellOf(level.getBlockState(pos))) scratch.add(pos);
            });
            if (level instanceof ServerLevel server) {
                for (int i = 0; i < scratch.size(); i++) MultiblockCoreIndex.put(server, BlockPos.of(scratch.get(i)), core);
            }
        } finally { scratch.release(); }
    }

    @Override protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level instanceof ServerLevel server) removedAt(state, server, pos, movedByPiston);
    }

    /** Split out so own-block cells such as RBMK columns can route removal through the recorded owner. */
    protected void removedAt(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        LevelNodeGraph.invalidateEndpointsAround(level, pos);
        if (!movedByPiston && !BUSY.get()) demolish(level, pos, this, state.getValue(FACING), false);
    }

    public static void foldedCellRemoved(ServerLevel level, BlockPos cell, boolean movedByPiston) {
        long packed = MultiblockCoreIndex.lookup(level, cell);
        MultiblockCoreIndex.remove(level, cell);
        LevelNodeGraph.invalidateEndpointsAround(level, cell);
        if (movedByPiston || BUSY.get() || packed == Long.MIN_VALUE) return;
        BlockPos core = BlockPos.of(packed);
        BlockMultiblockCore block = MultiblockSurface.foldedCore(level.getBlockState(core));
        if (block != null) demolish(level, core, block, level.getBlockState(core).getValue(FACING), true);
    }

    private static void demolish(ServerLevel level, BlockPos core, BlockMultiblockCore block, Direction facing, boolean removeCore) {
        withoutTeardown(() -> {
            block.visitTeardownCells(core, facing, (cell, mask) -> {
                if (!block.isCellOf(level.getBlockState(cell)) || MultiblockCoreIndex.lookup(level, cell) != core.asLong()) return;
                BlockPos at = cell.immutable();
                level.removeBlock(at, false);
                MultiblockCoreIndex.remove(level, at);
            });
            if (removeCore) level.removeBlock(core, false);
        });
    }

    /**
     * A cell answers with a core at another position, where capability invalidation is position-keyed, so
     * the core's own removal retires nothing at its cells. Geometry only — no world read, so it is safe on
     * the unload path.
     */
    public void invalidateCellCaps(ServerLevel level, BlockPos core, Direction facing) {
        visitCells(core, facing, (pos, mask) -> LevelNodeGraph.invalidateEndpointsAround(level, pos));
    }

    /** Re-index and re-notify every cell of a freshly loaded core; the index is the authority. */
    public void reindexLoadedCells(ServerLevel level, BlockPos core) {
        Direction facing = level.getBlockState(core).getValue(FACING);
        long corePacked = core.asLong();
        visitCells(core, facing, (pos, mask) -> {
            long recorded = MultiblockCoreIndex.lookup(level, pos);
            if (recorded != Long.MIN_VALUE && recorded != corePacked) return;
            BlockPos at = pos.immutable();
            MultiblockCoreIndex.put(level, at, core);
            LevelNodeGraph.invalidateEndpointsAround(level, at);
        });
        LevelNodeGraph.invalidateEndpointsAround(level, core);
    }

    /** Does this machine read the redstone at its cells as well as at its core? */
    /**
     * 1.7's machine-base interaction range, squared: twice vanilla's, which HBM widened precisely because
     * its machines are multiblocks.
     */
    public static final double BASE_INTERACTION_RANGE_SQ = 128D;

    /**
     * How far from the <b>core</b> a player may be and still keep this machine's menu open, squared. A
     * multiblock's menu is bound to its core, but the player interacts with whatever cell they are standing
     * at, and on a big machine those are nowhere near each other.
     *
     * <p>1.7 widened the flat range by the half-diagonal of {@code getDimensions}, then scanned the world
     * for a cell when that fell short -- because five machines build far more than they declare. The folded
     * table holds every cell, so the widened radius is exact and there is nothing left to scan for.
     */
    public double getInteractionRangeSq() {
        double range = Math.sqrt(BASE_INTERACTION_RANGE_SQ) + maskTable().cornerRadius();
        return range * range;
    }

    /** Whether {@code player} is close enough to interact with the machine cored at {@code core}. */
    public static boolean isWithinInteractionRange(BlockGetter level, BlockPos core, Player player) {
        BlockMultiblockCore block = MultiblockSurface.foldedCore(level.getBlockState(core));
        double dx = core.getX() + 0.5D - player.getX();
        double dy = core.getY() + 0.5D - player.getY();
        double dz = core.getZ() + 0.5D - player.getZ();
        double distance = dx * dx + dy * dy + dz * dz;
        return distance <= (block == null ? BASE_INTERACTION_RANGE_SQ : block.getInteractionRangeSq());
    }

    public boolean wantsNeighborUpdates() { return false; }

    /**
     * A neighbour of {@code cell} changed; {@code cell} may be the core itself. The cell position goes
     * through, not the core's: 1.7 keys its redstone bookkeeping on the edge position.
     */
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {}

    @Override protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                               boolean movedByPiston) {
        if (wantsNeighborUpdates() && level instanceof ServerLevel server) cellNeighborChanged(server, pos, pos);
    }

    /**
     * What a right-click on any part of the machine does: open the machine's menu.
     *
     * <p>The menu belongs to the core's block entity, and the player has clicked whichever cell they
     * could reach, so the position is not incidental — the core is what the screen is bound to, and a
     * cell carries no block entity to bind to at all. Every caller reaches this with the core already
     * resolved.
     *
     * <p>A machine with no menu is answered {@code TRY_WITH_EMPTY_HAND} rather than swallowed, so that
     * a right-click on it still falls through to whatever else the click could have meant.
     *
     * <p>Override for a machine that does something other than open its menu, or that treats a
     * sneak-click differently; a machine with a menu and no special behaviour states nothing.
     */
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(core) instanceof net.minecraft.world.MenuProvider menu)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        dev.golem.core.gui.Menus.openWithPos(serverPlayer, menu, core);
        return InteractionResult.SUCCESS;
    }

    public void onExplosionHitAtCore(BlockState state, ServerLevel level, BlockPos core, Explosion explosion,
                                     BiConsumer<ItemStack, BlockPos> onHit) {
        forwardExplosionHit(state, level, core, explosion, onHit);
    }

    @Override protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useAtCore(state, level, pos, player, hit);
    }

    @Override public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ICopiable copiable ? copiable.getSettings(level, pos) : null;
    }

    @Override public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ICopiable copiable) copiable.pasteSettings(nbt, index, level, player, pos);
    }

    @Override public String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ICopiable copiable ? copiable.infoForDisplay(level, pos) : null;
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** How {@link #visitPassiveCells} reports the proxy accept surface. */
    @FunctionalInterface public interface PassiveCellVisitor {
        void passiveCell(BlockPos pos, int faces, int domains);
    }

    /** How {@link #visitCells} reports geometry. */
    public interface CellVisitor extends MultiblockHandlerXR.BoxVisitor {

        void cell(BlockPos pos, int mask);

        /**
         * A cell open on {@code mask} for {@code roles} alone. Roles left unnamed keep whatever they had, so
         * two emissions on one cell compose; only the mask table distinguishes them, every other visitor
         * wants the union and takes the default.
         */
        default void cell(BlockPos pos, int mask, int roles) { cell(pos, mask); }

        @Override default void cell(BlockPos pos, Direction facingCore) { cell(pos, MASK_NONE); }
    }

    public record FoldedOwner(BlockPos pos, BlockState state, BlockMultiblockCore block, Direction facing) {}
}
