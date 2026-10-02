package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerMachineCustom;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.registration.Reg;

/** The custom machine controller's position-bound menu. */
public final class ModCustomMachineMenus {

    private ModCustomMachineMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerMachineCustom>> CUSTOM_MACHINE =
            REG.menu("custom_machine", () -> Menus.typeWithPos(ContainerMachineCustom::new));

    public static void init() {}
}
