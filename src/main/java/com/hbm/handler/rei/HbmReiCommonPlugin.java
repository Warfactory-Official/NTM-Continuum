package com.hbm.handler.rei;

//? if rei {

import com.hbm.handler.jei.HbmViewerSubtypes;
import dev.golem.core.rei.ReiCommonPlugin;

/**
 * HBM's REI common plugin: registers {@link HbmViewerSubtypes} as REI item comparators, so fluid icons, tanks,
 * blueprints and the other component-backed families stay distinct entries (JEI's subtypes). Server-safe. Loader glue
 * only: {@code @REIPluginCommon} on Forge-likes, the {@code rei_common} entrypoint on Fabric.
 */
//? if forgelike {
@me.shedaniel.rei.forge.REIPluginCommon
//?}
public final class HbmReiCommonPlugin extends ReiCommonPlugin {

    public HbmReiCommonPlugin() {
        super(HbmViewerSubtypes::register);
    }
}
//?}
