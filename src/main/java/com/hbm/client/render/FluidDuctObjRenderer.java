package com.hbm.client.render;

import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.client.model.WavefrontObj;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws HBM's {@code fluid_duct_mk2} as the real {@code pipe_neo.obj} model (replacing the 1.12.2
 * {@code DuctNeoBakedModel}). Reads the six connection booleans from the {@link BlockFluidDuct} state to
 * pick OBJ parts (straight run on one axis, else connected arms + corner cubes), then draws each part
 * with the metal {@code fluid_duct_mk2} base sprite and — for a typed duct — again with the
 * {@code fluid_duct_mk2_overlay} sprite vertex-tinted by the carried fluid's colour. The overlay is
 * mostly transparent, so only its small "fluid window" band shows as a coloured indicator (faithful to
 * the 1.12.2 {@code DuctNeoBakedModel}, which bakes a base + overlay quad per face). The block renders
 * {@code INVISIBLE}; this BER draws it, like {@link CableObjRenderer}.
 */
public class FluidDuctObjRenderer extends GemBER<TileEntityFluidDuctNT, FluidDuctRenderState> {

    private static final Identifier OBJ = dev.golem.core.Ids.of("hbm", "models/blocks/pipe_neo.obj");
    private static final String[] PARTS = {
            "pX", "nX", "pY", "nY", "pZ", "nZ",
            "ppn", "ppp", "npn", "npp", "pnn", "pnp", "nnn", "nnp"};

    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final Surface base = Surface.cutout("hbm:textures/models/blocks/fluid_duct_mk2.png");
    private final Surface overlay = Surface.cutout("hbm:textures/models/blocks/fluid_duct_mk2_overlay.png");

    /**
     * The base metal pass, then — for a typed duct — the same geometry again in the overlay sprite,
     * tinted by the fluid colour. The overlay is mostly transparent, so only its small "fluid window"
     * band shows as a coloured indicator, which is what the 1.12.2 DuctNeoBakedModel drew per face. It
     * is pushed a hair outward because it is the same geometry as the base and would otherwise z-fight
     * with it. The OBJ is centred on the origin (-0.5..0.5), hence the half-block shift.
     */
    @Override
    protected void setup(Gem.Model<FluidDuctRenderState> declaration) {
        for (String name : PARTS) {
            declaration.part(model.part(name), base)
                    .visibleIf(state -> state.parts.contains(name))
                    .translate(0.5F, 0.5F, 0.5F);
        }
        for (String name : PARTS) {
            declaration.part(model.part(name), overlay)
                    .visibleIf(state -> state.fluidColor != 0 && state.parts.contains(name))
                    .tint(state -> state.fluidColor)
                    .translate(0.5F, 0.5F, 0.5F)
                    .scale(1.02F);
        }
    }

    @Override
    public FluidDuctRenderState createRenderState() {
        return new FluidDuctRenderState();
    }

    @Override
    public void extract(TileEntityFluidDuctNT be, FluidDuctRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        boolean pX = bs.getValue(BlockFluidDuct.EAST);
        boolean nX = bs.getValue(BlockFluidDuct.WEST);
        boolean pY = bs.getValue(BlockFluidDuct.UP);
        boolean nY = bs.getValue(BlockFluidDuct.DOWN);
        boolean pZ = bs.getValue(BlockFluidDuct.SOUTH);
        boolean nZ = bs.getValue(BlockFluidDuct.NORTH);
        state.parts = selectParts(pX, nX, pY, nY, pZ, nZ);
        // A single un-connected NONE duct still draws its core (all parts) so the placed block is visible.
        if (state.parts.isEmpty()) state.parts = selectParts(false, false, false, false, false, false);
        state.fluidColor = be.getFluidType() == Fluids.NONE ? 0 : (0xFF000000 | (be.getFluidType().getColor() & 0xFFFFFF));
    }

    /** The 1.12.2 {@code DuctNeoBakedModel.buildWorldQuads} part selection, verbatim. */
    private static Set<String> selectParts(boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
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
        return parts;
    }
}
