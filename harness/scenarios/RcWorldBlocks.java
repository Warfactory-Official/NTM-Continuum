import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

import java.util.ArrayList;
import java.util.List;

/** Terrain, ore, plant and cave-spike blocks in rows on grass, screenshotted from the front. */
public final class RcWorldBlocks implements Scenario {

    private static final String[][] ROWS = {
            {"ore_uranium", "ore_thorium", "ore_titanium", "ore_sulfur", "ore_aluminium", "ore_fluorite", "ore_niter",
                    "ore_tungsten", "ore_lead", "ore_beryllium", "ore_rare", "ore_lignite", "ore_asbestos", "ore_cinnabar", "ore_cobalt"},
            {"ore_gneiss_iron", "ore_gneiss_gold", "ore_gneiss_uranium", "ore_gneiss_copper", "ore_gneiss_gas", "ore_gneiss_rare",
                    "ore_gneiss_lithium", "ore_gneiss_asbestos", "stone_gneiss", "stone_resource_hematite", "stone_resource_malachite",
                    "stone_resource_bauxite", "stone_resource_limestone", "stone_resource_sulfur", "stone_resource_asbestos", "stone_porous"},
            {"ore_depth_borax", "ore_depth_cinnabar", "ore_depth_zirconium", "cluster_iron", "cluster_titanium", "cluster_aluminium",
                    "cluster_copper", "cluster_depth_iron", "stone_depth", "ore_oil", "ore_bedrock_block", "ore_alexandrite",
                    "ore_basalt_sulfur", "ore_basalt_gem", "ore_meteor_iron", "block_meteor_molten"},
            {"block_meteor", "block_meteor_broken", "block_meteor_cobble", "block_meteor_treasure", "burning_earth", "waste_earth",
                    "waste_mycelium", "waste_trinitite", "impact_dirt", "ntm_dirt", "taint", "glyphid_base", "sellafield_0",
                    "sellafield_3", "ore_volcano", "stone_biome_desert"},
            {"stalagmite_sulfur", "stalagmite_asbestos", "stalagmite_ice", "stalagmite_snow", "stalagmite_glyphid1", "stalagmite_glyphid2",
                    "stalagmite_glyphid3", "plant_flower_foxglove", "plant_flower_nightshade", "plant_flower_tobacco", "plant_flower_weed",
                    "plant_tall_weed", "plant_dead_grass", "plant_dead_flower", "plant_reeds", "mush"},
    };

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(120_000L);
        c.useFlatWorld();
        c.cmd("gamemode creative");
        Fixtures.platform(c, 0, 100, 0, 24, "minecraft:grass_block");
        c.cmd("fill -24 101 -24 24 125 24 minecraft:air");
        c.markLog();
        List<String> failed = new ArrayList<>();
        for (int r = 0; r < ROWS.length; r++) {
            for (int i = 0; i < ROWS[r].length; i++) {
                int x = -16 + i * 2, z = -10 + r * 4;
                c.cmd("setblock " + x + " 101 " + z + " hbm:" + ROWS[r][i]);
            }
        }
        // stalactites hang from a ceiling slab
        c.cmd("fill -16 104 12 14 104 12 minecraft:stone");
        String[] hang = {"stalactite_sulfur", "stalactite_asbestos", "stalactite_ice", "stalactite_snow"};
        for (int i = 0; i < hang.length; i++) c.cmd("setblock " + (-16 + i * 2) + " 103 12 hbm:" + hang[i]);
        c.waitTicks(20);
        for (int r = 0; r < ROWS.length; r++) {
            for (int i = 0; i < ROWS[r].length; i++) {
                String b = c.block(-16 + i * 2, 101, -10 + r * 4).get("block").getAsString();
                if (b.equals("minecraft:air") || !b.startsWith("hbm:")) failed.add(ROWS[r][i] + " -> " + b);
            }
        }
        for (int r = 0; r < ROWS.length; r++) {
            c.teleport(-1.0, 103, -10 + r * 4 - 4.5);
            c.lookAt(-1.0, 101, -10 + r * 4);
            c.waitTicks(20);
            c.screenshot("row" + r);
        }
        c.teleport(-1.0, 102, 8.0);
        c.lookAt(-10.0, 102.5, 12);
        c.waitTicks(20);
        c.screenshot("stalactites");
        System.out.println("[RcWorldBlocks] " + c.node() + " not present after placing: " + failed);
        Expect.that(failed.isEmpty(), "every block stays where it is placed: " + failed);
        Expect.logCleanSinceMark(c, "/(ERROR|FATAL)[]]|Missing model|Failed to load texture");
    }
}
