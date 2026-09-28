package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerICF;
import com.hbm.inventory.container.ContainerICFPress;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Position-bound menus of the ICF reactor and its pellet maker. The laser controller has none. */
public final class ModIcfMenus {

    private ModIcfMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerICF>> ICF =
            MENUS.register("icf", () -> Menus.typeWithPos(ContainerICF::new));
    public static final RegistryObject<MenuType<ContainerICFPress>> ICF_PRESS =
            MENUS.register("machine_icf_press", () -> Menus.typeWithPos(ContainerICFPress::new));

    public static void init() {}
}
