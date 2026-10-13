package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * A single block that is one OBJ body standing still.
 *
 * <p>{@link GemMachineRenderer} states the same thing for a machine and takes its facing from the
 * multiblock core's own property. A plain block states its facing the vanilla way, which that lookup
 * cannot see: it answers {@code UNKNOWN} and the model would sit at a fixed yaw whichever way the block
 * was placed. This reads {@link BlockStateProperties#HORIZONTAL_FACING} and maps it through 1.7's table:
 * north 0°, west 90°, south 180°, east 270°, and leaves a block that has no facing at zero.
 *
 * <p>The body is placed at the cell centre because these meshes are authored about the origin, which is
 * the frame 1.7's own handlers compensate for: a {@code TileEntitySpecialRenderer} opens with
 * {@code translate(x + 0.5, y, z + 0.5)} and an {@code ISimpleBlockRenderingHandler} adds the same to the
 * tessellator. A mesh already authored in 0..1 block space must not come through here.
 */
public class BlockObjRenderer<T extends BlockEntity> extends GemBER<T, MachineRenderState> {

    private final WavefrontObj model;
    private final Surface surface;

    /** @param obj path under {@code models/}; @param texture path under the mod's assets root */
    public BlockObjRenderer(String obj, String texture) {
        this(obj, texture, false);
    }

    /** @param noCull for a body the old renderer drew with {@code GL_CULL_FACE} switched off */
    public BlockObjRenderer(String obj, String texture, boolean noCull) {
        this.model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/" + obj));
        String id = Constants.MOD_ID + ":" + texture;
        this.surface = noCull ? Surface.cutoutNoCull(id) : Surface.cutout(id);
    }

    @Override
    protected void setup(Gem.Model<MachineRenderState> declaration) {
        declaration.part(model.all(), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);
    }

    @Override
    public MachineRenderState createRenderState() {
        return new MachineRenderState();
    }

    @Override
    public void extract(T blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        if (!bs.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state.yaw = 0F;
            return;
        }
        state.yaw = switch (bs.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case WEST -> 90F;
            case SOUTH -> 180F;
            case EAST -> 270F;
            default -> 0F;
        };
    }
}
