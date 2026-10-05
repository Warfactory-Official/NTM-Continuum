package com.hbm.items;

import dev.golem.core.dejson.Equipment;

/**
 * Equipment assets of every armour set: the texture each worn piece draws on the body and leggings
 * layers. Below 26.1 {@code legacy-assets} copies these textures to the paths the old armour layer reads.
 *
 * <p>Touches no Minecraft class: Dejson runs {@link #declare()} in a plain JVM at build time.
 */
public final class ArmorEquipmentDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private ArmorEquipmentDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        part0();
        part1();
    }

    private static void part0() {
        Equipment.humanoid(NS, "ajr", "hbm:ajr");
        Equipment.humanoid(NS, "alloy", "hbm:alloy");
        Equipment.humanoid(NS, "asbestos", "hbm:asbestos");
        Equipment.humanoid(NS, "ashglasses", "hbm:ashglasses");
        Equipment.humanoid(NS, "bismuth", "hbm:bismuth");
        Equipment.humanoid(NS, "bj", "hbm:bj");
        Equipment.humanoid(NS, "cmb", "hbm:cmb");
        Equipment.humanoid(NS, "cobalt", "hbm:cobalt");
        Equipment.humanoid(NS, "dieselsuit", "hbm:dieselsuit");
        Equipment.humanoid(NS, "dns", "hbm:dns");
        Equipment.humanoid(NS, "dnt", "hbm:dnt");
        Equipment.humanoid(NS, "envsuit", "hbm:envsuit");
        Equipment.humanoid(NS, "euphemium", "hbm:euphemium");
        Equipment.humanoid(NS, "fau", "hbm:fau");
        Equipment.humanoid(NS, "gas_mask", "hbm:gas_mask");
        Equipment.humanoid(NS, "goggles", "hbm:goggles");
        Equipment.humanoid(NS, "hazmat", "hbm:hazmat");
        Equipment.humanoid(NS, "hazmat_grey", "hbm:hazmat_grey");
        Equipment.humanoid(NS, "hazmat_grey_hood", "hbm:hazmat_grey_hood");
        Equipment.humanoid(NS, "hazmat_paa", "hbm:hazmat_paa");
        Equipment.humanoid(NS, "hazmat_red", "hbm:hazmat_red");
        Equipment.humanoid(NS, "hazmat_red_hood", "hbm:hazmat_red_hood");
        Equipment.humanoid(NS, "hev", "hbm:hev");
        Equipment.of(NS, "infamy").layer("humanoid", "hbm:infamy").register();
        Equipment.humanoid(NS, "jackt", "hbm:jackt");
        Equipment.humanoid(NS, "jackt2", "hbm:jackt2");
        Equipment.of(NS, "jetpack").layer("humanoid", "hbm:jetpack").register();
        Equipment.humanoid(NS, "liquidator", "hbm:liquidator");
        Equipment.humanoid(NS, "no9", "hbm:no9");
        Equipment.humanoid(NS, "nossy_hat", "hbm:nossy_hat");
        Equipment.humanoid(NS, "paa", "hbm:paa");
        Equipment.humanoid(NS, "rag_damp", "hbm:rag_damp");
        Equipment.humanoid(NS, "rag_piss", "hbm:rag_piss");
    }

    private static void part1() {
        Equipment.humanoid(NS, "robes", "hbm:robes");
        Equipment.humanoid(NS, "schrabidium", "hbm:schrabidium");
        Equipment.humanoid(NS, "security", "hbm:security");
        Equipment.humanoid(NS, "starmetal", "hbm:starmetal");
        Equipment.humanoid(NS, "steamsuit", "hbm:steamsuit");
        Equipment.humanoid(NS, "steel", "hbm:steel");
        Equipment.humanoid(NS, "t51", "hbm:t51");
        Equipment.humanoid(NS, "taurun", "hbm:taurun");
        Equipment.humanoid(NS, "titanium", "hbm:titanium");
        Equipment.humanoid(NS, "trenchmaster", "hbm:trenchmaster");
        Equipment.humanoid(NS, "zirconium", "hbm:zirconium");
    }
}
