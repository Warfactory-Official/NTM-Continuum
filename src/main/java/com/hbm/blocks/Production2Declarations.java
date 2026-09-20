package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import com.hbm.blocks.machine.MachineBoiler;
import com.hbm.blocks.machine.MachineDiFurnaceRTG;
import com.hbm.blocks.machine.MachineRtgFurnace;

/**
 * Dejson declarations for the production machines (group B). Touches no Minecraft class: it runs in a
 * plain JVM from {@code dejson.gradle.kts}.
 *
 * <p>The three OBJ-rendered multiblocks get the shared empty blockstate placeholder here and an authored
 * {@code hbm:machine_obj} item definition under {@code assets/hbm/items/}; the three cubes (old boiler,
 * RTG furnace, nuclear blast furnace) are fully declared from the 1.7 icon tables.
 */
public final class Production2Declarations {

    private static final String NS = "hbm";
    private static final String EMPTY = "hbm:block/machine_dummy_empty";
    private static final String[] BEDROCK_TYPES = {"light", "heavy", "rare", "actinide", "nonmetal", "crystal"};
    private static boolean declared;

    private Production2Declarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // Renderer-drawn folded multiblocks: the blockstate exists only because vanilla demands one.
        for (String folded : new String[]{"machine_excavator", "machine_ore_slopper", "machine_annihilator"}) {
            Dejson.block(NS, folded).rendered(EMPTY)
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).register();
        }

        // 1.7 MachineBoiler icons: top/bottom = machine_boiler_base, front = machine_boiler_front, else machine_boiler_side.
        Dejson.block(NS, "machine_boiler_off")
                .models(Models.orientable("hbm:blocks/machine_boiler_front", "hbm:blocks/machine_boiler_side", "hbm:blocks/machine_boiler_base"))
                .states(Variants.facing())
                .blockItemDirect()
                .register();

        // 1.7 MachineRtgFurnace icons: top/bottom = machine_rtg_furnace_base_alt, front = machine_rtg_furnace_off_alt / _on_alt, side = machine_rtg_furnace_side_alt.
        Dejson.block(NS, "machine_rtg_furnace_off")
                .models(Models.group()
                        .and("", Models.orientable("hbm:blocks/machine_rtg_furnace_off_alt", "hbm:blocks/machine_rtg_furnace_side_alt", "hbm:blocks/machine_rtg_furnace_base_alt"))
                        .and("_on", Models.orientable("hbm:blocks/machine_rtg_furnace_on_alt", "hbm:blocks/machine_rtg_furnace_side_alt", "hbm:blocks/machine_rtg_furnace_base_alt")))
                .states(Variants.product(Variants.facing(), Variants.flag("lit", "_on")))
                .blockItemDirect()
                .register();

        // 1.7 MachineDiFurnaceRTG icons: top = rtg_difurnace_top_off/_on, front = rtg_difurnace_front_off/_on, side and bottom = machine_rtg_furnace_side_alt.
        Dejson.block(NS, "machine_difurnace_rtg_off")
                .models(Models.group()
                        .and("", Models.parented("minecraft:block/orientable_with_bottom", Slots.of(
                                "front", "hbm:blocks/rtg_difurnace_front_off", "side", "hbm:blocks/machine_rtg_furnace_side_alt",
                                "top", "hbm:blocks/rtg_difurnace_top_off").and("bottom", "hbm:blocks/machine_rtg_furnace_side_alt")))
                        .and("_on", Models.parented("minecraft:block/orientable_with_bottom", Slots.of(
                                "front", "hbm:blocks/rtg_difurnace_front_on", "side", "hbm:blocks/machine_rtg_furnace_side_alt",
                                "top", "hbm:blocks/rtg_difurnace_top_on").and("bottom", "hbm:blocks/machine_rtg_furnace_side_alt"))))
                .states(Variants.product(Variants.facing(), Variants.flag("lit", "_on")))
                .blockItemDirect()
                .register();

        // The six BASE-grade graded bedrock ores the slopper produces (1.7 bedrock_ore_base_<type>.png).
        for (String type : BEDROCK_TYPES) {
            Dejson.item(NS, "bedrock_ore_base_" + type).models(Models.generated("hbm:items/bedrock_ore_base_" + type)).register();
        }

        for (String block : new String[]{"machine_excavator", "machine_ore_slopper", "machine_annihilator",
                "machine_difurnace_rtg_off"}) {
            Drops.self(NS, block);
        }
        // 1.7 MachineBoiler.getDrops: 3-6 steel scraps, spawned in Java; the table itself is empty.
        Drops.nothing(NS, "machine_boiler_off");
        // 1.7 MachineRtgFurnace.getItemDropped returns null: the deprecated RTG furnace drops nothing.
        Drops.nothing(NS, "machine_rtg_furnace_off");
    }
}
