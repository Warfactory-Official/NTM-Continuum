package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import com.hbm.inventory.container.ContainerMachineRTG;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Feature-local menu registration for the grey RTG. */
public final class ModRtgMenus {
    private ModRtgMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerMachineRTG>> MACHINE_RTG = REG.menu(
            "machine_rtg_grey", () -> Menus.typeWithPos(ContainerMachineRTG::new));

    public static void init() {}
}
