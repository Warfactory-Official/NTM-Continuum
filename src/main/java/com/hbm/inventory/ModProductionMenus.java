package com.hbm.inventory;

import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerAutocrafter;
import com.hbm.inventory.container.ContainerMachineRotaryFurnace;
import com.hbm.inventory.container.ContainerMicrowave;

/** Feature-local position-bound menus for the production machines. */
public final class ModProductionMenus {
    private ModProductionMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerMachineRotaryFurnace>> ROTARY_FURNACE =
            MENUS.register("machine_rotary_furnace", () -> Menus.typeWithPos(ContainerMachineRotaryFurnace::new));
    public static final RegistryObject<MenuType<ContainerMicrowave>> MICROWAVE =
            MENUS.register("machine_microwave", () -> Menus.typeWithPos(ContainerMicrowave::new));
    public static final RegistryObject<MenuType<ContainerAutocrafter>> AUTOCRAFTER =
            MENUS.register("machine_autocrafter", () -> Menus.typeWithPos(ContainerAutocrafter::new));

    public static void init() {}
}
