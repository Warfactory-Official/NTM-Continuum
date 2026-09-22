package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModWeaponTableBlocks;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModWeaponModItems;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.container.ContainerWeaponTable;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModSpecial;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.mags.MagazineElectricEngine;
import com.hbm.items.weapon.sedna.mags.MagazineLiquidEngine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.util.EntityDamageUtil;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.handler.HbmKeybindsServer;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Server contracts for the real weapon-table interaction and strict moved-stack behavior. */
public final class WeaponTableGameTests {
    private WeaponTableGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function INTERACTION = TESTS.function(
            "weapon_table_interaction_opens_menu", 40, 8, () -> WeaponTableGameTests::interactionOpensMenu);
    public static final GameTests.Function TRANSFER_SAFETY = TESTS.function(
            "weapon_table_failed_transfer_preserves_gun", 20, () -> WeaponTableGameTests::failedTransferPreservesGun);
    public static final GameTests.Function ORDINARY_PICKUP = TESTS.function(
            "weapon_table_ordinary_pickup_carries_mods", 20, () -> WeaponTableGameTests::ordinaryPickupCarriesMods);
    public static final GameTests.Function QUICK_MOVE = TESTS.function(
            "weapon_table_quick_move_carries_mods", 20, () -> WeaponTableGameTests::quickMoveCarriesMods);
    public static final GameTests.Function SHIFT_CLICK = TESTS.function(
            "weapon_table_shift_click_conserves_items", 20,
            () -> WeaponTableGameTests::shiftClickConservesItems);
    public static final GameTests.Function SWAP_CLICK = TESTS.function(
            "weapon_table_hotbar_swap_conserves_items", 20,
            () -> WeaponTableGameTests::hotbarSwapConservesItems);
    public static final GameTests.Function MOD_QUICK_MOVE = TESTS.function(
            "weapon_table_modifier_quick_move_conserves_items", 20,
            () -> WeaponTableGameTests::modifierQuickMoveConservesItems);
    public static final GameTests.Function PLAYER_CLICK = TESTS.function(
            "weapon_table_player_inventory_click_conserves_items", 20,
            () -> WeaponTableGameTests::playerInventoryClickConservesItems);
    public static final GameTests.Function ON_TAKE_SNAPSHOT = TESTS.function(
            "weapon_table_on_take_uses_removal_snapshot", 20,
            () -> WeaponTableGameTests::onTakeUsesRemovalSnapshot);
    public static final GameTests.Function CLOSE_DROPS = TESTS.function(
            "weapon_table_close_drops_one_gun_and_modifier", 20,
            () -> WeaponTableGameTests::closeDropsOneGunAndModifier);
    public static final GameTests.Function TAB_UNIQUENESS = TESTS.function(
            "weapon_table_weapons_tab_has_no_duplicate_entries", 20,
            () -> WeaponTableGameTests::weaponsTabHasNoDuplicateEntries);
    public static final GameTests.Function DRILL_FORTUNE = TESTS.function(
            "weapon_table_drill_fortune_install_uninstall", 20, () -> WeaponTableGameTests::drillFortuneInstallUninstall);
    public static final GameTests.Function CONFIG_SYNC = TESTS.function(
            "weapon_table_config_sync_contract", 20, () -> WeaponTableGameTests::configSyncContract);
    public static final GameTests.Function DRILL_SOURCE = TESTS.function(
            "drill_source_contracts", 40, () -> WeaponTableGameTests::drillSourceContracts);
    public static final GameTests.Function DRILL_BLOCK_BREAK = TESTS.function(
            "drill_actual_block_break", 40, () -> WeaponTableGameTests::drillActualBlockBreak);
    public static final GameTests.Function DRILL_KEYBIND_LOOT = TESTS.function(
            "drill_keybind_breaks_stone_with_loot", 40,
            () -> WeaponTableGameTests::drillKeybindBreaksStoneWithLoot);
    public static final GameTests.Function DRILL_ENTITY_DAMAGE = TESTS.function(
            "drill_actual_entity_damage", 40, () -> WeaponTableGameTests::drillActualEntityDamage);

    public static final GameTests.Function STALE_SOURCE = TESTS.function(
            "weapon_table_second_gun_after_shift_click", 20,
            () -> WeaponTableGameTests::secondGunAfterShiftClick);
    public static final GameTests.Function CONFIG1_QUICK_MOVE = TESTS.function(
            "weapon_table_config1_quick_move_keeps_mod", 20,
            () -> WeaponTableGameTests::config1QuickMoveKeepsMod);

    public static void init() {}

    /** Regression: a gun that entered by shift-click after another gun left must not crash or leak mods. */
    private static void secondGunAfterShiftClick(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack bayonet = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET);
        Item bayonetItem = bayonet.getItem();
        player.getInventory().setItem(0, new ItemStack(ModGunItems.gun_carbine.get()));
        player.getInventory().setItem(1, new ItemStack(ModGunItems.gun_carbine.get()));
        player.getInventory().setItem(2, bayonet);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        int hotbar = ContainerWeaponTable.PLAYER_START + 27;
        Menus.click(menu, hotbar, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.QUICK_MOVE, player);
        Menus.click(menu, hotbar + 1, 0, CompatContainerMenu.ClickAction.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().is(ModGunItems.gun_carbine.get()),
                "the second gun must enter the table by shift-click");
        Menus.click(menu, hotbar + 2, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, 0, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        int modded = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModGunItems.gun_carbine.get())
                    && XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_CARBINE_BAYONET)) modded++;
        }
        helper.assertTrue(modded == 0, "a gun outside the table must not receive the modifier");
        helper.assertTrue(XWeaponModManager.hasUpgrade(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem(), 0,
                        XWeaponModManager.ID_CARBINE_BAYONET),
                "the gun in the table must receive the modifier");
        helper.succeed();
    }

    /** Regression: a modifier installed on configuration 1 must survive the gun leaving the table. */
    private static void config1QuickMoveKeepsMod(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        player.getInventory().setItem(0, new ItemStack(ModGunItems.gun_light_revolver_dani.get()));
        player.getInventory().setItem(1, ModWeaponModItems.weapon_mod_test.get()
                .of(com.hbm.items.weapon.sedna.factory.GunFactory.EnumModTest.FIRERATE));
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        int hotbar = ContainerWeaponTable.PLAYER_START + 27;
        Menus.click(menu, hotbar, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        helper.assertTrue(menu.clickMenuButton(player, 1) && menu.configIndex() == 1, "config 1 must be selectable");
        Menus.click(menu, hotbar + 1, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, 0, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.QUICK_MOVE, player);
        ItemStack out = find(player, ModGunItems.gun_light_revolver_dani.get());
        helper.assertTrue(!out.isEmpty() && XWeaponModManager.hasUpgrade(out, 1, 0),
                "the outgoing gun must keep its configuration-1 modifier");
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.configIndex() == 0,
                "the consumed modifier slot must clear and the configuration must reset");
        helper.succeed();
    }

    private static void interactionOpensMenu(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, ModWeaponTableBlocks.MACHINE_WEAPON_TABLE.get().defaultBlockState());

        try (GameTestHelpers.MockServerPlayer mock = GameTestHelpers.mockServerPlayer(helper)) {
            mock.player().setPos(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D);
            mock.drainOutbound();
            helper.useBlock(relative, mock.player());

            GameTestHelpers.MenuOpening opening =
                    GameTestHelpers.menuOpening(helper, mock.drainOutbound().packets());
            helper.assertTrue(opening.menuType() == ModMenus.WEAPON_TABLE.get(),
                    "weapon-table interaction opened the wrong menu");
            helper.assertTrue(opening.position().equals(absolute),
                    "weapon-table menu did not carry the clicked position");
        }
        helper.succeed();
    }

    private static void failedTransferPreservesGun(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }

        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(new ItemStack(ModGunItems.gun_carbine.get()));
        helper.assertFalse(menu.getSlot(0).mayPlace(new ItemStack(Items.COBBLESTONE)),
                "a non-mod item must not enter a modifier slot");
        helper.assertFalse(menu.getSlot(ContainerWeaponTable.GUN_SLOT).mayPlace(new ItemStack(Items.COBBLESTONE)),
                "a non-gun item must not enter the gun slot");
        helper.assertTrue(menu.quickMoveStack(player, ContainerWeaponTable.GUN_SLOT).isEmpty(),
                "a full player inventory must reject the gun transfer");
        helper.assertTrue(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().is(ModGunItems.gun_carbine.get()),
                "a failed transfer must leave the gun in the table");
        helper.assertTrue(menu.clickMenuButton(player, -1) == false && menu.clickMenuButton(player, 999) == false,
                "invalid config buttons must be rejected");
        helper.succeed();
    }

    private static void ordinaryPickupCarriesMods(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        ItemStack gun = new ItemStack(ModGunItems.gun_carbine.get());
        ItemStack bayonet = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET);
        Item bayonetItem = bayonet.getItem();
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(gun);
        menu.getSlot(0).set(bayonet);

        ItemStack outgoing = menu.getSlot(ContainerWeaponTable.GUN_SLOT).remove(1);
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).onTake(player, outgoing);
        helper.assertTrue(XWeaponModManager.hasUpgrade(outgoing, 0, XWeaponModManager.ID_CARBINE_BAYONET),
                "ordinary pickup must finalize the outgoing gun, not the emptied table slot");
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "ordinary pickup must consume the installed modifier after finalization");
        helper.assertTrue(menu.configIndex() == 0, "ordinary pickup must reset the synchronized configuration");
        helper.succeed();
    }

    private static void quickMoveCarriesMods(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        ItemStack gun = new ItemStack(ModGunItems.gun_carbine.get());
        ItemStack bayonet = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET);
        Item bayonetItem = bayonet.getItem();
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(gun);
        menu.getSlot(0).set(bayonet);

        ItemStack moved = menu.quickMoveStack(player, ContainerWeaponTable.GUN_SLOT);
        helper.assertTrue(!moved.isEmpty(), "an empty inventory must accept the gun");
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "successful quick-move must consume the installed modifier");
        ItemStack destination = ItemStack.EMPTY;
        int gunCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack candidate = player.getInventory().getItem(i);
            if (candidate.is(ModGunItems.gun_carbine.get())) {
                destination = candidate;
                gunCount += candidate.getCount();
            }
        }
        helper.assertTrue(gunCount == 1, "successful quick-move must create exactly one gun stack");
        helper.assertTrue(XWeaponModManager.hasUpgrade(destination, 0, XWeaponModManager.ID_CARBINE_BAYONET),
                "quick-move must put the finalized gun in the actual player destination");
        helper.assertTrue(count(player, bayonetItem) == 0,
                "successful gun quick-move must not leave a loose modifier copy");
        helper.assertTrue(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().isEmpty(),
                "successful quick-move must clear the table gun slot");
        helper.succeed();
    }

    private static void modifierQuickMoveConservesItems(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        ItemStack gun = new ItemStack(ModGunItems.gun_carbine.get());
        ItemStack bayonet = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET);
        Item bayonetItem = bayonet.getItem();
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(gun);
        menu.getSlot(0).set(bayonet);

        ItemStack moved = menu.quickMoveStack(player, 0);
        helper.assertTrue(!moved.isEmpty() && count(player, bayonetItem) == 1,
                "modifier quick-move must move exactly one modifier to the player");
        helper.assertTrue(!XWeaponModManager.hasUpgrade(gun, 0, XWeaponModManager.ID_CARBINE_BAYONET),
                "moving a modifier out must uninstall it from the live gun");
        helper.assertTrue(menu.getSlot(0).getItem().isEmpty(),
                "modifier quick-move must empty the table slot");
        helper.succeed();
    }

    private static void shiftClickConservesItems(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        ItemStack gun = new ItemStack(ModGunItems.gun_carbine.get());
        ItemStack bayonet = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET);
        Item bayonetItem = bayonet.getItem();
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(gun);
        menu.getSlot(0).set(bayonet);

        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.QUICK_MOVE, player);
        helper.assertTrue(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().isEmpty()
                        && count(player, ModGunItems.gun_carbine.get()) == 1,
                "a shift-click must remove the table gun exactly once");
        ItemStack destination = find(player, ModGunItems.gun_carbine.get());
        helper.assertTrue(XWeaponModManager.hasUpgrade(destination, 0, XWeaponModManager.ID_CARBINE_BAYONET)
                        && menu.getSlot(0).getItem().isEmpty(),
                "a shift-click destination must carry the table modifiers without leaving copies");
        helper.succeed();
    }

    private static void hotbarSwapConservesItems(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        ItemStack gun = new ItemStack(ModGunItems.gun_carbine.get());
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(gun);
        menu.getSlot(0).set(ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET));

        // Vanilla swap semantics (as in the original 1.7 slotClick mode 2): a hotbar item that the gun
        // slot cannot accept means no swap at all; an empty hotbar slot takes the gun.
        player.getInventory().setItem(0, new ItemStack(Items.STICK));
        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.SWAP, player);
        helper.assertTrue(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().is(ModGunItems.gun_carbine.get())
                        && player.getInventory().getItem(0).is(Items.STICK),
                "a hotbar swap against a non-gun hotbar item must not move anything");

        player.getInventory().setItem(0, ItemStack.EMPTY);
        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.SWAP, player);
        helper.assertTrue(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().isEmpty()
                        && count(player, ModGunItems.gun_carbine.get()) == 1,
                "a hotbar swap must move exactly one gun to the player");
        helper.assertTrue(XWeaponModManager.hasUpgrade(player.getInventory().getItem(0), 0,
                        XWeaponModManager.ID_CARBINE_BAYONET)
                        && menu.getSlot(0).getItem().isEmpty(),
                "the swapped-out gun must carry the table modifiers and consume them");
        helper.succeed();
    }

    private static void playerInventoryClickConservesItems(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ItemStack gun = new ItemStack(ModGunItems.gun_carbine.get());
        ItemStack bayonet = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET);
        Item bayonetItem = bayonet.getItem();
        player.getInventory().setItem(0, gun);
        player.getInventory().setItem(1, bayonet);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());

        int hotbar = ContainerWeaponTable.PLAYER_START + 27;
        Menus.click(menu, hotbar, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, hotbar + 1, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, 0, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        helper.assertTrue(XWeaponModManager.hasUpgrade(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem(), 0,
                        XWeaponModManager.ID_CARBINE_BAYONET),
                "a modifier placed through player inventory clicking must install on the gun in the table");
        Menus.click(menu, ContainerWeaponTable.GUN_SLOT, 0, CompatContainerMenu.ClickAction.PICKUP, player);
        Menus.click(menu, hotbar, 0, CompatContainerMenu.ClickAction.PICKUP, player);

        helper.assertTrue(count(player, ModGunItems.gun_carbine.get()) == 1,
                "player inventory clicking must conserve exactly one gun");
        helper.assertTrue(count(player, bayonetItem) == 0 && menu.getSlot(0).getItem().isEmpty(),
                "the installed modifier must not also remain as an item after the click transfer");
        helper.assertTrue(XWeaponModManager.hasUpgrade(player.getInventory().getItem(0), 0,
                        XWeaponModManager.ID_CARBINE_BAYONET),
                "the clicked destination must contain the configured gun");
        helper.succeed();
    }

    private static void onTakeUsesRemovalSnapshot(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        ItemStack outgoing = new ItemStack(ModGunItems.gun_carbine.get());
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(outgoing);
        menu.getSlot(0).set(ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET));

        ItemStack removed = menu.getSlot(ContainerWeaponTable.GUN_SLOT).remove(1);
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(new ItemStack(ModGunItems.gun_light_revolver.get()));
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).onTake(player, removed);

        helper.assertTrue(XWeaponModManager.hasUpgrade(removed, 0, XWeaponModManager.ID_CARBINE_BAYONET),
                "Slot.onTake must finalize the detached gun with the modifiers present at removal");
        helper.assertTrue(menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().is(ModGunItems.gun_light_revolver.get()),
                "a later slot change must not be overwritten by the earlier gun callback");
        helper.succeed();
    }

    private static void closeDropsOneGunAndModifier(GameTestHelper helper) {
        // Dropping is a ServerPlayer operation on 1.21.1 NeoForge. Keep the fixture scoped so its
        // connection and entity cannot leak into the next test, and count only entities created by this
        // close: neighboring plots may legitimately have their own item drops in the legacy packer.
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            var player = fixture.player();
            BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
            player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);

            ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
            ItemStack gun = new ItemStack(ModGunItems.gun_carbine.get());
            ItemStack bayonet = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.BAYONET);
            Item bayonetItem = bayonet.getItem();
            menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(gun);
            menu.getSlot(0).set(bayonet);

            java.util.Set<Integer> before = new java.util.HashSet<>();
            for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(4D))) before.add(entity.getId());
            menu.removed(player);

            List<ItemStack> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            player.getBoundingBox().inflate(4D)).stream()
                    .filter(entity -> !before.contains(entity.getId()))
                    .map(ItemEntity::getItem).toList();
            helper.assertTrue(drops.stream().mapToInt(stack -> stack.is(ModGunItems.gun_carbine.get()) ? stack.getCount() : 0).sum() == 1,
                    "closing the table must drop exactly one gun");
            helper.assertTrue(drops.stream().mapToInt(stack -> stack.is(bayonetItem) ? stack.getCount() : 0).sum() == 1,
                    "closing the table must drop exactly one modifier");
            ItemStack droppedGun = drops.stream().filter(stack -> stack.is(ModGunItems.gun_carbine.get())).findFirst().orElse(ItemStack.EMPTY);
            helper.assertTrue(!XWeaponModManager.hasUpgrade(droppedGun, 0, XWeaponModManager.ID_CARBINE_BAYONET),
                    "the dropped gun must be uninstalled exactly once");
        }
        helper.succeed();
    }

    private static void weaponsTabHasNoDuplicateEntries(GameTestHelper helper) {
        var weapons = com.hbm.creativetabs.ModTabs.get(com.hbm.creativetabs.HbmTab.WEAPONS).getKey();
        List<?> contents = com.hbm.creativetabs.ModTabs.CONTENTS.get(weapons);
        helper.assertTrue(contents.size() == contents.stream().distinct().count(),
                "Weapons tab registry entries must not be emitted more than once");
        List<ItemStack> output = new java.util.ArrayList<>();
        for (var entry : com.hbm.creativetabs.ModTabs.CONTENTS.get(weapons)) {
            Item item = entry.get();
            if (com.hbm.creativetabs.ModTabs.shouldDisplay(weapons, item)) output.add(new ItemStack(item));
        }
        var custom = com.hbm.creativetabs.ModTabs.STACK_CONTENTS.get(weapons);
        if (custom != null) custom.forEach(supplier -> output.addAll(supplier.get()));
        for (int i = 0; i < output.size(); i++) for (int j = i + 1; j < output.size(); j++) {
            helper.assertFalse(ItemStacks.sameItemAndData(output.get(i), output.get(j)),
                    "Weapons tab output contains duplicate item data at " + i + " and " + j);
        }
        helper.succeed();
    }

    private static int count(net.minecraft.world.entity.player.Player player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static ItemStack find(net.minecraft.world.entity.player.Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void drillSourceContracts(GameTestHelper helper) {
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
        var player = fixture.player();
        ItemGunDrill drillItem = ModGunItems.gun_drill.get();
        ItemStack drill = new ItemStack(drillItem);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        IMagazine<?> initialMagazine = drillItem.getConfig(drill, 0).getReceivers(drill)[0].getMagazine(drill);
        helper.assertTrue(initialMagazine instanceof MagazineLiquidEngine,
                "the drill must start with a liquid engine magazine");
        MagazineLiquidEngine liquid = (MagazineLiquidEngine) initialMagazine;
        liquid.setAmount(drill, 100);

        helper.assertTrue(drillItem.canHarvestBlock(Blocks.BEDROCK.defaultBlockState()),
                "the drill canHarvestBlock gate must remain unconditionally true");
        helper.assertTrue(drillItem.tryEmpty(com.hbm.inventory.fluid.Fluids.GASOLINE, 321, drill) == 321
                        && drillItem.getFill(drill) == 100,
                "tryEmpty must report the requested amount without draining the drill");

        prepareDrill(player, drillItem, drill, InteractionHand.MAIN_HAND);
        fireDrillKey(player);
        helper.assertTrue(liquid.getAmount(drill, null) == 90,
                "ordinary drill fire must consume the source 10mB cost");

        BlockPos bedrock = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(bedrock, Blocks.BEDROCK.defaultBlockState(), 3);
        player.setPos(bedrock.getX() + 0.5D, bedrock.getY() - 1D, bedrock.getZ() - 3D);
        player.setYRot(0F);
        player.setYHeadRot(0F);
        player.setXRot(0F);
        readyDrill(player, drillItem, drill, InteractionHand.MAIN_HAND);
        fireDrillKey(player);
        helper.assertTrue(helper.getLevel().getBlockState(bedrock).is(Blocks.BEDROCK),
                "the unconditional harvest gate must not bypass unbreakable hardness");

        ItemStack dieselMod = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.ENGINE_DIESEL);
        ItemStack canisters = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.CANISTERS);
        XWeaponModManager.install((net.minecraft.server.level.ServerLevel) player.level(), drill, 0,
                dieselMod, canisters);
        IMagazine<?> dieselMagazine = drillItem.getConfig(drill, 0).getReceivers(drill)[0].getMagazine(drill);
        helper.assertTrue(dieselMagazine instanceof MagazineLiquidEngine diesel
                        && diesel.accepted()[0] == com.hbm.inventory.fluid.Fluids.DIESEL
                        && diesel.getCapacity(drill) == 12_000,
                "the diesel engine and canister must compose through the compatible liquid magazine");

        ItemStack electricMod = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.ENGINE_ELECTRIC);
        XWeaponModManager.install((net.minecraft.server.level.ServerLevel) player.level(), drill, 0, electricMod);
        IMagazine<?> electricMagazine = drillItem.getConfig(drill, 0).getReceivers(drill)[0].getMagazine(drill);
        helper.assertTrue(electricMagazine instanceof MagazineElectricEngine,
                "the electric engine must replace the drill magazine with MagazineElectricEngine");
        MagazineElectricEngine electric = (MagazineElectricEngine) electricMagazine;
        electric.setAmount(drill, 5_000);
        readyDrill(player, drillItem, drill, InteractionHand.MAIN_HAND);
        fireDrillKey(player);
        helper.assertTrue(electric.getAmount(drill, null) == 4_000,
                "electric drill fire must consume the source 1,000HE cost");

        player.setPos(bedrock.getX() + 0.5D, bedrock.getY() + 1D, bedrock.getZ() - 0.5D);
        // The wall and the stand sit down +z, and a living entity looks along its head yaw, not its body
        // yaw: a joined server player keeps whatever head yaw the join gave it unless it is set here.
        player.setYRot(0F);
        player.setYHeadRot(0F);
        BlockPos wall = helper.absolutePos(new BlockPos(2, 3, 4));
        helper.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        ArmorStand target = new ArmorStand(helper.getLevel(), wall.getX() + 0.5D, wall.getY() - 1D,
                wall.getZ() + 2D);
        target.setNoGravity(true);
        helper.getLevel().addFreshEntity(target);
        HitResult ray = EntityDamageUtil.getMouseOver(player, 5D);
        helper.assertTrue(ray instanceof EntityHitResult && ((EntityHitResult) ray).getEntity() == target,
                "the drill ray must retain source full-reach entity selection past the block trace; ray " + ray
                        + " from " + player.getEyePosition() + " look " + player.getViewVector(1F) + " yaw "
                        + player.getYRot() + " head " + player.getYHeadRot() + " target " + target.getBoundingBox()
                        + " alive " + target.isAlive() + " pickable " + target.isPickable());
        }
        helper.succeed();
    }

    private static void drillActualBlockBreak(GameTestHelper helper) {
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
        ServerPlayer player = fixture.player();
        ItemGunDrill drillItem = ModGunItems.gun_drill.get();
        ItemStack drill = new ItemStack(drillItem);
        BlockPos block = helper.absolutePos(new BlockPos(2, 2, 5));
        helper.getLevel().setBlock(block, Blocks.STONE.defaultBlockState(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setItemInHand(InteractionHand.OFF_HAND, drill);
        player.setPos(block.getX() + 0.5D, block.getY() - 1D, block.getZ() - 3D);
        player.setYRot(0F);
        player.setYHeadRot(0F);
        player.setXRot(0F);
        ((MagazineLiquidEngine) (Object) drillItem.getConfig(drill, 0).getReceivers(drill)[0].getMagazine(drill)).setAmount(drill, 100);
        readyDrill(player, drillItem, drill, InteractionHand.OFF_HAND);

        HitResult ray = EntityDamageUtil.getMouseOver(player, 5D);
        helper.assertTrue(ray instanceof net.minecraft.world.phys.BlockHitResult hit
                        && hit.getBlockPos().equals(block),
                "the real drill trigger must aim at the placed breakable block");
        fireDrillKey(player);
        helper.assertTrue(helper.getLevel().getBlockState(block).isAir(),
                "the real drill trigger must break a harvestable block through the server destroy path");
        helper.assertTrue(drillItem.getFill(drill) == 90,
                "the real drill trigger must consume exactly 10mB");
        }
        helper.succeed();
    }

    private static void drillKeybindBreaksStoneWithLoot(GameTestHelper helper) {
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = fixture.player();
            ItemGunDrill drillItem = ModGunItems.gun_drill.get();
            ItemStack drill = new ItemStack(drillItem);
            BlockPos block = helper.absolutePos(new BlockPos(2, 2, 5));
            helper.getLevel().setBlock(block, Blocks.STONE.defaultBlockState(), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            player.setItemInHand(InteractionHand.OFF_HAND, drill);
            player.setPos(block.getX() + 0.5D, block.getY() - 1D, block.getZ() - 3D);
            player.setYRot(0F);
            player.setYHeadRot(0F);
            player.setXRot(0F);
            ((MagazineLiquidEngine) (Object) drillItem.getConfig(drill, 0).getReceivers(drill)[0].getMagazine(drill))
                    .setAmount(drill, 100);
            readyDrill(player, drillItem, drill, InteractionHand.OFF_HAND);

            fireDrillKey(player);
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(block).inflate(2D));
            int cobblestone = drops.stream().map(ItemEntity::getItem)
                    .filter(stack -> stack.is(Items.COBBLESTONE))
                    .mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(helper.getLevel().getBlockState(block).isAir(),
                    "the real server keybind path must remove the placed stone block");
            helper.assertTrue(cobblestone == 1,
                    "the server harvest path must emit one normal stone cobblestone drop, got " + cobblestone);
            helper.assertTrue(drillItem.getFill(drill) == 90,
                    "the real keybind path must consume exactly 10mB of liquid fuel");
            helper.assertTrue(player.getMainHandItem().is(Items.STICK) && player.getOffhandItem() == drill,
                    "off-hand drill harvesting must not replace or consume the main-hand callback stack");
        }
        helper.succeed();
    }

    private static void drillActualEntityDamage(GameTestHelper helper) {
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
        var player = fixture.player();
        ItemGunDrill drillItem = ModGunItems.gun_drill.get();
        ItemStack drill = new ItemStack(drillItem);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        IMagazine<?> drillMagazine = drillItem.getConfig(drill, 0).getReceivers(drill)[0].getMagazine(drill);
        helper.assertTrue(drillMagazine instanceof MagazineLiquidEngine,
                "drill must use the liquid engine magazine for entity damage");
        ((MagazineLiquidEngine) (Object) drillMagazine).setAmount(drill, 100);

        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        player.setPos(origin.getX() + 0.5D, origin.getY() + 1D, origin.getZ() + 0.5D);
        player.setYRot(0F);
        player.setYHeadRot(0F);
        player.setXRot(0F);
        ArmorStand target = new ArmorStand(helper.getLevel(), origin.getX() + 0.5D, origin.getY() + 1.5D,
                origin.getZ() + 3D);
        target.setNoGravity(true);
        helper.getLevel().addFreshEntity(target);

        prepareDrill(player, drillItem, drill, InteractionHand.MAIN_HAND);
        float health = target.getHealth();
        fireDrillKey(player);
        helper.assertTrue(target.getHealth() < health, "the drill must damage an entity on its ray");
        helper.assertTrue(drillItem.getFill(drill) == 90, "an ordinary drill shot must consume only 10mB");
        }
        helper.succeed();
    }

    private static void prepareDrill(ServerPlayer player, ItemGunDrill drillItem, ItemStack drill,
                                     InteractionHand hand) {
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setItemInHand(hand, drill);
        for (int i = 0; i < 2; i++) drillItem.serverTick(drill, (net.minecraft.server.level.ServerLevel) player.level(), player, true, hand);
    }

    private static void readyDrill(ServerPlayer player, ItemGunDrill drillItem, ItemStack drill,
                                    InteractionHand hand) {
        for (int i = 0; i < 20; i++) drillItem.serverTick(drill, (net.minecraft.server.level.ServerLevel) player.level(), player, true, hand);
    }

    private static void fireDrillKey(ServerPlayer player) {
        HbmKeybindsServer.onPressedServer(player, EnumKeybind.GUN_PRIMARY, true);
        HbmKeybindsServer.onPressedServer(player, EnumKeybind.GUN_PRIMARY, false);
    }

    private static void drillFortuneInstallUninstall(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        ItemStack drill = new ItemStack(ModGunItems.gun_drill.get());
        ItemStack magnet = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.MAGNET);
        ItemStack sifter = ModWeaponModItems.weapon_mod_special.get().of(EnumModSpecial.SIFTER);
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(drill);
        menu.getSlot(0).set(magnet);
        menu.getSlot(1).set(sifter);
        helper.assertTrue(XWeaponModManager.hasUpgrade(drill, 0, 230)
                        && XWeaponModManager.hasUpgrade(drill, 0, 231),
                "drill magnet and sifter must install through the server-aware path");

        menu.getSlot(0).remove(1);
        helper.assertTrue(!XWeaponModManager.hasUpgrade(drill, 0, 230)
                        && XWeaponModManager.hasUpgrade(drill, 0, 231),
                "removing magnet must uninstall only magnet's fortune effect");
        menu.removed(player);
        helper.assertTrue(!XWeaponModManager.hasUpgrade(drill, 0, 231),
                "closing the table must uninstall the remaining server-side fortune effect");
        helper.succeed();
    }

    private static void configSyncContract(GameTestHelper helper) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockServerPlayerInLevel(helper);
        ContainerWeaponTable menu = new ContainerWeaponTable(0, player.getInventory());
        ItemStack stack = new ItemStack(ModGunItems.gun_light_revolver_dani.get());
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        helper.assertTrue(gun.getConfigCount() > 1, "config sync fixture must expose more than one configuration");
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(stack);
        helper.assertTrue(menu.configIndex() == 0, "inserting a gun must reset its configuration");
        helper.assertTrue(menu.clickMenuButton(player, 1), "valid configuration selection must be accepted");
        helper.assertTrue(menu.configIndex() == 1, "server configuration selection must update the menu state");
        menu.getSlot(ContainerWeaponTable.GUN_SLOT).set(ItemStack.EMPTY);
        helper.assertTrue(menu.configIndex() == 0, "removing a gun must reset the synchronized configuration");
        helper.succeed();
    }
}
