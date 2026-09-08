package com.hbm.client.render.turret;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import net.minecraft.resources.Identifier;

/** The turret models and the standalone textures their parts sample. */
public final class TurretResources {

    public static final WavefrontObj CHEKHOV = model("turret_chekhov");
    public static final WavefrontObj JEREMY = model("turret_jeremy");
    public static final WavefrontObj SENTRY_MODEL = model("turret_sentry");
    public static final WavefrontObj HOWARD = model("turret_howard");
    public static final WavefrontObj HOWARD_DAMAGED = model("turret_howard_damaged");
    public static final WavefrontObj TAUON = model("turret_tauon");
    public static final WavefrontObj MAXWELL = model("turret_microwave");

    public static final Identifier BASE = texture("base");
    public static final Identifier BASE_FRIENDLY = texture("base_friendly");
    public static final Identifier CARRIAGE = texture("carriage");
    public static final Identifier CARRIAGE_FRIENDLY = texture("carriage_friendly");
    public static final Identifier CARRIAGE_CIWS = texture("carriage_ciws");
    public static final Identifier CONNECTOR = texture("connector");
    public static final Identifier CHEKHOV_BODY = texture("chekhov");
    public static final Identifier CHEKHOV_BARRELS = texture("chekhov_barrels");
    public static final Identifier JEREMY_GUN = texture("jeremy");
    public static final Identifier SENTRY = texture("sentry");
    public static final Identifier SENTRY_DAMAGED = texture("sentry_damaged");
    public static final Identifier HOWARD_BODY = texture("howard");
    public static final Identifier HOWARD_BARRELS = texture("howard_barrels");
    public static final Identifier TAUON_BODY = texture("tauon");
    public static final Identifier MAXWELL_BODY = texture("maxwell");
    public static final Identifier BASE_RUSTED = texture("rusted/base");
    public static final Identifier CARRIAGE_CIWS_RUSTED = texture("rusted/carriage_ciws");
    public static final Identifier HOWARD_RUSTED = texture("rusted/howard");
    public static final Identifier HOWARD_BARRELS_RUSTED = texture("rusted/howard_barrels");

    private TurretResources() {}

    private static WavefrontObj model(String name) {
        return WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/turrets/" + name + ".obj"));
    }

    private static Identifier texture(String name) {
        return Ids.of(Constants.MOD_ID, "textures/models/turrets/" + name + ".png");
    }
}
