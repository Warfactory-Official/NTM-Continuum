package com.hbm.radio;

import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Feature-local position-bound menus. */
public final class ModRadioMenus {
    private ModRadioMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    public static final RegistryObject<MenuType<RadioTorchMenu>> RADIO_TORCH =
            MENUS.register("radio_torch", () -> Menus.typeWithPos(RadioTorchMenu::new));
    public static final RegistryObject<MenuType<RadioReaderMenu>> RADIO_READER =
            MENUS.register("radio_reader", () -> Menus.typeWithPos(RadioReaderMenu::new));
    public static final RegistryObject<MenuType<RadioLogicMenu>> RADIO_LOGIC =
            MENUS.register("radio_logic", () -> Menus.typeWithPos(RadioLogicMenu::new));
    public static final RegistryObject<MenuType<RadioCounterMenu>> COUNTER =
            MENUS.register("radio_counter", () -> Menus.typeWithPos(RadioCounterMenu::new));
    public static final RegistryObject<MenuType<RadioControllerMenu>> CONTROLLER =
            MENUS.register("radio_controller", () -> Menus.typeWithPos(RadioControllerMenu::new));
    public static final RegistryObject<MenuType<RadioTelexMenu>> TELEX =
            MENUS.register("radio_telex", () -> Menus.typeWithPos(RadioTelexMenu::new));
    public static final RegistryObject<MenuType<RadioAutocalMenu>> AUTOCAL =
            MENUS.register("radio_autocal", () -> Menus.typeWithPos(RadioAutocalMenu::new));

    public static void init() {}
}
