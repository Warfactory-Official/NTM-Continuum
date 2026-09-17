package com.hbm.items;

import java.util.Set;

/**
 * The ammo enum tables from original-hbm.  The modern registry flattens each value into its own item,
 * but keeps this table so ordinal order and the legacy internal names cannot drift independently.
 */
public final class ItemAmmoEnums {

    private ItemAmmoEnums() {}

    public enum AmmoItemTrait {
        CON_ACCURACY2, CON_DAMAGE, CON_HEAVY_WEAR, CON_LING_FIRE, CON_NN, CON_NO_DAMAGE,
        CON_NO_EXPLODE1, CON_NO_EXPLODE2, CON_NO_EXPLODE3, CON_NO_FIRE, CON_NO_MIRV,
        CON_NO_PROJECTILE, CON_PENETRATION, CON_RADIUS, CON_RANGE2, CON_SING_PROJECTILE,
        CON_SPEED, CON_SUPER_WEAR, CON_WEAR, NEU_40MM, NEU_BLANK, NEU_BOAT, NEU_BOXCAR,
        NEU_BUILDING, NEU_CHLOROPHYTE, NEU_ERASER, NEU_FUN, NEU_HEAVY_METAL, NEU_HOMING,
        NEU_JOLT, NEU_LESS_BOUNCY, NEU_MASKMAN_FLECHETTE, NEU_MASKMAN_METEORITE,
        NEU_MORE_BOUNCY, NEU_NO_BOUNCE, NEU_NO_CON, NEU_STARMETAL, NEU_TRACER, NEU_UHH,
        NEU_LEADBURSTER, NEU_WARCRIME1, NEU_WARCRIME2, PRO_ACCURATE1, PRO_ACCURATE2,
        PRO_BALEFIRE, PRO_BOMB_COUNT, PRO_CAUSTIC, PRO_CHAINSAW, PRO_CHLORINE, PRO_DAMAGE,
        PRO_DAMAGE_SLIGHT, PRO_EMP, PRO_EXPLOSIVE, PRO_FALLOUT, PRO_FIT_357, PRO_FLAMES,
        PRO_GRAVITY, PRO_HEAVY_DAMAGE, PRO_INCENDIARY, PRO_LUNATIC, PRO_MARAUDER, PRO_MINING,
        PRO_NO_GRAVITY, PRO_NUCLEAR, PRO_PENETRATION, PRO_PERCUSSION, PRO_PHOSPHORUS,
        PRO_PHOSPHORUS_SPLASH, PRO_POISON_GAS, PRO_RADIUS, PRO_RADIUS_HIGH, PRO_RANGE,
        PRO_ROCKET, PRO_ROCKET_PROPELLED, PRO_SHRAPNEL, PRO_SPEED, PRO_STUNNING, PRO_TOXIC,
        PRO_WEAR, PRO_WITHERING
    }

    public enum AmmoFireExt implements IAmmoItemEnum {
        WATER("ammo_fireext"),
        FOAM("ammo_fireext_foam"),
        SAND("ammo_fireext_sand");

        public static final AmmoFireExt[] VALUES = values();
        private final Set<AmmoItemTrait> traits;
        private final String internalName;

        AmmoFireExt(String internalName, AmmoItemTrait... traits) {
            this.traits = traits == null ? Set.of() : Set.copyOf(java.util.List.of(traits));
            this.internalName = internalName;
        }

        @Override public Set<AmmoItemTrait> getTraits() { return traits; }
        @Override public String getInternalName() { return internalName; }
    }

    public enum AmmoMisc implements IAmmoItemEnum {
        // LUNA_SNIPER was commented out in original-hbm and is intentionally absent here.
        DGK("ammo_dkg");

        public static final AmmoMisc[] VALUES = values();
        private final Set<AmmoItemTrait> traits;
        private final String internalName;

        AmmoMisc(String internalName, AmmoItemTrait... traits) {
            this.traits = traits == null ? Set.of() : Set.copyOf(java.util.List.of(traits));
            this.internalName = internalName;
        }

        @Override public Set<AmmoItemTrait> getTraits() { return traits; }
        @Override public String getInternalName() { return internalName; }
    }

    public enum Ammo240Shell implements IAmmoItemEnum {
        STOCK("ammo_shell"),
        EXPLOSIVE("ammo_shell_explosive"),
        APFSDS_T("ammo_shell_apfsds_t"),
        APFSDS_DU("ammo_shell_apfsds_du"),
        W9("ammo_shell_w9");

        public static final Ammo240Shell[] VALUES = values();
        private final Set<AmmoItemTrait> traits;
        private final String internalName;

        Ammo240Shell(String internalName, AmmoItemTrait... traits) {
            this.traits = traits == null ? Set.of() : Set.copyOf(java.util.List.of(traits));
            this.internalName = internalName;
        }

        @Override public Set<AmmoItemTrait> getTraits() { return traits; }
        @Override public String getInternalName() { return internalName; }
    }

    public interface IAmmoItemEnum {
        Set<AmmoItemTrait> getTraits();
        String getInternalName();
    }
}
