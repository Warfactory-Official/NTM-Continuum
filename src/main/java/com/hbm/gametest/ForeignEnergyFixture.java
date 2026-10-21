package com.hbm.gametest;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.EnergyExposure;
import dev.golem.core.world.EnergySource;
import net.minecraft.core.BlockPos;
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
    public static final RegistryObject<BlockEntityType<CellEntity>> CELL_ENTITY = BLOCK_ENTITIES.register(
            "test_fe_cell", () -> BlockEntities.type(CellEntity::new, CELL.get()));

    private ForeignEnergyFixture() {}

    public static void init() {
        EnergyExposure.expose(new CellSource(), () -> new Block[]{CELL.get()});
    }

    public static long stored(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CellEntity cell ? cell.stored : -1L;
    }

    public static final class Cell extends CompatBlock implements EntityBlock {

        private Cell(Properties properties) {
            super(properties);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CellEntity(pos, state);
        }
    }

    public static final class CellEntity extends BlockEntity {

        private long stored;

        private CellEntity(BlockPos pos, BlockState state) {
            super(CELL_ENTITY.get(), pos, state);
        }
    }

    private static final class CellSource implements EnergySource {

        private static @Nullable CellEntity at(ServerLevel level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof CellEntity cell ? cell : null;
        }

        @Override
        public long amountFe(ServerLevel level, BlockPos pos) {
            CellEntity cell = at(level, pos);
            return cell == null ? 0L : cell.stored;
        }

        @Override
        public long capacityFe(ServerLevel level, BlockPos pos) {
            return CAPACITY;
        }

        @Override
        public long insertableFe(ServerLevel level, BlockPos pos) {
            CellEntity cell = at(level, pos);
            return cell == null ? 0L : Math.min(RATE, CAPACITY - cell.stored);
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
