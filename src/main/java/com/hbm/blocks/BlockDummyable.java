package com.hbm.blocks;

import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;

/**
 * Port of HBM's {@code BlockDummyable} multiblock base. A machine is built from one <b>core</b> block
 * (which carries the {@link BlockEntity} and is drawn by a TESR/BER) surrounded by invisible
 * <b>dummy</b> blocks that occupy the rest of the footprint. All parts are the same block,
 * distinguished by the {@link #META} state:
 *
 * <ul>
 *   <li><b>0–5</b> dummy — value = {@link ForgeDirection} ordinal; walking the <i>opposite</i> way
 *       steps toward the core.</li>
 *   <li><b>6–11</b> "extra" dummy — same as 0–5 but flagged (value − {@link #extra} = direction).</li>
 *   <li><b>12–15</b> core — value − {@link #offset} = facing direction (used for render rotation).</li>
 * </ul>
 *
 * <p>Render is {@link RenderShape#INVISIBLE}: vanilla draws no block model, and a
 * {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer} on the core draws the whole
 * machine across the footprint. Collision stays a full cube per part, so the structure occupies its
 * real space. Deferred vs the 1.12.2 original: detailed rotated hitboxes, NBT copy/transform, and the
 * advanced "extra"-meta core routing (see IMPLEMENTATION.md §18).
 */
public abstract class BlockDummyable extends dev.golem.core.world.CompatBlock implements EntityBlock {

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);

    /** meta offset from dummy direction to core (rotation) meta. */
    public static final int offset = 10;
    /** meta offset from dummy direction to "extra" dummy meta. */
    public static final int extra = 6;
    /** Re-entry guard: suppresses the break cascade while we tear the structure down ourselves. */
    public static boolean safeRem = false;

    /**
     * Optional whole-structure hitboxes (the old {@code bounding} list): boxes in core-relative
     * coordinates, centred on the core cell's x/z middle (the old convention — a box spanning
     * −1.5..1.5 covers the full 3×3 footprint). When non-empty, every cell of the multiblock reports
     * the SAME structure shape translated into its local frame, exactly like the old custom collision.
     * Empty (the default) = plain full cubes per cell.
     */
    protected final java.util.List<net.minecraft.world.phys.AABB> bounding = new java.util.ArrayList<>();
    /** Structure shape per core-relative cell offset (shape queries are hot + run on meshing threads). */
    private final java.util.Map<BlockPos, net.minecraft.world.phys.shapes.VoxelShape> boundingCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    public BlockDummyable(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, 0));
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos,
                                                                  net.minecraft.world.phys.shapes.CollisionContext context) {
        if (bounding.isEmpty()) return super.getShape(state, world, pos, context);
        BlockPos core = findCore(world, pos);
        if (core == null) return net.minecraft.world.phys.shapes.Shapes.block();
        return boundingCache.computeIfAbsent(core.subtract(pos), off -> {
            net.minecraft.world.phys.shapes.VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
            for (net.minecraft.world.phys.AABB box : bounding) {
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape,
                        net.minecraft.world.phys.shapes.Shapes.create(box.move(off.getX() + 0.5, off.getY(), off.getZ() + 0.5)));
            }
            return shape;
        });
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    // --- multiblock shape ---

    /** Dummy counts around the core, order [UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT] (when facing south). */
    public abstract int[] getDimensions();

    /** Distance (in blocks) the core sits in front of the clicked position. */
    public abstract int getOffset();

    public int getHeightOffset() {
        return 0;
    }

    /**
     * Upstream's machine-base interaction range, squared: {@code getDistanceSq(...) <= 128}, twice
     * vanilla's, which HBM widened precisely because its machines are multiblocks.
     */
    public static final double BASE_INTERACTION_RANGE_SQ = 128D;

    /**
     * How far from the <b>core</b> a player may be and still keep this machine's menu open, squared.
     *
     * <p>This exists because a multiblock's menu is bound to its core cell, but the player interacts
     * with whatever cell they are standing next to — and on a big machine those are nowhere near each
     * other. Vanilla's {@code stillValid} measures interaction range to the bound position, so on the
     * fusion torus (core seven blocks from the footprint edge) the menu is out of range before you can
     * even touch the structure.
     *
     * <p>The radius is upstream's flat range widened by the half-diagonal of the machine's declared
     * bounding box — all three axes, because HBM's multiblocks are as often tall-and-thin (the coker is
     * 22 high over a 1-block footprint) as wide-and-flat.
     *
     * <p><b>This is a fast path, not the whole answer.</b> {@link #getDimensions()} is what a machine
     * declares for <i>placement</i>, and several build far more than that in {@code fillSpace} — the
     * fracking tower declares a radius of 3 and erects a derrick 24 blocks tall. So a machine can be
     * outside this radius and still be the machine the player is standing on; see
     * {@link #isWithinInteractionRange}, which falls back to asking the world.
     */
    public double getInteractionRangeSq() {
        int[] dim = getDimensions();
        double range = Math.sqrt(BASE_INTERACTION_RANGE_SQ);
        if (dim != null && dim.length == 6) {
            double y = Math.max(dim[0], dim[1]);
            double z = Math.max(dim[2], dim[3]);
            double x = Math.max(dim[4], dim[5]);
            range += Math.sqrt(x * x + y * y + z * z);
        }
        return range * range;
    }

    /** How far around the player the cell fallback looks for a piece of the machine. */
    private static final int CELL_SCAN_RADIUS = 5;

    /**
     * Whether {@code player} is close enough to interact with the machine whose <b>core</b> sits at
     * {@code corePos}. Use this anywhere a server-side check gates on "is the player still at the
     * machine" — menu validity, control packets, button presses — so all of them agree, and so none of
     * them measures a big machine's range from a cell the player is nowhere near.
     *
     * <p>Two steps. The cheap one is a radius around the core ({@link #getInteractionRangeSq}), which
     * settles the common case in a few arithmetic ops. When that fails we ask the question that is
     * actually being asked — <i>is the player standing at a piece of this machine?</i> — by looking for
     * a cell of the same block within reach that chains back to this same core. That is correct however
     * badly a machine's declared dimensions understate what it actually builds, which matters because
     * five of them already do.
     *
     * <p>The fallback only runs when the player is beyond the radius, i.e. rarely, and stops at the
     * first matching cell — normally immediately, since the player is pressed against the structure.
     */
    public static boolean isWithinInteractionRange(net.minecraft.world.level.BlockGetter level,
                                                   BlockPos corePos, Player player) {
        Block block = level.getBlockState(corePos).getBlock();
        if (!(block instanceof BlockDummyable dummyable)) {
            return distanceSqTo(corePos, player) <= BASE_INTERACTION_RANGE_SQ;
        }
        if (distanceSqTo(corePos, player) <= dummyable.getInteractionRangeSq()) return true;
        return isStandingAtMachine(level, corePos, dummyable, player);
    }

    private static double distanceSqTo(BlockPos pos, Player player) {
        double dx = pos.getX() + 0.5D - player.getX();
        double dy = pos.getY() + 0.5D - player.getY();
        double dz = pos.getZ() + 0.5D - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /** True if any cell within reach of the player belongs to the multiblock cored at {@code corePos}. */
    private static boolean isStandingAtMachine(net.minecraft.world.level.BlockGetter level, BlockPos corePos,
                                               BlockDummyable block, Player player) {
        BlockPos at = player.blockPosition();
        BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
        for (int dx = -CELL_SCAN_RADIUS; dx <= CELL_SCAN_RADIUS; dx++) {
            for (int dy = -CELL_SCAN_RADIUS; dy <= CELL_SCAN_RADIUS; dy++) {
                for (int dz = -CELL_SCAN_RADIUS; dz <= CELL_SCAN_RADIUS; dz++) {
                    scratch.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (level.getBlockState(scratch).getBlock() != block) continue;
                    if (corePos.equals(block.findCore(level, scratch))) return true;
                }
            }
        }
        return false;
    }

    public ForgeDirection getDirModified(ForgeDirection dir) {
        return dir;
    }

    protected int getMetaForCore(Level world, BlockPos pos, Player player, int original) {
        return original;
    }

    // --- core finding: walk the dummy chain to the core block ---

    protected int getMaxCoreSearchSteps() {
        return 512;
    }

    protected boolean isSameMultiblock(Block other) {
        return other == this;
    }

    @Nullable
    public BlockPos findCore(BlockGetter world, BlockPos pos) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos().set(pos);
        for (int steps = 0, max = getMaxCoreSearchSteps(); steps < max; steps++) {
            BlockState state = world.getBlockState(m);
            if (!isSameMultiblock(state.getBlock())) return null;
            int meta = state.getValue(META);
            if (meta >= 12) return m.immutable();
            if (meta >= extra) meta -= extra;
            ForgeDirection dir = ForgeDirection.getOrientation(meta).getOpposite();
            m.move(dir.offsetX, dir.offsetY, dir.offsetZ);
        }
        return null;
    }

    @Nullable
    public BlockEntity findCoreTE(BlockGetter world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        return core == null ? null : world.getBlockEntity(core);
    }

    // --- EntityBlock: only the core carries a BlockEntity ---

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? createCoreBlockEntity(pos, state) : null;
    }

    protected abstract BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state);

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // the BlockEntityRenderer draws the machine; the block itself is invisible
    }

    /** Right-clicking any part of the multiblock opens the core BlockEntity's menu (if it provides one). */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockPos core = findCore(level, pos);
            if (core != null && level.getBlockEntity(core) instanceof MenuProvider provider) {
                dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, core);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // --- placement: cancel cleanly if the footprint is blocked, otherwise build core + dummies ---

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null) return this.defaultBlockState();
        Placement p = resolvePlacement(player, context.getClickedPos());
        if (!checkRequirement(context.getLevel(), p.x, p.y, p.z, p.dir, p.o)) {
            return null; // no room — cancel placement so the item is not consumed
        }
        return this.defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (world.isClientSide() || !(placer instanceof Player player)) return;

        safeRem = true;
        world.removeBlock(pos, false); // remove the single placeholder block; we rebuild the real structure
        safeRem = false;

        Placement p = resolvePlacement(player, pos);
        if (!checkRequirement(world, p.x, p.y, p.z, p.dir, p.o)) return; // double-check; getStateForPlacement already gated

        BlockPos core = new BlockPos(p.x + p.dir.offsetX * p.o, p.y + p.dir.offsetY * p.o, p.z + p.dir.offsetZ * p.o);
        // dir is one of NORTH/SOUTH/WEST/EAST (ForgeDirection ordinals 2..5); core meta = ordinal + offset => 12..15.
        int meta = getMetaForCore(world, core, player, p.dir.ordinal() + offset);
        world.setBlock(core, this.defaultBlockState().setValue(META, meta), 3);
        fillSpace(world, p.x, p.y, p.z, p.dir, p.o);
    }

    private record Placement(ForgeDirection dir, int x, int y, int z, int o) {}

    private Placement resolvePlacement(Player player, BlockPos clicked) {
        int i = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        ForgeDirection dir = switch (i) {
            case 0 -> ForgeDirection.getOrientation(2); // NORTH
            case 1 -> ForgeDirection.getOrientation(5); // EAST
            case 2 -> ForgeDirection.getOrientation(3); // SOUTH
            case 3 -> ForgeDirection.getOrientation(4); // WEST
            default -> ForgeDirection.NORTH;
        };
        dir = getDirModified(dir);
        BlockPos base = clicked.above(getHeightOffset());
        return new Placement(dir, base.getX(), base.getY(), base.getZ(), -getOffset());
    }

    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), x, y, z, dir);
    }

    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        formStructure(world, new BlockPos(x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o), dir);
    }

    /**
     * Forms the live dummy shell around an <b>existing</b> core block. Split out of {@link #fillSpace}
     * so callers that already hold the core position can build the structure without going through
     * placement: struct-core assembly ({@link com.hbm.tileentity.machine.struct.TileEntityStructBase})
     * and the layout GameTests. Machines with an irregular footprint override <b>this</b> — stacking
     * further {@link MultiblockHandlerXR#fillSpace} passes and {@link #makeExtra} port cells — rather
     * than {@code fillSpace}, so both entry points agree.
     */
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(), getDimensions(), this, dir);
    }

    /** Upgrades an adjacent plain dummy (meta 0–5) to an "extra" dummy (meta 6–11). */
    public void makeExtra(Level world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState s = world.getBlockState(pos);
        if (s.getBlock() != this) return;
        int meta = s.getValue(META);
        if (meta > 5) return;
        safeRem = true;
        world.setBlock(pos, s.setValue(META, meta + extra), 3);
        safeRem = false;
    }

    // --- breaking: removing any part tears down the whole structure ---

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (safeRem) return;
        int meta = state.getValue(META);
        safeRem = true;
        try {
            // If a dummy was broken, remove its core first; the now-coreless dummies become orphans below.
            if (meta < 12) {
                int m = meta >= extra ? meta - extra : meta;
                ForgeDirection toCore = ForgeDirection.getOrientation(m).getOpposite();
                BlockPos core = findCore(level, pos.offset(toCore.offsetX, toCore.offsetY, toCore.offsetZ));
                if (core != null) {
                    level.removeBlock(core, false);
                    cascadeOrphans(level, core);
                }
            }
            // Remove only cells that chain back to the removed core — NOT an adjacent same-block machine,
            // whose cells still reach their own intact core (so they are not orphans).
            cascadeOrphans(level, pos);
        } finally {
            safeRem = false;
        }
    }

    /** True if {@code state} is a dummy whose step-toward-core neighbour is no longer part of this block. */
    private boolean isOrphan(BlockGetter world, BlockPos pos, BlockState state) {
        int meta = state.getValue(META);
        if (meta >= 12) return false; // a core is never an orphan
        if (meta >= extra) meta -= extra;
        ForgeDirection toCore = ForgeDirection.getOrientation(meta).getOpposite();
        BlockPos towards = pos.offset(toCore.offsetX, toCore.offsetY, toCore.offsetZ);
        return world.getBlockState(towards).getBlock() != this;
    }

    /** BFS from {@code seed}'s neighbours, removing dummies that have become orphaned (chained to {@code seed}). */
    private void cascadeOrphans(Level level, BlockPos seed) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            queue.add(seed.offset(d.offsetX, d.offsetY, d.offsetZ));
        }
        int cap = 4096;
        while (!queue.isEmpty() && cap-- > 0) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (s.getBlock() != this || !isOrphan(level, p, s)) continue;
            level.removeBlock(p, false);
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                queue.add(p.offset(d.offsetX, d.offsetY, d.offsetZ));
            }
        }
    }
}
