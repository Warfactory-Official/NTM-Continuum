package com.hbm.inventory;

import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerMachineReactorBreeding;
import com.hbm.inventory.container.ContainerRadiolysis;
import com.hbm.inventory.container.ContainerReactorControl;
import com.hbm.inventory.container.ContainerReactorResearch;

/** Feature-local position-bound menus. */
public final class ModNuclearMiscMenus {
    private ModNuclearMiscMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerRadiolysis>> RADIOLYSIS =
            MENUS.register("machine_radiolysis", () -> Menus.typeWithPos(ContainerRadiolysis::new));
    public static final RegistryObject<MenuType<ContainerMachineReactorBreeding>> REACTOR_BREEDING =
            MENUS.register("machine_reactor", () -> Menus.typeWithPos(ContainerMachineReactorBreeding::new));
    public static final RegistryObject<MenuType<ContainerReactorResearch>> REACTOR_RESEARCH =
            MENUS.register("machine_reactor_small", () -> Menus.typeWithPos(ContainerReactorResearch::new));
    public static final RegistryObject<MenuType<ContainerReactorControl>> REACTOR_CONTROL =
            MENUS.register("machine_controller", () -> Menus.typeWithPos(ContainerReactorControl::new));

    public static void init() {}
}
