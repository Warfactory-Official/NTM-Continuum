package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.inventory.container.ContainerForceField;
import com.hbm.inventory.container.ContainerMachineRadarNT;
import com.hbm.inventory.container.ContainerSoyuzCapsule;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/**
 * Position-bound menus of the radar and utility machines. The radar map itself is a screen with no menu; its
 * link-slot view is {@link #RADAR}. The orbus opens the barrel's menu.
 */
public final class ModRadarUtilityMenus {

    private ModRadarUtilityMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerMachineRadarNT>> RADAR =
            REG.menu("machine_radar", () -> Menus.typeWithPos(ContainerMachineRadarNT::new));
    public static final RegistryObject<MenuType<ContainerForceField>> FORCEFIELD =
            REG.menu("machine_forcefield", () -> Menus.typeWithPos(ContainerForceField::new));
    public static final RegistryObject<MenuType<ContainerSoyuzCapsule>> SOYUZ_CAPSULE =
            REG.menu("soyuz_capsule", () -> Menus.typeWithPos(ContainerSoyuzCapsule::new));

    public static void init() {}
}
