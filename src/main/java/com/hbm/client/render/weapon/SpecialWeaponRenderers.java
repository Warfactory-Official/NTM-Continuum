package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Surface;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Factory and small state queries shared by the special Sedna weapon declarations. */
public final class SpecialWeaponRenderers {

    private SpecialWeaponRenderers() {}

    public static void register(BiConsumer<String, WeaponRenderer> sink) {
        declarations().forEach(sink);
    }

    /** Names match the {@code weapon} values used by the 26.1 item definitions. */
    public static Map<String, WeaponRenderer> declarations() {
        Map<String, WeaponRenderer> out = new LinkedHashMap<>();
        out.put("flamer", new FlamerWeaponRenderer("flamethrower"));
        out.put("flamer_topaz", new FlamerWeaponRenderer("flamethrower_topaz"));
        out.put("flamer_daybreaker", new FlamerWeaponRenderer("flamethrower_daybreaker"));
        out.put("chemthrower", new ChemthrowerWeaponRenderer());
        out.put("panzerschreck", new PanzerschreckWeaponRenderer());
        out.put("stinger", new StingerWeaponRenderer());
        out.put("missile_launcher", new MissileLauncherWeaponRenderer());
        out.put("tesla_cannon", new TeslaCannonWeaponRenderer());
        out.put("laser_pistol", new LaserPistolWeaponRenderer("laser_pistol"));
        out.put("laser_pistol_pew_pew", new LaserPistolWeaponRenderer("laser_pistol_pew_pew"));
        out.put("laser_pistol_morning_glory", new LaserPistolWeaponRenderer("laser_pistol_morning_glory"));
        out.put("tau", new TauWeaponRenderer());
        out.put("fatman", new FatManWeaponRenderer());
        out.put("lasrifle", new LasrifleWeaponRenderer());
        out.put("coilgun", new CoilgunWeaponRenderer());
        out.put("folly", new FollyWeaponRenderer());
        out.put("aberrator", new AberratorWeaponRenderer());
        out.put("aberrator_eott", new EottWeaponRenderer());
        out.put("n_i_4_n_i", new Ni4niWeaponRenderer());
        out.put("charge_thrower", new ChargeThrowerWeaponRenderer());
        out.put("drill", new DrillWeaponRenderer());
        out.put("pa_melee", new PAMeleeWeaponRenderer());
        out.put("fireext", new FireExtWeaponRenderer());
        return Map.copyOf(out);
    }

    public static WeaponRenderer byName(String name) {
        return switch (name) {
            case "flamer" -> new FlamerWeaponRenderer("flamethrower");
            case "flamer_topaz" -> new FlamerWeaponRenderer("flamethrower_topaz");
            case "flamer_daybreaker" -> new FlamerWeaponRenderer("flamethrower_daybreaker");
            case "chemthrower" -> new ChemthrowerWeaponRenderer();
            case "panzerschreck" -> new PanzerschreckWeaponRenderer();
            case "stinger" -> new StingerWeaponRenderer();
            case "missile_launcher" -> new MissileLauncherWeaponRenderer();
            case "tesla_cannon" -> new TeslaCannonWeaponRenderer();
            case "laser_pistol" -> new LaserPistolWeaponRenderer("laser_pistol");
            case "laser_pistol_pew_pew" -> new LaserPistolWeaponRenderer("laser_pistol_pew_pew");
            case "laser_pistol_morning_glory" -> new LaserPistolWeaponRenderer("laser_pistol_morning_glory");
            case "tau" -> new TauWeaponRenderer();
            case "fatman" -> new FatManWeaponRenderer();
            case "lasrifle" -> new LasrifleWeaponRenderer();
            case "coilgun" -> new CoilgunWeaponRenderer();
            case "folly" -> new FollyWeaponRenderer();
            case "aberrator" -> new AberratorWeaponRenderer();
            case "aberrator_eott" -> new EottWeaponRenderer();
            case "n_i_4_n_i" -> new Ni4niWeaponRenderer();
            case "charge_thrower" -> new ChargeThrowerWeaponRenderer();
            case "drill" -> new DrillWeaponRenderer();
            case "pa_melee" -> new PAMeleeWeaponRenderer();
            case "fireext" -> new FireExtWeaponRenderer();
            default -> throw new IllegalArgumentException("Unknown special weapon renderer: " + name);
        };
    }

    static WavefrontObj model(String path) {
        return WavefrontObj.load(Ids.of("hbm", "models/weapons/" + path + ".obj"));
    }

    static Surface texture(String path) {
        return Surface.cutout("hbm:textures/models/weapons/" + path + ".png");
    }

    static Surface noCullTexture(String path) {
        return Surface.cutoutNoCull("hbm:textures/models/weapons/" + path + ".png");
    }

    static org.joml.Vector3fc[] extents(WavefrontObj obj) {
        float[] min = obj.boundsMin();
        float[] max = obj.boundsMax();
        org.joml.Vector3fc[] out = new org.joml.Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            out[i++] = new org.joml.Vector3f(x == 0 ? min[0] : max[0], y == 0 ? min[1] : max[1],
                    z == 0 ? min[2] : max[2]);
        }
        return out;
    }

    static int amount(WeaponPose state) {
        if (!(state.stack.getItem() instanceof ItemGunBaseNT gun)) return 0;
        if (!gun.isValidConfigIndex(0) || gun.getPrimaryMagazine(state.stack, 0) == null) return 0;
        return gun.getPrimaryMagazine(state.stack, 0).getAmount(state.stack, null);
    }

    static String itemPath(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    static String ammoPath(WeaponPose state) {
        if (!(state.stack.getItem() instanceof ItemGunBaseNT gun)
                || !gun.isValidConfigIndex(0) || gun.getPrimaryMagazine(state.stack, 0) == null) return "";
        BulletConfig config = gun.getPrimaryMagazine(state.stack, 0).getType(state.stack, null);
        return config == null || config.id == null ? "" : config.id.getPath();
    }

    static boolean animated(WeaponPose state) {
        return state.animated;
    }

    static boolean fullyAimed(WeaponPose state) {
        return ItemGunBaseNT.prevAimingProgress == 1F && ItemGunBaseNT.aimingProgress == 1F;
    }

    static boolean hasPath(WeaponPose state, String path) {
        return itemPath(state.stack).equals(path);
    }

    static boolean hasUpgrade(WeaponPose state, int id) {
        return XWeaponModManager.hasUpgrade(state.stack, 0, id);
    }

    static boolean hasUpgrade(ItemStack stack, int id) {
        return XWeaponModManager.hasUpgrade(stack, 0, id);
    }

    static boolean hasUpgrade(ItemStack stack, int config, int id) {
        return XWeaponModManager.hasUpgrade(stack, config, id);
    }

    /**
     * How many rounds a belt shows. Upstream prefers a {@code SHELLS} bus over the live magazine
     * whenever the running animation declares one, so a reload can lay the belt down round by round
     * rather than snapping to the count the server has already applied.
     */
    static int shellCount(WeaponPose state, int rail) {
        com.hbm.client.anim.HbmAnimations.Animation running =
                com.hbm.client.anim.HbmAnimations.getRelevantAnim(state.hand, rail);
        if (running != null && running.animation.getBus("SHELLS") != null) {
            return (int) state.bus("SHELLS", rail)[0];
        }
        return state.stack.getItem() instanceof ItemGunBaseNT gun
                ? gun.getPrimaryMagazine(state.stack, rail).getAmount(state.stack, null) : 0;
    }
}
