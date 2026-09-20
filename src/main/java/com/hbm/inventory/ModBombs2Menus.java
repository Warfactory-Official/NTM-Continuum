package com.hbm.inventory;

import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerNukeCustom;
import com.hbm.inventory.container.ContainerNukeFleija;
import com.hbm.inventory.container.ContainerNukeFstbmb;
import com.hbm.inventory.container.ContainerNukePrototype;
import com.hbm.inventory.container.ContainerNukeSolinium;

/** Position-bound menus for the five special bombs that had a GUI. */
public final class ModBombs2Menus {

    private ModBombs2Menus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerNukeCustom>> NUKE_CUSTOM =
            MENUS.register("nuke_custom", () -> Menus.typeWithPos(ContainerNukeCustom::new));
    public static final RegistryObject<MenuType<ContainerNukeFleija>> NUKE_FLEIJA =
            MENUS.register("nuke_fleija", () -> Menus.typeWithPos(ContainerNukeFleija::new));
    public static final RegistryObject<MenuType<ContainerNukeSolinium>> NUKE_SOLINIUM =
            MENUS.register("nuke_solinium", () -> Menus.typeWithPos(ContainerNukeSolinium::new));
    public static final RegistryObject<MenuType<ContainerNukePrototype>> NUKE_PROTOTYPE =
            MENUS.register("nuke_prototype", () -> Menus.typeWithPos(ContainerNukePrototype::new));
    public static final RegistryObject<MenuType<ContainerNukeFstbmb>> NUKE_FSTBMB =
            MENUS.register("nuke_fstbmb", () -> Menus.typeWithPos(ContainerNukeFstbmb::new));

    public static void init() {}
}
