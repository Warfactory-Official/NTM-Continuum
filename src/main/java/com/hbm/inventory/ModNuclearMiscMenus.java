package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.registration.Reg;
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
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerRadiolysis>> RADIOLYSIS =
            REG.menu("machine_radiolysis", () -> Menus.typeWithPos(ContainerRadiolysis::new));
    public static final RegistryObject<MenuType<ContainerMachineReactorBreeding>> REACTOR_BREEDING =
            REG.menu("machine_reactor", () -> Menus.typeWithPos(ContainerMachineReactorBreeding::new));
    public static final RegistryObject<MenuType<ContainerReactorResearch>> REACTOR_RESEARCH =
            REG.menu("machine_reactor_small", () -> Menus.typeWithPos(ContainerReactorResearch::new));
    public static final RegistryObject<MenuType<ContainerReactorControl>> REACTOR_CONTROL =
            REG.menu("machine_controller", () -> Menus.typeWithPos(ContainerReactorControl::new));

    public static void init() {}
}
