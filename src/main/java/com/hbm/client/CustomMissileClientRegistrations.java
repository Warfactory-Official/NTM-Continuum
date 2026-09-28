package com.hbm.client;

import com.hbm.client.render.CustomMissileRenderers;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.StructPreviewRenderer;
import com.hbm.inventory.ModCustomMissileMenus;
import com.hbm.inventory.gui.GUILaunchPadRusted;
import com.hbm.inventory.gui.GUIMachineCompactLauncher;
import com.hbm.inventory.gui.GUIMachineLaunchTable;
import com.hbm.inventory.gui.GUIMachineMissileAssembly;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import com.hbm.tileentity.machine.TileEntityMultiblock;

/** Feature-local client registrations for the custom missile system. */
public final class CustomMissileClientRegistrations {

    private CustomMissileClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModCustomMissileMenus.MISSILE_ASSEMBLY.get(), GUIMachineMissileAssembly::new);
        sink.register(ModCustomMissileMenus.COMPACT_LAUNCHER.get(), GUIMachineCompactLauncher::new);
        sink.register(ModCustomMissileMenus.LAUNCH_TABLE.get(), GUIMachineLaunchTable::new);
        sink.register(ModCustomMissileMenus.LAUNCH_PAD_RUSTED.get(), GUILaunchPadRusted::new);
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        sink.register(ModCustomMissileBlockEntities.MACHINE_MISSILE_ASSEMBLY.get(), ctx -> new CustomMissileRenderers.Assembly());
        sink.register(ModCustomMissileBlockEntities.COMPACT_LAUNCHER.get(), ctx -> new CustomMissileRenderers.Compact());
        sink.register(ModCustomMissileBlockEntities.LAUNCH_TABLE.get(), ctx -> new CustomMissileRenderers.Table());
        sink.register(ModCustomMissileBlockEntities.LAUNCH_PAD_LARGE.get(), ctx -> new CustomMissileRenderers.PadLarge());
        sink.register(ModCustomMissileBlockEntities.LAUNCH_PAD_RUSTED.get(), ctx -> new CustomMissileRenderers.PadRusted());
        // 1.7 RenderMultiblock: the ghost of what the core component still needs
        sink.register(ModCustomMissileBlockEntities.STRUCT_LAUNCHER_CORE.get(),
                ctx -> new StructPreviewRenderer<TileEntityMultiblock>());
        sink.register(ModCustomMissileBlockEntities.STRUCT_LAUNCHER_CORE_LARGE.get(),
                ctx -> new StructPreviewRenderer<TileEntityMultiblock>());
    }
}
