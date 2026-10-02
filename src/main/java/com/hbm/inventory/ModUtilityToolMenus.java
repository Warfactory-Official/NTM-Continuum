package com.hbm.inventory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.inventory.container.ContainerLeadBox;
import com.hbm.inventory.container.ContainerLemegeton;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Menus of the utility tools and books: the containment box and the Lemegeton. */
public final class ModUtilityToolMenus {

    private ModUtilityToolMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<ContainerLeadBox>> LEAD_BOX = REG.menu("containment_box", () ->
            Menus.typeWithHand((id, inventory, hand) -> new ContainerLeadBox(leadBoxType(), id, inventory, hand)));

    public static final RegistryObject<MenuType<ContainerLemegeton>> LEMEGETON = REG.menu("book_lemegeton", () ->
            Menus.typeWithHand((id, inventory, hand) -> new ContainerLemegeton(lemegetonType(), id, inventory)));

    private static MenuType<ContainerLeadBox> leadBoxType() { return LEAD_BOX.get(); }
    private static MenuType<ContainerLemegeton> lemegetonType() { return LEMEGETON.get(); }

    public static void init() {}
}
