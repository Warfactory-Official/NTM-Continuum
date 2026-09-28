package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerCrateTungsten;
import com.hbm.inventory.container.ContainerFileCabinet;
import com.hbm.inventory.container.ContainerSafe;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Position-bound menus of the tungsten crate, the safe and the filing cabinet. */
public final class ModDecoCrateMenus {

    private ModDecoCrateMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerCrateTungsten>> CRATE_TUNGSTEN =
            MENUS.register("crate_tungsten", () -> Menus.typeWithPos(ContainerCrateTungsten::new));
    public static final RegistryObject<MenuType<ContainerSafe>> SAFE =
            MENUS.register("safe", () -> Menus.typeWithPos(ContainerSafe::new));
    public static final RegistryObject<MenuType<ContainerFileCabinet>> FILE_CABINET =
            MENUS.register("filing_cabinet", () -> Menus.typeWithPos(ContainerFileCabinet::new));

    public static void init() {}
}
