package com.hbm.client;

import com.hbm.Constants;
import com.hbm.client.render.BombObjRenderer;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.client.render.PrimedExplosiveRenderer;
import com.hbm.entity.explosive.ModExplosiveEntities;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.gui.GUINukeMike;
import com.hbm.tileentity.ModBlockEntities;

/** Feature-local client registrations; shared client wiring only needs to call these three methods. */
public final class BombClientRegistrations {

    private BombClientRegistrations() {}

    public static void registerScreens(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(ModMenus.NUKE_MIKE.get(), GUINukeMike::new);
    }

    public static void registerBlockEntityRenderers(HbmBlockEntityRenderers.Sink sink) {
        sink.register(ModBlockEntities.NUKE_MIKE.get(), context -> new BombObjRenderer<>(
                dev.golem.core.Ids.of(Constants.MOD_ID, "models/bombs/ivymike.obj"),
                dev.golem.core.Ids.of(Constants.MOD_ID, "textures/models/bombs/ivymike.png"),
                1F, com.hbm.blocks.bomb.NukeBombBlock.FACING, 0F, 0F, 0F,
                // 1.7 RenderNukeMike: 3 -> 0, 5 -> 90, 2 -> 180, 4 -> 270, remapped to the port's
                // opposite-facing numbering (1.7 meta 5/4/2/3 for port facing 2/3/4/5).
                90F, 270F, 180F, 0F));
    }

    public static void registerEntityRenderers(HbmEntityRenderers.Sink sink) {
        HbmEntityRenderers.regGem(sink, ModExplosiveEntities.PRIMED_EXPLOSIVE.get(), PrimedExplosiveRenderer::new);
    }
}
