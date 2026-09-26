package com.hbm.network.fluid;

import dev.golem.core.graph.GraphProvider;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A duct carries one fluid, and two ducts are one network only if it is the same fluid.
 *
 * <p>A freshly placed duct carries {@link com.hbm.inventory.fluid.Fluids#NONE} — its type is set
 * afterwards with a fluid identifier and lives in its block entity, not its state, so
 * {@link #createData} can only ever answer for the untyped case. Retyping goes through
 * {@link dev.golem.core.graph.LevelNodeGraph#replaceNodeData}, which re-partitions around the cell.
 */
public final class FluidGraphProvider implements GraphProvider<DuctData> {

    public static final FluidGraphProvider INSTANCE = new FluidGraphProvider();

    private FluidGraphProvider() {}

    @Override
    public DuctData createData(BlockState state) {
        return DuctData.NONE;
    }

    /**
     * Same fluid, or the exhaust's {@link DuctData#SMOKE_ANY} against any smoke: 1.7's exhaust sat in all
     * three smoke nodespaces, so a smoke duct joined it just as it joins another smoke duct. Not
     * transitive (a {@code SMOKE} duct and a {@code SMOKE_LEADED} duct joined through an exhaust are one
     * net); {@link FluidNetwork} rounds such a net once per smoke and every endpoint still gates the fluid
     * through its own {@code canConnect}. Every non-smoke pairing is exactly the equality it always was.
     */
    @Override
    public boolean dataCompatible(DuctData a, DuctData b) {
        if (a.typeId() == b.typeId()) return true;
        if (a.isSmokeAny()) return DuctData.isSmoke(b.type());
        if (b.isSmokeAny()) return DuctData.isSmoke(a.type());
        return false;
    }

    @Override
    public Codec<DuctData> dataCodec() {
        return DuctData.CODEC;
    }
}
