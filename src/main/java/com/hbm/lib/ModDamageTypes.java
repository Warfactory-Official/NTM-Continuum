package com.hbm.lib;

import dev.golem.core.dejson.Damage;

import static dev.golem.core.dejson.Damage.BYPASSES_ARMOR;
import static dev.golem.core.dejson.Damage.BYPASSES_EFFECTS;
import static dev.golem.core.dejson.Damage.BYPASSES_INVULNERABILITY;
import static dev.golem.core.dejson.Damage.BY_LIVING_NON_PLAYER;
import static dev.golem.core.dejson.Damage.IS_EXPLOSION;
import static dev.golem.core.dejson.Damage.IS_FIRE;
import static dev.golem.core.dejson.Damage.IS_PROJECTILE;
import static dev.golem.core.dejson.Damage.type;

/**
 * Every hbm damage type and the vanilla tags it belongs to, declared through Golem Core's
 * {@code dejson} vocabulary instead of written as JSON.
 *
 * <p>The {@value #FILES_REPLACED} files these declarations stand in for have been deleted: 32
 * damage types and the 6 vanilla tag files that said which of them ignore armour, ignore
 * resistance potions, or count as a projectile. Those were the same fact written twice in two
 * namespaces, and the second copy was the one nothing forced you to update — a damage type added
 * without its {@code bypasses_armor} entry does not fail, it just starts being stopped by a
 * breastplate.
 *
 * <p>{@link ModDamageSources} holds the {@code ResourceKey} for each of these and is what the game
 * code uses; it cannot hold the table itself because the generator runs in a plain JVM and that
 * class is full of Minecraft. {@link com.hbm.gametest.DamageTypeGameTests} holds the two against
 * each other, so a key without a type, or a type without a key, is a failing test.
 */
public final class ModDamageTypes {

    private static final String NS = "hbm";

    /** How many data files the declarations below stand in for; quoted in this class's own javadoc. */
    static final int FILES_REPLACED = 38;

    private static boolean declared;

    private ModDamageTypes() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        Damage.declare(NS,
                type("acid"),
                type("ams").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS, BYPASSES_INVULNERABILITY),
                type("ams_core").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("asbestos").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("bang").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("blackhole").tags(BYPASSES_ARMOR),
                type("blacklung").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("blender").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                // old-hbm ModDamageSource crucible/slicer: setDamageIsAbsolute().setDamageBypassesArmor()
                type("crucible").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("slicer").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("broadcast").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("boil").tags(IS_FIRE),
                type("digamma").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS, BYPASSES_INVULNERABILITY),
                type("electricity").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("enervation").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("euthanized_self").message("euthanizedSelf").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("euthanized_self2").message("euthanizedSelf2").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("hard_landing").scaling(BY_LIVING_NON_PLAYER).tags(BYPASSES_ARMOR),
                type("ice"),
                type("lead").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("lunar").tags(BYPASSES_ARMOR),
                type("microwave").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("mku").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("monoxide").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("overdose").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("radiation").tags(BYPASSES_ARMOR),
                type("spikes").tags(BYPASSES_ARMOR),
                type("taint").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),

                type("acid_player").message("acidPlayer").exhaustion(0.1),
                type("boat").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("boxcar").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("building").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("combine").message("cmb").tags(BYPASSES_ARMOR, IS_PROJECTILE),
                type("electric").exhaustion(0.1),
                type("emplacer").message("chopperBullet").exhaustion(0.1).tags(IS_PROJECTILE),
                type("exhaust").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("explosive").exhaustion(0.1).tags(IS_EXPLOSION),
                type("fire").exhaustion(0.1).tags(IS_FIRE),
                type("flamethrower").exhaustion(0.1),
                type("laser").exhaustion(0.1),
                type("meteorite").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("mud_poisoning").message("mudPoisoning").tags(BYPASSES_ARMOR),
                type("nuclear_blast").message("nuclearBlast").exhaustion(0.1).tags(IS_EXPLOSION),
                type("revolver_bullet").message("revolverBullet").exhaustion(0.1).tags(IS_PROJECTILE),
                type("subatomic").exhaustion(0.1),
                type("tau").tags(BYPASSES_ARMOR, IS_PROJECTILE),
                // 1.7 ItemBoltgun: DamageSource.causePlayerDamage(player).setDamageBypassesArmor(), so vanilla's
                // "was slain by" player message
                type("boltgun").message("player").tags(BYPASSES_ARMOR),

                type("cloud").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),
                type("pink_cloud").message("pc").tags(BYPASSES_ARMOR, BYPASSES_EFFECTS),

                type("bullet_electric").projectile(),
                type("bullet_explosive").projectile().tags(IS_EXPLOSION),
                type("bullet_fire").projectile().tags(IS_FIRE),
                type("bullet_laser").projectile(),
                type("bullet_microwave").projectile(),
                type("bullet_other").projectile(),
                type("bullet_physical").projectile(),
                type("bullet_piercing").message("bullet_physical").projectile().tags(BYPASSES_ARMOR),
                type("bullet_plasma").projectile(),
                type("bullet_subatomic").projectile(),
                type("rubble").projectile(),
                type("shrapnel").projectile());
    }
}
