package com.hbm.client;

import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockSnowglobe;
import com.hbm.blocks.machine.RadioRec;
import com.hbm.client.render.BlockObjRenderer;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RenderBobble;
import com.hbm.client.render.RenderDecoBlock;
import com.hbm.client.render.RenderEmitter;
import com.hbm.client.render.RenderFileCabinet;
import com.hbm.client.render.RenderPlushie;
import com.hbm.client.render.RenderSnowglobe;
import com.hbm.client.render.RenderVendingMachine;
import com.hbm.inventory.ModDecoCrateMenus;
import com.hbm.inventory.gui.GUICrateTungsten;
import com.hbm.inventory.gui.GUIFileCabinet;
import com.hbm.inventory.gui.GUIRadioRec;
import com.hbm.inventory.gui.GUISafe;
import com.hbm.inventory.gui.GUIScreenBobble;
import com.hbm.inventory.gui.GUIScreenSnowglobe;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import com.hbm.tileentity.deco.TileEntityCanCrate;
import com.hbm.tileentity.machine.TileEntityBroadcaster;
import com.hbm.tileentity.machine.TileEntityRadioRec;
import com.hbm.tileentity.machine.TileEntityRadiobox;
import net.minecraft.client.Minecraft;

/** Screens and block-entity renderers of the storage crates, decorative props and simple blocks. */
public final class DecoCrateClientRegistrations {

    private DecoCrateClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModDecoCrateMenus.CRATE_TUNGSTEN.get(), GUICrateTungsten::new);
        sink.register(ModDecoCrateMenus.SAFE.get(), GUISafe::new);
        sink.register(ModDecoCrateMenus.FILE_CABINET.get(), GUIFileCabinet::new);
        // 1.7 opened these three client-side, with no container
        BlockBobble.OPEN_SCREEN = bobble -> Minecraft.getInstance().setScreen(new GUIScreenBobble(bobble));
        BlockSnowglobe.OPEN_SCREEN = globe -> Minecraft.getInstance().setScreen(new GUIScreenSnowglobe(globe));
        RadioRec.OPEN_SCREEN = radio -> Minecraft.getInstance().setScreen(new GUIRadioRec(radio));
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.CRATE_CAN.get(),
                () -> new BlockObjRenderer<TileEntityCanCrate>("blocks/conservecrate.obj", "textures/block/crate_can.png"));
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.FILE_CABINET.get(), RenderFileCabinet::new);
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.BOBBLE.get(), RenderBobble::new);
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.PLUSHIE.get(), RenderPlushie::new);
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.SNOWGLOBE.get(), RenderSnowglobe::new);
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.VENDING_MACHINE.get(), RenderVendingMachine::new);
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.EMITTER.get(), RenderEmitter::new);
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.BROADCASTER.get(),
                () -> new RenderDecoBlock<TileEntityBroadcaster>(RenderDecoBlock.Kind.BROADCASTER));
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.RADIOREC.get(),
                () -> new RenderDecoBlock<TileEntityRadioRec>(RenderDecoBlock.Kind.RADIOREC));
        HbmBlockEntityRenderers.regGem(sink, ModDecoCrateBlockEntities.RADIOBOX.get(),
                () -> new RenderDecoBlock<TileEntityRadiobox>(RenderDecoBlock.Kind.RADIOBOX));
    }
}
