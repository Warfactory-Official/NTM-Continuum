package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.tileentity.machine.TileEntityMachineRadiolysis;
import com.hbm.tileentity.MachineCore;
import net.minecraft.world.phys.Vec3;

/**
 * The radiolysis chamber body. The old TESR turned N=180, S=0, W=90, E=270 with culling off, which is a
 * mirror of the standard map, so the yaw is restated here rather than a base rotation.
 */
public final class RenderRadiolysis extends MachineObjRenderer<TileEntityMachineRadiolysis> {

    public RenderRadiolysis() {
        super(0F, 1F, new ModelDef(dev.golem.core.Ids.of(Constants.MOD_ID, "models/machines/radiolysis.obj"),
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/machines/radiolysis.png"), true));
    }

    @Override
    public void extract(TileEntityMachineRadiolysis blockEntity, MachineRenderState state, float partialTicks, Vec3 cameraPosition) {
        super.extract(blockEntity, state, partialTicks, cameraPosition);
        state.yaw = switch (MachineCore.facingOrdinal(blockEntity.getBlockState(), 2)) {
            case 2 -> 180F;
            case 4 -> 90F;
            case 5 -> 270F;
            default -> 0F;
        };
    }
}
