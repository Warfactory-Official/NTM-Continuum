package com.hbm.client.render;

import com.hbm.blocks.network.BlockCable;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws HBM's red cable as the real {@code cable_neo.obj} model, replacing the 1.12.2 baked model
 * ({@code BlockCableBakedModel}). Reads the six connection booleans from the {@link BlockCable} state
 * and draws the matching OBJ parts: a {@code Core} plus an arm per connected side, with the
 * straight-through optimisations {@code CX}/{@code CY}/{@code CZ} for a cable that only connects along
 * one axis. (The block renders {@code INVISIBLE}; this BER draws it.)
 *
 * <p>Mirrors the original part selection exactly, including the Z-axis quirk (a NORTH connection draws
 * the {@code posZ} part, SOUTH draws {@code negZ}).
 */
public class CableObjRenderer<T extends BlockEntity> extends GemBER<T, CableRenderState> {

    private static final Identifier OBJ = Ids.of("hbm", "models/blocks/cable_neo.obj");
    private static final String[] PARTS = {"CX", "CY", "CZ", "Core", "posX", "negX", "posY", "negY", "posZ", "negZ"};

    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final Surface surface = Surface.cutout("hbm:textures/models/blocks/cable_neo.png");

    /**
     * Every piece the cable can show, each behind the connection that calls for it. The OBJ is centred
     * on the origin (-0.5..0.5), hence the half-block shift.
     */
    @Override
    protected void setup(Gem.Model<CableRenderState> declaration) {
        for (String name : PARTS) {
            declaration.part(model.part(name), surface)
                    .visibleIf(state -> state.parts.contains(name))
                    .translate(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    public CableRenderState createRenderState() {
        return new CableRenderState();
    }

    @Override
    public void extract(T be, CableRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        boolean pX = bs.getValue(BlockCable.EAST);
        boolean nX = bs.getValue(BlockCable.WEST);
        boolean pY = bs.getValue(BlockCable.UP);
        boolean nY = bs.getValue(BlockCable.DOWN);
        boolean pZ = bs.getValue(BlockCable.SOUTH);
        boolean nZ = bs.getValue(BlockCable.NORTH);
        state.parts = selectParts(pX, nX, pY, nY, pZ, nZ);
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
        return parts;
    }
}
