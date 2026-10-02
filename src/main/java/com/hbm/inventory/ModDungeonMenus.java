package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.inventory.container.ContainerRebar;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** The rebar placer's held-item menu. */
public final class ModDungeonMenus {

    private ModDungeonMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerRebar>> REBAR = REG.menu("rebar_placer", () ->
            Menus.typeWithHand((id, inventory, hand) -> new ContainerRebar(rebarType(), id, inventory, hand)));

    private static MenuType<ContainerRebar> rebarType() { return REBAR.get(); }

    public static void init() {}
}
