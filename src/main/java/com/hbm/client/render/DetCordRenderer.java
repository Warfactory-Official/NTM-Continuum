package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.bomb.DetCord;
import com.hbm.tileentity.bomb.TileEntityDetCord;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code RenderDetCord}: the red cable's {@code cable_neo.obj} with the det cord icon, parts chosen
 * from the six connection flags exactly as upstream chose them (straight-through {@code CX}/{@code CY}/
 * {@code CZ} for a single axis, otherwise a core plus an arm per connection, including upstream's
 * Z quirk of drawing {@code posZ} for a NORTH connection and {@code negZ} for SOUTH).
 */
public final class DetCordRenderer extends GemBER<TileEntityDetCord, DetCordRenderer.State> {

    private static final String[] PARTS = {"CX", "CY", "CZ", "Core", "posX", "negX", "posY", "negY", "posZ", "negZ"};

    private final WavefrontObj model = WavefrontObj.load(Ids.of("hbm", "models/blocks/cable_neo.obj"));
    private final Surface surface = Surface.cutout("hbm:textures/blocks/det_cord.png");

    @Override
    protected void setup(Gem.Model<State> declaration) {
        for (String name : PARTS) {
            declaration.part(model.part(name), surface)
                    .visibleIf(state -> state.parts.contains(name))
                    .translate(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityDetCord be, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        boolean pX = bs.getValue(DetCord.EAST);
        boolean nX = bs.getValue(DetCord.WEST);
        boolean pY = bs.getValue(DetCord.UP);
        boolean nY = bs.getValue(DetCord.DOWN);
        boolean pZ = bs.getValue(DetCord.SOUTH);
        boolean nZ = bs.getValue(DetCord.NORTH);
        state.parts = selectParts(pX, nX, pY, nY, pZ, nZ);
    }

    /** 1.7 renderWorldBlock's mask table, verbatim. */
    static Set<String> selectParts(boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        int mask = (pX ? 32 : 0) + (nX ? 16 : 0) + (pY ? 8 : 0) + (nY ? 4 : 0) + (pZ ? 2 : 0) + (nZ ? 1 : 0);
        if (mask == 0b110000 || mask == 0b100000 || mask == 0b010000) return Set.of("CX");
        if (mask == 0b001100 || mask == 0b001000 || mask == 0b000100) return Set.of("CY");
        if (mask == 0b000011 || mask == 0b000010 || mask == 0b000001) return Set.of("CZ");
        Set<String> parts = new HashSet<>();
        parts.add("Core");
        if (pX) parts.add("posX");
        if (nX) parts.add("negX");
        if (pY) parts.add("posY");
        if (nY) parts.add("negY");
        if (nZ) parts.add("posZ");
        if (pZ) parts.add("negZ");
        return parts;
    }

    public static final class State extends CompatRenderState {
        Set<String> parts = Set.of("Core");
    }
}
