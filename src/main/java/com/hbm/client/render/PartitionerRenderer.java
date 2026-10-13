package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.blocks.network.CranePartitioner;
import com.hbm.tileentity.network.TileEntityCranePartitioner;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderPartitioner}: the {@code crane_buffer.obj} parts, each wearing its own block sprite. */
public class PartitionerRenderer extends GemBER<TileEntityCranePartitioner, PartitionerRenderer.State> {

    public static class State extends CompatRenderState {
        public float yaw;
    }

    private static final String T = Constants.MOD_ID + ":textures/blocks/";

    private final WavefrontObj model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/blocks/crane_buffer.obj"));

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen().viewDistance(256);
        Gem.Part<State> root = declaration.joint().translate(0.5F, 0F, 0.5F).angle(Gem.Axes.Y, state -> state.yaw);
        root.child(model.part("Side"), Surface.cutout(T + "crane_partitioner_side.png"));
        root.child(model.part("Back"), Surface.cutout(T + "crane_partitioner_back.png"));
        root.child(model.part("Top_Top.001"), Surface.cutout(T + "crane_top.png"));
        root.child(model.part("Inner"), Surface.cutout(T + "crane_splitter_inner.png"));
        root.child(model.part("InnerSide"), Surface.cutout(T + "crane_splitter_inner_side.png"));
        root.child(model.part("Belt"), Surface.cutout(T + "crane_splitter_belt_still.png"));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityCranePartitioner be, State state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = be.getBlockState();
        // 1.7: meta 2 -> 90, 4 -> 180, 5 -> 0, 3 -> 270
        state.yaw = !bs.hasProperty(CranePartitioner.FACING) ? 0F : switch (bs.getValue(CranePartitioner.FACING)) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
    }
}
