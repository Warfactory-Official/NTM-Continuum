package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.machine.storage.TileEntityMachineFENSU;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemBER;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/** FEnSU: a dyed casing, the flywheel turning on the X axis, and the status lights over it. */
public final class FensuRenderer extends GemBER<TileEntityMachineFENSU, FensuRenderState> {

    private static final Map<DyeColor, Surface> SKINS = new EnumMap<>(DyeColor.class);

    private final WavefrontObj model = WavefrontObj.load(
            Ids.of(Constants.MOD_ID, "models/machines/fensu.obj"));

    @Override
    protected void setup(Gem.Model<FensuRenderState> declaration) {
        Gem.Part<FensuRenderState> machine = declaration.offScreen().viewDistance(256)
                .joint()
                .translate(0.5F, 0F, 0.5F)
                .angle(Gem.Axes.Y, state -> state.yaw);

        machine.child(model.part("Base"), skinOf(DyeColor.LIGHT_BLUE)).skin(FensuRenderer::skin);
        machine.child(model.part("Disc"), skinOf(DyeColor.LIGHT_BLUE)).skin(FensuRenderer::skin)
                .spin(Gem.Axes.X, 0F, 2.5F, 0F, state -> state.rotation);
        machine.child(model.part("Lights"), skinOf(DyeColor.LIGHT_BLUE)).skin(FensuRenderer::skin)
                .light(state -> FoundryRenderHelper.FULLBRIGHT);
    }

    private static Surface skin(FensuRenderState state) {
        return skinOf(state.color);
    }

    private static Surface skinOf(DyeColor color) {
        return SKINS.computeIfAbsent(color, dye -> Surface.cutout(
                Constants.MOD_ID + ":textures/models/machines/fensus/fensu_" + dye.getName() + ".png"));
    }

    @Override
    public FensuRenderState createRenderState() {
        return new FensuRenderState();
    }

    @Override
    public void extract(TileEntityMachineFENSU be, FensuRenderState state, float partialTicks,
                        Vec3 cameraPosition) {
        BlockState blockState = be.getBlockState();
        int meta = MachineCore.facingOrdinal(blockState, 2);
        state.yaw = switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
        state.rotation = be.prevRotation + (be.rotation - be.prevRotation) * partialTicks;
        state.color = be.color;
    }
}
