package com.hbm.client;

import com.hbm.Constants;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.MachineObjRenderer;
import com.hbm.client.render.MachineObjRenderer.ModelDef;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.inventory.ModFluidNetMenus;
import com.hbm.blocks.network.FlowControlPump;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityBarrelDeco;
import com.hbm.tileentity.network.TileEntityFluidPump;
import com.hbm.client.render.BarrelRenderer;
import com.hbm.client.render.FoundryTankRenderer;
import com.hbm.client.render.RenderFlowControlPump;
import com.hbm.inventory.gui.GUIBarrel;
import com.hbm.inventory.gui.GUIPump;
import dev.golem.core.Ids;
import net.minecraft.client.Minecraft;

/** Client hooks for the fluid-network family: the barrel screen, the pump screen and the block renderers. */
public final class FluidNetClientRegistrations {

    private FluidNetClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModFluidNetMenus.BARREL.get(), GUIBarrel::new);
        FlowControlPump.OPEN_SCREEN = pump -> Minecraft.getInstance().setScreen(new GUIPump(pump));
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModFluidNetBlockEntities.BARREL.get(), BarrelRenderer<TileEntityBarrel>::new);
        HbmBlockEntityRenderers.regGem(sink, ModFluidNetBlockEntities.BARREL_DECO.get(), BarrelRenderer<TileEntityBarrelDeco>::new);
        HbmBlockEntityRenderers.regGem(sink, ModFluidNetBlockEntities.FOUNDRY_TANK.get(), FoundryTankRenderer::new);
        // The deprecated fluid pump: fluid_diode.obj; 1.7 yaw map N=180, E=90, S=0, W=270 = the standard map + 180.
        HbmBlockEntityRenderers.regGem(sink, ModFluidNetBlockEntities.PUMP.get(),
                () -> new MachineObjRenderer<TileEntityFluidPump>(180F, 1F,
                        new ModelDef(Ids.of(Constants.MOD_ID, "models/network/fluid_diode.obj"),
                                Ids.of(Constants.MOD_ID, "textures/models/network/fluid_diode.png"))));
        HbmBlockEntityRenderers.regGem(sink, ModFluidNetBlockEntities.FLOW_CONTROL_PUMP.get(), RenderFlowControlPump::new);
    }
}
