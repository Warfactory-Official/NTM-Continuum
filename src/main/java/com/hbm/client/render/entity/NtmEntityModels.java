package com.hbm.client.render.entity;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;

/**
 * The OBJ meshes the ntm-next entity renderers draw, loaded once on first use. ntm-next keeps these on
 * its {@code ResourceManager} beside the textures; here they are client-only, since loading an OBJ
 * needs the client resource manager and {@code ResourceManager} is reachable from common code.
 *
 * <p>Every file is byte-identical to 1.7's (a few carry original's camelCase names re-spelled
 * lowercase, since modern identifiers reject uppercase): see {@code entity-port-manifest.md}.
 */
public final class NtmEntityModels {

    private NtmEntityModels() {}

    private static WavefrontObj load(String path) {
        return WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/" + path));
    }

    public static final WavefrontObj SPHERE = load("sphere.obj");
    public static final WavefrontObj SPHERE_NEW = load("sphere_new.obj");
    public static final WavefrontObj STIRLING = load("machines/stirling.obj");
    public static final WavefrontObj PROJECTILES = load("projectiles/projectiles.obj");

    public static final WavefrontObj GLYPHID = load("mobs/glyphid.obj");
    public static final WavefrontObj QUADCOPTER = load("mobs/quadcopter.obj");
    public static final WavefrontObj TESLA_CRAB = load("mobs/teslacrab.obj");
    public static final WavefrontObj TAINT_CRAB = load("mobs/taintcrab.obj");
    public static final WavefrontObj BLOCK_SPIDER = load("mobs/blockspider.obj");
    public static final WavefrontObj PLASTIC_BAG = load("mobs/plasticbag.obj");
    public static final WavefrontObj MASK_MAN = load("mobs/maskman.obj");
    public static final WavefrontObj UFO = load("mobs/ufo.obj");
    public static final WavefrontObj BOT_PRIME_HEAD = load("mobs/bot_prime_head.obj");
    public static final WavefrontObj BOT_PRIME_BODY = load("mobs/bot_prime_body.obj");

    public static final WavefrontObj DORNIER = load("dornier.obj");
    public static final WavefrontObj B29 = load("b29.obj");
    public static final WavefrontObj C130 = load("weapons/c130.obj");
    public static final WavefrontObj BOMBLET_THETA = load("bomblet_theta.obj");
    public static final WavefrontObj BOXCAR = load("boxcar.obj");
    public static final WavefrontObj DUCHESS_GAMBIT = load("duchessgambit.obj");
    public static final WavefrontObj BUILDING = load("weapons/building.obj");
    public static final WavefrontObj TORPEDO = load("weapons/torpedo.obj");
    public static final WavefrontObj SOYUZ = load("soyuz.obj");
    public static final WavefrontObj SOYUZ_LANDER = load("soyuz_lander.obj");
    public static final WavefrontObj MINER_ROCKET = load("miner_rocket.obj");
    public static final WavefrontObj LIL_BOY = load("lil_boy.obj");
    public static final WavefrontObj EMP_RING = load("ring.obj");
    public static final WavefrontObj SAT_FOEQ_BURNING = load("sat_foeq_burning.obj");
    public static final WavefrontObj SAT_FOEQ_FIRE = load("sat_foeq_fire.obj");
    public static final WavefrontObj TOM_MAIN = load("weapons/tom_main.obj");
    public static final WavefrontObj TOM_FLAME = load("weapons/tom_flame.obj");
    public static final WavefrontObj DELIVERY_DRONE = load("machines/drone.obj");
    public static final WavefrontObj CONSERVE_CRATE = load("blocks/conservecrate.obj");
    public static final WavefrontObj TRAIN_CARGO_TRAM = load("vehicles/tram.obj");
    public static final WavefrontObj TRAIN_CARGO_TRAM_TRAILER = load("vehicles/tram_trailer.obj");
}
