package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerMachineSatLinker;
import com.hbm.inventory.container.ContainerSatDock;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Position-bound menus of the satellite family. The sat designator's screen has no menu. */
public final class ModSatelliteMenus {

    private ModSatelliteMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerMachineSatLinker>> SATLINKER =
            MENUS.register("machine_satlinker", () -> Menus.typeWithPos(ContainerMachineSatLinker::new));
    public static final RegistryObject<MenuType<ContainerSatDock>> SAT_DOCK =
            MENUS.register("sat_dock", () -> Menus.typeWithPos(ContainerSatDock::new));

    public static void init() {}
}
