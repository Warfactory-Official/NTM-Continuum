package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import com.hbm.items.machine.ItemPWRFuel;

/**
 * Dejson declarations for the PWR. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}.
 *
 * <p>The three pillar parts are 1.7 {@code BlockPillarPWR} (top texture on both ends, no axis); the six plain
 * parts are cubes; the controller is an orientable cube with its front on the facing side over the blank
 * casing; the assembled shell picks {@code pwr_block} or {@code pwr_casing_port} by its {@code port} flag and
 * has neither item nor drop. The fifteen fuel rods each have their own sprite; every hot and depleted rod
 * shares one.
 */
public final class PwrDeclarations {

    private static final String NS = "hbm";
    /** {@link ItemPWRFuel.EnumPWRFuel} ids, spelled out so this class loads no item class in the plain JVM. */
    private static final String[] FUELS = {"meu", "heu233", "heu235", "men", "hen237", "mox", "mep", "hep239",
            "hep241", "mea", "hea242", "hes326", "hes327", "bfb_am_mix", "bfb_pu241"};
    private static boolean declared;

    private PwrDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        for (String pillar : new String[]{"pwr_fuel", "pwr_control", "pwr_channel"}) {
            Dejson.block(NS, pillar)
                    .models(Models.column("hbm:blocks/" + pillar + "_top", "hbm:blocks/" + pillar + "_side"))
                    .states(Variants.simple())
                    .blockItemDirect()
                    .register();
            Drops.self(NS, pillar);
        }

        for (String cube : new String[]{"pwr_heatex", "pwr_heatsink", "pwr_neutron_source", "pwr_reflector",
                "pwr_casing", "pwr_port"}) {
            Dejson.block(NS, cube)
                    .models(Models.cubeAll("hbm:blocks/" + cube))
                    .states(Variants.simple())
                    .blockItemDirect()
                    .register();
            Drops.self(NS, cube);
        }

        Dejson.block(NS, "pwr_controller")
                .models(Models.orientable("hbm:blocks/pwr_controller", "hbm:blocks/pwr_casing_blank",
                        "hbm:blocks/pwr_casing_blank"))
                .states(Variants.facing())
                .blockItemDirect()
                .register();
        Drops.self(NS, "pwr_controller");

        Dejson.block(NS, "pwr_block")
                .models(Models.group()
                        .and("", Models.cubeAll("hbm:blocks/pwr_block"))
                        .and("_port", Models.cubeAll("hbm:blocks/pwr_casing_port")))
                .states(Variants.flag("port", "_port"))
                .register();
        Drops.nothing(NS, "pwr_block");

        for (String fuel : FUELS) {
            Dejson.item(NS, "pwr_fuel_" + fuel).models(Models.generated("hbm:items/pwr_fuel." + fuel)).register();
            Dejson.item(NS, "pwr_fuel_hot_" + fuel).models(Models.generated("hbm:items/pwr_fuel_hot")).register();
            Dejson.item(NS, "pwr_fuel_depleted_" + fuel).models(Models.generated("hbm:items/pwr_fuel_depleted")).register();
        }
        Dejson.item(NS, "pwr_printer").models(Models.generated("hbm:items/pwr_printer")).register();
    }
}
