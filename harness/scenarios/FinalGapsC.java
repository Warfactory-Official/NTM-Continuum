import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Gap batch C (§19ed-gaps-c), rendering and input, every item where it is seen and by value where it can be read
 * (query {@code hbm_gapsc}):
 * <ol>
 *   <li>swing/equip animations started by the server: chainsaw swing, crucible deploy + swing (old-hbm's since 2026-10-02; pinned equip,
 *       suppressed vanilla swing), boltgun recoil on a bolted hit, the gavel's plain vanilla swing;</li>
 *   <li>crucible blade (flat, fullbright) in first person;</li>
 *   <li>RPA / NCRPA worn OBJ suits;</li>
 *   <li>machine / missile / 1.7-IItemRenderer items on pedestals and skeleton holders (framed routing);</li>
 *   <li>connected textures: hadron coils, coated wire, ICF shell (+port), PWR shell (+port);</li>
 *   <li>RBMK lever sparks, graph line, numitron digits close up;</li>
 *   <li>large radar icon (turning dish), power net tool floating ids, custom-missile launch smoke;</li>
 *   <li>RenderOverhead markers: ICF assembly error, PWR assembly error, neutrino lens scan;</li>
 *   <li>ICF item icon;</li>
 *   <li>advancement window (1.20.1 overflow);</li>
 *   <li>HEV HUD hides hearts/armour;</li>
 *   <li>armour-mod shield bar and its client sync;</li>
 *   <li>magnet toggle key.</li>
 * </ol>
 */
public final class FinalGapsC implements Scenario {

    private static final int X = 400;
    private static final int Y = 100;
    private static final int Z = 400;

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.markLog();
        Fixtures.platform(c, X, Y, Z, 24);
        c.cmd("gamerule doDaylightCycle false");
        c.cmd("gamerule doMobSpawning false");
        c.cmd("time set 6000");
        c.cmd("weather clear");
        c.cmd("kill @e[type=!minecraft:player]");
        c.cmd("gamemode creative");
        c.setOption("camera", "first_person");
        c.setOption("gui_scale", "2");

        inventory(c);
        animations(c);
        armor(c);
        framed(c);
        connected(c);
        rbmk(c);
        powerNet(c);
        markers(c);
        lens(c);
        launchSmoke(c);
        hud(c);
        magnet(c);
        advancements(c);

        c.setOption("camera", "first_person");
        c.cmd("gamemode creative");
        c.cmd("clear");
        c.cmd("item replace entity @s armor.head with minecraft:air");
        List<String> bad = c.logSinceMark("/(ERROR|FATAL)[]]|Exception|Failed to load texture|Missing model");
        List<String> relevant = new ArrayList<>();
        for (String line : bad) {
            // Realms/auth noise of an offline dev client is not ours
            if (line.contains("Realms") || line.contains("authlib") || line.contains("Loaded ")) continue;
            relevant.add(line);
        }
        Expect.that(relevant.isEmpty(), "log clean since the mark: " + relevant);
    }

    private JsonObject q(Client c) {
        return c.query("hbm_gapsc", new JsonObject());
    }

    private JsonObject q(Client c, String key, com.google.gson.JsonElement value) {
        JsonObject p = new JsonObject();
        p.add(key, value);
        return c.query("hbm_gapsc", p);
    }

    private static double x(JsonObject json, String bus) {
        return json.getAsJsonArray(bus).get(0).getAsDouble();
    }

    private static double abs(JsonObject json, String bus) {
        double sum = 0;
        for (var v : json.getAsJsonArray(bus)) sum += Math.abs(v.getAsDouble());
        return sum;
    }

    // ------------------------------------------------------------------ 7 / 9: inventory icons

    private void inventory(Client c) {
        c.cmd("clear");
        String[] items = {"chainsaw", "crucible", "boltgun", "wood_gavel", "machine_radar_large", "icf",
                "rpa_helmet", "rpa_plate", "rpa_legs", "rpa_boots", "ncrpa_helmet", "ncrpa_plate", "ncrpa_legs",
                "ncrpa_boots", "power_net_tool", "neutrino_lens", "hadron_coil_alloy", "red_wire_coated",
                "machine_radar"};
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 600 4 true");
        for (String id : items) c.cmd("give @s hbm:" + id);
        c.waitTicks(10);
        c.setOption("gui_scale", "3");
        c.waitTicks(3);
        c.key("e");
        c.waitTicks(15);
        JsonObject status = c.status();
        int gw = status.get("gui_width").getAsInt();
        int gh = status.get("gui_height").getAsInt();
        int[] crop = {(gw - 176) / 2, (gh - 166) / 2 + 76, 176, 90};
        c.screenshot("00-inventory-a", crop);
        c.waitTicks(20); // one second: the large radar's dish turns 100 degrees in 1.7's 3.6 s cycle
        c.screenshot("00-inventory-b", crop);
        c.screenshot("00-inventory-full");
        c.key("escape");
        c.waitTicks(3);
        c.setOption("gui_scale", "2");
        c.cmd("clear");
        c.cmd("gamemode creative");
    }

    // ------------------------------------------------------------------ 1 / 2: animations

    private void animations(Client c) {
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.setOption("camera", "first_person");

        // chainsaw: every swing the server sees starts SWING_ROT/SWING_TRANS; the vanilla swing is cleared
        Fixtures.giveOnly(c, "hbm:chainsaw", 1);
        c.waitTicks(20);
        JsonObject rest = q(c);
        Expect.that(abs(rest, "swing_rot") == 0, "chainsaw at rest: " + rest.get("swing_rot"));
        c.screenshot("10-fp-chainsaw-rest", new int[]{320, 120, 320, 240});
        c.attack();
        c.waitTicks(3);
        JsonObject swing = q(c);
        c.screenshot("11-fp-chainsaw-swing", new int[]{320, 120, 320, 240});
        Expect.that(abs(swing, "swing_rot") > 0 || abs(swing, "swing_trans") > 0,
                "chainsaw swing bus running after a left click: " + swing.get("swing_rot") + " " + swing.get("swing_trans"));
        Expect.that(!swing.get("swinging").getAsBoolean(), "vanilla swing cleared under the chainsaw");
        c.setOption("camera", "third_person_front");
        c.waitTicks(4);
        c.screenshot("12-tp-chainsaw", new int[]{200, 60, 240, 240});
        c.setOption("camera", "first_person");

        // crucible (old-hbm since 2026-10-02, see CrucibleOldHbm): the Collada deploy on equip, then the random swing
        Fixtures.giveOnly(c, "hbm:wood_gavel", 1);
        c.waitTicks(10);
        c.cmd("clear");
        c.cmd("give @s " + (c.node().startsWith("1.20") ? "hbm:crucible{\"hbm:charges\":16}" : "hbm:crucible[hbm:charges=16]"));
        c.waitTicks(4);
        JsonObject deploy = c.query("hbm_crucible", new JsonObject());
        c.screenshot("13-fp-crucible-deploy", new int[]{320, 120, 320, 240});
        Expect.that(deploy.get("blender").getAsBoolean() && deploy.get("deploy_ms").getAsLong() < 700,
                "crucible deploying: " + deploy);
        c.waitTicks(30);
        JsonObject settled = c.query("hbm_crucible", new JsonObject());
        c.screenshot("14-fp-crucible-blade", new int[]{320, 120, 320, 240});
        Expect.that(settled.get("deploy_ms").getAsLong() >= 1050, "crucible deployed: " + settled);
        Expect.that(settled.get("equip").getAsDouble() == 1.0 && settled.get("equip_prev").getAsDouble() == 1.0,
                "crucible equip pinned at 1: " + settled.get("equip") + "/" + settled.get("equip_prev"));
        c.attack();
        c.waitTicks(2);
        JsonObject cswing = c.query("hbm_crucible", new JsonObject());
        c.screenshot("15-fp-crucible-swing", new int[]{320, 120, 320, 240});
        Expect.that(cswing.get("swing_rot_x").getAsDouble() != 0, "crucible swing bus running: " + cswing);
        Expect.that(!cswing.get("swinging").getAsBoolean(), "vanilla swing cleared under the crucible");
        c.waitTicks(20);
        c.setOption("camera", "third_person_front");
        c.waitTicks(4);
        c.screenshot("16-tp-crucible", new int[]{200, 60, 240, 240});
        c.setOption("camera", "first_person");

        // boltgun: a bolted hit on a pig plays the barrel recoil
        c.cmd("clear");
        c.cmd("give @s hbm:boltgun");
        c.cmd("give @s hbm:bolt_spike 8");
        c.waitTicks(3);
        c.cmd("summon minecraft:pig " + (X + 0.5) + " " + (Y + 1) + " " + (Z + 2.5) + " {NoAI:1b,Health:100f,Attributes:[]}");
        c.waitTicks(5);
        c.lookAt(X + 0.5, Y + 1.5, Z + 2.5);
        c.waitTicks(3);
        c.attack();
        c.screenshot("17-fp-boltgun-recoil", new int[]{320, 120, 320, 240});
        c.waitTicks(5);
        // the 150 ms RECOIL bus is over before a query round-trip returns; the server cue is proven by the bolt spent
        Expect.equal(7, held(c, "hbm:bolt_spike"), "bolts left after one bolted hit");
        c.cmd("kill @e[type=minecraft:pig]");

        // gavel: 1.7 had no bus for it; the vanilla swing plays
        Fixtures.giveOnly(c, "hbm:wood_gavel", 1);
        c.look(0, 10);
        c.waitTicks(10);
        c.attack();
        c.waitTicks(2);
        JsonObject gavel = q(c);
        c.screenshot("18-fp-gavel-swing", new int[]{320, 120, 320, 240});
        Expect.that(!gavel.get("anim_running").getAsBoolean(), "gavel starts no bus animation");
        c.cmd("clear");
    }

    // ------------------------------------------------------------------ 3: RPA / NCRPA

    private void armor(Client c) {
        JsonObject r = q(c);
        Expect.that(r.get("rpa_renderer").getAsBoolean() && r.get("ncrpa_renderer").getAsBoolean(),
                "RPA and NCRPA worn renderers registered");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        for (String set : new String[]{"rpa", "ncrpa"}) {
            c.cmd("item replace entity @s armor.head with hbm:" + set + "_helmet");
            c.cmd("item replace entity @s armor.chest with hbm:" + set + "_plate");
            c.cmd("item replace entity @s armor.legs with hbm:" + set + "_legs");
            c.cmd("item replace entity @s armor.feet with hbm:" + set + "_boots");
            c.cmd("effect clear @s");
            c.setOption("camera", "third_person_front");
            c.waitTicks(10);
            c.screenshot("20-tp-front-" + set, new int[]{200, 40, 240, 260});
            c.setOption("camera", "third_person_back");
            c.waitTicks(6);
            c.screenshot("21-tp-back-" + set, new int[]{200, 40, 240, 260});
        }
        c.setOption("camera", "first_person");
        for (String slot : new String[]{"head", "chest", "legs", "feet"}) {
            c.cmd("item replace entity @s armor." + slot + " with minecraft:air");
        }
        c.cmd("effect clear @s");
    }

    // ------------------------------------------------------------------ 4: framed items

    private void framed(Client c) {
        String[] items = {"hbm:machine_radar_large", "hbm:icf", "hbm:missile_buster", "hbm:chainsaw", "hbm:crucible",
                "hbm:boltgun", "hbm:wood_gavel"};
        JsonArray ids = new JsonArray();
        for (String id : items) ids.add(id);
        JsonObject routed = q(c, "framed", ids);
        JsonObject framed = routed.getAsJsonObject("framed");
        Expect.that(routed.get("framed_routes").getAsInt() > 100, "framed routing loaded: " + routed.get("framed_routes"));
        for (String id : items) Expect.that(framed.get(id).getAsBoolean(), id + " takes Forge's ENTITY route: " + framed);

        int gz = Z - 12;
        for (int i = 0; i < items.length; i++) {
            int x = X - 9 + i * 3;
            Fixtures.setBlock(c, x, Y + 1, gz, "hbm:pedestal");
            c.cmd("data merge block " + x + " " + (Y + 1) + " " + gz + " {item:{id:\"" + items[i] + "\"," + count(c) + "}}");
            Fixtures.setBlock(c, x, Y + 1, gz - 4, "hbm:skeleton_holder[facing=south]");
            c.cmd("data merge block " + x + " " + (Y + 1) + " " + (gz - 4) + " {item:{id:\"" + items[i] + "\"," + count(c) + "}}");
        }
        c.waitTicks(20);
        worldShot(c, "30-pedestals", X + 0.5, Y + 4, gz + 7.5, X + 0.5, Y + 1.5, gz + 0.5);
        worldShot(c, "31-skeleton-holders", X + 0.5, Y + 3.5, gz + 3.5, X + 0.5, Y + 1.2, gz - 3.5);
        worldShot(c, "32-pedestal-radar-close", X - 9 + 0.5, Y + 3, gz + 3.5, X - 9 + 0.5, Y + 1.8, gz + 0.5);
    }

    private static int held(Client c, String id) {
        int total = 0;
        for (var e : c.inventory().getAsJsonArray("slots")) {
            JsonObject slot = e.getAsJsonObject();
            if (slot.get("id").getAsString().equals(id)) total += slot.get("count").getAsInt();
        }
        return total;
    }

    private static String count(Client c) {
        return c.node().startsWith("1.20") ? "Count:1b" : "count:1";
    }

    // ------------------------------------------------------------------ 5: connected textures

    private void connected(Client c) {
        JsonObject ct = q(c);
        Expect.that(ct.get("ct_registered").getAsInt() == 45, "45 connected-texture blocks (1.7 IBlockCT set): " + ct.get("ct_registered"));
        int wz = Z + 14;
        String[] walls = {"hbm:hadron_coil_alloy", "hbm:hadron_coil_mese", "hbm:red_wire_coated", "hbm:icf_block",
                "hbm:pwr_block"};
        for (int w = 0; w < walls.length; w++) {
            int x0 = X - 12 + w * 5;
            c.cmd("fill " + x0 + " " + (Y + 1) + " " + wz + " " + (x0 + 2) + " " + (Y + 3) + " " + (wz + 1) + " " + walls[w]);
        }
        // a lone coil, a port cell in the ICF and PWR walls, and two coil kinds joined
        Fixtures.setBlock(c, X + 14, Y + 1, wz, "hbm:hadron_coil_gold");
        Fixtures.setBlock(c, X - 12 + 3 * 5 + 1, Y + 2, wz, "hbm:icf_block[port=true]");
        Fixtures.setBlock(c, X - 12 + 4 * 5 + 1, Y + 2, wz, "hbm:pwr_block[port=true]");
        c.waitTicks(20);
        worldShot(c, "40-ct-walls", X + 0.5, Y + 4, wz - 12.5, X + 0.5, Y + 2, wz);
        worldShot(c, "41-ct-coils-close", X - 10.5, Y + 2.5, wz - 4.5, X - 7.5, Y + 2, wz);
        worldShot(c, "42-ct-icf-pwr-close", X + 5.5, Y + 2.5, wz - 5.5, X + 5.5, Y + 2, wz);
        worldShot(c, "43-ct-top", X + 0.5, Y + 9, wz - 3.5, X + 0.5, Y + 3, wz + 0.5);
        JsonObject after = q(c);
        Expect.that(after.get("ct_wrapped").getAsInt() > 0, "block-state models wrapped: " + after.get("ct_wrapped"));
    }

    // ------------------------------------------------------------------ 6: RBMK panels

    private void rbmk(Client c) {
        int pz = Z - 20;
        Fixtures.setBlock(c, X, Y + 1, pz, "hbm:rbmk_lever[facing=south]");
        Fixtures.setBlock(c, X + 2, Y + 1, pz, "hbm:rbmk_numitron[facing=south]");
        Fixtures.setBlock(c, X + 4, Y + 1, pz, "hbm:rbmk_graph[facing=south]");
        c.cmd("data merge block " + (X + 2) + " " + (Y + 1) + " " + pz
                + " {active0:1b,value0:1234567L,active_digits0:127L,active1:1b,value1:42L,active_digits1:127L}");
        StringBuilder graph = new StringBuilder("{active0:1b,active1:1b");
        for (int i = 0; i < 30; i++) graph.append(",value0_").append(i).append(":").append((long) (50 + 40 * Math.sin(i / 3.0))).append("L");
        for (int i = 0; i < 30; i++) graph.append(",value1_").append(i).append(":").append(i * 3L).append("L");
        graph.append("}");
        c.cmd("data merge block " + (X + 4) + " " + (Y + 1) + " " + pz + " " + graph);
        c.cmd("data merge block " + X + " " + (Y + 1) + " " + pz + " {active0:1b,active1:1b}");
        c.waitTicks(10);
        // throw lever 0 on: its arc flashes as the contact makes (flip reaches 1 after 10 ticks)
        int before = q(c).get("sparks_spawned").getAsInt();
        c.cmd("gamemode spectator");
        c.teleport(X + 0.5, Y + 1.6, pz + 1.6);
        c.lookAt(X + 0.5, Y + 1.4, pz + 0.5);
        c.cmd("data merge block " + X + " " + (Y + 1) + " " + pz + " {isTurningOn0:1b}");
        c.waitTicks(9);
        c.screenshot("50-rbmk-lever-sparks");
        c.waitTicks(1);
        c.screenshot("50-rbmk-lever-sparks-b");
        c.waitTicks(10);
        c.cmd("gamemode creative");
        int sparks = q(c).get("sparks_spawned").getAsInt() - before;
        Expect.that(sparks >= 10, "lever arc: two bursts of five sparks spawned: " + sparks);
        c.waitTicks(30);
        worldShot(c, "51-rbmk-panels", X + 2.5, Y + 2.5, pz + 4.5, X + 2.5, Y + 1.5, pz + 0.5);
        worldShot(c, "52-rbmk-numitron-close", X + 2.5, Y + 1.8, pz + 1.35, X + 2.5, Y + 1.55, pz + 0.5);
        worldShot(c, "53-rbmk-graph-close", X + 4.5, Y + 1.6, pz + 1.6, X + 4.5, Y + 1.5, pz + 0.5);
    }

    // ------------------------------------------------------------------ 7: power net tool

    private void powerNet(Client c) {
        int cz = Z + 6;
        c.cmd("fill " + (X - 3) + " " + (Y + 1) + " " + cz + " " + (X + 3) + " " + (Y + 1) + " " + cz + " hbm:red_cable");
        c.waitTicks(10);
        Fixtures.giveOnly(c, "hbm:power_net_tool", 1);
        c.teleport(X + 0.5, Y + 1, cz - 2.5);
        c.lookAt(X + 0.5, Y + 1.5, cz + 0.5);
        c.waitTicks(5);
        c.use();
        c.waitTicks(10);
        JsonObject texts = q(c);
        c.screenshot("60-power-net-ids");
        Expect.that(texts.getAsJsonArray("texts").size() >= 7, "a floating net id over every cable: " + texts.get("texts"));
    }

    // ------------------------------------------------------------------ 8: markers

    private void markers(Client c) {
        int mz = Z + 20;
        Fixtures.setBlock(c, X - 6, Y + 1, mz, "hbm:icf_controller[facing=south]");
        c.cmd("clear");
        c.cmd("gamemode survival");
        c.teleport(X - 5.5, Y + 1, mz + 3.5);
        c.lookAt(X - 5.5, Y + 1.5, mz + 0.5);
        c.waitTicks(5);
        c.use();
        c.waitTicks(10);
        JsonObject icf = q(c);
        c.cmd("gamemode creative");
        Expect.that(icf.getAsJsonArray("marker_labels").toString().contains("Non-laser block"),
                "ICF controller scan marks the offending block: " + icf.get("marker_labels"));
        c.teleport(X - 5.5, Y + 3, mz + 6.5);
        c.lookAt(X - 5.5, Y + 1.5, mz - 0.5);
        c.waitTicks(5);
        c.screenshot("70-marker-icf");

        Fixtures.setBlock(c, X + 6, Y + 1, mz, "hbm:pwr_controller[facing=south]");
        c.cmd("gamemode survival");
        c.teleport(X + 6.5, Y + 1, mz + 3.5);
        c.lookAt(X + 6.5, Y + 1.5, mz + 0.5);
        c.waitTicks(5);
        c.use();
        c.waitTicks(10);
        JsonObject pwr = q(c);
        c.cmd("gamemode creative");
        c.key("escape"); // an assembled-state menu, if any
        c.waitTicks(3);
        c.screenshot("71-marker-pwr");
        Expect.that(pwr.getAsJsonArray("marker_labels").size() > icf.getAsJsonArray("marker_labels").size()
                        || pwr.getAsJsonArray("marker_labels").toString().contains("reactor")
                        || pwr.getAsJsonArray("marker_labels").toString().contains("Fuel"),
                "PWR controller scan marks an error: " + pwr.get("marker_labels"));
    }

    // ------------------------------------------------------------------ 8: neutrino lens

    private void lens(Client c) {
        int lz = Z - 4;
        c.cmd("fill " + (X + 8) + " " + (Y + 2) + " " + lz + " " + (X + 9) + " " + (Y + 2) + " " + (lz + 1) + " hbm:ore_alexandrite");
        JsonObject applied = q(c, "lens", new com.google.gson.JsonPrimitive(4815));
        Expect.that(applied.get("lens_applied").getAsBoolean(), "lens applied to the helmet: " + applied);
        c.teleport(X + 0.5, Y + 1, lz + 0.5);
        boolean found = false;
        JsonObject seen = null;
        for (int t = 0; t < 30 && !found; t++) {
            c.waitTicks(10);
            seen = q(c);
            found = seen.getAsJsonArray("marker_labels").toString().contains("Alexandrite");
        }
        c.teleport(X + 3.5, Y + 3, lz + 5.5);
        c.lookAt(X + 8.5, Y + 2.5, lz + 0.5);
        c.waitTicks(5);
        c.screenshot("72-marker-lens-scan");
        c.cmd("item replace entity @s armor.head with minecraft:air");
        Expect.that(found, "neutrino lens marked the alexandrite: " + (seen == null ? "" : seen.get("marker_labels")));
    }

    // ------------------------------------------------------------------ 7: launch smoke

    private void launchSmoke(Client c) {
        int sx = X + 16;
        int sz = Z - 16;
        Fixtures.setBlock(c, sx, Y + 1, sz, "hbm:struct_launcher_core");
        c.cmd("fill " + (sx - 1) + " " + (Y + 1) + " " + (sz - 1) + " " + (sx + 1) + " " + (Y + 1) + " " + (sz + 1)
                + " hbm:struct_launcher replace minecraft:air");
        c.waitTicks(10);
        Expect.block(c, sx, Y + 1, sz, "hbm:compact_launcher");
        int before = q(c).get("launch_smoke").getAsInt();
        c.cmd("gamemode spectator");
        c.teleport(sx + 6.5, Y + 4, sz + 6.5);
        c.lookAt(sx + 0.5, Y + 2, sz + 0.5);
        c.cmd("summon hbm:entity_custom_missile " + (sx + 0.5) + " " + (Y + 8) + " " + (sz + 0.5) + " {NoGravity:1b}");
        c.waitTicks(6);
        c.screenshot("61-launch-smoke");
        int after = q(c).get("launch_smoke").getAsInt();
        c.cmd("kill @e[type=hbm:entity_custom_missile]");
        c.cmd("gamemode creative");
        Expect.that(after - before >= 15, "15 smoke plumes a tick under a custom missile: " + (after - before));
        // the plumes move and grow (1.7: gaussian sideways push, friction 0.925): a pile at the pad is not the effect
        JsonObject drift = q(c);
        Expect.that(drift.get("launch_smoke_ticks").getAsInt() > 0 && drift.get("launch_smoke_drift").getAsDouble() > 0.5,
                "smoke plumes tick and drift off the pad: ticks " + drift.get("launch_smoke_ticks") + " drift "
                        + drift.get("launch_smoke_drift"));
    }

    // ------------------------------------------------------------------ 11 / 12: HUD

    private void hud(Client c) {
        c.cmd("clear");
        c.cmd("kill @e[type=!minecraft:player]");
        c.teleport(X + 0.5, Y + 1, Z + 0.5);
        c.look(0, 10);
        c.cmd("gamemode survival");
        c.cmd("effect give @s minecraft:resistance 600 4 true");
        c.cmd("item replace entity @s armor.chest with minecraft:iron_chestplate");

        // 12: the shield bar and its sync (1.7 serialize carried shield and max shield to the client)
        q(c, "shield", arr(12.5, 20.0));
        JsonObject synced = null;
        for (int t = 0; t < 20; t++) {
            c.waitTicks(5);
            synced = q(c);
            if (synced.get("client_max_shield").getAsDouble() == 20.0) break;
        }
        c.screenshot("80-hud-shield-bar");
        Expect.that(synced.get("client_max_shield").getAsDouble() == 20.0, "shield max synced: " + synced);
        // the shield regenerates between the client and the server read, so the client may trail the server
        Expect.that(synced.get("client_shield").getAsDouble() >= 12.5
                        && synced.get("client_shield").getAsDouble() <= synced.get("server_shield").getAsDouble(),
                "shield value synced: " + synced);
        q(c, "shield", arr(0, 0));

        // 11: the HEV suit replaces hearts and armour on every node
        for (String piece : new String[]{"head:hev_helmet", "chest:hev_plate", "legs:hev_legs", "feet:hev_boots"}) {
            String[] p = piece.split(":");
            c.cmd("item replace entity @s armor." + p[0] + " with hbm:" + p[1]);
        }
        c.waitTicks(20);
        JsonObject hev = q(c);
        c.screenshot("81-hud-hev");
        Expect.that(hev.get("hud_suppressible").getAsBoolean(), "vanilla HUD rows suppressible on this node");
        Expect.that(hev.get("hide_health").getAsBoolean() && hev.get("hide_armor").getAsBoolean(),
                "HEV hides hearts and armour: " + hev);
        for (String slot : new String[]{"head", "chest", "legs", "feet"}) {
            c.cmd("item replace entity @s armor." + slot + " with minecraft:air");
        }
        c.cmd("gamemode creative");
    }

    private static JsonArray arr(double a, double b) {
        JsonArray out = new JsonArray();
        out.add(a);
        out.add(b);
        return out;
    }

    // ------------------------------------------------------------------ 13: magnet key

    private void magnet(Client c) {
        boolean start = q(c).get("server_magnet").getAsBoolean();
        c.key("key.hbm.toggleMagnet");
        c.waitTicks(5);
        c.screenshot("90-magnet-toggled");
        JsonObject one = q(c);
        c.key("key.hbm.toggleMagnet");
        c.waitTicks(5);
        JsonObject two = q(c);
        Expect.that(one.get("server_magnet").getAsBoolean() != start, "magnet key toggles the server flag: " + one);
        Expect.that(two.get("server_magnet").getAsBoolean() == start, "second press toggles it back: " + two);
    }

    // ------------------------------------------------------------------ 10: advancements

    private void advancements(Client c) {
        c.cmd("advancement grant @s everything");
        c.waitTicks(20);
        c.key("key.advancements");
        c.waitTicks(20);
        c.screenshot("95-advancements");
        // tab order differs per loader: show the first three tabs so Nuclear Tech is seen on every node
        // (vanilla AdvancementsScreen: 252x140 window centred, tabs 28 wide in a row above it)
        JsonObject st = c.status();
        int left = (st.get("gui_width").getAsInt() - 252) / 2;
        int top = (st.get("gui_height").getAsInt() - 140) / 2;
        for (int tab = 0; tab < 3; tab++) {
            c.click(left + 28 * tab + 14, top - 14);
            c.waitTicks(5);
            c.screenshot("96-advancements-tab" + tab);
        }
        c.key("escape");
        c.waitTicks(3);
        c.cmd("advancement revoke @s everything");
    }

    // ------------------------------------------------------------------ helpers

    private static void worldShot(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.hotbar(9);
        c.waitTicks(1);
        c.cmd("gamemode spectator");
        c.waitTicks(2);
        c.teleport(x, y, z);
        c.waitTicks(8);
        c.lookAt(tx, ty, tz);
        c.waitTicks(8);
        c.screenshot(name);
        c.cmd("gamemode creative");
        c.waitTicks(2);
    }

    private static void worldShotNoWait(Client c, String name, double x, double y, double z, double tx, double ty, double tz) {
        c.teleport(x, y, z);
        c.lookAt(tx, ty, tz);
        c.waitTicks(1);
        c.screenshot(name);
    }
}
