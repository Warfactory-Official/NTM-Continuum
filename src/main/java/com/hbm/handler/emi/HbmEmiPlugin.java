package com.hbm.handler.emi;

//? if emi {
/*import com.hbm.handler.jei.HbmRecipeViewer;
import com.hbm.handler.jei.HbmViewerSubtypes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.golem.core.emi.EmiViewerPlugin;

/^*
 * HBM's EMI plugin: the declarations the JEI and REI plugins use, fed to EMI by Golem Core. EMI finds it by
 * {@code @EmiEntrypoint} on Forge-likes and by the {@code emi} entrypoint in {@code fabric.mod.json}.
 ^/
@EmiEntrypoint
public final class HbmEmiPlugin extends EmiViewerPlugin {

    public HbmEmiPlugin() {
        super(HbmRecipeViewer.INSTANCE, HbmViewerSubtypes::register);
    }
}
*///?}
