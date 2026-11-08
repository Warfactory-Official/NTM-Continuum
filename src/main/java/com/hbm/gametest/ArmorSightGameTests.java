package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.EntityHbmProps;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.handler.JetpackHandler;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.ModArmorSetItems;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ArmorFSBPowered;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GitHub issue 163: the sets 1.7 gives VATS health bars and thermal sight, the full charged set those need
 * ({@code ArmorFSB.hasFSBArmor}), and the HUD key that switches both, as the server flips it and syncs it to the client.
 */
public final class ArmorSightGameTests {

    private ArmorSightGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function FLAGS =
            TESTS.function("armor_vats_and_thermal_sets_are_17", 20, () -> ArmorSightGameTests::flags);
    public static final GameTests.Function CHARGE =
            TESTS.function("armor_sight_needs_a_full_charged_set", 20, () -> ArmorSightGameTests::charge);
    public static final GameTests.Function TOGGLE =
            TESTS.function("armor_hud_key_toggles_and_syncs", 20, () -> ArmorSightGameTests::toggle);

    public static void init() {}

    /** 1.7 {@code ModItemsArmor}: {@code enableVATS(true)} and {@code enableThermalSight(true)} per set. */
    private static void flags(GameTestHelper helper) {
        Map<String, Supplier<? extends Item>> plates = Map.ofEntries(
                Map.entry("t51", ModArmorItems.t51_plate), Map.entry("dieselsuit", ModArmorSetBItems.dieselsuit_plate),
                Map.entry("ajr", ModArmorSetItems.ajr_plate), Map.entry("ajro", ModArmorSetItems.ajro_plate),
                Map.entry("rpa", ModArmorItems.rpa_plate), Map.entry("ncrpa", ModArmorItems.ncrpa_plate),
                Map.entry("bj", ModArmorItems.bj_plate), Map.entry("fau", ModArmorSetItems.fau_plate),
                Map.entry("dns", ModArmorItems.dns_plate), Map.entry("trenchmaster", ModArmorSetItems.trenchmaster_plate),
                Map.entry("steamsuit", ModArmorSetBItems.steamsuit_plate), Map.entry("envsuit", ModArmorSetBItems.envsuit_plate),
                Map.entry("hev", ModArmorSetItems.hev_plate), Map.entry("taurun", ModArmorItems.taurun_plate));
        List<String> vats = List.of("t51", "dieselsuit", "ajr", "ajro", "rpa", "ncrpa", "bj", "dns", "trenchmaster");
        List<String> thermal = List.of("dieselsuit", "bj", "fau", "dns");
        List<String> wrong = new ArrayList<>();
        plates.forEach((name, plate) -> {
            ArmorFSB set = (ArmorFSB) plate.get();
            if (set.vats != vats.contains(name)) wrong.add(name + " vats " + set.vats);
            if (set.thermal != thermal.contains(name)) wrong.add(name + " thermal " + set.thermal);
        });
        helper.assertTrue(wrong.isEmpty(), "sets differ from 1.7: " + wrong);
        helper.succeed();
    }

    private static void charge(GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        Item[] dns = {ModArmorItems.dns_helmet.get(), ModArmorItems.dns_plate.get(), ModArmorItems.dns_legs.get(),
                ModArmorItems.dns_boots.get()};
        for (int i = 0; i < 4; i++) {
            ItemStack piece = new ItemStack(dns[i]);
            ((ArmorFSBPowered) dns[i]).setCharge(piece, 1000L);
            player.setItemSlot(slots[i], piece);
        }
        helper.assertTrue(ArmorFSB.hasFSBArmor(player), "a full charged DNT set is a working set");
        ((ArmorFSBPowered) dns[2]).setCharge(player.getItemBySlot(EquipmentSlot.LEGS), 0L);
        helper.assertTrue(!ArmorFSB.hasFSBArmor(player), "one flat piece switches the set off, as 1.7's isArmorEnabled");
        ((ArmorFSBPowered) dns[2]).setCharge(player.getItemBySlot(EquipmentSlot.LEGS), 1L);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        helper.assertTrue(!ArmorFSB.hasFSBArmor(player), "a missing helmet breaks the set");
        helper.succeed();
    }

    private static void toggle(GameTestHelper helper) {
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        boolean before = HbmLivingProps.getEnableHUD(player);
        JetpackHandler.handleKeybind(player, EnumKeybind.TOGGLE_HEAD, true);
        JetpackHandler.handleKeybind(player, EnumKeybind.TOGGLE_HEAD, true);
        helper.assertTrue(HbmLivingProps.getEnableHUD(player) == !before, "one press edge flips the HUD once");
        JetpackHandler.handleKeybind(player, EnumKeybind.TOGGLE_HEAD, false);
        JetpackHandler.handleKeybind(player, EnumKeybind.TOGGLE_HEAD, true);
        helper.assertTrue(HbmLivingProps.getEnableHUD(player) == before, "the next press flips it back");

        JetpackHandler.handleKeybind(player, EnumKeybind.TOGGLE_HEAD, false);
        JetpackHandler.handleKeybind(player, EnumKeybind.TOGGLE_HEAD, true);
        ByteBuf buf = Unpooled.buffer();
        HbmLivingProps.getData(player).serialize(buf);
        EntityHbmProps client = new EntityHbmProps();
        client.deserialize(buf);
        helper.assertTrue(client.getEnableHUD() == !before, "the player sync carries the HUD switch to the client");
        helper.succeed();
    }
}
