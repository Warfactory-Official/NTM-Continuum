package com.hbm.handler.rei;

//? if rei {

import com.hbm.handler.jei.HbmRecipeViewer;
import com.hbm.handler.jei.HbmViewerSubtypes;
import dev.golem.core.rei.ReiClientPlugin;

/**
 * HBM's REI client plugin: the same recipe-viewer declarations the JEI plugin uses ({@link HbmRecipeViewer},
 * {@link HbmViewerSubtypes}), fed to REI by Golem Core's {@link ReiClientPlugin}. Loader glue only: REI finds it by the
 * {@code @REIPluginClient} annotation on Forge-likes and by the {@code rei_client} entrypoint in {@code fabric.mod.json}.
 *
 * <p>Both viewers may be installed at once; each lists HBM's categories once, from its own plugin. On Forge 1.20.1 the
 * JEI plugin opts out of "REI Plugin Compatibilities" (JEI plugins run inside REI), which would otherwise list them a
 * second time in REI.
 */
//? if forgelike {
@me.shedaniel.rei.forge.REIPluginClient
//?}
public final class HbmReiClientPlugin extends ReiClientPlugin {

    public HbmReiClientPlugin() {
        super(HbmRecipeViewer.INSTANCE, HbmViewerSubtypes::register);
    }
}
//?}
