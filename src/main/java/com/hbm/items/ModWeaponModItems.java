package com.hbm.items;

import com.hbm.registration.Reg;
import com.hbm.registration.Presets;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import dev.golem.core.registry.DeferredRegister;

import java.util.Locale;

/**
 * The four 1.7 {@code ItemEnumMulti} weapon-mod carriers, one item per constant as ntm-next registers them
 * ({@code ModItems.dotted}: {@code <base>_<constant>}, drawn with the 1.7 sprite {@code <base>.<constant>};
 * History §19es-itemvariant).
 */
public final class ModWeaponModItems {

    private ModWeaponModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final ItemFamily<GunFactory.EnumModTest, ItemEnumMulti> weapon_mod_test =
            dotted("weapon_mod_test", GunFactory.EnumModTest.class);
    public static final ItemFamily<GunFactory.EnumModGeneric, ItemEnumMulti> weapon_mod_generic =
            dotted("weapon_mod_generic", GunFactory.EnumModGeneric.class);
    public static final ItemFamily<GunFactory.EnumModSpecial, ItemEnumMulti> weapon_mod_special =
            dotted("weapon_mod_special", GunFactory.EnumModSpecial.class);
    public static final ItemFamily<GunFactory.EnumModCaliber, ItemEnumMulti> weapon_mod_caliber =
            dotted("weapon_mod_caliber", GunFactory.EnumModCaliber.class);

    private static <E extends Enum<E>> ItemFamily<E, ItemEnumMulti> dotted(String base, Class<E> type) {
        ItemFamily<E, ItemEnumMulti> family = ItemFamily.of(type, constant -> {
            String name = constant.name().toLowerCase(Locale.ROOT);
            return REG.item(base + "_" + name, properties -> new ItemEnumMulti(type, constant, properties),
                            properties -> properties.stacksTo(1))
                    .transform(Presets.sprite("hbm:items/" + base + "." + name));
        });
        ModTabs.addStacksTo(HbmTab.WEAPONS, family::stacks);
        return family;
    }

    public static void init() {}
}
