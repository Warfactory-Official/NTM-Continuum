package com.hbm.inventory;

import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerMachineSiren;

/** Feature-local position-bound menus: only the siren opens one. */
public final class ModPowerGenMenus {
    private ModPowerGenMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerMachineSiren>> SIREN =
            MENUS.register("machine_siren", () -> Menus.typeWithPos(ContainerMachineSiren::new));

    public static void init() {}
}
