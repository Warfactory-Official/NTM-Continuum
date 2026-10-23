package com.hbm.client.render;

import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.blocks.network.FluidDuctNeo;
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
 * The 1.7 {@code fluid_duct_neo} in its three skins ({@code pipe_neo}, {@code pipe_silver}, {@code pipe_colored}) in
 * the chunk mesh: the {@code pipe_neo.obj} parts picked from the connection flags in the base sprite, then the same
 * parts in the {@code _overlay} sprite on tint index 0, the carried fluid's colour (NONE's grey when empty).
 */
public final class NeoDuctModel implements Disguises.Look {

    private static final String[] SKINS = {"pipe_neo", "pipe_silver", "pipe_colored"};
    /** The overlay a hair larger than the pipe, as 1.7 drew it. */
    private static final Matrix4fc OVERLAY = new Matrix4f().translation(0.5F, 0.5F, 0.5F).scale(1.02F);

    private record Key(Set<String> parts, int skin) {}

    private WavefrontObj model;

    private synchronized WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(Ids.of("hbm", "models/blocks/pipe_neo.obj"));
        return model;
    }

    @Override
    public Object meshKey(BlockGetter level, BlockPos pos, BlockState state) {
        Set<String> parts = selectParts(state.getValue(BlockFluidDuct.EAST), state.getValue(BlockFluidDuct.WEST),
                state.getValue(BlockFluidDuct.UP), state.getValue(BlockFluidDuct.DOWN),
                state.getValue(BlockFluidDuct.SOUTH), state.getValue(BlockFluidDuct.NORTH));
        int skin = state.getBlock() instanceof FluidDuctNeo neo ? Math.min(neo.skin, SKINS.length - 1) : 0;
        return new Key(parts, skin);
    }

    @Override
    public void mesh(Object key, Disguises.Meshes out) {
        Key k = (Key) key;
        String sprite = "hbm:blocks/" + SKINS[k.skin()];
        for (String part : k.parts()) out.add(model().part(part), sprite, -1, CableModel.CENTRE);
        // 1.7 RenderTestPipe.renderDuct draws the overlay for every duct, empty ones in NONE's grey: the pipe_colored
        // skin is all overlay (its base sprite is only the bolt)
        for (String part : k.parts()) out.add(model().part(part), sprite + "_overlay", 0, OVERLAY);
    }

    @Override
    public boolean ambientOcclusion() {
        return false;
    }

    /** The 1.12.2 {@code DuctNeoBakedModel.buildWorldQuads} part selection, verbatim. */
    static Set<String> selectParts(boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        int mask = (pX ? 32 : 0) | (nX ? 16 : 0) | (pY ? 8 : 0) | (nY ? 4 : 0) | (pZ ? 2 : 0) | (nZ ? 1 : 0);
        Set<String> parts = new HashSet<>();
        switch (mask) {
            case 0 -> {
                parts.add("pX"); parts.add("nX");
                parts.add("pY"); parts.add("nY");
                parts.add("pZ"); parts.add("nZ");
            }
            case 32, 16 -> { parts.add("pX"); parts.add("nX"); }
            case 8, 4 -> { parts.add("pY"); parts.add("nY"); }
            case 2, 1 -> { parts.add("pZ"); parts.add("nZ"); }
            default -> {
                if (pX) parts.add("pX");
                if (nX) parts.add("nX");
                if (pY) parts.add("pY");
                if (nY) parts.add("nY");
                if (pZ) parts.add("nZ"); // mirrors original (pZ -> nZ)
                if (nZ) parts.add("pZ"); // mirrors original (nZ -> pZ)

                if (!pX && !pY && !pZ) parts.add("ppn");
                if (!pX && !pY && !nZ) parts.add("ppp");
                if (!nX && !pY && !pZ) parts.add("npn");
                if (!nX && !pY && !nZ) parts.add("npp");
                if (!pX && !nY && !pZ) parts.add("pnn");
                if (!pX && !nY && !nZ) parts.add("pnp");
                if (!nX && !nY && !pZ) parts.add("nnn");
                if (!nX && !nY && !nZ) parts.add("nnp");
            }
        }
        return Set.copyOf(parts);
    }
}
