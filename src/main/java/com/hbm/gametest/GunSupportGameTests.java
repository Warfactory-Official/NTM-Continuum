package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.ItemStackContainer;
import com.hbm.inventory.ModGunSupportMenus;
import com.hbm.inventory.container.MenuItemBox;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGunSupportItems;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.tool.ItemCasingBag;
import dev.golem.core.data.ItemContents;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Server-side contracts for stack-backed gun support items and their menus. */
public final class GunSupportGameTests {

    private GunSupportGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function STORAGE = TESTS.function(
            "gun_support_storage", 40, () -> GunSupportGameTests::storageAndFraction);
    public static final GameTests.Function MENU = TESTS.function(
            "gun_support_menu", 40, 8, () -> GunSupportGameTests::menuAuthority);
    public static final GameTests.Function REGISTRY = TESTS.function(
            "gun_support_registry", 20, () -> GunSupportGameTests::registry);

    public static void init() {}

    public static void storageAndFraction(GameTestHelper helper) {
        ItemStack ammoBag = new ItemStack(ModGunSupportItems.ammo_bag.get());
        ItemAmmoBag.InventoryAmmoBag ammo = ItemAmmoBag.contents(ammoBag);
        ammo.setStackInSlot(0, new ItemStack(ModAmmoItems.m357_sp.get(), 12));
        helper.assertTrue(ModDataComponents.AMMO_BAG.getOr(ammoBag, ItemContents.EMPTY)
                        .copies().get(0).getCount() == 12,
                "Ammo bag changes must persist to its component");

        ItemStack casingBag = new ItemStack(ModGunSupportItems.casing_bag.get());
        ItemStack casing = new ItemStack(ModCasingItems.casing(EnumCasingType.SMALL));
        helper.assertTrue(ItemCasingBag.pushCasing(casingBag, casing, 1.25F),
                "Casing bag must accept a whole casing and retain the fraction");
        helper.assertTrue(ItemCasingBag.contents(casingBag).getStackInSlot(0).getCount() == 1,
                "Casing bag must materialize one casing");
        helper.assertTrue(ItemCasingBag.pushCasing(casingBag, casing, 0.75F)
                        && ItemCasingBag.contents(casingBag).getStackInSlot(0).getCount() == 2,
                "Casing fraction must produce the next casing exactly at one");
        helper.succeed();
    }

    public static void menuAuthority(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack bag = new ItemStack(ModGunSupportItems.ammo_bag.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        MenuItemBox menu = new MenuItemBox(ModGunSupportMenus.AMMO_BAG.get(), 0, player.getInventory(),
                ItemAmmoBag.LAYOUT,
                new ItemStackContainer(player, bag, ItemAmmoBag.SLOTS, ModDataComponents.AMMO_BAG));
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ModAmmoItems.m357_sp.get())),
                "Ammo slots must accept uncomponentized ammo");
        ItemStack named = new ItemStack(ModAmmoItems.m357_sp.get());
        ItemStacks.setCustomName(named, Component.literal("x"));
        helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(ModAmmoItems.secretAmmo("FOLLY_SM").get()))
                        && !menu.getSlot(0).mayPlace(new ItemStack(Items.ARROW))
                        && !menu.getSlot(0).mayPlace(new ItemStack(ModCasingItems.casing(EnumCasingType.SMALL)))
                        && !menu.getSlot(0).mayPlace(named),
                "1.7 ammo bag slots hold only plain ammo_standard and ammo_secret");
        ItemStack casingBag = new ItemStack(ModGunSupportItems.casing_bag.get());
        MenuItemBox casingMenu = new MenuItemBox(ModGunSupportMenus.CASING_BAG.get(), 1,
                player.getInventory(), ItemCasingBag.LAYOUT,
                new ItemStackContainer(player, casingBag, ItemCasingBag.SLOTS, ModDataComponents.CASING_BAG));
        helper.assertFalse(casingMenu.getSlot(0).mayPlace(new ItemStack(Items.STICK)),
                "Casing slots must remain recovery-only");
        helper.assertTrue(menu.quickMoveStack(player, ItemAmmoBag.SLOTS + 27) == ItemStack.EMPTY,
                "The held bag must not be quick-moved out of its open menu");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertFalse(menu.stillValid(player), "Menu must close when its owning stack leaves the hand");
        helper.succeed();
    }

    public static void registry(GameTestHelper helper) {
        helper.assertTrue(ModGunSupportItems.ammo_container.get() != null
                        && ModGunSupportItems.ammo_bag.get() != null
                        && ModGunSupportItems.ammo_bag_infinite.get() != null
                        && ModGunSupportItems.casing_bag.get() != null,
                "All four gun support items must be registered");
        helper.assertTrue(ModGunSupportMenus.AMMO_BAG.get() != null
                        && ModGunSupportMenus.CASING_BAG.get() != null,
                "Both stack-backed menus must be registered");
        helper.succeed();
    }
}
