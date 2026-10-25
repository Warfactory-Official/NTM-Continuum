package com.hbm.gametest;

import com.hbm.items.ModArmorItems;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.handler.HazmatRegistry;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;

/** Contracts for the two plain ArmorFSB sets that are also crafting bases for later armour. */
public final class SteelTitaniumArmorGameTests {

    private SteelTitaniumArmorGameTests() {}

    public static void registryAndStats(GameTestHelper helper) {
        assertPiece(helper, ModArmorItems.steel_helmet, "steel_helmet", EquipmentSlot.HEAD, 3, 330);
        assertPiece(helper, ModArmorItems.steel_plate, "steel_plate", EquipmentSlot.CHEST, 8, 480);
        assertPiece(helper, ModArmorItems.steel_legs, "steel_legs", EquipmentSlot.LEGS, 6, 450);
        assertPiece(helper, ModArmorItems.steel_boots, "steel_boots", EquipmentSlot.FEET, 3, 390);

        assertPiece(helper, ModArmorItems.titanium_helmet, "titanium_helmet", EquipmentSlot.HEAD, 3, 275);
        assertPiece(helper, ModArmorItems.titanium_plate, "titanium_plate", EquipmentSlot.CHEST, 8, 400);
        assertPiece(helper, ModArmorItems.titanium_legs, "titanium_legs", EquipmentSlot.LEGS, 6, 375);
        assertPiece(helper, ModArmorItems.titanium_boots, "titanium_boots", EquipmentSlot.FEET, 3, 325);

        assertSet(helper, ModArmorItems.steel_helmet, ModArmorItems.steel_plate,
                ModArmorItems.steel_legs, ModArmorItems.steel_boots, "steel", 0.045D);
        assertSet(helper, ModArmorItems.titanium_helmet, ModArmorItems.titanium_plate,
                ModArmorItems.titanium_legs, ModArmorItems.titanium_boots, "titanium", 0.045D);
        helper.succeed();
    }

    private static void assertPiece(GameTestHelper helper, RegistryObject<? extends Item> entry, String path,
            EquipmentSlot slot, int defense, int maxDamage) {
        Item item = entry.get();
        Identifier expected = Ids.of("hbm", path);
        helper.assertTrue(expected.equals(BuiltInRegistries.ITEM.getKey(item)), "Wrong armour id for " + path);

        ItemStack stack = new ItemStack(item);
        helper.assertTrue(stack.getMaxDamage() == maxDamage,
                path + " max damage is " + stack.getMaxDamage() + ", expected " + maxDamage);

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemSlot(slot, stack);
        // Equipment attributes are reconciled by LivingEntity's normal tick path, not by setItemSlot.
        player.tick();
        double armor = player.getAttributeValue(Attributes.ARMOR);
        helper.assertTrue(armor == defense,
                path + " provides " + armor + " armour, expected " + defense);
    }

    private static void assertSet(GameTestHelper helper, RegistryObject<? extends Item> helmet,
            RegistryObject<? extends Item> plate, RegistryObject<? extends Item> legs,
            RegistryObject<? extends Item> boots, String name, double coefficient) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(plate.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots.get()));
        player.tick();

        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 20,
                name + " set armour total is not 20");
        helper.assertTrue(ArmorFSB.hasFSBArmor(player), name + " set is not detected as a complete ArmorFSB set");
        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(new ItemStack(helmet.get())) - coefficient * 0.2D) < 1.0E-6,
                name + " helmet radiation resistance is incorrect");
        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(new ItemStack(plate.get())) - coefficient * 0.4D) < 1.0E-6,
                name + " chest radiation resistance is incorrect");
        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(new ItemStack(legs.get())) - coefficient * 0.3D) < 1.0E-6,
                name + " legs radiation resistance is incorrect");
        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(new ItemStack(boots.get())) - coefficient * 0.1D) < 1.0E-6,
                name + " boots radiation resistance is incorrect");

        ArmorFSB chest = (ArmorFSB) plate.get();
        helper.assertTrue(chest.effects.isEmpty() && !chest.vats && !chest.thermal && !chest.hardLanding,
                name + " unexpectedly gained an ArmorFSB-configured full-set bonus");
    }
}
