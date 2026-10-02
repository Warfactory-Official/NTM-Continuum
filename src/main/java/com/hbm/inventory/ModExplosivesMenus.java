package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.inventory.container.ContainerBombMulti;

/** The multi purpose bomb's position-bound menu. */
public final class ModExplosivesMenus {
    private ModExplosivesMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerBombMulti>> BOMB_MULTI =
            REG.menu("bomb_multi", () -> Menus.typeWithPos(ContainerBombMulti::new));

    public static void init() {}
}
