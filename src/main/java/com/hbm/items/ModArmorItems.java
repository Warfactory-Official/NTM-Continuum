package com.hbm.items;

import com.hbm.items.armor.ArmorNCRPAMelee;
import com.hbm.items.armor.ArmorNCRPARanged;
import com.hbm.items.armor.ArmorRPAMelee;
import com.hbm.potion.ModEffects;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.Ids;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.gear.ArmorFSBPowered;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.ArmorBJ;
import com.hbm.items.armor.ArmorBJJetpack;
import com.hbm.items.armor.ArmorDNT;
import com.hbm.items.armor.ArmorPAWeapons;
import com.hbm.items.armor.WingsMurk;
import com.hbm.items.armor.JetpackBase;
import com.hbm.items.gear.JetpackRegular;
import com.hbm.items.gear.JetpackBreak;
import com.hbm.items.gear.JetpackVectorized;
import com.hbm.items.gear.JetpackBooster;
import com.hbm.items.gear.JetpackGlider;
import com.hbm.items.gear.IArmorDisableModel.EnumPlayerPart;
import com.hbm.sound.ModSounds;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Effects;
import dev.golem.core.world.GolemArmorMaterial;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;

/**
 * Hand-registered armour. Kept out of the generated {@link ModItems} so codegen never clobbers these
 * stateful {@link ArmorFSB} items, the same arrangement {@link ModGearItems} and {@code ModGunItems}
 * use.
 *
 * <p>Every suit follows upstream's shape: the <b>helmet</b> carries the whole set's configuration and
 * the other three pieces {@code cloneStats} it, so the numbers exist in exactly one place. Statics are
 * declared helmet-first for that reason, since a clone of a not-yet-initialised field would silently copy
 * defaults.
 */
public final class ModArmorItems {

    private ModArmorItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    // Regular steel and titanium armour
    // 1.12.2 ModItems.java:484-491. ArmorFSB has no custom set flags for either set.
    public static final RegistryObject<ArmorFSB> steel_helmet =
            plain("steel_helmet", () -> HbmArmorMaterials.STEEL, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> steel_plate =
            plain("steel_plate", () -> HbmArmorMaterials.STEEL, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> steel_legs =
            plain("steel_legs", () -> HbmArmorMaterials.STEEL, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> steel_boots =
            plain("steel_boots", () -> HbmArmorMaterials.STEEL, ArmorSlot.BOOTS);

    public static final RegistryObject<ArmorFSB> titanium_helmet =
            plain("titanium_helmet", () -> HbmArmorMaterials.TITANIUM, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> titanium_plate =
            plain("titanium_plate", () -> HbmArmorMaterials.TITANIUM, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> titanium_legs =
            plain("titanium_legs", () -> HbmArmorMaterials.TITANIUM, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> titanium_boots =
            plain("titanium_boots", () -> HbmArmorMaterials.TITANIUM, ArmorSlot.BOOTS);

    // Ordinary ArmorFSB families
    public static final RegistryObject<ArmorFSB> cobalt_helmet =
            plain("cobalt_helmet", () -> HbmArmorMaterials.COBALT, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> cobalt_plate =
            plain("cobalt_plate", () -> HbmArmorMaterials.COBALT, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> cobalt_legs =
            plain("cobalt_legs", () -> HbmArmorMaterials.COBALT, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> cobalt_boots =
            plain("cobalt_boots", () -> HbmArmorMaterials.COBALT, ArmorSlot.BOOTS);

    public static final RegistryObject<ArmorFSB> starmetal_helmet =
            plain("starmetal_helmet", () -> HbmArmorMaterials.STARMETAL, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> starmetal_plate =
            plain("starmetal_plate", () -> HbmArmorMaterials.STARMETAL, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> starmetal_legs =
            plain("starmetal_legs", () -> HbmArmorMaterials.STARMETAL, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> starmetal_boots =
            plain("starmetal_boots", () -> HbmArmorMaterials.STARMETAL, ArmorSlot.BOOTS);

    public static final RegistryObject<ArmorFSB> schrabidium_helmet =
            plain("schrabidium_helmet", () -> HbmArmorMaterials.SCHRABIDIUM, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> schrabidium_plate =
            plain("schrabidium_plate", () -> HbmArmorMaterials.SCHRABIDIUM, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> schrabidium_legs =
            plain("schrabidium_legs", () -> HbmArmorMaterials.SCHRABIDIUM, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> schrabidium_boots =
            plain("schrabidium_boots", () -> HbmArmorMaterials.SCHRABIDIUM, ArmorSlot.BOOTS);

    // Taurun (1.12.2 ModItemsArmor.java:308-318): the undead soldier's loadout. The icons are 1.7
    // ArmorTaurun's renderStandard OBJ icons (ArmorSetDeclarations); the worn OBJ body model is a renderer
    // follow-up recorded in the entity port manifest. ----
    public static final RegistryObject<ArmorFSB> taurun_helmet =
            modelled("taurun_helmet", () -> HbmArmorMaterials.TAURUN, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> taurun_plate =
            modelled("taurun_plate", () -> HbmArmorMaterials.TAURUN, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> taurun_legs =
            modelled("taurun_legs", () -> HbmArmorMaterials.TAURUN, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> taurun_boots =
            modelled("taurun_boots", () -> HbmArmorMaterials.TAURUN, ArmorSlot.BOOTS);

    public static final RegistryObject<ArmorFSB> security_helmet =
            plain("security_helmet", () -> HbmArmorMaterials.SECURITY, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> security_plate =
            plain("security_plate", () -> HbmArmorMaterials.SECURITY, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> security_legs =
            plain("security_legs", () -> HbmArmorMaterials.SECURITY, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> security_boots =
            plain("security_boots", () -> HbmArmorMaterials.SECURITY, ArmorSlot.BOOTS);

    /** The old casual robes use steel's numbers and have no full-set bonus. */
    public static final RegistryObject<ArmorFSB> robes_helmet =
            plain("robes_helmet", () -> HbmArmorMaterials.ROBES, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> robes_plate =
            plain("robes_plate", () -> HbmArmorMaterials.ROBES, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> robes_legs =
            plain("robes_legs", () -> HbmArmorMaterials.ROBES, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> robes_boots =
            plain("robes_boots", () -> HbmArmorMaterials.ROBES, ArmorSlot.BOOTS);

    public static final RegistryObject<ArmorFSB> asbestos_helmet =
            plain("asbestos_helmet", () -> HbmArmorMaterials.ASBESTOS, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSB> asbestos_plate =
            plain("asbestos_plate", () -> HbmArmorMaterials.ASBESTOS, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> asbestos_legs =
            plain("asbestos_legs", () -> HbmArmorMaterials.ASBESTOS, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> asbestos_boots =
            plain("asbestos_boots", () -> HbmArmorMaterials.ASBESTOS, ArmorSlot.BOOTS);

    /** Power-assisted armor without a helmet, as in the legacy PAA partial set. */
    public static final RegistryObject<ArmorFSB> paa_plate =
            plain("paa_plate", () -> HbmArmorMaterials.PAA, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSB> paa_legs =
            plain("paa_legs", () -> HbmArmorMaterials.PAA, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSB> paa_boots =
            plain("paa_boots", () -> HbmArmorMaterials.PAA, ArmorSlot.BOOTS);

    // T-51b power armour
    // 1.12.2 ModItems.java:532-544. maxPower 1,000,000 / chargeRate 10,000 / consumption 1,000 / drain 5.
    private static final long T51_POWER = 1_000_000L;
    private static final long T51_CHARGE_RATE = 10_000L;
    private static final long T51_CONSUMPTION = 1_000L;
    private static final long T51_DRAIN = 5L;

    public static final RegistryObject<ArmorFSBPowered> t51_helmet =
            powered("t51_helmet", () -> HbmArmorMaterials.T51, ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSBPowered> t51_plate =
            powered("t51_plate", () -> HbmArmorMaterials.T51, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSBPowered> t51_legs =
            powered("t51_legs", () -> HbmArmorMaterials.T51, ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSBPowered> t51_boots =
            powered("t51_boots", () -> HbmArmorMaterials.T51, ArmorSlot.BOOTS);

    // Blackjack electric armor and its jetpack chest variant
    private static final long BJ_POWER = 10_000_000L;
    private static final long BJ_CHARGE_RATE = 10_000L;
    private static final long BJ_CONSUMPTION = 1_000L;
    private static final long BJ_DRAIN = 100L;

    public static final RegistryObject<ArmorBJ> bj_helmet = bj("bj_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorBJ> bj_plate = bj("bj_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorBJJetpack> bj_plate_jetpack =
            REG.item("bj_plate_jetpack",
                    p -> new ArmorBJJetpack(HbmArmorMaterials.BJ, ArmorSlot.CHESTPLATE, p,
                            BJ_POWER, BJ_CHARGE_RATE, BJ_CONSUMPTION, BJ_DRAIN), p -> p.stacksTo(1));
    public static final RegistryObject<ArmorBJ> bj_legs = bj("bj_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorBJ> bj_boots = bj("bj_boots", ArmorSlot.BOOTS);

    // DNT nanosuit
    private static final long DNS_POWER = 1_000_000_000L;
    private static final long DNS_CHARGE_RATE = 1_000_000L;
    private static final long DNS_CONSUMPTION = 100_000L;
    private static final long DNS_DRAIN = 115L;

    public static final RegistryObject<ArmorDNT> dns_helmet = dns("dns_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorDNT> dns_plate = dns("dns_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorDNT> dns_legs = dns("dns_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorDNT> dns_boots = dns("dns_boots", ArmorSlot.BOOTS);

    private static final long AJR_POWER = 2_500_000L;
    public static final RegistryObject<ArmorFSBPowered> rpa_helmet = paPowered("rpa_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSBPowered> rpa_plate = paPowered("rpa_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSBPowered> rpa_legs = paPowered("rpa_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSBPowered> rpa_boots = paPowered("rpa_boots", ArmorSlot.BOOTS);
    public static final RegistryObject<ArmorFSBPowered> ncrpa_helmet = paPowered("ncrpa_helmet", ArmorSlot.HELMET);
    public static final RegistryObject<ArmorFSBPowered> ncrpa_plate = paPowered("ncrpa_plate", ArmorSlot.CHESTPLATE);
    public static final RegistryObject<ArmorFSBPowered> ncrpa_legs = paPowered("ncrpa_legs", ArmorSlot.LEGGINGS);
    public static final RegistryObject<ArmorFSBPowered> ncrpa_boots = paPowered("ncrpa_boots", ArmorSlot.BOOTS);

    // Direct-wear and armor-module jetpacks
    public static final RegistryObject<JetpackRegular> jetpack_fly =
            jetpack("jetpack_fly", p -> new JetpackRegular(Fluids.KEROSENE, 12_000, p));
    public static final RegistryObject<JetpackBreak> jetpack_break =
            jetpack("jetpack_break", p -> new JetpackBreak(Fluids.KEROSENE, 12_000, p));
    public static final RegistryObject<JetpackVectorized> jetpack_vector =
            jetpack("jetpack_vector", p -> new JetpackVectorized(Fluids.KEROSENE, 16_000, p));
    public static final RegistryObject<JetpackBooster> jetpack_boost =
            jetpack("jetpack_boost", p -> new JetpackBooster(Fluids.BALEFIRE, 32_000, p));
    public static final RegistryObject<JetpackGlider> jetpack_glider =
            glider("jetpack_glider", p -> new JetpackGlider(20_000, p));
    public static final RegistryObject<WingsMurk> wings_murk =
            jetpack("wings_murk", p -> new WingsMurk(true, p));
    public static final RegistryObject<WingsMurk> wings_limp =
            jetpack("wings_limp", p -> new WingsMurk(false, p));

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, bj_plate_jetpack);
    }

    private static RegistryObject<ArmorFSBPowered> powered(String name, Supplier<GolemArmorMaterial> material,
            ArmorSlot slot) {
        RegistryObject<ArmorFSBPowered> ro = REG.item(name,
                p -> new ArmorFSBPowered(material.get(), slot, p, T51_POWER, T51_CHARGE_RATE, T51_CONSUMPTION,
                        T51_DRAIN),
                p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    /** RPA and NCRPA draw 1.7's renderStandard OBJ icons (ArmorRPA/ArmorNCRPA renderers; ArmorSetDeclarations). */
    private static RegistryObject<ArmorFSBPowered> paPowered(String name, ArmorSlot slot) {
        RegistryObject<ArmorFSBPowered> ro = REG.item(name,
                p -> new ArmorFSBPowered(HbmArmorMaterials.AJR, slot, p, AJR_POWER, 10_000L, 1_000L, 25L),
                p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<ArmorBJ> bj(String name, ArmorSlot slot) {
        RegistryObject<ArmorBJ> ro = REG.item(name,
                p -> new ArmorBJ(HbmArmorMaterials.BJ, slot, p, BJ_POWER, BJ_CHARGE_RATE, BJ_CONSUMPTION, BJ_DRAIN),
                p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<ArmorDNT> dns(String name, ArmorSlot slot) {
        RegistryObject<ArmorDNT> ro = REG.item(name,
                p -> new ArmorDNT(HbmArmorMaterials.DNS, slot, p, DNS_POWER, DNS_CHARGE_RATE, DNS_CONSUMPTION, DNS_DRAIN),
                p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static <T extends JetpackBase> RegistryObject<T> jetpack(String name,
                                                                       Function<Item.Properties, T> factory) {
        RegistryObject<T> ro = REG.item(name, factory, p -> p.stacksTo(1)).transform(Presets.sprite());
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<JetpackGlider> glider(String name,
                                                         Function<Item.Properties, JetpackGlider> factory) {
        RegistryObject<JetpackGlider> ro = REG.item(name, factory, p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    /**
     * A regular armour piece with its 1.7 sprite. The material is read when the item is built, not here: the
     * materials are registry-backed, and this class also runs at build time (Dejson) without a game.
     */
    private static RegistryObject<ArmorFSB> plain(String name, Supplier<GolemArmorMaterial> material, ArmorSlot slot) {
        RegistryObject<ArmorFSB> ro = REG.item(name,
                p -> new ArmorFSB(material.get(), slot, p), p -> p.stacksTo(1)).transform(Presets.sprite());
        // ArmorFSB itself did not assign a tab in 1.12.2. The port has no combat tab, so regular armour
        // uses CONTROL rather than disappearing from the creative inventory.
        ModTabs.addTo(HbmTab.CONTROL, ro);
        return ro;
    }

    /** A regular armour piece whose icon is a 1.7 renderStandard OBJ, declared in ArmorSetDeclarations. */
    private static RegistryObject<ArmorFSB> modelled(String name, Supplier<GolemArmorMaterial> material,
                                                     ArmorSlot slot) {
        RegistryObject<ArmorFSB> ro = REG.item(name, p -> new ArmorFSB(material.get(), slot, p), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONTROL, ro);
        return ro;
    }

    /**
     * Applies each suit's configuration, after registration so the items exist.
     *
     * <p>Called from {@code Hazards.init}. Radiation resistance, hazard classes, full sets and damage resistance are the
     * {@code hbm:radiation_resistance}, {@code hbm:protection_classes}, {@code hbm:armor_sets} and
     * {@code hbm:damage_resistance} data maps; what is left here is suit behaviour.
     */
    public static void configure() {
        configurePlainSet(steel_helmet.get(), steel_plate.get(), steel_legs.get(), steel_boots.get(), 0.045D);
        configurePlainSet(titanium_helmet.get(), titanium_plate.get(), titanium_legs.get(), titanium_boots.get(), 0.045D);
        configurePlainSet(cobalt_helmet.get(), cobalt_plate.get(), cobalt_legs.get(), cobalt_boots.get(), 0.125D);
        configurePlainSet(starmetal_helmet.get(), starmetal_plate.get(), starmetal_legs.get(), starmetal_boots.get(), 1.0D);
        configurePlainSet(security_helmet.get(), security_plate.get(), security_legs.get(), security_boots.get(), 0.825D);

        // 1.12.2 ModItemsArmor.java:310-318: strength I, step size 1, hides the hat, full package, 25% rad resist.
        ArmorFSB taurunHelmet = taurun_helmet.get()
                .addEffect(Effects.instance(Effects.strength(), 20, 0))
                .setStepSize(1)
                .setHides(EnumPlayerPart.HAT);
        taurun_plate.get().cloneStats(taurunHelmet);
        taurun_legs.get().cloneStats(taurunHelmet);
        taurun_boots.get().cloneStats(taurunHelmet);

        ArmorFSB schrabHelmet = schrabidium_helmet.get();
        schrabHelmet.addEffect(Effects.instance(Effects.haste(), 20, 2))
                .addEffect(Effects.instance(Effects.strength(), 20, 2))
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 1))
                .addEffect(Effects.instance(Effects.speed(), 20, 2));
        configurePlainSet(schrabHelmet, schrabidium_plate.get(), schrabidium_legs.get(), schrabidium_boots.get(), 3.0D);

        ArmorFSB asbestosHelmet = asbestos_helmet.get().setOverlay(
                Ids.of(Constants.MOD_ID, "textures/misc/overlay_asbestos.png"));

        ArmorFSB paa = paa_plate.get().setNoHelmet(true)
                .addEffect(Effects.instance(Effects.haste(), 30, 0));
        paa_legs.get().cloneStats(paa);
        paa_boots.get().cloneStats(paa);

        ArmorFSB helmet = t51_helmet.get();

        helmet.enableVATS(true)
                .setHasGeigerSound(true)
                .setHasHardLanding(true)
                .addEffect(Effects.instance(Effects.strength(), 20, 0))
                .setHides(EnumPlayerPart.HAT)
                .setArmorModel("t51")
                .setStep(ModSounds.METAL_STEP)
                .setJump(ModSounds.IRON_JUMP)
                .setFall(ModSounds.IRON_LAND);

        // Only the helmet carries the hazard classes: upstream's cloneStats does not copy
        // them, and ArmorRegistry's checks read the HEAD slot. Nor is T-51b registered as a hazmat set:
        // it is not in any of 1.12.2's checkForHazmat lists; its radiation protection is setRadResist
        // alone.
        t51_plate.get().cloneStats(helmet);
        t51_legs.get().cloneStats(helmet);
        t51_boots.get().cloneStats(helmet);

        ArmorBJ bjHelmet = bj_helmet.get();
        bjHelmet.enableVATS(true)
                .enableThermalSight(true)
                .setHasHardLanding(true)
                .setHasGeigerSound(true)
                .addEffect(Effects.instance(Effects.speed(), 30, 1))
                .addEffect(Effects.instance(Effects.jumpBoost(), 30, 0))
                .addEffect(Effects.instance(MobEffects.SATURATION, 30, 0))
                 .addEffect(Effects.instanceRegistered(ModEffects.RADX.get(), 30, 0))
                .setStep(ModSounds.METAL_STEP)
                .setJump(ModSounds.IRON_JUMP)
                .setFall(ModSounds.IRON_LAND);
        bj_plate.get().cloneStats(bjHelmet);
        bj_plate_jetpack.get().cloneStats(bjHelmet);
        bj_legs.get().cloneStats(bjHelmet);
        bj_boots.get().cloneStats(bjHelmet);

        ArmorDNT dnsHelmet = dns_helmet.get();
        dnsHelmet.setHasGeigerSound(true)
                .enableVATS(true)
                .enableThermalSight(true)
                .setHasHardLanding(true)
                .addEffect(Effects.instance(Effects.strength(), 20, 9))
                .addEffect(Effects.instance(Effects.haste(), 20, 7))
                .addEffect(Effects.instance(Effects.jumpBoost(), 20, 2))
                .setHides(EnumPlayerPart.HAT)
                .setStep(ModSounds.METAL_STEP)
                .setJump(ModSounds.IRON_JUMP)
                .setFall(ModSounds.IRON_LAND);
        dns_plate.get().cloneStats(dnsHelmet);
        dns_legs.get().cloneStats(dnsHelmet);
        dns_boots.get().cloneStats(dnsHelmet);

        // Client render registration consumes these neutral model names; the gameplay classes stay client-free.
        bjHelmet.setArmorModel("bj");
        bj_plate.get().setArmorModel("bj");
        bj_plate_jetpack.get().setArmorModel("bj_jetpack");
        bj_legs.get().setArmorModel("bj");
        bj_boots.get().setArmorModel("bj");
        dnsHelmet.setArmorModel("dns");
        dns_plate.get().setArmorModel("dns");
        dns_legs.get().setArmorModel("dns");
        dns_boots.get().setArmorModel("dns");

        ArmorFSBPowered rpa = rpa_helmet.get();
        rpa.addEffect(Effects.instance(Effects.strength(), 20, 2)).enableVATS(true)
                .setHasGeigerSound(true).setHasHardLanding(true)
                .setStep(ModSounds.STEP_POWERED)
                .setJump(ModSounds.STEP_POWERED)
                .setFall(ModSounds.STEP_POWERED);
        rpa_plate.get().cloneStats(rpa);
        rpa_legs.get().cloneStats(rpa);
        rpa_boots.get().cloneStats(rpa);
        for (var piece : List.of(rpa_helmet, rpa_plate, rpa_legs, rpa_boots)) piece.get().setArmorModel("rpa"); // G-C: worn ModelArmorRPA

        ArmorFSBPowered ncrpa = ncrpa_helmet.get();
        ncrpa.addEffect(Effects.instance(Effects.strength(), 20, 2)).enableVATS(true)
                .setHasGeigerSound(true).setHasHardLanding(true)
                .setStep(ModSounds.STEP_POWERED)
                .setJump(ModSounds.STEP_POWERED)
                .setFall(ModSounds.STEP_POWERED);
        ncrpa_plate.get().cloneStats(ncrpa);
        for (var piece : List.of(ncrpa_helmet, ncrpa_plate, ncrpa_legs, ncrpa_boots)) piece.get().setArmorModel("ncrpa"); // G-C: worn ModelArmorNCRPA
        ncrpa_legs.get().cloneStats(ncrpa);
        ncrpa_boots.get().cloneStats(ncrpa);
        ArmorPAWeapons.registerMelee(rpa_plate.get(), new ArmorRPAMelee());
        ArmorPAWeapons.registerMelee(ncrpa_plate.get(), new ArmorNCRPAMelee());
        ArmorPAWeapons.registerRanged(ncrpa_plate.get(), new ArmorNCRPARanged());
        jetpack_fly.get().setArmorModel("jetpack_regular");
        jetpack_break.get().setArmorModel("jetpack_break");
        jetpack_vector.get().setArmorModel("jetpack_vector");
        jetpack_boost.get().setArmorModel("jetpack_booster");
        wings_murk.get().setArmorModel("wings_murk");
        wings_limp.get().setArmorModel("wings_limp");
    }

    private static void configurePlainSet(ArmorFSB helmet, ArmorFSB plate, ArmorFSB legs, ArmorFSB boots,
            double radiationResistance) {
        // Old HazmatRegistry registered 0.045 * the four slot weights for both metal sets.
        plate.cloneStats(helmet);
        legs.cloneStats(helmet);
        boots.cloneStats(helmet);
    }



    public static void init() {}
}
