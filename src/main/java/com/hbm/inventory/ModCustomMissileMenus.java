package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.inventory.container.ContainerCompactLauncher;
import com.hbm.inventory.container.ContainerLaunchPadRusted;
import com.hbm.inventory.container.ContainerLaunchTable;
import com.hbm.inventory.container.ContainerMachineMissileAssembly;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Position-bound menus of the custom missile system. The large pad reuses the launch pad menu, as in 1.7. */
public final class ModCustomMissileMenus {

    private ModCustomMissileMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerMachineMissileAssembly>> MISSILE_ASSEMBLY =
            REG.menu("machine_missile_assembly", () -> Menus.typeWithPos(ContainerMachineMissileAssembly::new));
    public static final RegistryObject<MenuType<ContainerCompactLauncher>> COMPACT_LAUNCHER =
            REG.menu("compact_launcher", () -> Menus.typeWithPos(ContainerCompactLauncher::new));
    public static final RegistryObject<MenuType<ContainerLaunchTable>> LAUNCH_TABLE =
            REG.menu("launch_table", () -> Menus.typeWithPos(ContainerLaunchTable::new));
    public static final RegistryObject<MenuType<ContainerLaunchPadRusted>> LAUNCH_PAD_RUSTED =
            REG.menu("launch_pad_rusted", () -> Menus.typeWithPos(ContainerLaunchPadRusted::new));

    public static void init() {}
}
