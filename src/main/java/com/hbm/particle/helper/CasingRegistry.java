package com.hbm.particle.helper;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.SpentCasing.CasingType;

/** The unique source casing table, installed after every source ammo config exists. */
public final class CasingRegistry {
    private static boolean installed;

    private CasingRegistry() {}

    public static synchronized void install() {
        if (installed) return;

        register("ammo_debug", "DEBUG0", CasingType.STRAIGHT, 1.5F, 1F, 1.5F, SpentCasing.COLOR_CASE_44);
        register("ammo_debug_shot", "DEBUG1", CasingType.STRAIGHT, 1.5F, 1F, 1.5F, SpentCasing.COLOR_CASE_44);

        register("m44_bp", "m44bp", CasingType.STRAIGHT, 1F, 1F, 1F, SpentCasing.COLOR_CASE_BRASS,
                1F, 0.5D, 60, 20);
        register("m44_sp", "m44", CasingType.STRAIGHT, 1F, 1F, 1F, SpentCasing.COLOR_CASE_BRASS,
                1F, 0.5D, 60, 20);
        register("m44_fmj", "m44fmj", CasingType.STRAIGHT, 1F, 1F, 1F, SpentCasing.COLOR_CASE_BRASS,
                1F, 0.5D, 60, 20);
        register("m44_jhp", "m44jhp", CasingType.STRAIGHT, 1F, 1F, 1F, SpentCasing.COLOR_CASE_BRASS,
                1F, 0.5D, 60, 20);
        register("m44_ap", "m44ap", CasingType.STRAIGHT, 1F, 1F, 1F, SpentCasing.COLOR_CASE_44,
                1F, 0.5D, 60, 20);
        register("m44_express", "m44express", CasingType.STRAIGHT, 1F, 1F, 1F,
                SpentCasing.COLOR_CASE_BRASS, 1F, 0.5D, 60, 20);
        register("m44_equestrian_pip", "m44equestrianPip", CasingType.STRAIGHT, 1F, 1F, 1F,
                SpentCasing.COLOR_CASE_EQUESTRIAN, 1F, 0.5D, 60, 20);
        register("m44_equestrian_mn7", "m44equestrianMn7", CasingType.STRAIGHT, 1F, 1F, 1F,
                SpentCasing.COLOR_CASE_EQUESTRIAN, 1F, 0.5D, 60, 20);

        registerSmallStraight("p9_sp", "p9", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p9_fmj", "p9fmj", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p9_jhp", "p9jhp", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p9_ap", "p9ap", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_44);

        registerSmallStraight("p45_sp", "p45", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p45_fmj", "p45fmj", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p45_jhp", "p45jhp", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p45_ap", "p45ap", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_44);
        registerSmallStraight("p45_du", "p45du", 1F, 1F, 0.75F, SpentCasing.COLOR_CASE_44);

        registerSmallStraight("p22_sp", "p22", 0.5F, 0.5F, 0.5F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p22_fmj", "p22fmj", 0.5F, 0.5F, 0.5F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p22_jhp", "p22jhp", 0.5F, 0.5F, 0.5F, SpentCasing.COLOR_CASE_BRASS);
        registerSmallStraight("p22_ap", "p22ap", 0.5F, 0.5F, 0.5F, SpentCasing.COLOR_CASE_44);

        register("r556_sp", "r556", CasingType.BOTTLENECK, 0.8F, 0.8F, 0.8F, SpentCasing.COLOR_CASE_BRASS);
        register("r556_fmj", "r556fmj", CasingType.BOTTLENECK, 0.8F, 0.8F, 0.8F,
                SpentCasing.COLOR_CASE_BRASS);
        register("r556_jhp", "r556jhp", CasingType.BOTTLENECK, 0.8F, 0.8F, 0.8F,
                SpentCasing.COLOR_CASE_BRASS);
        register("r556_ap", "r556ap", CasingType.BOTTLENECK, 0.8F, 0.8F, 0.8F, SpentCasing.COLOR_CASE_44);
        alias("r556_inc_sp", "r556_sp");
        alias("r556_inc_fmj", "r556_fmj");
        alias("r556_inc_jhp", "r556_jhp");
        alias("r556_inc_ap", "r556_ap");

        register("r762_sp", "r762", CasingType.BOTTLENECK, 1F, 1F, 1F, SpentCasing.COLOR_CASE_BRASS);
        register("r762_fmj", "r762fmj", CasingType.BOTTLENECK, 1F, 1F, 1F, SpentCasing.COLOR_CASE_BRASS);
        register("r762_jhp", "r762jhp", CasingType.BOTTLENECK, 1F, 1F, 1F, SpentCasing.COLOR_CASE_BRASS);
        register("r762_ap", "r762ap", CasingType.BOTTLENECK, 1F, 1F, 1F, SpentCasing.COLOR_CASE_44);
        register("r762_du", "r762du", CasingType.BOTTLENECK, 1F, 1F, 1F, SpentCasing.COLOR_CASE_44);
        register("r762_he", "r762he", CasingType.BOTTLENECK, 1F, 1F, 1F, SpentCasing.COLOR_CASE_44);

        register("bmg50_sp", "bmg50", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_BRASS);
        register("bmg50_fmj", "bmg50fmj", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_BRASS);
        register("bmg50_jhp", "bmg50jhp", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_BRASS);
        register("bmg50_ap", "bmg50ap", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_44);
        register("bmg50_du", "bmg50du", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_44);
        register("bmg50_he", "bmg50he", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_44);
        register("bmg50_sm", "bmg50sm", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_44);
        register("bmg50_black", "bmg50black", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_EQUESTRIAN);
        register("bmg50_equestrian", "bmg50equestrian", CasingType.BOTTLENECK, 1.5F, 1.5F, 1.5F,
                SpentCasing.COLOR_CASE_EQUESTRIAN);

        registerShotgun("g12_bp", "12GA_BP", SpentCasing.COLOR_CASE_BRASS, SpentCasing.COLOR_CASE_BRASS);
        registerShotgun("g12_bp_magnum", "12GA_BP_MAGNUM", SpentCasing.COLOR_CASE_BRASS,
                SpentCasing.COLOR_CASE_BRASS);
        registerShotgun("g12_bp_slug", "12GA_BP_SLUG", SpentCasing.COLOR_CASE_BRASS,
                SpentCasing.COLOR_CASE_BRASS);
        registerShotgun("g12", "12GA", 0xB52B2B, SpentCasing.COLOR_CASE_BRASS);
        registerShotgun("g12_slug", "12GA_SLUG", 0x393939, SpentCasing.COLOR_CASE_BRASS);
        registerShotgun("g12_flechette", "12GA_FLECHETTE", 0x3C80F0, SpentCasing.COLOR_CASE_BRASS);
        registerShotgun("g12_magnum", "12GA_MAGNUM", 0x278400, SpentCasing.COLOR_CASE_12GA);
        registerShotgun("g12_explosive", "12GA_EXPLOSIVE", 0xDA4127, SpentCasing.COLOR_CASE_12GA);
        registerShotgun("g12_phosphorus", "12GA_PHOSPHORUS", 0x910001, SpentCasing.COLOR_CASE_12GA);
        registerShotgun("g12_equestrian_bj", "12gaEquestrianBJ", 0xB52B2B,
                SpentCasing.COLOR_CASE_EQUESTRIAN);
        registerShotgun("g12_equestrian_tkr", "12gaEquestrianTKR", 0xB52B2B,
                SpentCasing.COLOR_CASE_EQUESTRIAN);
        alias("g12_sub", "g12");
        alias("g12_sub_slug", "g12_slug");
        alias("g12_sub_flechette", "g12_flechette");
        alias("g12_sub_magnum", "g12_magnum");
        alias("g12_sub_explosive", "g12_explosive");
        alias("g12_sub_phosphorus", "g12_phosphorus");
        alias("g12_shredder", "g12");
        alias("g12_shredder_slug", "g12_slug");
        alias("g12_shredder_flechette", "g12_flechette");
        alias("g12_shredder_magnum", "g12_magnum");
        alias("g12_shredder_explosive", "g12_explosive");
        alias("g12_shredder_phosphorus", "g12_phosphorus");

        register("g26_flare", "g26Flare", CasingType.STRAIGHT, 2F, 2F, 2F, 0x9E1616);
        register("g26_flare_supply", "g26FlareSupply", CasingType.STRAIGHT, 2F, 2F, 2F, 0x3C80F0);
        register("g26_flare_weapon", "g26FlareWeapon", CasingType.STRAIGHT, 2F, 2F, 2F, 0x278400);
        register("g40_he", "g40", CasingType.STRAIGHT, 2F, 2F, 1.5F, 0x777777);
        register("g40_heat", "g40heat", CasingType.STRAIGHT, 2F, 2F, 1.5F, 0x5E6854);
        register("g40_demo", "g40demo", CasingType.STRAIGHT, 2F, 2F, 1.5F, 0xE30000);
        register("g40_inc", "g40inc", CasingType.STRAIGHT, 2F, 2F, 1.5F, 0xE86F20);
        register("g40_phosphorus", "g40phos", CasingType.STRAIGHT, 2F, 2F, 1.5F, 0xC8C8C8);

        registerShotgun("g10", "10GA", 0xB52B2B, SpentCasing.COLOR_CASE_12GA, 1F);
        registerShotgun("g10_shrapnel", "10GAShrapnel", 0xE5DD00, SpentCasing.COLOR_CASE_12GA, 1F);
        registerShotgun("g10_du", "10GADU", 0x538D53, SpentCasing.COLOR_CASE_12GA, 1F);
        registerShotgun("g10_slug", "10GASlug", 0x808080, SpentCasing.COLOR_CASE_12GA, 1F);
        registerShotgun("g10_explosive", "10GAEXP", 0xFAC943, SpentCasing.COLOR_CASE_12GA, 1F);

        register("b75", "b75", CasingType.STRAIGHT, 2F, 2F, 1.5F, SpentCasing.COLOR_CASE_BRASS);
        register("b75_inc", "b75inc", CasingType.STRAIGHT, 2F, 2F, 1.5F, SpentCasing.COLOR_CASE_BRASS);
        register("b75_exp", "b75exp", CasingType.STRAIGHT, 2F, 2F, 1.5F, SpentCasing.COLOR_CASE_BRASS);
        register("p35800", "35-800", CasingType.STRAIGHT, 1F, 1F, 1F, 0xCEB78E);
        alias("p35800_bl", "p35800");

        registerShell240("shell_normal", "240standard");
        registerShell240("shell_explosive", "240ext");
        registerShell240("shell_ap", "240w");
        registerShell240("shell_du", "240u");
        registerShell240("shell_w9", "240n");

        installed = true;
    }

    private static void registerSmallStraight(String configId, String id, float x, float y, float z, int color) {
        register(configId, id, CasingType.STRAIGHT, x, y, z, color);
    }

    private static void registerShell240(String configId, String id) {
        register(configId, id, CasingType.BOTTLENECK, 7.5F, 7.5F, 7.5F,
                SpentCasing.COLOR_CASE_BRASS, 1F, 0.5D, 60, 20);
        BulletConfig config = config(configId);
        config.setCasing(config.casing.setBounceMotion(0.02F, 0.05F));
    }

    private static void registerShotgun(String configId, String id, int shellColor, int caseColor) {
        registerShotgun(configId, id, shellColor, caseColor, 0.75F);
    }

    private static void registerShotgun(String configId, String id, int shellColor, int caseColor, float scale) {
        register(configId, id, CasingType.SHOTGUN, scale, scale, scale, shellColor, caseColor);
    }

    private static void register(String configId, String id, CasingType type, float x, float y, float z,
            int... colors) {
        register(configId, id, type, x, y, z, colors, 0F, 0D, 0, 0);
    }

    private static void register(String configId, String id, CasingType type, float x, float y, float z,
            int color, float smokeChance, double smokeLift, int smokeDuration, int smokeNodeLife) {
        register(configId, id, type, x, y, z, new int[] {color}, smokeChance, smokeLift, smokeDuration,
                smokeNodeLife);
    }

    private static void register(String configId, String id, CasingType type, float x, float y, float z,
            int[] colors, float smokeChance, double smokeLift, int smokeDuration, int smokeNodeLife) {
        BulletConfig config = config(configId);
        if (config == null) throw new IllegalStateException("Missing bullet config for casing: " + configId);
        SpentCasing casing = new SpentCasing(type).setScale(x, y, z).setColor(colors).register(id);
        if (smokeChance > 0F) casing.setupSmoke(smokeChance, smokeLift, smokeDuration, smokeNodeLife);
        config.setCasing(casing);
    }

    private static void alias(String configId, String sourceConfigId) {
        BulletConfig config = config(configId);
        BulletConfig source = config(sourceConfigId);
        if (config == null || source == null) throw new IllegalStateException("Missing casing alias: " + configId);
        config.setCasing(source.casing);
    }

    private static BulletConfig config(String id) {
        return BulletConfigs.get(com.hbm.Constants.MOD_ID + ":" + id);
    }
}
