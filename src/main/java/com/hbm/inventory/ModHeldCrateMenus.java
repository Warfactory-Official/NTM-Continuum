package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerCrateTungsten;
import com.hbm.inventory.container.ContainerSafe;
import com.hbm.inventory.container.MenuCrate;
import com.hbm.items.block.CrateHeldOpening;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;

/**
 * The menus of a crate opened from the hand ({@code /ntmserver CRATE_OPEN_HELD}, {@link CrateHeldOpening}): the client
 * reads which crate from the stack in the named hand and builds the layout over a stand-in; the slots sync as usual.
 */
public final class ModHeldCrateMenus {

    private ModHeldCrateMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<MenuCrate>> CRATE_HELD = MENUS.register("crate_held",
            () -> Menus.typeWithHand((id, inventory, hand) -> {
                CrateHeldOpening.Kind kind = CrateHeldOpening.kindOf(inventory.player.getItemInHand(hand));
                if (kind == null) kind = CrateHeldOpening.Kind.IRON;
                return new MenuCrate(ModHeldCrateMenus.CRATE_HELD.get(), id, inventory, CrateHeldOpening.crateType(kind),
                        new SimpleContainer(CrateHeldOpening.slots(kind)), Compat.selectedSlot(inventory.player));
            }));
    public static final RegistryObject<MenuType<ContainerCrateTungsten>> CRATE_TUNGSTEN_HELD = MENUS.register(
            "crate_tungsten_held", () -> Menus.typeWithHand((id, inventory, hand) ->
                    new ContainerCrateTungsten(id, inventory, new SimpleContainer(27), Compat.selectedSlot(inventory.player))));
    public static final RegistryObject<MenuType<ContainerSafe>> SAFE_HELD = MENUS.register(
            "safe_held", () -> Menus.typeWithHand((id, inventory, hand) ->
                    new ContainerSafe(id, inventory, new SimpleContainer(15), Compat.selectedSlot(inventory.player))));

    public static void init() {}
}
