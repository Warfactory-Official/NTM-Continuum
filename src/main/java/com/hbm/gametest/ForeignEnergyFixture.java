package com.hbm.gametest;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.EnergyExposure;
import dev.golem.core.world.EnergySource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * {@code test_fe_cell}: a dev-only block that is a foreign energy store on every node (the loader's capability, or
 * Team Reborn Energy on Fabric) and knows nothing of HBM's connector interfaces. It stores up to 1,000,000 FE and takes
 * at most 10,000 per call.
 */
public final class ForeignEnergyFixture {

    public static final long CAPACITY = 1_000_000L;
    private static final long RATE = 10_000L;

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    private static final DeferredRegister.BlockEntityTypes BLOCK_ENTITIES =
            DeferredRegister.createBlockEntityTypes(Constants.MOD_ID);
    public static final RegistryObject<Cell> CELL = BLOCKS.registerBlock("test_fe_cell", Cell::new);
    public static final RegistryObject<Cell> OPTIONAL_CELL = BLOCKS.registerBlock("test_fe_optional_cell",
            properties -> new Cell(properties, true));
    public static final RegistryObject<BlockEntityType<CellEntity>> CELL_ENTITY = BLOCK_ENTITIES.register(
            "test_fe_cell", () -> BlockEntities.type(CellEntity::new, CELL.get(), OPTIONAL_CELL.get()));

    private ForeignEnergyFixture() {
    }

    public static void init() {
        EnergyExposure.expose(new CellSource(), () -> new Block[]{CELL.get(), OPTIONAL_CELL.get()});
    }

    public static void receiver(ServerLevel level, BlockPos pos, long capacity, int faceMask) {
        var cell = (CellEntity) level.getBlockEntity(pos);
        cell.capacity = capacity;
        cell.faceMask = faceMask;
        cell.exposed = true;
    }

    public static void removeEnergy(ServerLevel level, BlockPos pos) {
        ((CellEntity) level.getBlockEntity(pos)).exposed = false;
    }

    public static void forget(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CellEntity cell) {
            cell.exposed = false;
            cell.stored = 0;
        }
    }

    public static long stored(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CellEntity cell ? cell.stored : -1L;
    }

    public static final class Cell extends CompatBlock implements EntityBlock {
        private final boolean optional;

        private Cell(Properties properties) {
            this(properties, false);
        }

        private Cell(Properties properties, boolean optional) {
            super(properties);
            this.optional = optional;
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CellEntity(pos, state);
        }
    }

    public static final class CellEntity extends BlockEntity {

        private long stored;
        private long capacity = CAPACITY;
        private int faceMask = 0x3F;
        private boolean exposed;

        private CellEntity(BlockPos pos, BlockState state) {
            super(CELL_ENTITY.get(), pos, state);
            exposed = !((Cell) state.getBlock()).optional;
        }
    }

    private static final class CellSource implements EnergySource {

        @Override
        public @Nullable EnergySource forFace(ServerLevel level, BlockPos pos, @Nullable Direction side) {
            var cell = at(level, pos);
            return cell != null && cell.exposed && (side == null || (cell.faceMask & 1 << side.ordinal()) != 0)
                    ? this : null;
        }

        @Override
        public @Nullable EnergySource forOperation(ServerLevel level, BlockPos pos) {
            var cell = at(level, pos);
            return cell != null && cell.exposed ? this : null;
        }

        private static @Nullable CellEntity at(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof CellEntity cell ? cell : null;
        }

        @Override
        public long amountFe(ServerLevel level, BlockPos pos) {
            CellEntity cell = at(level, pos);
            return cell == null || !cell.exposed ? 0L : cell.stored;
        }

        @Override
        public long capacityFe(ServerLevel level, BlockPos pos) {
            var cell = at(level, pos);
            return cell == null || !cell.exposed ? 0L : cell.capacity;
        }

        @Override
        public long insertableFe(ServerLevel level, BlockPos pos) {
            CellEntity cell = at(level, pos);
            return cell == null || !cell.exposed ? 0L : Math.min(RATE, cell.capacity - cell.stored);
        }

        @Override
        public long extractableFe(ServerLevel level, BlockPos pos) {
            return 0L;
        }

        @Override
        public long insertFe(ServerLevel level, BlockPos pos, @Nullable BlockPos from, long fe) {
            CellEntity cell = at(level, pos);
            if (cell == null) return 0L;
            long taken = Math.max(0L, Math.min(fe, insertableFe(level, pos)));
            cell.stored += taken;
            return taken;
        }

        @Override
        public long extractFe(ServerLevel level, BlockPos pos, long fe) {
            return 0L;
        }

        @Override
        public Object snapshot(ServerLevel level, BlockPos pos) {
            return stored(level, pos);
        }

        @Override
        public void restore(ServerLevel level, BlockPos pos, Object state) {
            CellEntity cell = at(level, pos);
            if (cell != null && state instanceof Long value) cell.stored = value;
        }
    }
}
