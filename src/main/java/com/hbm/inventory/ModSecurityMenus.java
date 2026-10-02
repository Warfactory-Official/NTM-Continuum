package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerMachineKeyForge;

/** Feature-local position-bound menus: the locksmith table. */
public final class ModSecurityMenus {
    private ModSecurityMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);
    public static final RegistryObject<MenuType<ContainerMachineKeyForge>> MACHINE_KEYFORGE =
            REG.menu("machine_keyforge", () -> Menus.typeWithPos(ContainerMachineKeyForge::new));

    public static void init() {}
}
