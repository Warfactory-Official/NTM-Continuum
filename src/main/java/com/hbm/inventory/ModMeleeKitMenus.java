package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerBook;
import com.hbm.inventory.container.ContainerToolBox;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Menus of the special melee / kit family: the toolbox and the Book of Boxcars. */
public final class ModMeleeKitMenus {

    private ModMeleeKitMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerToolBox>> TOOLBOX = MENUS.register("toolbox", () ->
            Menus.typeWithHand((id, inventory, hand) -> new ContainerToolBox(toolboxType(), id, inventory, hand)));

    public static final RegistryObject<MenuType<ContainerBook>> BOOK = MENUS.register("book_of_", () ->
            Menus.typeWithHand((id, inventory, hand) -> new ContainerBook(bookType(), id, inventory)));

    private static MenuType<ContainerToolBox> toolboxType() { return TOOLBOX.get(); }
    private static MenuType<ContainerBook> bookType() { return BOOK.get(); }

    public static void init() {}
}
