package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.FoundryChannel;
import com.hbm.tileentity.machine.TileEntityFoundryChannel;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer for the foundry channel (1.12.2 {@code RenderFoundryChannel}): draws the molten metal level
 * inside the gutter — a brightened tint of the material's {@code moltenColor} on {@code lava_gray},
 * covering the centre plus each connected arm at {@code y = 0.125 + amount/capacity × 0.25}.
 */
public class FoundryChannelRenderer extends GemBER<TileEntityFoundryChannel, FoundryChannelRenderer.State> {

    public static class State extends CompatRenderState {
        public boolean hasMelt;
        public int color = 0xFFFFFF;
        public float y;
        public int connections;
    }

    /** Emissive = uniform fullbright, no per-face shading (the old lighting-off molten quads). */
    private static final Surface MELT =
            Surface.emissive(Constants.MOD_ID + ":textures/models/machines/lava_gray.png");

    private static final Geometry CENTRE = surface("centre", .375F, .375F, .625F, .625F);
    private static final Geometry EAST = surface("east", .625F, .3125F, 1F, .6875F);
    private static final Geometry WEST = surface("west", 0F, .3125F, .375F, .6875F);
    private static final Geometry SOUTH = surface("south", .3125F, .625F, .6875F, 1F);
    private static final Geometry NORTH = surface("north", .3125F, 0F, .6875F, .375F);

    private static Geometry surface(String name, float x0, float z0, float x1, float z1) {
        return Quads.named(Constants.MOD_ID + ":foundry_channel_" + name).normal(0F, 1F, 0F)
                .vertex(x0, 0F, z0, 0F, 0F).vertex(x0, 0F, z1, 0F, 1F)
                .vertex(x1, 0F, z1, 1F, 1F).vertex(x1, 0F, z0, 1F, 0F)
                .build();
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> melt = declaration.light(state -> FoundryRenderHelper.FULLBRIGHT)
                .joint()
                .visibleIf(state -> state.hasMelt)
                .slide(Gem.Axes.Y, state -> state.y);

        arm(melt, CENTRE, 0);
        arm(melt, EAST, 1);
        arm(melt, WEST, 2);
        arm(melt, SOUTH, 4);
        arm(melt, NORTH, 8);
    }

    /** The centre, at mask 0, always shows; an arm shows when the connection mask names it. */
    private static void arm(Gem.Part<State> melt, Geometry geometry, int mask) {
        melt.child(geometry, MELT)
                .visibleIf(state -> mask == 0 || (state.connections & mask) != 0)
                .tint(state -> 0xFF000000 | state.color);
    }

    @Override public State createRenderState() { return new State(); }
    @Override public boolean shouldRender(TileEntityFoundryChannel be, Vec3 cameraPosition) { return true; }

    @Override
    public void extract(TileEntityFoundryChannel be, State state, float partialTicks, Vec3 cameraPosition) {
        state.hasMelt = be.type != null && be.amount > 0;
        if (!state.hasMelt) return;

        int hex = be.type.moltenColor;
        int r = (hex >> 16) & 0xFF, g = (hex >> 8) & 0xFF, b = hex & 0xFF;
        r = (int) (255 - (255 - r) * 0.7D);
        g = (int) (255 - (255 - g) * 0.7D);
        b = (int) (255 - (255 - b) * 0.7D);
        state.color = (r << 16) | (g << 8) | b;

        state.y = (float) (0.125D + be.amount * 0.25D / be.getCapacity());
        BlockState bs = be.getBlockState();
        state.connections = bs.hasProperty(FoundryChannel.META) ? bs.getValue(FoundryChannel.META) : 0;
    }
}
