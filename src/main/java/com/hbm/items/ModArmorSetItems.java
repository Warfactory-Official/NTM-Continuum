package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import java.util.function.Supplier;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.ArmorAJR;
import com.hbm.items.armor.ArmorAJRO;
import com.hbm.items.armor.ArmorBismuth;
import com.hbm.items.armor.ArmorDigamma;
import com.hbm.items.armor.ArmorEuphemium;
import com.hbm.items.armor.ArmorHEV;
import com.hbm.items.armor.ArmorTrenchmaster;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.gear.ArmorFSBPowered;
import com.hbm.items.gear.IArmorDisableModel.EnumPlayerPart;
import com.hbm.sound.ModSounds;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.DamageResistanceHandler.ResistanceStats;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.Effects;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.function.Function;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import static com.hbm.util.DamageResistanceHandler.CATEGORY_EXPLOSION;
import static com.hbm.util.DamageResistanceHandler.CATEGORY_FIRE;
import static com.hbm.util.DamageResistanceHandler.CATEGORY_PHYSICAL;

/**
 * The remaining full armour sets of 1.7 {@code ModItemsArmor} (part A): advanced alloy, CMB, bismuth,
 * euphemium, the zirconium pants, the dineutronium joke set, Steel Ranger (AJR) and its AJRO repaint, the
 * HEV Mark IV, Fau's digamma suit and the Trenchmaster.
 *
 * <p>The shape is {@link ModArmorItems}': the helmet (the chestplate for helmet-less suits) carries the
 * whole set's configuration in {@link #configure()} and the other pieces {@code cloneStats} it. Numbers are
 * the 1.7 declarations verbatim. The AJR suits share {@link HbmArmorMaterials#AJR} with RPA/NCRPA exactly as
 * upstream's four suits all used {@code aMatAJR}, so mixed pieces of those four count as one set.
 */
public final class ModArmorSetItems {

    private ModArmorSetItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    // ---- plain ArmorFSB sets (ModItemsArmor.java:70-81, 110-120) ----
    public static final RegistryObject<ArmorFSB> alloy_helmet = plain("alloy_helmet", () -> HbmArmorMaterials.ALLOY, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> alloy_plate = plain("alloy_plate", () -> HbmArmorMaterials.ALLOY, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> alloy_legs = plain("alloy_legs", () -> HbmArmorMaterials.ALLOY, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> alloy_boots = plain("alloy_boots", () -> HbmArmorMaterials.ALLOY, ArmorSlot.BOOTS);

    public static final RegistryObject<ArmorFSB> cmb_helmet = plain("cmb_helmet", () -> HbmArmorMaterials.CMB, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> cmb_plate = plain("cmb_plate", () -> HbmArmorMaterials.CMB, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> cmb_legs = plain("cmb_legs", () -> HbmArmorMaterials.CMB, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> cmb_boots = plain("cmb_boots", () -> HbmArmorMaterials.CMB, ArmorSlot.BOOTS);

    public static final RegistryObject<ArmorFSB> zirconium_legs = plain("zirconium_legs", () -> HbmArmorMaterials.ZIRCONIUM, ArmorSlot.LEGGINGS);

    public static final RegistryObject<ArmorFSB> dnt_helmet = plain("dnt_helmet", () -> HbmArmorMaterials.DNT, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> dnt_plate = plain("dnt_plate", () -> HbmArmorMaterials.DNT, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> dnt_legs = plain("dnt_legs", () -> HbmArmorMaterials.DNT, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> dnt_boots = plain("dnt_boots", () -> HbmArmorMaterials.DNT, ArmorSlot.BOOTS);

    // ---- ArmorBismuth (ModItemsArmor.java:52-61) ----
    public static final RegistryObject<ArmorBismuth> bismuth_helmet = bismuth("bismuth_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorBismuth> bismuth_plate = bismuth("bismuth_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorBismuth> bismuth_legs = bismuth("bismuth_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorBismuth> bismuth_boots = bismuth("bismuth_boots", ArmorSlot.BOOTS);

    // ---- ArmorEuphemium (ModItemsArmor.java:37-40): no creative tab, epic, never wears ----
    public static final RegistryObject<ArmorEuphemium> euphemium_helmet = euphemium("euphemium_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorEuphemium> euphemium_plate = euphemium("euphemium_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorEuphemium> euphemium_legs = euphemium("euphemium_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorEuphemium> euphemium_boots = euphemium("euphemium_boots", ArmorSlot.BOOTS);

    // ---- ArmorAJR / ArmorAJRO (ModItemsArmor.java:161-189): 2.5 MHE, 10 kHE/t charge, 2 kHE per point, 25/t ----
    private static final long AJR_POWER = 2_500_000L;
    private static final long AJR_CHARGE = 10_000L;
    private static final long AJR_CONSUMPTION = 2_000L;
    private static final long AJR_DRAIN = 25L;

    public static final RegistryObject<ArmorAJR> ajr_helmet = powered("ajr_helmet",
            p -> new ArmorAJR(HbmArmorMaterials.AJR, ArmorSlot.HELMET, p, AJR_POWER, AJR_CHARGE, AJR_CONSUMPTION, AJR_DRAIN));
    public static final RegistryObject<ArmorAJR> ajr_plate = powered("ajr_plate",
            p -> new ArmorAJR(HbmArmorMaterials.AJR, ArmorSlot.CHESTPLATE, p, AJR_POWER, AJR_CHARGE, AJR_CONSUMPTION, AJR_DRAIN));
    public static final RegistryObject<ArmorAJR> ajr_legs = powered("ajr_legs",
            p -> new ArmorAJR(HbmArmorMaterials.AJR, ArmorSlot.LEGGINGS, p, AJR_POWER, AJR_CHARGE, AJR_CONSUMPTION, AJR_DRAIN));
    public static final RegistryObject<ArmorAJR> ajr_boots = powered("ajr_boots",
            p -> new ArmorAJR(HbmArmorMaterials.AJR, ArmorSlot.BOOTS, p, AJR_POWER, AJR_CHARGE, AJR_CONSUMPTION, AJR_DRAIN));

    public static final RegistryObject<ArmorAJRO> ajro_helmet = powered("ajro_helmet",
            p -> new ArmorAJRO(HbmArmorMaterials.AJR, ArmorSlot.HELMET, p, AJR_POWER, AJR_CHARGE, AJR_CONSUMPTION, AJR_DRAIN));
    public static final RegistryObject<ArmorAJRO> ajro_plate = powered("ajro_plate",
            p -> new ArmorAJRO(HbmArmorMaterials.AJR, ArmorSlot.CHESTPLATE, p, AJR_POWER, AJR_CHARGE, AJR_CONSUMPTION, AJR_DRAIN));
    public static final RegistryObject<ArmorAJRO> ajro_legs = powered("ajro_legs",
            p -> new ArmorAJRO(HbmArmorMaterials.AJR, ArmorSlot.LEGGINGS, p, AJR_POWER, AJR_CHARGE, AJR_CONSUMPTION, AJR_DRAIN));
    public static final RegistryObject<ArmorAJRO> ajro_boots = powered("ajro_boots",
            p -> new ArmorAJRO(HbmArmorMaterials.AJR, ArmorSlot.BOOTS, p, AJR_POWER, AJR_CHARGE, AJR_CONSUMPTION, AJR_DRAIN));

    // ---- ArmorHEV (ModItemsArmor.java:254-266): 1 MHE, 10 kHE/t, 2.5 kHE per point, no idle drain ----
    public static final RegistryObject<ArmorHEV> hev_helmet = powered("hev_helmet",
            p -> new ArmorHEV(HbmArmorMaterials.HEV, ArmorSlot.HELMET, p, 1_000_000L, 10_000L, 2_500L, 0L));
    public static final RegistryObject<ArmorHEV> hev_plate = powered("hev_plate",
            p -> new ArmorHEV(HbmArmorMaterials.HEV, ArmorSlot.CHESTPLATE, p, 1_000_000L, 10_000L, 2_500L, 0L));
    public static final RegistryObject<ArmorHEV> hev_legs = powered("hev_legs",
            p -> new ArmorHEV(HbmArmorMaterials.HEV, ArmorSlot.LEGGINGS, p, 1_000_000L, 10_000L, 2_500L, 0L));
    public static final RegistryObject<ArmorHEV> hev_boots = powered("hev_boots",
            p -> new ArmorHEV(HbmArmorMaterials.HEV, ArmorSlot.BOOTS, p, 1_000_000L, 10_000L, 2_500L, 0L));

    // ---- ArmorDigamma (ModItemsArmor.java:271-286): 10 MHE, 10 kHE/t, 2.5 kHE per point, no idle drain ----
    public static final RegistryObject<ArmorDigamma> fau_helmet = powered("fau_helmet",
            p -> new ArmorDigamma(HbmArmorMaterials.FAU, ArmorSlot.HELMET, p, 10_000_000L, 10_000L, 2_500L, 0L));
    public static final RegistryObject<ArmorDigamma> fau_plate = powered("fau_plate",
            p -> new ArmorDigamma(HbmArmorMaterials.FAU, ArmorSlot.CHESTPLATE, p, 10_000_000L, 10_000L, 2_500L, 0L));
    public static final RegistryObject<ArmorDigamma> fau_legs = powered("fau_legs",
            p -> new ArmorDigamma(HbmArmorMaterials.FAU, ArmorSlot.LEGGINGS, p, 10_000_000L, 10_000L, 2_500L, 0L));
    public static final RegistryObject<ArmorDigamma> fau_boots = powered("fau_boots",
            p -> new ArmorDigamma(HbmArmorMaterials.FAU, ArmorSlot.BOOTS, p, 10_000_000L, 10_000L, 2_500L, 0L));

    // ---- ArmorTrenchmaster (ModItemsArmor.java:319-333): setMaxDamage(0) ----
    public static final RegistryObject<ArmorTrenchmaster> trenchmaster_helmet = trenchmaster("trenchmaster_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorTrenchmaster> trenchmaster_plate = trenchmaster("trenchmaster_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorTrenchmaster> trenchmaster_legs = trenchmaster("trenchmaster_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorTrenchmaster> trenchmaster_boots = trenchmaster("trenchmaster_boots", ArmorSlot.BOOTS);

    /** A plain piece with its 1.7 sprite; the material is read when the item is built (see {@link ModArmorItems}). */
    private static RegistryObject<ArmorFSB> plain(String name, Supplier<GolemArmorMaterial> material, ArmorSlot slot) {
        RegistryObject<ArmorFSB> ro = REG.item(name, p -> new ArmorFSB(material.get(), slot, p), p -> p.stacksTo(1))
                .transform(Presets.sprite());
        // Same convention as ModArmorItems.plain: 1.7 ItemArmor's combat tab does not exist in the port.
        ModTabs.addTo(HbmTab.CONTROL, ro);
        return ro;
    }

    private static RegistryObject<ArmorBismuth> bismuth(String name, ArmorSlot slot) {
        RegistryObject<ArmorBismuth> ro = REG.item(name,
                p -> new ArmorBismuth(HbmArmorMaterials.BISMUTH, slot, p), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONTROL, ro);
        return ro;
    }

    private static RegistryObject<ArmorEuphemium> euphemium(String name, ArmorSlot slot) {
        return REG.item(name,
                p -> new ArmorEuphemium(HbmArmorMaterials.EUPHEMIUM, slot, CompatArmorItem.unbreakable(p)),
                p -> p.stacksTo(1).rarity(Rarity.EPIC)).transform(Presets.sprite());
    }

    private static RegistryObject<ArmorTrenchmaster> trenchmaster(String name, ArmorSlot slot) {
        RegistryObject<ArmorTrenchmaster> ro = REG.item(name,
                p -> new ArmorTrenchmaster(HbmArmorMaterials.TRENCHMASTER, slot, CompatArmorItem.unbreakable(p)),
                p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONTROL, ro);
        return ro;
    }

    private static <T extends ArmorFSBPowered> RegistryObject<T> powered(String name, Function<Item.Properties, T> factory) {
        RegistryObject<T> ro = REG.item(name, factory, p -> p.stacksTo(1));
        // Same convention as ModArmorItems.powered.
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    /** Set bonuses, radiation resistance, hazard classes and worn models; see {@link ModArmorItems#configure()}. */
    public static void configure() {
        // HazmatRegistry: alloy 0.07 (15%), cmb 1.3 (95%), euphemium 10 (<100%). Neither alloy nor dnt had a bonus.
        alloy_plate.get().cloneStats(alloy_helmet.get());
        alloy_legs.get().cloneStats(alloy_helmet.get());
        alloy_boots.get().cloneStats(alloy_helmet.get());

        ArmorFSB cmb = cmb_helmet.get()
                .addEffect(Effects.instance(Effects.speed(), 20, 2))
                .addEffect(Effects.instance(Effects.haste(), 20, 2))
                .addEffect(Effects.instance(Effects.strength(), 20, 4));
        cmb_plate.get().cloneStats(cmb);
        cmb_legs.get().cloneStats(cmb);
        cmb_boots.get().cloneStats(cmb);

        dnt_plate.get().cloneStats(dnt_helmet.get());
        dnt_legs.get().cloneStats(dnt_helmet.get());
        dnt_boots.get().cloneStats(dnt_helmet.get());

        ArmorFSB bismuth = bismuth_helmet.get()
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 6))
                .addEffect(Effects.instance(Effects.speed(), 20, 6))
                .addEffect(Effects.instance(MobEffects.REGENERATION, 20, 1))
                .addEffect(Effects.instance(MobEffects.NIGHT_VISION, 15 * 20, 0))
                .setDashCount(3)
                .setArmorModel("bismuth");
        bismuth_plate.get().cloneStats(bismuth);
        bismuth_legs.get().cloneStats(bismuth);
        bismuth_boots.get().cloneStats(bismuth);

        // 1.7 ArmorUtil.register: the euphemium helmet protects against every hazard class.

        configurePowerArmor(ajr_helmet.get(), "ajr");
        ajr_plate.get().cloneStats(ajr_helmet.get());
        ajr_legs.get().cloneStats(ajr_helmet.get());
        ajr_boots.get().cloneStats(ajr_helmet.get());
        configurePowerArmor(ajro_helmet.get(), "ajro");
        ajro_plate.get().cloneStats(ajro_helmet.get());
        ajro_legs.get().cloneStats(ajro_helmet.get());
        ajro_boots.get().cloneStats(ajro_helmet.get());

        ArmorFSB hev = hev_helmet.get()
                .addEffect(Effects.instance(Effects.speed(), 20, 1))
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 0))
                .setHasGeigerSound(true)
                .setHasCustomGeiger(true)
                .setHides(EnumPlayerPart.HAT)
                .setArmorModel("hev");
        hev_plate.get().cloneStats(hev);
        hev_legs.get().cloneStats(hev);
        hev_boots.get().cloneStats(hev);

        ArmorFSB fau = fau_helmet.get()
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 1))
                .setHasGeigerSound(true)
                .enableThermalSight(true)
                .setHasHardLanding(true)
                .setStep(ModSounds.METAL_STEP)
                .setJump(ModSounds.IRON_JUMP)
                .setFall(ModSounds.IRON_LAND)
                .setHides(EnumPlayerPart.HAT)
                .setArmorModel("fau");
        fau_plate.get().cloneStats(fau).setFullSetForHide();
        fau_legs.get().cloneStats(fau).setHides(EnumPlayerPart.LEFT_LEG, EnumPlayerPart.RIGHT_LEG).setFullSetForHide();
        fau_boots.get().cloneStats(fau);

        ArmorFSB trench = trenchmaster_helmet.get()
                .addEffect(Effects.instance(Effects.strength(), 20, 2))
                .addEffect(Effects.instance(Effects.haste(), 20, 1))
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 1))
                .addEffect(Effects.instance(Effects.speed(), 20, 0))
                .enableVATS(true)
                .setStepSize(1)
                .setHides(EnumPlayerPart.HAT)
                .setArmorModel("trenchmaster");
        trenchmaster_plate.get().cloneStats(trench);
        trenchmaster_legs.get().cloneStats(trench);
        trenchmaster_boots.get().cloneStats(trench);
    }

    /** The shared AJR/AJRO helmet declaration (ModItemsArmor.java:163-172 and 177-186 are identical). */
    private static void configurePowerArmor(ArmorFSB helmet, String model) {
        helmet.enableVATS(true)
                .setHasGeigerSound(true)
                .setHasHardLanding(true)
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 0))
                .addEffect(Effects.instance(Effects.strength(), 20, 0))
                .setStep(ModSounds.METAL_STEP)
                .setJump(ModSounds.IRON_JUMP)
                .setFall(ModSounds.IRON_LAND)
                .setHides(EnumPlayerPart.HAT)
                .setArmorModel(model);
    }

    /** 1.7 {@code DamageResistanceHandler.initDefaults}, the entries for this family, in source order. */
    public static void registerResistances() {
        // Upstream registers the pants as all four slots of a "set", which no player can ever wear. Kept.
        // hbm:damage_resistance and the resistance of hbm:armor_sets (1.7 DamageResistanceHandler.initDefaults)
    }


    public static void init() {}
}
