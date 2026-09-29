import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/** Authoring tool (run once, then copy saves/harness/generated/harness/structures/ore_depot.nbt to harness/fixtures/): builds the ore depot and saves it as structure harness:ore_depot with a structure block. */
public class MakeOreDepotFixture implements Scenario {
    public void run(Client c) throws Exception {
        int ox = 40, y = 101, oz = 40;   // away from the factory site
        Fixtures.platform(c, ox + 2, 100, oz + 2, 6);
        // 5 x 4 x 5: steel-beam corners, a slab roof, the stocked crate in the middle, spare crates behind it
        for (int[] p : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) {
            c.cmd("fill " + (ox + p[0]) + " " + y + " " + (oz + p[1]) + " " + (ox + p[0]) + " " + (y + 2) + " " + (oz + p[1]) + " hbm:steel_beam");
        }
        c.cmd("fill " + ox + " " + (y + 3) + " " + oz + " " + (ox + 4) + " " + (y + 3) + " " + (oz + 4) + " minecraft:smooth_stone_slab");
        c.cmd("fill " + ox + " " + (y - 1) + " " + oz + " " + (ox + 4) + " " + (y - 1) + " " + (oz + 4) + " minecraft:yellow_concrete");
        c.cmd("setblock " + (ox + 2) + " " + y + " " + (oz + 2) + " hbm:crate_steel{inventory:{Items:["
                + "{Slot:0b,id:\"minecraft:iron_ingot\",count:10},"
                + "{Slot:1b,id:\"minecraft:coal_block\",count:8},"
                + "{Slot:2b,id:\"minecraft:coal\",count:64},"
                + "{Slot:3b,id:\"minecraft:copper_ingot\",count:6}]}}");
        c.cmd("setblock " + (ox + 1) + " " + y + " " + (oz + 1) + " hbm:crate_iron");
        c.cmd("setblock " + (ox + 1) + " " + y + " " + (oz + 3) + " hbm:crate_iron");
        c.cmd("setblock " + (ox + 1) + " " + (y + 1) + " " + (oz + 1) + " hbm:crate_iron");
        c.waitTicks(2);
        // A structure block beside the depot in SAVE mode; its menu's SAVE button writes the file to disk
        // (a redstone pulse only captures the template in memory).
        c.cmd("setblock " + (ox - 2) + " " + y + " " + oz + " minecraft:structure_block{mode:\"SAVE\",name:\"harness:ore_depot\","
                + "posX:2,posY:-1,posZ:0,sizeX:5,sizeY:5,sizeZ:5,ignoreEntities:1b,showboundingbox:1b}");
        c.waitTicks(2);
        c.teleport(ox - 3.5, y, oz + 0.5);
        c.waitTicks(3);
        Fixtures.interact(c, ox - 2, y, oz);
        c.waitScreen("Structure", 5000);
        c.screenshot("structure-block");
        c.clickWidget("SAVE");
        c.waitScreen("none", 5000);
        c.waitTicks(5);
        c.cmd("save-all flush");
        c.waitTicks(20);
    }
}
