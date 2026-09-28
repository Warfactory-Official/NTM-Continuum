package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RebarPlacerPreview;
import com.hbm.client.render.RenderLanternBehemoth;
import com.hbm.client.render.RenderLogicBlock;
import com.hbm.client.render.RenderLoot;
import com.hbm.client.render.RenderPedestalTile;
import com.hbm.client.render.RenderRebarFill;
import com.hbm.client.render.RenderSkeletonHolder;
import com.hbm.inventory.ModDungeonMenus;
import com.hbm.inventory.gui.GUIRebar;
import com.hbm.tileentity.ModDungeonBlockEntities;

/** Screen, block-entity renderers and the rebar placer's world preview of the dungeon family. */
public final class DungeonClientRegistrations {

    private DungeonClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModDungeonMenus.REBAR.get(), GUIRebar::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        sink.register(ModDungeonBlockEntities.PEDESTAL.get(), ctx -> new RenderPedestalTile());
        HbmBlockEntityRenderers.regGem(sink, ModDungeonBlockEntities.SKELETON_HOLDER.get(), RenderSkeletonHolder::new);
        sink.register(ModDungeonBlockEntities.LOOT.get(), ctx -> new RenderLoot());
        HbmBlockEntityRenderers.regGem(sink, ModDungeonBlockEntities.LANTERN_BEHEMOTH.get(), RenderLanternBehemoth::new);
        sink.register(ModDungeonBlockEntities.LOGIC_BLOCK.get(), ctx -> new RenderLogicBlock());
        sink.register(ModDungeonBlockEntities.REBAR.get(), ctx -> new RenderRebarFill());
        RebarPlacerPreview.init();
    }
}
