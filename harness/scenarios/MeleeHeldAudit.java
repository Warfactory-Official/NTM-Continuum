import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * melee (2026-10-02): every melee weapon held in third person at a fixed pose, measured against 1.7.
 *
 * <p>A player's arm bobs with its age, so the measured hand is an invisible armor stand instead: its right arm is
 * posed at 1.7's held-item angle ({@code Pose.RightArm [-18, 0, 0]}, ModelBiped's {@code -pi/10}) and stays still,
 * and its {@code ItemInHandLayer} is the same item chain a player's is. The camera is the player's own first-person
 * eye at fixed spots on the ground (survival, so not flying: no FOV modifier), {@code fov 50}, HUD hidden. Each view
 * is shot empty-handed, with a gold calibration block, and once per item; {@code port-tools/melee_held_ref17.py
 * audit <run>} measures the item pixels (difference against the empty shot) and compares them with the projection of
 * 1.7's chain and of the port's JSON through the same camera ({@code meta.json} carries the camera).
 *
 * <p>Then, for the eye, the player itself holds each item: third person front and back (F5) and first person.
 *
 * <p>{@code MELEE_ITEMS=chainsaw,steel_sword} limits the run to those ids.
 */
public final class MeleeHeldAudit implements Scenario {

    private static final int X = 900;
    private static final int Y = 100;
    private static final int Z = 900;
    private static final int L = Y + 1;

    private static final double SX = X + 0.5;
    private static final double SZ = Z + 0.5;
    private static final double DIST = 3.2;
    private static final int FOV = 50;

    private static final List<String> ALL = List.of(
            // ItemRenderTransformer (2D, 1.7 held at 1.7x)
            "steel_sword", "titanium_sword", "alloy_sword", "desh_sword", "cobalt_sword", "cobalt_decorated_sword",
            "starmetal_sword", "schrabidium_sword", "cmb_sword", "dnt_sword",
            "bismuth_pickaxe", "bismuth_axe", "volcanic_pickaxe", "volcanic_axe", "chlorophyte_pickaxe",
            "chlorophyte_axe", "mese_pickaxe", "mese_axe",
            // ItemRendererMeteorSword
            "meteorite_sword_seared", "meteorite_sword_fused", "meteorite_sword_baleful",
            // plain full-3D sprites
            "meteorite_sword", "schrabidium_hammer", "wrench_flipped", "memespoon", "weapon_pipe_lead", "reer_graar",
            "ullapool_caber", "wrench", "crowbar", "elec_sword", "titanium_pickaxe", "steel_axe", "rod_of_discord",
            "mysteryshovel", "schrabidium_hoe", "wrench_archineer", "screwdriver_desh", "hand_drill_desh",
            "acetylene_torch",
            // 3D renderers
            "shimmer_sledge", "shimmer_axe", "stopsign", "sopsign", "chernobylsign",
            "wood_gavel", "lead_gavel", "diamond_gavel", "mese_gavel", "big_sword", "redstone_sword", "chainsaw");

    private record View(String name, double x, double z, float yaw) {}

    @Override
    public void run(Client c) throws Exception {
        List<String> items = items();
        c.waitInWorld(120_000L);
        Fixtures.platform(c, X, Y, Z, 12);
        c.cmd("time set 6000");
        c.setOption("hide_gui", "true");
        c.setOption("fov", String.valueOf(FOV));
        c.setOption("render_distance", "6");
        c.setOption("camera", "first_person");
        // creative flight persists in the save and adds a 1.1 FOV modifier: survival lands the camera on the ground
        c.cmd("gamemode survival");
        c.cmd("summon minecraft:armor_stand " + SX + " " + L + " " + SZ
                + " {NoGravity:1b,Invisible:1b,ShowArms:1b,NoBasePlate:1b,Rotation:[180f,0f],"
                + "Pose:{RightArm:[-18f,0f,0f],LeftArm:[0f,0f,0f],Body:[0f,0f,0f],Head:[0f,0f,0f]},Tags:[\"melee\"]}");
        c.waitTicks(10);
        JsonObject status = c.status();

        View[] views = {
                new View("front", SX, SZ - DIST, 0F),     // facing the stand: its front, right hand on screen left
                new View("side", SX + DIST, SZ, 90F)};    // looking west at its right side
        JsonArray viewMeta = new JsonArray();
        for (View v : views) {
            c.teleport(v.x, L, v.z);
            c.waitTicks(10);
            double eyeY = c.player().get("eye_y").getAsDouble();
            float pitch = (float) Math.toDegrees(Math.atan2(eyeY - (L + 1.0), DIST));
            c.look(v.yaw, pitch);
            c.waitTicks(6);
            JsonObject p = c.player();
            Expect.that(p.get("on_ground").getAsBoolean() && !p.get("flying").getAsBoolean(),
                    "camera stands on the ground, not flying (no FOV modifier): " + p);

            hand(c, "minecraft:air");
            c.waitTicks(40); // the freshly filled platform's light settles first (an early base shot was dark)
            c.screenshot(v.name + "-base");
            c.cmd("setblock " + (X - 2) + " " + L + " " + (Z + 2) + " minecraft:gold_block");
            c.waitTicks(6);
            c.screenshot(v.name + "-calib");
            c.cmd("setblock " + (X - 2) + " " + L + " " + (Z + 2) + " minecraft:air");
            c.waitTicks(4);
            for (String item : items) {
                hand(c, "hbm:" + item);
                c.screenshot(v.name + "-" + item);
            }
            hand(c, "minecraft:air");

            JsonObject vm = new JsonObject();
            vm.addProperty("name", v.name);
            vm.addProperty("baseline", v.name + "-base.png");
            vm.addProperty("calib", v.name + "-calib.png");
            JsonArray eye = new JsonArray();
            eye.add(p.get("x").getAsDouble());
            eye.add(p.get("eye_y").getAsDouble());
            eye.add(p.get("z").getAsDouble());
            vm.add("eye", eye);
            vm.addProperty("yaw", p.get("yaw").getAsDouble());
            vm.addProperty("pitch", p.get("pitch").getAsDouble());
            viewMeta.add(vm);
        }

        JsonObject meta = new JsonObject();
        meta.addProperty("node", c.node());
        meta.addProperty("fov", FOV);
        meta.addProperty("width", status.get("window_width").getAsInt());
        meta.addProperty("height", status.get("window_height").getAsInt());
        JsonArray stand = new JsonArray();
        stand.add(SX);
        stand.add((double) L);
        stand.add(SZ);
        meta.add("stand", stand);
        meta.addProperty("stand_yaw", 180.0);
        JsonArray calib = new JsonArray();
        calib.add(X - 2);
        calib.add(L);
        calib.add(Z + 2);
        meta.add("calib_block", calib);
        meta.add("views", viewMeta);
        JsonArray list = new JsonArray();
        items.forEach(list::add);
        meta.add("items", list);
        Files.writeString(c.screenshotDir().resolve("meta.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(meta), StandardCharsets.UTF_8);
        c.cmd("kill @e[type=armor_stand,tag=melee]");

        // the player holding each item, for the eye: F5 front and back, then first person
        c.cmd("gamemode creative");
        c.teleport(SX, L, SZ - 6);
        c.look(0, 0);
        c.waitTicks(10);
        for (String item : items) {
            c.cmd("item replace entity @s hotbar.0 with hbm:" + item);
            c.hotbar(1);
            c.setOption("hide_gui", "true");
            c.setOption("camera", "third_person_front");
            c.waitTicks(6);
            c.screenshot("player-front-" + item);
            c.setOption("camera", "third_person_back");
            c.waitTicks(4);
            c.screenshot("player-back-" + item);
            c.setOption("camera", "first_person");
            c.setOption("hide_gui", "false");
            c.waitTicks(8);
            c.screenshot("player-first-" + item);
        }
        c.setOption("hide_gui", "false");
        c.setOption("fov", "70");
        c.cmd("clear");
    }

    private static void hand(Client c, String item) {
        c.cmd("item replace entity @e[type=armor_stand,tag=melee,limit=1] weapon.mainhand with " + item);
        c.waitTicks(10); // the equipment update reaches the client a few ticks later (4 was once too few on 26.1)
    }

    private static List<String> items() {
        String env = System.getenv("MELEE_ITEMS");
        if (env == null || env.isBlank()) return ALL;
        List<String> out = new ArrayList<>();
        for (String s : env.split("[,\\s]+")) if (!s.isBlank()) out.add(s.replace("hbm:", ""));
        return out;
    }
}
