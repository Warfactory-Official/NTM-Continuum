package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import com.hbm.inventory.container.*;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** RBMK menus are separate from the general machine registrar. */
public final class ModRBMKMenus {
    private ModRBMKMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);
    public static final RegistryObject<MenuType<MenuRBMKRod>> RBMK_ROD = register("rbmk_fuel_rod", MenuRBMKRod::new);
    public static final RegistryObject<MenuType<MenuRBMKControl>> RBMK_CONTROL = register("rbmk_control", MenuRBMKControl::new);
    public static final RegistryObject<MenuType<MenuRBMKControlAuto>> RBMK_CONTROL_AUTO = register("rbmk_control_auto", MenuRBMKControlAuto::new);
    public static final RegistryObject<MenuType<MenuRBMKBoiler>> RBMK_BOILER = register("rbmk_boiler", MenuRBMKBoiler::new);
    public static final RegistryObject<MenuType<MenuRBMKStorage>> RBMK_STORAGE = register("rbmk_storage", MenuRBMKStorage::new);
    public static final RegistryObject<MenuType<MenuRBMKHeater>> RBMK_HEATER = register("rbmk_heater", MenuRBMKHeater::new);
    public static final RegistryObject<MenuType<MenuRBMKOutgasser>> RBMK_OUTGASSER = register("rbmk_outgasser", MenuRBMKOutgasser::new);
    public static final RegistryObject<MenuType<MenuRBMKConsole>> RBMK_CONSOLE = register("rbmk_console", MenuRBMKConsole::new);
    public static final RegistryObject<MenuType<ContainerRBMKAutoloader>> RBMK_AUTOLOADER = register("rbmk_autoloader", ContainerRBMKAutoloader::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> register(
            String id, Menus.PosFactory<T> factory) {
        return REG.menu(id, () -> Menus.typeWithPos(factory));
    }

    public static void init() {}
}
