package com.hbm.api.fluidmk2;

import com.hbm.uninos.graph.IGraphProvider;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A duct carries one fluid, and two ducts are one network only if it is the same fluid.
 *
 * <p>A freshly placed duct carries {@link com.hbm.inventory.fluid.Fluids#NONE} — its type is set
 * afterwards with a fluid identifier and lives in its block entity, not its state, so
 * {@link #createData} can only ever answer for the untyped case. Retyping goes through
 * {@link com.hbm.uninos.graph.LevelNodeGraph#replaceNodeData}, which re-partitions around the cell.
 */
public final class FluidGraphProvider implements IGraphProvider<DuctData> {

    public static final FluidGraphProvider INSTANCE = new FluidGraphProvider();

    private FluidGraphProvider() {}

    @Override
    public DuctData createData(BlockState state) {
        return DuctData.NONE;
    }

    @Override
    public boolean dataCompatible(DuctData a, DuctData b) {
        return a.typeId() == b.typeId();
    }

    @Override
    public Codec<DuctData> dataCodec() {
        return DuctData.CODEC;
    }
}
