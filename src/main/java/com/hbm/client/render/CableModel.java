package com.hbm.client.render;

import com.hbm.blocks.network.BlockCable;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.Disguises;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.HashSet;
import java.util.Set;

/**
 * HBM's red cable as the real {@code cable_neo.obj}, drawn into the chunk mesh: a {@code Core} plus an arm per
 * connected side, or the straight {@code CX}/{@code CY}/{@code CZ} piece for a cable along one axis. Includes the
 * original's Z-axis quirk (a NORTH connection draws {@code posZ}, SOUTH draws {@code negZ}).
 */
public final class CableModel implements Disguises.Look {

    private static final String SPRITE = "hbm:models/blocks/cable_neo";
    /** The OBJ is centred on the origin. */
    static final Matrix4fc CENTRE = new Matrix4f().translation(0.5F, 0.5F, 0.5F);

    private WavefrontObj model;

    private synchronized WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(Ids.of("hbm", "models/blocks/cable_neo.obj"));
        return model;
    }

    @Override
    public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
        return selectParts(state.getValue(BlockCable.EAST), state.getValue(BlockCable.WEST), state.getValue(BlockCable.UP),
                state.getValue(BlockCable.DOWN), state.getValue(BlockCable.SOUTH), state.getValue(BlockCable.NORTH));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void mesh(Object key, Disguises.Meshes out) {
        for (String part : (Set<String>) key) out.add(model().part(part), SPRITE, -1, CENTRE);
    }

    @Override
    public boolean ambientOcclusion() {
        return false;
    }

    /** The 1.12.2 {@code BlockCableBakedModel.buildWorldQuads} part selection, verbatim. */
    private static Set<String> selectParts(boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        if (pX && nX && !pY && !nY && !pZ && !nZ) return Set.of("CX");
        if (!pX && !nX && pY && nY && !pZ && !nZ) return Set.of("CY");
        if (!pX && !nX && !pY && !nY && pZ && nZ) return Set.of("CZ");
        Set<String> parts = new HashSet<>();
        parts.add("Core");
        if (pX) parts.add("posX");
        if (nX) parts.add("negX");
        if (pY) parts.add("posY");
        if (nY) parts.add("negY");
        if (nZ) parts.add("posZ"); // mirrors the original (NORTH -> posZ)
        if (pZ) parts.add("negZ"); // mirrors the original (SOUTH -> negZ)
        return Set.copyOf(parts);
    }
}
