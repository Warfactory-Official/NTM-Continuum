package com.hbm.client.render;

import com.hbm.blocks.machine.MachineFan;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Feature-owned client registration hook; the central renderer list calls this when integration lands. */
public final class IntakeFanRenderers {

    private IntakeFanRenderers() {}

    public static void register(HbmBlockEntityRenderers.Sink sink,
                                BlockEntityType<TileEntityMachineIntake> intake,
                                BlockEntityType<MachineFan.TileEntityFan> fan) {
        HbmBlockEntityRenderers.regGem(sink, intake, IntakeRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, fan, FanRenderer::new);
    }
}
