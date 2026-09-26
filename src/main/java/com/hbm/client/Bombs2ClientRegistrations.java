package com.hbm.client;

import com.hbm.Constants;
import com.hbm.blocks.bomb.NukeBombBlock;
import com.hbm.tileentity.ModBombs2BlockEntities;
import com.hbm.inventory.ModBombs2Menus;
import com.hbm.client.render.BombObjRenderer;
import com.hbm.client.render.HbmBlockEntityRenderers;
import dev.golem.core.Ids;
import net.minecraft.resources.Identifier;
import com.hbm.client.render.RenderNukeFstbmb;
import com.hbm.inventory.gui.GUINukeCustom;
import com.hbm.inventory.gui.GUINukeFleija;
import com.hbm.inventory.gui.GUINukeFstbmb;
import com.hbm.inventory.gui.GUINukePrototype;
import com.hbm.inventory.gui.GUINukeSolinium;

/** Feature-local client registrations for the special bombs. */
public final class Bombs2ClientRegistrations {

    private Bombs2ClientRegistrations() {}

    public static void registerScreens(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(ModBombs2Menus.NUKE_CUSTOM.get(), GUINukeCustom::new);
        sink.register(ModBombs2Menus.NUKE_FLEIJA.get(), GUINukeFleija::new);
        sink.register(ModBombs2Menus.NUKE_SOLINIUM.get(), GUINukeSolinium::new);
        sink.register(ModBombs2Menus.NUKE_PROTOTYPE.get(), GUINukePrototype::new);
        sink.register(ModBombs2Menus.NUKE_FSTBMB.get(), GUINukeFstbmb::new);
    }

    /**
     * Yaw tables are per legacy facing 2..5 in the port's numbering, transcribed from the 1.7 TESR switch the
     * same way the MK5 bombs were (1.7 meta 5/4/2/3 for port facing 2/3/4/5).
     */
    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        // RenderNukeCustom: Little Boy's OBJ in the CustomNuke skin, shifted two blocks like the Boy itself.
        HbmBlockEntityRenderers.regGem(sink, ModBombs2BlockEntities.NUKE_CUSTOM.get(),
                () -> new BombObjRenderer<>(obj("lilboy.obj"), tex("customnuke.png"), 1F,
                        NukeBombBlock.FACING, -2F, 0F, 0F, 0F, 180F, 90F, 270F));
        // RenderNukeFleija / RenderNukePrototype: 5 -> 90, 2 -> 180, 4 -> 270, 3 -> 0.
        HbmBlockEntityRenderers.regGem(sink, ModBombs2BlockEntities.NUKE_FLEIJA.get(),
                () -> new BombObjRenderer<>(obj("fleija.obj"), tex("fleija.png"), 1F,
                        NukeBombBlock.FACING, 0F, 0F, 0F, 90F, 270F, 180F, 0F));
        HbmBlockEntityRenderers.regGem(sink, ModBombs2BlockEntities.NUKE_PROTOTYPE.get(),
                () -> new BombObjRenderer<>(obj("prototype.obj"), tex("prototype.png"), 1F,
                        NukeBombBlock.FACING, 0F, 0F, 0F, 90F, 270F, 180F, 0F));
        // RenderNukeSolinium: a fixed 90 then 2 -> 90, 4 -> 180, 3 -> 270, 5 -> 0 - the same totals.
        HbmBlockEntityRenderers.regGem(sink, ModBombs2BlockEntities.NUKE_SOLINIUM.get(),
                () -> new BombObjRenderer<>(obj("ufp.obj"), tex("ufp.png"), 1F,
                        NukeBombBlock.FACING, 0F, 0F, 0F, 90F, 270F, 180F, 0F));
        HbmBlockEntityRenderers.regGem(sink, ModBombs2BlockEntities.NUKE_FSTBMB.get(), RenderNukeFstbmb::new);
    }

    private static Identifier obj(String file) {
        return Ids.of(Constants.MOD_ID, "models/bombs/" + file);
    }

    private static Identifier tex(String file) {
        return Ids.of(Constants.MOD_ID, "textures/models/bombs/" + file);
    }
}
