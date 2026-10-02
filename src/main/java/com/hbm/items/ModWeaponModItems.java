package com.hbm.items;

import com.hbm.registration.Reg;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.ItemModels;
import dev.golem.core.dejson.Model;
import java.util.function.UnaryOperator;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/** The four source ItemEnumMulti weapon-mod carriers, flattened onto ITEM_VARIANT. */
public final class ModWeaponModItems {

    private ModWeaponModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemEnumMulti> weapon_mod_test = register(
            "weapon_mod_test", GunFactory.EnumModTest.class,
            d -> d
                    .models(Model.parent("hbm:item/weapon_mod_test.firerate"))
                    .definition(ItemModels.select("hbm:item_variant")
                            .when("0", ItemModels.model("hbm:item/weapon_mod_test.firerate"))
                            .when("1", ItemModels.model("hbm:item/weapon_mod_test.damage"))
                            .when("2", ItemModels.model("hbm:item/weapon_mod_test.multi"))
                            .when("3", ItemModels.model("hbm:item/weapon_mod_test.override_2_5"))
                            .when("4", ItemModels.model("hbm:item/weapon_mod_test.override_5"))
                            .when("5", ItemModels.model("hbm:item/weapon_mod_test.override_7_5"))
                            .when("6", ItemModels.model("hbm:item/weapon_mod_test.override_10"))
                            .when("7", ItemModels.model("hbm:item/weapon_mod_test.override_12_5"))
                            .when("8", ItemModels.model("hbm:item/weapon_mod_test.override_15"))
                            .when("9", ItemModels.model("hbm:item/weapon_mod_test.override_20"))
                            .fallback(ItemModels.model("hbm:item/weapon_mod_test.firerate"))));
    public static final RegistryObject<ItemEnumMulti> weapon_mod_generic = register(
            "weapon_mod_generic", GunFactory.EnumModGeneric.class,
            d -> d
                    .models(Model.parent("hbm:item/weapon_mod_generic.iron_damage"))
                    .definition(ItemModels.select("hbm:item_variant")
                            .when("0", ItemModels.model("hbm:item/weapon_mod_generic.iron_damage"))
                            .when("1", ItemModels.model("hbm:item/weapon_mod_generic.iron_dura"))
                            .when("2", ItemModels.model("hbm:item/weapon_mod_generic.steel_damage"))
                            .when("3", ItemModels.model("hbm:item/weapon_mod_generic.steel_dura"))
                            .when("4", ItemModels.model("hbm:item/weapon_mod_generic.dura_damage"))
                            .when("5", ItemModels.model("hbm:item/weapon_mod_generic.dura_dura"))
                            .when("6", ItemModels.model("hbm:item/weapon_mod_generic.desh_damage"))
                            .when("7", ItemModels.model("hbm:item/weapon_mod_generic.desh_dura"))
                            .when("8", ItemModels.model("hbm:item/weapon_mod_generic.wsteel_damage"))
                            .when("9", ItemModels.model("hbm:item/weapon_mod_generic.wsteel_dura"))
                            .when("10", ItemModels.model("hbm:item/weapon_mod_generic.ferro_damage"))
                            .when("11", ItemModels.model("hbm:item/weapon_mod_generic.ferro_dura"))
                            .when("12", ItemModels.model("hbm:item/weapon_mod_generic.tcalloy_damage"))
                            .when("13", ItemModels.model("hbm:item/weapon_mod_generic.tcalloy_dura"))
                            .when("14", ItemModels.model("hbm:item/weapon_mod_generic.bigmt_damage"))
                            .when("15", ItemModels.model("hbm:item/weapon_mod_generic.bigmt_dura"))
                            .when("16", ItemModels.model("hbm:item/weapon_mod_generic.bronze_damage"))
                            .when("17", ItemModels.model("hbm:item/weapon_mod_generic.bronze_dura"))
                            .fallback(ItemModels.model("hbm:item/weapon_mod_generic.iron_damage"))));
    public static final RegistryObject<ItemEnumMulti> weapon_mod_special = register(
            "weapon_mod_special", GunFactory.EnumModSpecial.class,
            d -> d
                    .models(Model.parent("hbm:item/weapon_mod_special.silencer"))
                    .definition(ItemModels.select("hbm:item_variant")
                            .when("0", ItemModels.model("hbm:item/weapon_mod_special.silencer"))
                            .when("1", ItemModels.model("hbm:item/weapon_mod_special.scope"))
                            .when("2", ItemModels.model("hbm:item/weapon_mod_special.saw"))
                            .when("3", ItemModels.model("hbm:item/weapon_mod_special.greasegun"))
                            .when("4", ItemModels.model("hbm:item/weapon_mod_special.slowdown"))
                            .when("5", ItemModels.model("hbm:item/weapon_mod_special.speedup"))
                            .when("6", ItemModels.model("hbm:item/weapon_mod_special.choke"))
                            .when("7", ItemModels.model("hbm:item/weapon_mod_special.speedloader"))
                            .when("8", ItemModels.model("hbm:item/weapon_mod_special.furniture_green"))
                            .when("9", ItemModels.model("hbm:item/weapon_mod_special.furniture_black"))
                            .when("10", ItemModels.model("hbm:item/weapon_mod_special.bayonet"))
                            .when("11", ItemModels.model("hbm:item/weapon_mod_special.stack_mag"))
                            .when("12", ItemModels.model("hbm:item/weapon_mod_special.skin_saturnite"))
                            .when("13", ItemModels.model("hbm:item/weapon_mod_special.las_shotgun"))
                            .when("14", ItemModels.model("hbm:item/weapon_mod_special.las_capacitor"))
                            .when("15", ItemModels.model("hbm:item/weapon_mod_special.las_auto"))
                            .when("16", ItemModels.model("hbm:item/weapon_mod_special.nickel"))
                            .when("17", ItemModels.model("hbm:item/weapon_mod_special.doubloons"))
                            .when("18", ItemModels.model("hbm:item/weapon_mod_special.drill_hss"))
                            .when("19", ItemModels.model("hbm:item/weapon_mod_special.drill_weaponsteel"))
                            .when("20", ItemModels.model("hbm:item/weapon_mod_special.drill_tcalloy"))
                            .when("21", ItemModels.model("hbm:item/weapon_mod_special.drill_saturnite"))
                            .when("22", ItemModels.model("hbm:item/weapon_mod_special.engine_diesel"))
                            .when("23", ItemModels.model("hbm:item/weapon_mod_special.engine_aviation"))
                            .when("24", ItemModels.model("hbm:item/weapon_mod_special.engine_electric"))
                            .when("25", ItemModels.model("hbm:item/weapon_mod_special.engine_turbo"))
                            .when("26", ItemModels.model("hbm:item/weapon_mod_special.magnet"))
                            .when("27", ItemModels.model("hbm:item/weapon_mod_special.sifter"))
                            .when("28", ItemModels.model("hbm:item/weapon_mod_special.canisters"))
                            .fallback(ItemModels.model("hbm:item/weapon_mod_special.silencer"))));
    public static final RegistryObject<ItemEnumMulti> weapon_mod_caliber = register(
            "weapon_mod_caliber", GunFactory.EnumModCaliber.class,
            d -> d
                    .models(Model.parent("hbm:item/weapon_mod_caliber.p9"))
                    .definition(ItemModels.select("hbm:item_variant")
                            .when("0", ItemModels.model("hbm:item/weapon_mod_caliber.p9"))
                            .when("1", ItemModels.model("hbm:item/weapon_mod_caliber.p45"))
                            .when("2", ItemModels.model("hbm:item/weapon_mod_caliber.p22"))
                            .when("3", ItemModels.model("hbm:item/weapon_mod_caliber.m357"))
                            .when("4", ItemModels.model("hbm:item/weapon_mod_caliber.m44"))
                            .when("5", ItemModels.model("hbm:item/weapon_mod_caliber.r556"))
                            .when("6", ItemModels.model("hbm:item/weapon_mod_caliber.r762"))
                            .when("7", ItemModels.model("hbm:item/weapon_mod_caliber.bmg50"))
                            .fallback(ItemModels.model("hbm:item/weapon_mod_caliber.p9"))));

    /**
     * One item per mod family; {@code icon} is its variant-selected item definition over a base model. The base
     * model carries no {@code overrides}: 26.1 selects through the definition, and below 1.21.4 legacy-assets
     * writes the overrides onto this model from the same definition (every case, ascending).
     */
    private static RegistryObject<ItemEnumMulti> register(String id, Class<? extends Enum<?>> variants,
                                                          UnaryOperator<Dejson.ItemDeclaration> icon) {
        RegistryObject<ItemEnumMulti> item = REG.item(id,
                properties -> new ItemEnumMulti(variants, true, true, properties),
                properties -> properties.stacksTo(1)).icon(icon);
        ModTabs.addVariantsTo(HbmTab.WEAPONS, item);
        return item;
    }

    public static void init() {}
}
