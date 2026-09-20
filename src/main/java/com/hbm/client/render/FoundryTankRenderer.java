package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.machine.FoundryTank;
import com.hbm.tileentity.machine.TileEntityFoundryTank;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The molten pool of the 1.7 {@code RenderFoundryTank}: a fullbright {@code lava_gray} slab in the
 * brightened molten colour, from the floor (y = 0.125, or 0 over another basin) up by
 * {@code amount / capacity} of the basin's inner height (0.75, plus 0.125 for each vertically joined
 * basin), its top always drawn and its sides only toward horizontally joined basins. The walls and
 * floor are the block model ({@code blockstates/foundry_tank.json}).
 */
public class FoundryTankRenderer extends GemBER<TileEntityFoundryTank, FoundryTankRenderer.State> {

    public static class State extends CompatRenderState {
        public boolean hasMelt;
        public int color = 0xFFFFFFFF;
        public float bottom;
        public float height;
        public boolean[] side = new boolean[4]; // east, west, south, north
    }

    private static final Surface MELT = Surface.emissive(Constants.MOD_ID + ":textures/models/machines/lava_gray.png");

    private static final Geometry TOP = Quads.named("foundry_tank_melt_top").normal(0, 1, 0)
            .vertex(0, 1, 0, 0, 0).vertex(0, 1, 1, 0, 1).vertex(1, 1, 1, 1, 1).vertex(1, 1, 0, 1, 0).build();
    private static final Geometry EAST = Quads.named("foundry_tank_melt_e").normal(1, 0, 0)
            .vertex(1, 1, 1, 0, 0).vertex(1, 0, 1, 0, 1).vertex(1, 0, 0, 1, 1).vertex(1, 1, 0, 1, 0).build();
    private static final Geometry WEST = Quads.named("foundry_tank_melt_w").normal(-1, 0, 0)
            .vertex(0, 1, 0, 0, 0).vertex(0, 0, 0, 0, 1).vertex(0, 0, 1, 1, 1).vertex(0, 1, 1, 1, 0).build();
    private static final Geometry SOUTH = Quads.named("foundry_tank_melt_s").normal(0, 0, 1)
            .vertex(0, 1, 1, 0, 0).vertex(0, 0, 1, 0, 1).vertex(1, 0, 1, 1, 1).vertex(1, 1, 1, 1, 0).build();
    private static final Geometry NORTH = Quads.named("foundry_tank_melt_n").normal(0, 0, -1)
            .vertex(1, 1, 0, 0, 0).vertex(1, 0, 0, 0, 1).vertex(0, 0, 0, 1, 1).vertex(0, 1, 0, 1, 0).build();

    @Override
    protected void setup(Gem.Model<State> declaration) {
        // A unit slab scaled to the pool height and lifted to the floor.
        Gem.Part<State> pool = declaration.light(state -> FoundryRenderHelper.FULLBRIGHT)
                .joint()
                .visibleIf(state -> state.hasMelt)
                .slide(Gem.Axes.Y, state -> state.bottom)
                .stretch(Gem.Axes.Y, state -> state.height);
        pool.child(TOP, MELT).tint(state -> state.color);
        pool.child(EAST, MELT).visibleIf(state -> state.side[0]).tint(state -> state.color);
        pool.child(WEST, MELT).visibleIf(state -> state.side[1]).tint(state -> state.color);
        pool.child(SOUTH, MELT).visibleIf(state -> state.side[2]).tint(state -> state.color);
        pool.child(NORTH, MELT).visibleIf(state -> state.side[3]).tint(state -> state.color);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(TileEntityFoundryTank be, Vec3 cameraPosition) {
        return true;
    }

    @Override
    public void extract(TileEntityFoundryTank be, State state, float partialTicks, Vec3 cameraPosition) {
        state.hasMelt = be.type != null && be.amount > 0;
        if (!state.hasMelt) return;
        BlockState bs = be.getBlockState();
        boolean conNegY = bs.getValue(FoundryTank.DOWN), conPosY = bs.getValue(FoundryTank.UP);
        double max = 0.75D + (conNegY ? 0.125D : 0) + (conPosY ? 0.125D : 0);
        state.height = (float) Math.max(be.amount * max / Math.max(1, be.getCapacity()), 0.001D);
        state.bottom = conNegY ? 0F : 0.125F;
        state.side[0] = bs.getValue(FoundryTank.EAST);
        state.side[1] = bs.getValue(FoundryTank.WEST);
        state.side[2] = bs.getValue(FoundryTank.SOUTH);
        state.side[3] = bs.getValue(FoundryTank.NORTH);
        // 1.7: new Color(moltenColor).brighter(), then 70% toward white.
        int hex = be.type.moltenColor;
        int r = (hex >> 16) & 0xFF, g = (hex >> 8) & 0xFF, b = hex & 0xFF;
        r = Math.min(255, Math.max(r, 4) * 10 / 7);
        g = Math.min(255, Math.max(g, 4) * 10 / 7);
        b = Math.min(255, Math.max(b, 4) * 10 / 7);
        r = (int) (255 - (255 - r) * 0.7D);
        g = (int) (255 - (255 - g) * 0.7D);
        b = (int) (255 - (255 - b) * 0.7D);
        state.color = 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
