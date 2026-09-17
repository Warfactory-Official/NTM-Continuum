package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** OBJ renderer for the legacy two-fan Chemical Factory. */
public final class ChemicalFactoryRenderer
        extends GemBER<TileEntityMachineChemicalFactory, ChemicalFactoryRenderState> {

    private static final float BASE_YAW = 90F;
    private final WavefrontObj model = WavefrontObj.load(
            dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/chemical_factory.obj"));
    private final Surface surface = Surface.cutout(
            Constants.MOD_ID + ":textures/models/machines/chemical_factory.png");

    @Override
    protected void setup(Gem.Model<ChemicalFactoryRenderState> declaration) {
        Gem.Part<ChemicalFactoryRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> BASE_YAW + state.yaw);

        machine.child(model.part("Base"), surface);
        machine.child(model.part("Frame"), surface)
                .visibleIf(state -> state.showFrame);
        machine.child(model.part("Fan1"), surface)
                .spin(Gem.Axes.Y, 1F, 0F, 0F, ChemicalFactoryRenderer::fan);
        machine.child(model.part("Fan2"), surface)
                .spin(Gem.Axes.Y, -1F, 0F, 0F, ChemicalFactoryRenderer::fan);
    }

    private static float fan(ChemicalFactoryRenderState state) {
        return (float) ((-state.anim * 45D) % 360D);
    }

    @Override public ChemicalFactoryRenderState createRenderState() { return new ChemicalFactoryRenderState(); }

    @Override
    public void extract(TileEntityMachineChemicalFactory be, ChemicalFactoryRenderState state,
                        float partialTicks, Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 4 -> 90F;
            case 3 -> 180F;
            case 5 -> 270F;
            default -> 0F;
        };
        state.showFrame = be.frame;
        state.anim = be.prevAnim + (be.anim - be.prevAnim) * partialTicks;
    }

}
