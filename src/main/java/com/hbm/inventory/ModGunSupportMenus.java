package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.tool.ItemCasingBag;
import com.hbm.inventory.container.MenuItemBox;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Menu registrations for stack-backed gun support items. */
public final class ModGunSupportMenus {

    private ModGunSupportMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<MenuItemBox>> AMMO_BAG = REG.menu("ammo_bag", () ->
            Menus.typeWithHand((id, inventory, hand) -> new MenuItemBox(ammoBagType(), id, inventory,
                    ItemAmmoBag.LAYOUT, ModDataComponents.AMMO_BAG, hand)));

    public static final RegistryObject<MenuType<MenuItemBox>> CASING_BAG = REG.menu("casing_bag", () ->
            Menus.typeWithHand((id, inventory, hand) -> new MenuItemBox(casingBagType(), id, inventory,
                    ItemCasingBag.LAYOUT, ModDataComponents.CASING_BAG, hand)));

    private static MenuType<MenuItemBox> ammoBagType() { return AMMO_BAG.get(); }
    private static MenuType<MenuItemBox> casingBagType() { return CASING_BAG.get(); }

    public static void init() {}
}
