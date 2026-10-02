package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerCraneBoxer;
import com.hbm.inventory.container.ContainerCraneExtractor;
import com.hbm.inventory.container.ContainerCraneGrabber;
import com.hbm.inventory.container.ContainerCraneInserter;
import com.hbm.inventory.container.ContainerCraneRouter;
import com.hbm.inventory.container.ContainerCraneUnboxer;

/** Feature-local position-bound menus for the cranes. */
public final class ModConveyorMenus {

    private ModConveyorMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerCraneInserter>> CRANE_INSERTER =
            REG.menu("crane_inserter", () -> Menus.typeWithPos(ContainerCraneInserter::new));
    public static final RegistryObject<MenuType<ContainerCraneExtractor>> CRANE_EXTRACTOR =
            REG.menu("crane_extractor", () -> Menus.typeWithPos(ContainerCraneExtractor::new));
    public static final RegistryObject<MenuType<ContainerCraneGrabber>> CRANE_GRABBER =
            REG.menu("crane_grabber", () -> Menus.typeWithPos(ContainerCraneGrabber::new));
    public static final RegistryObject<MenuType<ContainerCraneRouter>> CRANE_ROUTER =
            REG.menu("crane_router", () -> Menus.typeWithPos(ContainerCraneRouter::new));
    public static final RegistryObject<MenuType<ContainerCraneBoxer>> CRANE_BOXER =
            REG.menu("crane_boxer", () -> Menus.typeWithPos(ContainerCraneBoxer::new));
    public static final RegistryObject<MenuType<ContainerCraneUnboxer>> CRANE_UNBOXER =
            REG.menu("crane_unboxer", () -> Menus.typeWithPos(ContainerCraneUnboxer::new));

    public static void init() {}
}
