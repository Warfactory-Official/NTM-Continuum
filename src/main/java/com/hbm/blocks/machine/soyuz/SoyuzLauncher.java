package com.hbm.blocks.machine.soyuz;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The launch complex. It is never placed by hand: the struct core assembles it once the pad, its legs and
 * the service tower are built, and this block's footprint is the whole thirteen-by-thirteen apron plus the
 * towers, filled in one pass from the same boxes upstream uses.
 */
public class SoyuzLauncher extends BlockDummyable {

    /** How far above the struct core the launcher's own core sits. */
    public static final int HEIGHT = 4;

    public SoyuzLauncher(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySoyuzLauncher(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** The apron, the four flame trenches and the two service towers, exactly upstream's boxes. */
    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        int x = core.getX(), y = core.getY(), z = core.getZ();

        MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{0, 1, 6, 6, 6, 6}, this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{-2, 4, -3, 6, -3, 6}, this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{-2, 4, 6, -3, -3, 6}, this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{-2, 4, 6, -3, 6, -3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{-2, 4, -3, 6, 6, -3}, this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{0, 4, 1, 1, -6, 8}, this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{0, 4, 2, 2, 9, -5}, this, dir);

        for (int ix = -6; ix <= 6; ix++) {
            for (int iz = -6; iz <= 6; iz++) {
                if (ix == 6 || ix == -6 || iz == 6 || iz == -6) {
                    makeExtra(level, x + ix, y, z + iz);
                    makeExtra(level, x + ix, y + 1, z + iz);
                }
            }
        }
    }

    /** The complex cannot be picked up; breaking it gives back the bulk it was built from. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.SOYUZ_LAUNCHER.get(), TileEntitySoyuzLauncher::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
