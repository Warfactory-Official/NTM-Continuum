package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.config.PotionConfig;
import com.hbm.capability.HbmLivingProps;
import com.hbm.api.item.IGasMask;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModGearItems;
import com.hbm.interfaces.IArmorModDash;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.ModMedicineItems;
import com.hbm.potion.ModEffects;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Effects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;

/** Contracts for the medicine registrations and the old use/remainder behaviour. */
public final class MedicineGameTests {

    private MedicineGameTests() {}

    private static final List<RegistryObject<? extends Item>> ITEMS = List.of(
            ModMedicineItems.iv_empty,
            ModMedicineItems.syringe_awesome, ModMedicineItems.syringe_antidote,
            ModMedicineItems.syringe_poison, ModMedicineItems.syringe_metal_medx,
            ModMedicineItems.syringe_metal_psycho, ModMedicineItems.syringe_metal_stimpak,
            ModMedicineItems.syringe_metal_super, ModMedicineItems.syringe_taint,
            ModMedicineItems.syringe_mkunicorn, ModMedicineItems.med_bag,
            ModMedicineItems.radx, ModMedicineItems.siox, ModMedicineItems.pill_herbal,
            ModMedicineItems.xanax, ModMedicineItems.fmn, ModMedicineItems.five_htp,
            ModMedicineItems.pill_iodine, ModMedicineItems.plan_c, ModMedicineItems.pill_red,
            ModMedicineItems.chocolate, ModMedicineItems.radaway, ModMedicineItems.radaway_strong,
            ModMedicineItems.radaway_flush, ModMedicineItems.jetpack_tank,
            ModMedicineItems.cbt_device, ModMedicineItems.euphemium_stopper);

    private static final List<String> IDS = List.of(
            "iv_empty", "syringe_awesome", "syringe_antidote", "syringe_poison", "syringe_metal_medx",
            "syringe_metal_psycho", "syringe_metal_stimpak", "syringe_metal_super", "syringe_taint",
            "syringe_mkunicorn", "med_bag", "radx", "siox", "pill_herbal", "xanax", "fmn",
            "five_htp", "pill_iodine", "plan_c", "pill_red", "chocolate", "radaway", "radaway_strong",
            "radaway_flush", "jetpack_tank", "cbt_device", "euphemium_stopper");

    public static void medicine(GameTestHelper helper) {
        for (int i = 0; i < ITEMS.size(); i++) {
            Item item = ITEMS.get(i).get();
            helper.assertTrue(Ids.of(Constants.MOD_ID, IDS.get(i)).equals(BuiltInRegistries.ITEM.getKey(item)),
                    "Wrong medicine id: " + IDS.get(i));
        }

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMedicineItems.radx.get()));
        Effects.addRegistered(player, ModEffects.POTION_SICKNESS.get(), 100, 0);
        ModMedicineItems.radx.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!Effects.hasRegistered(player, ModEffects.RADX.get()),
                "Potion sickness must block pill use");
        Effects.removeRegistered(player, ModEffects.POTION_SICKNESS.get());

        ModMedicineItems.radx.get().finishUsingItem(new ItemStack(ModMedicineItems.radx.get()), player.level(), player);
        assertEffect(helper, player, ModEffects.RADX.get(), 3 * 60 * 20, 0, "Rad-X");
        // 1.7 PotionConfig.potionSickness defaults OFF: pills, syringes and the canteen apply none
        helper.assertTrue(PotionConfig.potionSickness == 0, "potion sickness defaults off, as 1.7");
        helper.assertTrue(!Effects.hasRegistered(player, ModEffects.POTION_SICKNESS.get()),
                "a pill applies no potion sickness at the 1.7 default");
        ItemStack stim = new ItemStack(ModMedicineItems.syringe_metal_stimpak.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stim);
        ModMedicineItems.syringe_metal_stimpak.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!Effects.hasRegistered(player, ModEffects.POTION_SICKNESS.get()),
                "a syringe applies no potion sickness at the 1.7 default");
        // "NORMAL" routes the same call through VersatileConfig: five seconds
        PotionConfig.potionSickness = 1;
        try {
            ModMedicineItems.radx.get().finishUsingItem(new ItemStack(ModMedicineItems.radx.get()), player.level(), player);
            assertEffect(helper, player, ModEffects.POTION_SICKNESS.get(), 5 * 20, 0, "pill sickness when enabled");
        } finally {
            PotionConfig.potionSickness = 0;
        }
        Effects.removeRegistered(player, ModEffects.POTION_SICKNESS.get());

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMedicineItems.radaway.get()));
        ModMedicineItems.radaway.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).is(ModMedicineItems.iv_empty.get()),
                "RadAway must return an IV bag");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMedicineItems.radaway.get()));
        ModMedicineItems.radaway.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        // 1.7 doRadaway: 140 ticks of RadAway I, the second bag's 140 added to the first
        assertEffect(helper, player, ModEffects.RADAWAY.get(), 280, 0, "stacked RadAway");

        player.setHealth(10F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMedicineItems.syringe_metal_stimpak.get()));
        ModMedicineItems.syringe_metal_stimpak.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getHealth() == 15F
                        && player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.syringe_metal_empty.get()),
                "Stimpak must heal and return the metal syringe");

        Player target = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        target.setHealth(5F);
        ItemStack medBag = new ItemStack(ModMedicineItems.med_bag.get());
        ModMedicineItems.med_bag.get().hurtEnemy(medBag, target, player);
        helper.assertTrue(target.getHealth() == 5F && medBag.getCount() == 1,
                "Med bag must not trigger from a melee hit");
        helper.succeed();
    }


    /** Syringe and wearable corrections: Awesome's Rad-X, both taint containers, MKU melee-only, bottled cloud, RPA. */
    public static void medicineArmorRc(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMedicineItems.syringe_awesome.get()));
        ModMedicineItems.syringe_awesome.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        assertEffect(helper, player, ModEffects.RADX.get(), 50 * 20, 9, "Awesome syringe Rad-X");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMedicineItems.syringe_taint.get()));
        ModMedicineItems.syringe_taint.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        int metal = 0;
        int bottle = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack held = player.getInventory().getItem(i);
            if (held.is(ModItems.syringe_metal_empty.get())) metal += held.getCount();
            if (held.is(ModItems.bottle2_empty.get())) bottle += held.getCount();
        }
        helper.assertTrue(metal == 1 && bottle == 1, "taint syringe returns a metal syringe and a bottle, got " + metal + "/" + bottle);

        ItemStack mku = new ItemStack(ModMedicineItems.syringe_mkunicorn.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, mku);
        ModMedicineItems.syringe_mkunicorn.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(HbmLivingProps.getContagion(player) == 0 && mku.getCount() == 1,
                "the MKU syringe does nothing on right-click");

        Item cloud = ModArmorModItems.bottled_cloud.get();
        helper.assertTrue(cloud instanceof IArmorModDash dash && dash.getDashes() == 3
                        && ((ItemArmorMod) cloud).type == ArmorModHandler.plate_only, "bottled cloud: 3 dashes, chestplate module");

        for (ArmorFSB plate : List.of(ModArmorItems.rpa_plate.get(), ModArmorItems.ncrpa_plate.get())) {
            helper.assertTrue(plate.effects.size() == 1 && plate.effects.get(0).getAmplifier() == 3,
                    "power armour strength IV");
        }
        helper.assertTrue(ModArmorItems.rpa_plate.get().getMaxCharge(new ItemStack(ModArmorItems.rpa_plate.get())) == 2_500_000L,
                "RPA capacity");
        helper.succeed();
    }


    /** The 1.7 filter flow: right-click a filter with a mask worn (or in the helmet module) and with a filter already in. */
    public static void filterSwap(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModGearItems.gas_mask.get()));
        helper.assertTrue(new ItemStack(ModGearItems.gas_mask_filter.get()).getMaxDamage() == 20_000, "1.7 filters have 20,000 durability");
        ItemStack first = new ItemStack(ModGearItems.gas_mask_filter.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, first);
        first.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);
        helper.assertTrue(((IGasMask) mask.getItem()).getFilter(mask).is(ModGearItems.gas_mask_filter.get())
                && player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "the filter goes into the worn mask and is used up");
        ItemStack second = new ItemStack(ModGearItems.gas_mask_filter_combo.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, second);
        second.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(((IGasMask) mask.getItem()).getFilter(mask).is(ModGearItems.gas_mask_filter_combo.get())
                && player.getItemInHand(InteractionHand.MAIN_HAND).is(ModGearItems.gas_mask_filter.get()), "installing over a filter swaps the old one into the hand");
        helper.succeed();
    }

    private static void assertEffect(GameTestHelper helper, Player player, MobEffect effect, int duration,
                                     int amplifier, String name) {
        helper.assertTrue(Effects.hasRegistered(player, effect), name + " was not applied");
        var actual = Effects.getRegistered(player, effect);
        helper.assertTrue(actual != null && actual.getDuration() == duration && actual.getAmplifier() == amplifier,
                name + " has incorrect duration/amplifier");
    }
}
