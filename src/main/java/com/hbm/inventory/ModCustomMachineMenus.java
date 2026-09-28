package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerMachineCustom;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** The custom machine controller's position-bound menu. */
public final class ModCustomMachineMenus {

    private ModCustomMachineMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerMachineCustom>> CUSTOM_MACHINE =
            MENUS.register("custom_machine", () -> Menus.typeWithPos(ContainerMachineCustom::new));

    public static void init() {}
}
