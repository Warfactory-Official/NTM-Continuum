package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerAutocrafter;
import com.hbm.inventory.container.ContainerMachineRotaryFurnace;
import com.hbm.inventory.container.ContainerMicrowave;
import com.hbm.inventory.container.ContainerFunnel;

/** Feature-local position-bound menus for the production machines. */
public final class ModProductionMenus {
    private ModProductionMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerMachineRotaryFurnace>> ROTARY_FURNACE =
            REG.menu("machine_rotary_furnace", () -> Menus.typeWithPos(ContainerMachineRotaryFurnace::new));
    public static final RegistryObject<MenuType<ContainerMicrowave>> MICROWAVE =
            REG.menu("machine_microwave", () -> Menus.typeWithPos(ContainerMicrowave::new));
    public static final RegistryObject<MenuType<ContainerAutocrafter>> AUTOCRAFTER =
            REG.menu("machine_autocrafter", () -> Menus.typeWithPos(ContainerAutocrafter::new));
    public static final RegistryObject<MenuType<ContainerFunnel>> FUNNEL =
            REG.menu("machine_funnel", () -> Menus.typeWithPos(ContainerFunnel::new));

    public static void init() {}
}
