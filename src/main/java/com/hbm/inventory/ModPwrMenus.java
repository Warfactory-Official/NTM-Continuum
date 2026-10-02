package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerPWR;

/** Feature-local position-bound menu for the PWR controller. */
public final class ModPwrMenus {
    private ModPwrMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);
    public static final RegistryObject<MenuType<ContainerPWR>> PWR_CONTROLLER =
            REG.menu("pwr_controller", () -> Menus.typeWithPos(ContainerPWR::new));

    public static void init() {}
}
