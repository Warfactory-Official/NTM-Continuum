package com.hbm.compat.computercraft;

//? if computercraft {
import com.hbm.Constants;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.compat.computercraft.albion.AlbionRows;
import com.hbm.compat.computercraft.bomb.BombRows;
import com.hbm.compat.computercraft.fusion.FusionRows;
import com.hbm.compat.computercraft.machine.PowerRows;
import com.hbm.compat.computercraft.machine.ReactorRows;
import com.hbm.compat.computercraft.network.NetworkRows;
import com.hbm.compat.computercraft.rbmk.RbmkRows;
import com.hbm.compat.computercraft.storage.StorageRows;
import com.hbm.compat.computercraft.turret.TurretRows;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.golem.core.computer.PeripheralExposure;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.platform.Lifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Where each 1.7 OpenComputers component lives, and the peripheral CC: Tweaked finds there. One peripheral per block
 * entity, made at the first lookup and refreshed after every server tick until the block entity is removed.
 *
 * <p>A row names a block entity class; a subclass answers too, as an inherited 1.7 {@code SimpleComponent} did. Where
 * a computer or modem may touch the machine is the row's {@link Reach}, each one a 1.7 {@code canConnectNode} rule.
 */
public final class Peripherals {

    /** Where a component is reachable, besides nowhere. */
    public enum Reach {
        /** The block entity's own block, every side: 1.7's default {@code canConnectNode}. */
        BLOCK,
        /**
         * The core block and every proxy cell flagged for power or fluid, but not through a face that touches the
         * machine itself: 1.7 {@code TileEntityProxyCombo.canConnectNode} (a {@code makeExtra} cell, meta 6..11,
         * {@code power || fluid}, the neighbour not part of the same machine). The port states those cells on one
         * of two planes, so either counts: the passive plane's power-in/fluid-in cells, or an active port cell (a
         * cell open for power or fluid on some face, as the Orbus and the large radar declare theirs). The face the
         * computer touches need not be the port's face: 1.7's proxy connected on every outward side.
         */
        CELLS,
        /**
         * The column's bottom (core) block and its top block: 1.7 {@code RBMKBase.fillSpace} promotes the top block
         * alone to a {@code makeExtra} proxy, which the boiler, cooler, heater and outgasser flag for fluid.
         */
        RBMK_TOP,
        /** The block entity's own block, bottom side only: 1.7 {@code TileEntityTurretBaseNT.canConnectNode}. */
        DOWN,
        /**
         * Nowhere: a subclass whose 1.7 counterpart had no component although its port class inherits one (the
         * creative klystron). The most specific row wins, so this withholds the parent's peripheral.
         */
        NONE
    }

    public record Row<BE extends BlockEntity>(Class<BE> type,
                                              Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
                                              Reach reach) {
    }

    public static <BE extends BlockEntity> Row<BE> row(Class<BE> type,
                                                       Function<? super BE, ? extends SnapshotPeripheral<?>> factory,
                                                       Reach reach) {
        return new Row<>(type, factory, reach);
    }

    /** A class that answers no peripheral although a superclass's row would; see {@link Reach#NONE}. */
    public static <BE extends BlockEntity> Row<BE> none(Class<BE> type) {
        return new Row<>(type, be -> {
            throw new IllegalStateException(type.getName() + " has no peripheral");
        }, Reach.NONE);
    }

    /** One family's rows and its GameTests; the family classes own their lists. */
    private record Family(List<Row<?>> rows, GameTests.Suite tests) {
    }

    private static List<Family> families() {
        return List.of(
                new Family(PowerRows.ROWS, PowerRows.tests()),
                new Family(ReactorRows.ROWS, ReactorRows.tests()),
                new Family(StorageRows.ROWS, StorageRows.tests()),
                new Family(NetworkRows.ROWS, NetworkRows.tests()),
                new Family(RbmkRows.ROWS, RbmkRows.tests()),
                new Family(AlbionRows.ROWS, AlbionRows.tests()),
                new Family(FusionRows.ROWS, FusionRows.tests()),
                new Family(BombRows.ROWS, BombRows.tests()),
                new Family(TurretRows.ROWS, TurretRows.tests()),
                // a real CC computer running Lua; no rows. Listed here so init declares its bodies early enough:
                // 26.1 freezes the test-function registry before the loaders' GameTest hooks run
                new Family(List.of(), com.hbm.gametest.computer.ComputerLuaGameTests.TESTS));
    }

    // Server thread only: lookups and the refresh both run there.
    private static final Map<Class<?>, Row<?>> BY_TYPE = new IdentityHashMap<>();
    private static final Map<Class<?>, Optional<Row<?>>> RESOLVED = new HashMap<>();
    private static final Map<BlockEntity, SnapshotPeripheral<?>> BY_MACHINE = new IdentityHashMap<>();
    private static final ArrayList<SnapshotPeripheral<?>> LIVE = new ArrayList<>();

    private static final int PROXY_DOMAINS = BlockMultiblockCore.PASSIVE_POWER_IN | BlockMultiblockCore.PASSIVE_FLUID_IN;

    private Peripherals() {}

    static void init() {
        for (Family family : families()) {
            for (Row<?> row : family.rows()) {
                if (BY_TYPE.put(row.type(), row) != null) {
                    throw new IllegalStateException(row.type().getName() + " has two peripheral rows");
                }
            }
        }
        PeripheralExposure.exposeBlocks(Peripherals::at, Peripherals::carries);
        Lifecycle.onServerTick(Peripherals::tick);
        Lifecycle.onServerStopping(server -> {
            BY_MACHINE.clear();
            LIVE.clear();
        });
    }

    static List<GameTests.Suite> gameTestSuites() {
        List<GameTests.Suite> suites = new ArrayList<>();
        for (Family family : families()) suites.add(family.tests());
        return suites;
    }

    /** The rows, for the GameTest that checks the roster against the registered block entities. */
    public static List<Row<?>> rows() {
        List<Row<?>> rows = new ArrayList<>();
        for (Family family : families()) rows.addAll(family.rows());
        return rows;
    }

    /** HBM blocks that hold a block entity or are a folded cell of one, and other mods' component blocks. */
    private static boolean carries(Block block) {
        if (com.hbm.api.computer.ComputerComponents.carries(block)) return true;
        if (!(block instanceof EntityBlock) && !(block instanceof BlockMultiblockCore)
                && !(block instanceof com.hbm.blocks.multiblock.BlockMultiblockCell)) return false;
        return Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }

    /** The peripheral at {@code pos} through {@code side}, or null; what Core's lookup asks. Server thread. */
    public static @Nullable IPeripheral at(ServerLevel level, BlockPos pos, BlockState state, @Nullable BlockEntity be,
                                           @Nullable Direction side) {
        if (be != null && !be.isRemoved()) {
            Row<?> row = rowOf(be);
            if (row != null) return opensAtBlock(row.reach(), side) ? of(row, be) : null;
        }
        if (!MultiblockSurface.isFoldedCell(state)) return null;
        BlockPos core = MultiblockSurface.coreOfCell(level, pos);
        BlockEntity owner = core == null ? null : level.getBlockEntity(core);
        Row<?> row = owner == null || owner.isRemoved() ? null : rowOf(owner);
        if (row == null || !reachesCell(row.reach(), level, pos, state, core, side)) return null;
        return of(row, owner);
    }

    private static boolean opensAtBlock(Reach reach, @Nullable Direction side) {
        if (reach == Reach.NONE) return false;
        return reach != Reach.DOWN || side == null || side == Direction.DOWN;
    }

    private static boolean reachesCell(Reach reach, ServerLevel level, BlockPos pos, BlockState state, BlockPos core,
                                       @Nullable Direction side) {
        boolean proxy;
        switch (reach) {
            case CELLS:
                proxy = isProxyCell(level, pos);
                break;
            case RBMK_TOP:
                proxy = state.hasProperty(RBMKBase.PART) && state.getValue(RBMKBase.PART) == RBMKBase.Part.TOP;
                break;
            default:
                return false;
        }
        // 1.7 isComponent: a face that touches another block of the same machine is not a connection
        return proxy && (side == null || !core.equals(MultiblockSurface.coreOfAny(level, pos.relative(side))));
    }

    /** Whether the folded cell at {@code pos} stands for a 1.7 power/fluid proxy ({@link Reach#CELLS}). */
    public static boolean isProxyCell(ServerLevel level, BlockPos pos) {
        return MultiblockSurface.carriesPassive(level, pos, PROXY_DOMAINS) || isPort(level, pos, level.getBlockState(pos));
    }

    private static boolean isPort(ServerLevel level, BlockPos pos, BlockState state) {
        for (Direction face : Direction.values()) {
            if (MultiblockSurface.isOpen(level, pos, state, face)) return true;
        }
        return false;
    }

    private static @Nullable Row<?> rowOf(BlockEntity be) {
        Class<?> type = be.getClass();
        Optional<Row<?>> known = RESOLVED.get(type);
        if (known == null) {
            Row<?> found = null;
            for (Class<?> c = type; c != null && c != BlockEntity.class && found == null; c = c.getSuperclass()) {
                found = BY_TYPE.get(c);
            }
            if (found == null) {
                // another mod's component (com.hbm.api.computer.ComputerComponents), registered at its declare()
                com.hbm.api.computer.ComputerComponents.Component<?> component =
                        com.hbm.api.computer.ComputerComponents.of(be);
                if (component != null) {
                    found = new Row<>(BlockEntity.class, machine -> new ComponentPeripheral(component, machine), Reach.BLOCK);
                }
            }
            known = Optional.ofNullable(found);
            RESOLVED.put(type, known);
        }
        return known.orElse(null);
    }

    private static <BE extends BlockEntity> SnapshotPeripheral<?> of(Row<BE> row, BlockEntity be) {
        SnapshotPeripheral<?> known = BY_MACHINE.get(be);
        if (known != null) return known;
        SnapshotPeripheral<?> made = row.factory().apply(row.type().cast(be));
        made.refresh();
        BY_MACHINE.put(be, made);
        LIVE.add(made);
        return made;
    }

    private static void tick(MinecraftServer server) {
        for (int i = LIVE.size() - 1; i >= 0; i--) {
            SnapshotPeripheral<?> peripheral = LIVE.get(i);
            if (peripheral.target().isRemoved()) {
                BY_MACHINE.remove(peripheral.target());
                int last = LIVE.size() - 1;
                LIVE.set(i, LIVE.get(last));
                LIVE.remove(last);
            } else {
                peripheral.refresh();
            }
        }
    }
}
//?}
