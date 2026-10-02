package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerDroneCrate;
import com.hbm.inventory.container.ContainerDroneDock;
import com.hbm.inventory.container.ContainerDroneProvider;
import com.hbm.inventory.container.ContainerDroneRequester;

/** Feature-local position-bound menus for the delivery drone blocks. */
public final class ModDroneMenus {
    private ModDroneMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerDroneCrate>> DRONE_CRATE =
            REG.menu("drone_crate", () -> Menus.typeWithPos(ContainerDroneCrate::new));
    public static final RegistryObject<MenuType<ContainerDroneDock>> DRONE_DOCK =
            REG.menu("drone_dock", () -> Menus.typeWithPos(ContainerDroneDock::new));
    public static final RegistryObject<MenuType<ContainerDroneProvider>> DRONE_PROVIDER =
            REG.menu("drone_crate_provider", () -> Menus.typeWithPos(ContainerDroneProvider::new));
    public static final RegistryObject<MenuType<ContainerDroneRequester>> DRONE_REQUESTER =
            REG.menu("drone_crate_requester", () -> Menus.typeWithPos(ContainerDroneRequester::new));

    public static void init() {}
}
