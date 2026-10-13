package com.hbm.client.render;

import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.network.FluidDuctNeo;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws the 1.7 {@code fluid_duct_neo} in its three skins ({@code pipe_neo}, {@code pipe_silver},
 * {@code pipe_colored}): the {@code pipe_neo.obj} parts picked from the connection flags, the base
 * sprite, and the {@code _overlay} sprite tinted by the fluid colour (NONE's grey when empty). The skin comes
 * from the block, so the three blocks share one block-entity type and this one renderer.
 */
public class NeoDuctRenderer<T extends TileEntityFluidDuctNT> extends GemBER<T, NeoDuctRenderer.State> {

    public static class State extends CompatRenderState {
        public Set<String> parts = Set.of();
        public int fluidColor = 0;
        public int skin = 0;
    }

    private static final Identifier OBJ = Ids.of("hbm", "models/blocks/pipe_neo.obj");
    private static final String[] PARTS = {
            "pX", "nX", "pY", "nY", "pZ", "nZ",
            "ppn", "ppp", "npn", "npp", "pnn", "pnp", "nnn", "nnp"};
    private static final String[] SKINS = {"pipe_neo", "pipe_silver", "pipe_colored"};

    private final WavefrontObj model = WavefrontObj.load(OBJ);
    private final Surface[] base = new Surface[SKINS.length];
    private final Surface[] overlay = new Surface[SKINS.length];

    public NeoDuctRenderer() {
        for (int i = 0; i < SKINS.length; i++) {
            base[i] = Surface.cutout("hbm:textures/blocks/" + SKINS[i] + ".png");
            overlay[i] = Surface.cutout("hbm:textures/blocks/" + SKINS[i] + "_overlay.png");
        }
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        for (String name : PARTS) {
            declaration.part(model.part(name), base[0])
                    .skin(state -> base[state.skin])
                    .visibleIf(state -> state.parts.contains(name))
                    .translate(0.5F, 0.5F, 0.5F);
        }
        for (String name : PARTS) {
            declaration.part(model.part(name), overlay[0])
                    .skin(state -> overlay[state.skin])
                    .visibleIf(state -> state.fluidColor != 0 && state.parts.contains(name))
                    .tint(state -> state.fluidColor)
                    .translate(0.5F, 0.5F, 0.5F)
                    .scale(1.02F);
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T be, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        boolean pX = bs.getValue(BlockFluidDuct.EAST);
        boolean nX = bs.getValue(BlockFluidDuct.WEST);
        boolean pY = bs.getValue(BlockFluidDuct.UP);
        boolean nY = bs.getValue(BlockFluidDuct.DOWN);
        boolean pZ = bs.getValue(BlockFluidDuct.SOUTH);
        boolean nZ = bs.getValue(BlockFluidDuct.NORTH);
        state.parts = selectParts(pX, nX, pY, nY, pZ, nZ);
        if (state.parts.isEmpty()) state.parts = selectParts(false, false, false, false, false, false);
        // 1.7 RenderTestPipe.renderDuct draws the overlay for every duct, empty ones in NONE's 0x888888: the
        // pipe_colored skin is all overlay (its base sprite is only the bolt), so skipping NONE hid empty ducts.
        state.fluidColor = 0xFF000000 | (be.getFluidType().getColor() & 0xFFFFFF);
        state.skin = bs.getBlock() instanceof FluidDuctNeo neo ? Math.min(neo.skin, SKINS.length - 1) : 0;
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
        return parts;
    }
}
