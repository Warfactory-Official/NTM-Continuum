package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The declared form of {@link MachineObjRenderer}: one OBJ body, one texture, one facing. Covers the
 * plain case only — no conditional frame, no footprint offset; the machines that need those keep using
 * {@link MachineObjRenderer}.
 */
public class GemMachineRenderer<T extends BlockEntity> extends GemBER<T, MachineRenderState> {

    private final WavefrontObj model;
    private final Surface surface;
    private final float baseYaw;

    public GemMachineRenderer(float baseYaw, String obj, String texture) {
        this.model = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/" + obj));
        this.surface = Surface.cutout(Constants.MOD_ID + ":textures/models/" + texture);
        this.baseYaw = baseYaw;
    }

    @Override
    protected void setup(Gem.Model<MachineRenderState> declaration) {
        declaration.offScreen().viewDistance(256)
                .part(model.all(), surface)
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> baseYaw + state.yaw);
    }

    @Override
    public MachineRenderState createRenderState() {
        return new MachineRenderState();
    }

    @Override
    public void extract(T blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        BlockState bs = blockEntity.getBlockState();
        int meta = bs.hasProperty(BlockDummyable.META) ? bs.getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        state.yaw = switch (meta - BlockDummyable.offset) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
    }
}
