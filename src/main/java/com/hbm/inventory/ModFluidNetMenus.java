package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerBarrel;

/** Position-bound menus of the fluid-network family. */
public final class ModFluidNetMenus {

    private ModFluidNetMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerBarrel>> BARREL =
            REG.menu("barrel", () -> Menus.typeWithPos(ContainerBarrel::new));

    public static void init() {}
}
