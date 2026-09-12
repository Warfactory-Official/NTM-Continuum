package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.phys.Vec3;

/** Strand Caster BER: old caster/plate parts plus a fullbright tinted molten surface. */
public class StrandCasterRenderer extends GemBER<TileEntityMachineStrandCaster, StrandCasterRenderer.State> {

    public static class State extends CompatRenderState {
        public ForgeDirection direction = ForgeDirection.NORTH;
        public boolean hasMelt;
        public int color;
        public float level;
        public float plateOffset;
    }

    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/strand_caster.obj"));
    private final Surface body = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/strand_caster.png");
    private static final Surface LAVA =
            Surface.emissive(Constants.MOD_ID + ":textures/models/machines/lava_gray.png");

    private static final Geometry MELT = Quads.named(Constants.MOD_ID + ":strand_caster_melt")
            .normal(0F, 1F, 0F)
            .vertex(-0.9F, 0F, -0.999F, 0F, 0F).vertex(-0.9F, 0F, 0.999F, 0F, 1F)
            .vertex(0.9F, 0F, 0.999F, 1F, 1F).vertex(0.9F, 0F, -0.999F, 1F, 0F)
            .build();

    @Override
    protected void setup(Gem.Model<State> declaration) {
        Gem.Part<State> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> yaw(state.direction))
                .translate(0.5F, 0F, 0.5F)
                .rotate(Gem.Axes.Y, 180F);

        machine.child(model.part("caster"), body);
        machine.child(model.part("plate"), body)
                .visibleIf(state -> state.hasMelt)
                .slide(Gem.Axes.Z, state -> Math.max(-state.plateOffset + 3.4F, 0F));

        machine.child(MELT, LAVA)
                .visibleIf(state -> state.hasMelt)
                .slide(Gem.Axes.Y, state -> 2.3F + state.level)
                .tint(state -> state.color)
                .light(state -> FoundryRenderHelper.FULLBRIGHT);
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extract(TileEntityMachineStrandCaster be, State state, float partialTicks, Vec3 cameraPosition) {
        int meta = be.getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset;
        state.direction = ForgeDirection.getOrientation(meta);
        state.hasMelt = be.amount > 0 && be.type != null && be.getInstalledMold() != null;
        if (state.hasMelt) {
            state.color = 0xFF000000 | (be.type.moltenColor & 0xFFFFFF);
            state.level = (float) (be.amount * 0.675D / Math.max(be.getCapacity(), 1));
            state.plateOffset = (float) (be.amount * 0.375D / Math.max(be.getInstalledMold().getCost(), 1));
        }
    }

    /** The melt surface is one quad whose height is the fill level, built rather than placed. */

    private static float yaw(ForgeDirection direction) {
        return switch (direction) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }
}
