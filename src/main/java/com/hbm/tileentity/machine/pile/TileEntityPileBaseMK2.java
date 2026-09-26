package com.hbm.tileentity.machine.pile;

import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A plain cell of an assembled pile. It knows nothing except where its core is; every cell holds one so
 * that a device docked anywhere on the face can walk straight to the simulation, and so that the whole
 * structure collapses back to graphite the moment the core goes.
 */
public class TileEntityPileBaseMK2 extends SyncedBlockEntity {

    /**
     * "No core recorded". Upstream uses {@code coreY = -999} and tests {@code coreY >= 0}, which only
     * works while the world floor is y=0; here a legitimate core can sit at any negative y, so the
     * sentinel has to be a value no block position can take.
     */
    private static final int NO_CORE = Integer.MIN_VALUE;

    private TileEntityPileCore cachedCore;
    public int coreX;
    public int coreY = NO_CORE;
    public int coreZ;

    public TileEntityPileBaseMK2(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_BLOCK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileBaseMK2 pile) {
        if (!pile.hasCore()) return;

        TileEntityPileCore core = pile.getCore();
        if ((core == null || core.isRemoved()) && level.isLoaded(new BlockPos(pile.coreX, pile.coreY, pile.coreZ))) {
            level.destroyBlock(pos, false);
        }
    }

    public TileEntityPileBaseMK2 setCore(int x, int y, int z) {
        this.coreX = x;
        this.coreY = y;
        this.coreZ = z;
        setChanged();
        return this;
    }

    public boolean hasCore() {
        return coreY != NO_CORE;
    }

    @Nullable
    public TileEntityPileCore getCore() {
        if (cachedCore != null && !cachedCore.isRemoved()) return cachedCore;
        if (level == null || !hasCore()) return null;

        BlockPos corePos = new BlockPos(coreX, coreY, coreZ);
        if (!level.isLoaded(corePos)) return null;

        if (level.getBlockEntity(corePos) instanceof TileEntityPileCore core) {
            cachedCore = core;
            return core;
        }
        return null;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("cX", coreX);
        output.putInt("cY", coreY);
        output.putInt("cZ", coreZ);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        coreX = input.getIntOr("cX", 0);
        coreY = input.getIntOr("cY", NO_CORE);
        coreZ = input.getIntOr("cZ", 0);
    }
}
