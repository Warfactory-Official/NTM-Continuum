package com.hbm.client.render.weapon;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Registration declarations for the conventional original-hbm Sedna weapons.
 *
 * <p>This helper deliberately does not modify {@link WeaponRenderers}. The existing registry is owned
 * by the proving slice; callers can install this batch with {@link #register(BiConsumer)} when the
 * corresponding gun items and item definitions are present. Models and render types are not resolved
 * until a renderer is first used, so the helper is safe to call before resources are ready.
 */
public final class BallisticWeaponRenderers {
    private static final Map<String, WeaponRenderer> DECLARATIONS = createDeclarations();

    private BallisticWeaponRenderers() {}

    public static void register(BiConsumer<String, WeaponRenderer> sink) {
        declarations().forEach(sink);
    }

    public static WeaponRenderer byName(String name) {
        WeaponRenderer renderer = declarations().get(name);
        if (renderer == null) throw new IllegalArgumentException("Unknown ballistic weapon renderer: " + name);
        return renderer;
    }

    /** Names match the {@code weapon} values that the asset agent should place in item definitions. */
    public static Map<String, WeaponRenderer> declarations() {
        return DECLARATIONS;
    }

    private static Map<String, WeaponRenderer> createDeclarations() {
        Map<String, WeaponRenderer> out = new LinkedHashMap<>();
        out.put("maresleg", new MareslegWeaponRenderer("maresleg"));
        out.put("maresleg_akimbo", new MareslegAkimboWeaponRenderer());
        out.put("maresleg_broken", new MareslegWeaponRenderer("maresleg_broken"));
        out.put("flaregun", new FlaregunWeaponRenderer());
        out.put("carbine", new CarbineWeaponRenderer());
        out.put("liberator", new LiberatorWeaponRenderer());
        out.put("congolake", new CongoLakeWeaponRenderer());
        out.put("spas12", new Spas12WeaponRenderer());
        out.put("g3_zebra", new G3ZebraWeaponRenderer());
        out.put("mk108", new Mk108WeaponRenderer());
        out.put("amat", new AmatWeaponRenderer("amat"));
        out.put("amat_subtlety", new AmatWeaponRenderer("amat_subtlety"));
        out.put("amat_penance", new AmatWeaponRenderer("amat_penance"));
        out.put("m2", new M2WeaponRenderer());
        out.put("autoshotgun", new ShredderWeaponRenderer("shredder"));
        out.put("autoshotgun_shredder", new ShredderWeaponRenderer("shredder_orig"));
        out.put("autoshotgun_sexy", new SexyWeaponRenderer("sexy_real_no_fake"));
        out.put("autoshotgun_heretic", new SexyWeaponRenderer("sexy_heretic"));
        out.put("quadro", new QuadroWeaponRenderer());
        out.put("minigun", new MinigunWeaponRenderer("minigun"));
        out.put("minigun_lacunae", new MinigunWeaponRenderer("minigun_lacunae"));
        out.put("minigun_dual", new MinigunDualWeaponRenderer());
        out.put("stg77", new Stg77WeaponRenderer());
        out.put("mas36", new Mas36WeaponRenderer());
        out.put("bolter", new BolterWeaponRenderer());
        out.put("double_barrel", new DoubleBarrelWeaponRenderer("double_barrel"));
        out.put("double_barrel_sacred_dragon", new DoubleBarrelWeaponRenderer("double_barrel_sacred_dragon"));
        return Map.copyOf(out);
    }
}
