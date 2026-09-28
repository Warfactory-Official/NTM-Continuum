package com.hbm.client.render;

import com.hbm.blocks.generic.LogicBlock;
import com.hbm.blocks.generic.LogicBlock.TileEntityLogicBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBER;
import dev.golem.core.render.CompatBlockRenderState;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Geometry;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code LogicBlock.getIcon(IBlockAccess, ...)}: the dungeon action block wears its disguise block's faces
 * when it has one and its own {@code logic_block} face otherwise; the invisible variant draws nothing. A block's
 * look cannot depend on its block entity in a baked model, so the face is drawn here.
 */
public class RenderLogicBlock implements CompatBER<TileEntityLogicBlock, RenderLogicBlock.State> {

    public static final class State extends CompatRenderState {
        public final CompatBlockRenderState disguise = new CompatBlockRenderState();
        boolean invisible;
        boolean disguised;
    }

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/blocks/logic_block.png");
    private static final Geometry CUBE = TubeBoxes.box("logic_block_cube", 0, 0, 0, 1, 1, 1, TubeBoxes.ALL, 0);

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(TileEntityLogicBlock logic, State state, float partialTicks, Vec3 cameraPosition) {
        state.invisible = logic.getBlockState().getBlock() instanceof LogicBlock block && block.isInvisible();
        state.disguised = logic.disguise != null;
        state.disguise.extract(logic.disguise == null ? null : logic.disguise.defaultBlockState());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        if (state.invisible) return;
        if (state.disguised && !state.disguise.isEmpty()) {
            sink.submitBlock(pose, state.disguise, state.lightCoords, OverlayTexture.NO_OVERLAY);
            return;
        }
        int light = state.lightCoords;
        sink.submitCustomGeometry(pose, CompatRenderTypes.entitySolid(TEXTURE),
                (p, buffer) -> CUBE.draw(p, buffer, light, OverlayTexture.NO_OVERLAY, -1));
    }
}
