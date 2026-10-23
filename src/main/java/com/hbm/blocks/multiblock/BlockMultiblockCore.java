package com.hbm.blocks.multiblock;

import com.hbm.capability.CapRole;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ICopiable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.SubLevels;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
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
import net.minecraft.world.phys.shapes.EntityCollisionContext;
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
    /** NORTH|SOUTH|WEST|EAST, the {@code Direction.ordinal()} values 2..5. */
    public static final int MASK_HORIZONTAL = 0b111100;
    public static final int MASK_ALL = 0b111111;

    /**
     * The roles a {@link CellVisitor#cell(BlockPos, int, int)} emission speaks for. The whole vocabulary of
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
    /** The proxy's inventory surface, an independent flag rather than a third receive domain. */
    public static final int PASSIVE_ITEMS = 1 << 2;
    public static final int PASSIVE_ANY = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
    public static final int PASSIVE_DOMAINS = 3;

    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> SAFE_REM = ThreadLocal.withInitial(() -> false);

    /**
     * 1.7's {@code BlockDummyable} was no opaque cube, so its constructor gave core and dummies light opacity 0: sky
     * and block light pass through a machine undiminished and the sky stays visible under it ({@code canBlockSeeTheSky},
     * the solar panels' and mirrors' check). Modern derives a non-occluding full cell's dampening as 1 and stops the
     * sky column there; this restores 1.7 (ADDON-API-INVENTORY X-24, GameTest {@code multiblock_light_transparent}).
     */
    @Override
    protected boolean isLightTransparent(BlockState state) {
        return true;
    }
    /**
     * Per-cell collision and outline boxes in the core's own frame (1.7 {@code BlockDummyable#bounding});
     * the base answers the whole silhouette at every cell of the footprint.
     */
    protected final List<AABB> bounding = new ArrayList<>();
    private final ConcurrentHashMap<Long, VoxelShape> shapeCache =
            new ConcurrentHashMap<>();
    private @Nullable MultiblockMaskTable table;

    protected BlockMultiblockCore(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static boolean isBusy() { return BUSY.get(); }

    /** 1.7 {@code safeRem}: true while {@link #setPlacedBy} clears the block its item placed, which is no machine yet. */
    public static boolean isSafeRem() { return SAFE_REM.get(); }

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
     * 1.7 {@code BlockDummyable#getAllDimensions}: the boxes the placement highlight outlines, in
     * {@link #getDimensions} order (up, down, north, south, west, east), optionally followed by a forward, up and
     * lateral offset from the core. Only the highlight reads it; a machine whose real footprint is several boxes
     * overrides it, or the highlight shows the wrong shape (as in 1.7).
     */
    public int[][] getAllDimensions() {
        return new int[][] { getDimensions() };
    }

    /**
     * 1.7 {@code BlockDummyable#getAABBExtras}: extra boxes the placement highlight outlines, each
     * {up, down, forward, backward, left, right} from the core cell's bottom centre, in blue unless a seventh
     * value gives the colour (1 is a heat port's orange, else an RGB hex).
     */
    public double[][] getAABBExtras() {
        return new double[0][0];
    }

    /**
     * 1.7 {@code BlockDummyable#getAllPorts}: the port faces the placement highlight marks yellow, each
     * {x, y, z, face ordinal} from the placed block, already turned by {@link #rotatePorts}.
     */
    public int[][] getAllPorts(ForgeDirection dir) {
        return new int[0][0];
    }

    /**
     * 1.7 {@code rotatePorts}: turns ports written for a north-facing machine to {@code facing} and moves them from
     * the core to the placed block. East and west entries are written mirrored, as in 1.7.
     */
    public int[][] rotatePorts(int[][] ports, ForgeDirection facing) {
        ForgeDirection rot = facing.getRotation(ForgeDirection.UP);
        int[][] result = new int[ports.length][4];
        for (int i = 0; i < ports.length; i++) {
            int nx = ports[i][0];
            int ny = ports[i][1];
            int nz = ports[i][2];
            ForgeDirection face = ForgeDirection.getOrientation(ports[i][3]);

            result[i][0] = nx * rot.offsetX + nz * facing.offsetX;
            result[i][1] = ny;
            result[i][2] = nx * rot.offsetZ + nz * facing.offsetZ;

            int fx = face.offsetX * rot.offsetX + face.offsetZ * facing.offsetX;
            int fz = face.offsetX * rot.offsetZ + face.offsetZ * facing.offsetZ;
            int fy = face.offsetY;

            ForgeDirection rotatedFace = ForgeDirection.UNKNOWN;
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                if (dir.offsetX == fx && dir.offsetY == fy && dir.offsetZ == fz) {
                    rotatedFace = dir;
                    break;
                }
            }
            result[i][3] = rotatedFace.ordinal();
        }
        for (int[] port : result) {
            port[0] += facing.offsetX * -getOffset();
            port[2] += facing.offsetZ * -getOffset();
        }
        return result;
    }

    /**
     * The placement highlight's green/red answer: {@link #checkRequirement} for a block placed at {@code placed}
     * facing {@code facing}, the core found as {@link #setPlacedBy} finds it.
     */
    public final boolean canPlaceAt(Level level, BlockPos placed, Direction facing) {
        return checkRequirement(level, placed.relative(facing, -getOffset()), facing, placed);
    }

    /**
     * The yaw the placer turns through to cycle all four facings once. 1.7's large pylon halves it, so its
     * facing advances every 45 degrees and a full turn cycles the four twice.
     */
    public float placementYawSpan() { return 360.0F; }

    /**
     * Every cell this machine currently occupies (never the core). Runtime footprints override this;
     * fixed machines use their declaration in {@link #visitCells}. The position handed to the visitor
     * is mutable and reused; a caller that keeps it takes a copy.
     */
    public void visitPlacedCells(BlockGetter level, BlockPos core, Direction facing, CellVisitor visitor) {
        visitCells(core, facing, visitor);
    }

    /** State-only geometry used to bake masks and check placement clearance before a core exists. */
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, getDimensions(), facing, visitor);
    }

    /**
     * The cell set a teardown enumerates. A superset of {@link #visitPlacedCells} is allowed where the footprint
     * is a function of live configuration.
     */
    protected void visitTeardownCells(ServerLevel level, BlockPos core, Direction facing, CellVisitor visitor) {
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
     * read the stamp's arguments. Only {@code .power()} and {@code .fluid()} name a domain.
     */
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {}

    protected final void passiveEverywhere(BlockPos core, Direction facing, PassiveCellVisitor visitor, int domains) {
        visitCells(core, facing, (pos, mask) -> visitor.passiveCell(pos, MASK_ALL, domains));
    }

    /**
     * The core cell's own passive mask, in the local frame: {@link #MASK_ALL} for every folded machine,
     * because the core holds the real machine tile, reachable by a bare fetch on any face.
     */
    public int passiveCoreMask() { return MASK_ALL; }

    /**
     * Which domains {@link #passiveCoreMask} applies to: receive only, since no 1.7 asker ever pulls, so a
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
     * the packed table cannot size. An RBMK column answers analytically.
     */
    public int maskAt(int lx, int ly, int lz) { return maskTable().maskAtLocal(lx, ly, lz); }

    /**
     * The active mask at a local offset for a core facing this way. A narrow licence for a machine whose
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

    /** Runtime footprints may resolve their own cells instead of using the finite reverse offsets. */
    public @Nullable BlockPos findClientCore(BlockGetter level, BlockPos cell) {
        return MultiblockSurface.findClientCore(level, cell);
    }

    public boolean claimsCell(BlockGetter level, BlockPos core, BlockPos cell, Direction facing) {
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
     * {@link #isOpenAt} over the passive plane alone, with no active union; the item route's question, and the
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
     * the large turbine takes only a coolant or spent steam, the intake only air, so a folded machine
     * that inherits its geometry still has to say this, or it accepts every fluid at a port that refused
     * all but one.
     */
    public boolean acceptsFluid(FluidType type) {
        return true;
    }

    /** The duct answer; see {@link #canConnect(BlockGetter, BlockPos, ForgeDirection)}. */
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z,
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
        if (ignoresCollision(context)) return Shapes.empty();
        return cachedCellShape(0, 0, 0, state.getValue(FACING), true);
    }

    // --- hbm-space placement rules (ADDON-API-INVENTORY V-175), off in plain HBM ---

    /**
     * hbm-space {@code BlockDummyable.onBlockPlacedBy}: a multiblock placed against a wall faces away from that wall and
     * stands against it, one placed under a ceiling hangs from it (its top at the placed block). Off in plain HBM, which
     * places as 1.7 did: on the floor in front of the player, facing from the yaw. NTM: Space turns it on.
     */
    public static boolean wallPlacement = false;
    /**
     * hbm-space {@code BlockDummyable.internalPlayers}: a player standing inside a multiblock they just placed does not
     * collide with it until no such player is inside any multiblock of that type. Off in plain HBM.
     */
    public static boolean walkOutAfterPlacing = false;

    /** The face the last placement on this thread clicked, with the position it placed at. */
    private record PlacedFace(BlockPos pos, Direction face) {}
    private static final ThreadLocal<@Nullable PlacedFace> PLACED_FACE = new ThreadLocal<>();

    /** hbm-space's per-block-type {@code internalPlayers} (1.7 kept it on the Block, shared by every machine of the type). */
    private final Set<Player> internalPlayers = ConcurrentHashMap.newKeySet();

    /** Where a placement puts the core, which way it faces, and the cell 1.7 left out of the space check. */
    public record Placement(BlockPos core, Direction facing, BlockPos clicked) {}

    /** 1.7 {@code onBlockPlaced} returned the clicked side as metadata; this remembers it for {@link #setPlacedBy}. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        PLACED_FACE.set(new PlacedFace(context.getClickedPos().immutable(), context.getClickedFace()));
        BlockState state = super.getStateForPlacement(context);
        // Vanilla's BlockItem checks the returned state's collision shape against entities before setPlacedBy builds
        // the machine. With the default facing, a machine whose silhouette turns with it (hbm-space's greenhouse) was
        // tested lying across the wrong axis and refused a player standing beside it; 1.7 checked only the clicked
        // cell. Hand the check the facing the machine will really have.
        if (state != null && state.hasProperty(FACING) && context.getPlayer() != null) {
            Direction facing = placement(context.getClickedPos(), context.getClickedFace(), context.getPlayer().getYRot()).facing();
            if (FACING.getPossibleValues().contains(facing)) state = state.setValue(FACING, facing);
        }
        return state;
    }

    /**
     * The core and facing of a multiblock whose item block lands at {@code pos} after a click on face {@code face}, for
     * a placer turned to {@code yaw}. Plain HBM ({@link #wallPlacement} off) ignores the face, as 1.7 did.
     */
    public final Placement placement(BlockPos pos, Direction face, float yaw) {
        Direction yawFacing = switch (Mth.floor(yaw * 4.0F / placementYawSpan() + .5D) & 3) {
            case 1 -> Direction.EAST; case 2 -> Direction.SOUTH; case 3 -> Direction.WEST; default -> Direction.NORTH;
        };
        if (!wallPlacement) {
            Direction facing = getDirModified(yawFacing);
            BlockPos placed = pos.above(getHeightOffset());
            return new Placement(placed.relative(facing, -getOffset()), facing, placed);
        }
        // hbm-space: the direction the player faces offsets the block away from them; on a wall it is the wall's normal
        Direction facingDir = face.getAxis() == Direction.Axis.Y ? yawFacing : face;
        Direction dir = getDirModified(facingDir);
        int o = -getOffset();
        int ox = pos.getX() + facingDir.getStepX() * o;
        int oy = pos.getY() + getHeightOffset();
        int oz = pos.getZ() + facingDir.getStepZ() * o;
        if (face == Direction.DOWN) {
            int[] dim = getDimensions();
            oy -= dim[0] + dim[1];
        } else if (face != Direction.UP) {
            // rotated dims are {up, down, north, south, west, east}: the Direction ordinals 2..5 index the horizontals
            int[] rotDim = MultiblockHandlerXR.rotate(getDimensions(), dir);
            if (rotDim != null) {
                ox = pos.getX() + face.getStepX() * rotDim[face.getOpposite().ordinal()];
                oz = pos.getZ() + face.getStepZ() * rotDim[face.getOpposite().ordinal()];
            }
        }
        BlockPos core = new BlockPos(ox, oy, oz);
        // 1.7 checkRequirement(ox - dir.offsetX * o, oy, oz - dir.offsetZ * o, dir, o): that point is the cell skipped
        return new Placement(core, dir, new BlockPos(ox - dir.getStepX() * o, oy, oz - dir.getStepZ() * o));
    }

    /** The placement highlight's green/red answer for a {@link #placement}. */
    public final boolean canPlace(Level level, Placement placement) {
        return checkRequirement(level, placement.core(), placement.facing(), placement.clicked());
    }

    /**
     * hbm-space {@code addCollisionBoxesToList}: true while {@code context}'s player is one of this type's internal
     * players. As 1.7's per-tick check, the list is cleared only once none of them is inside any more.
     */
    protected final boolean ignoresCollision(CollisionContext context) {
        if (internalPlayers.isEmpty()
                || !(context instanceof EntityCollisionContext entityContext)
                || !(entityContext.getEntity() instanceof Player player) || !internalPlayers.contains(player)) {
            return false;
        }
        for (Player inside : internalPlayers) {
            if (isPlayerInside(inside)) return true;
        }
        internalPlayers.clear();
        return false;
    }

    /** Public spelling of {@link #ignoresCollision} for cell blocks of another class. */
    public final boolean ignoresCollisionFor(CollisionContext context) {
        return ignoresCollision(context);
    }

    /** hbm-space {@code isPlayerInside}: the block at the player's feet (+0.1) or the one above belongs to this type. */
    private boolean isPlayerInside(Player player) {
        if (player.isRemoved()) return false;
        Level level = player.level();
        BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.1D, player.getZ());
        return partOfThis(level, feet) || partOfThis(level, feet.above());
    }

    private boolean partOfThis(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() == this) return true;
        if (!MultiblockSurface.isFoldedCell(state)) return false;
        BlockPos core = MultiblockSurface.coreOfCell(level, pos);
        return core != null && MultiblockSurface.foldedCore(level.getBlockState(core)) == this;
    }

    /** Whether the structure at {@code core} occupies {@code pos} (the client's stand-in before the cells arrive). */
    private boolean occupies(BlockPos core, Direction facing, BlockPos pos) {
        if (core.equals(pos)) return true;
        boolean[] hit = {false};
        visitCells(core, facing, (cell, mask) -> { if (cell.equals(pos)) hit[0] = true; });
        return hit[0];
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(placer instanceof Player player)) return;
        SAFE_REM.set(true);
        try {
            withoutTeardown(() -> level.removeBlock(pos, false));
        } finally {
            SAFE_REM.set(false);
        }
        PlacedFace clicked = PLACED_FACE.get();
        PLACED_FACE.remove();
        Direction face = clicked != null && clicked.pos().equals(pos) ? clicked.face() : Direction.UP;
        Placement placement = placement(pos, face, player.getYRot());
        Direction facing = placement.facing();
        BlockPos core = placement.core();
        if (!checkRequirement(level, core, facing, placement.clicked())) {
            ItemStack refund = ItemStacks.copyWithCount(stack, 1);
            if (!player.getAbilities().instabuild && !player.getInventory().add(refund)) player.drop(refund, false);
            return;
        }
        if (level.isClientSide()) {
            // the cells have not arrived yet: the client asks the footprint instead of the world
            if (walkOutAfterPlacing) {
                BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.1D, player.getZ());
                if (occupies(core, facing, feet) || occupies(core, facing, feet.above())) internalPlayers.add(player);
            }
            return;
        }
        withoutTeardown(() -> {
            level.setBlock(core, defaultBlockState().setValue(FACING, facing), 3);
            fillSpace(level, core, facing);
        });
        MultiblockHandlerXR.refreshSpace(level, core, getDimensions(), facing);
        BlockEntity placedCore = level.getBlockEntity(core);
        if (placedCore != null) {
            ItemStacks.applyComponents(placedCore, stack);
            placedCore.setChanged();
        }
        // hbm-space: "check if the placing player is inside the placed multiblock"
        if (walkOutAfterPlacing && isPlayerInside(player)) internalPlayers.add(player);
    }

    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return MultiblockHandlerXR.checkSpace(level, core, getDimensions(), clicked, facing);
    }

    /** The state written for one cell. */
    protected abstract BlockState cellStateFor(int localX, int localY, int localZ, Direction facing, int shapeId);

    public final void fillSpace(LevelAccessor level, BlockPos core, Direction facing) {
        fillSpace(level, core, facing, null);
    }

    /**
     * {@link #fillSpace} restricted to the cells inside {@code clip}: a hardcoded world-generation structure builds
     * chunk by chunk, and 1.7's structure {@code fillSpace} wrote only the cells inside the populating box.
     */
    public final void fillSpace(LevelAccessor level, BlockPos core, Direction facing,
                                net.minecraft.world.level.levelgen.structure.@Nullable BoundingBox clip) {
        CoreIndexScratch scratch = CoreIndexScratch.borrow();
        try {
            visitPlacedCells(level, core, facing, (pos, mask) -> {
                if (clip != null && !clip.isInside(pos)) return;
                int dx = pos.getX() - core.getX(), dy = pos.getY() - core.getY(), dz = pos.getZ() - core.getZ();
                int lx = MultiblockMaskTable.localX(dx, dy, dz, facing), lz = MultiblockMaskTable.localZ(dx, dy, dz, facing);
                level.setBlock(pos, cellStateFor(lx, dy, lz, facing, 0), 3);
                if (isCellOf(level.getBlockState(pos))) scratch.add(pos);
            });
            if (level instanceof ServerLevel server) {
                for (int i = 0; i < scratch.size(); i++) MultiblockCoreIndex.put(server, BlockPos.of(scratch.get(i)), core);
            } else if (level instanceof WorldGenLevel worldgen && scratch.size() > 0) {
                // A structure placed during world generation (a worker thread): the index is level SavedData, so
                // its entries are handed to the server thread instead of being written from here.
                ServerLevel server = worldgen.getLevel();
                long[] cells = new long[scratch.size()];
                for (int i = 0; i < cells.length; i++) cells[i] = scratch.get(i);
                BlockPos owner = core.immutable();
                server.getServer().execute(() -> {
                    for (long cell : cells) MultiblockCoreIndex.put(server, BlockPos.of(cell), owner);
                });
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
            block.visitTeardownCells(level, core, facing, (cell, mask) -> {
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
     * the core's own removal retires nothing at its cells. Geometry only, with no world read, so it is safe on
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
     * for a cell when that fell short, because five machines build far more than they declare. The folded
     * table holds every cell, so the widened radius is exact and no scan is needed.
     */
    public double getInteractionRangeSq() {
        double range = Math.sqrt(BASE_INTERACTION_RANGE_SQ) + maskTable().cornerRadius();
        return range * range;
    }

    /** Whether {@code player} is close enough to interact with the machine cored at {@code core}. */
    public static boolean isWithinInteractionRange(BlockGetter level, BlockPos core, Player player) {
        BlockMultiblockCore block = MultiblockSurface.foldedCore(level.getBlockState(core));
        // Measured in the world: a machine on a moving sub-level keeps its blocks far from where the player sees it.
        double distance = SubLevels.distanceSq(player.level(),
                core.getX() + 0.5D, core.getY() + 0.5D, core.getZ() + 0.5D, player.getX(), player.getY(), player.getZ());
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
     * <p>The menu belongs to the core's block entity; a cell carries none. Every caller reaches this with
     * the core already resolved.
     *
     * <p>A machine with no menu answers {@code TRY_WITH_EMPTY_HAND} rather than swallowing the click.
     *
     * <p>Override for a machine that does something other than open its menu, or treats a sneak-click
     * differently.
     */
    protected InteractionResult useAtCore(BlockState state, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(core) instanceof MenuProvider menu)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        Menus.openWithPos(serverPlayer, menu, core);
        return InteractionResult.SUCCESS;
    }

    public void onExplosionHitAtCore(BlockState state, ServerLevel level, BlockPos core, Explosion explosion,
                                     BiConsumer<ItemStack, BlockPos> onHit) {
        forwardExplosionHit(state, level, core, explosion, onHit);
    }

    @Override protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useAtCore(state, level, pos, player, hit);
    }

    // 1.7 BlockDummyable.getSettings/pasteSettings/infoForDisplay: findCore first, so the settings tool works on
    // any part of the machine, not only the core.
    @Override public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        BlockPos core = copyCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof ICopiable copiable ? copiable.getSettings(level, core) : null;
    }

    @Override public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        BlockPos core = copyCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof ICopiable copiable) copiable.pasteSettings(nbt, index, level, player, core);
    }

    @Override public String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        BlockPos core = copyCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof ICopiable copiable ? copiable.infoForDisplay(level, core) : null;
    }

    private @Nullable BlockPos copyCore(Level level, BlockPos pos) {
        return isCellState(level.getBlockState(pos)) ? MultiblockSurface.coreOfCell(level, pos) : pos;
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
