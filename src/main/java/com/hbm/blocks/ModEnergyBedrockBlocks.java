package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.machine.Charger;
import com.hbm.blocks.network.PowerCableBox;
import com.hbm.blocks.network.WireCoated;
import com.hbm.creativetabs.HbmTab;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

/**
 * The energy extras 1.7 kept in {@code ModBlocks}: the boxed and the coated red copper cable and the charging
 * station. All three are {@code Material.iron} at 5.0 hardness / 10.0 resistance, which in 1.7
 * needed a pickaxe to drop.
 *
 * <p>{@code red_cable_box} was one block with sizes 0..4 in metadata; it is flattened to
 * {@code red_cable_box_<size>} like {@code fluid_duct_box}.
 */
public final class ModEnergyBedrockBlocks {

    private ModEnergyBedrockBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Every one drops itself and is pickaxe-mined. */
    private static final Preset IRON = handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE);

    private static UnaryOperator<BlockBehaviour.Properties> iron() {
        return p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    @SuppressWarnings("unchecked")
    public static final RegistryObject<PowerCableBox>[] red_cable_box = new RegistryObject[5];

    static {
        for (int size = 0; size < 5; size++) {
            final int s = size;
            // drawn by its renderer: the blockstate only carries the break particle; the item is 1.7's
            // RenderBoxDuct.renderInventoryBlock box, an authored element model per size
            red_cable_box[size] = REG.block("red_cable_box_" + size, p -> new PowerCableBox(p, s),
                            p -> iron().apply(p).noOcclusion())
                    .transform(Presets.rendered("hbm:block/red_cable_box_particle", Variants.simple()))
                    .transform(Presets.shares("hbm:block/red_cable_box_inventory_" + size))
                    .transform(IRON);
        }
    }

    /** 1.7 {@code red_wire_coated} (its {@code _ct} sheet is not used, see {@code WireCoated}). */
    public static final RegistryObject<WireCoated> red_wire_coated =
            REG.block("red_wire_coated", WireCoated::new, iron()).transform(Presets.cubeAll().and(IRON));
    /** 1.7 blockTab; renderer-drawn, so nothing may cull or suffocate against it. */
    public static final RegistryObject<Charger> charger = REG.block("charger", Charger::new,
            p -> iron().apply(p).noOcclusion().isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false))
            .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")).and(IRON))
            .transform(ModEnergyBedrockBlocks::chargerItem);

    static {
        // 1.7 ModBlocks order: red_wire_coated, red_cable_box (machineTab); charger (blockTab)
        item(HbmTab.MACHINES, "red_wire_coated", red_wire_coated);
        for (int size = 0; size < 5; size++) item(HbmTab.MACHINES, "red_cable_box_" + size, red_cable_box[size]);
        item(HbmTab.BLOCKS, "charger", charger);
    }

    private static void item(HbmTab tab, String name, RegistryObject<? extends Block> block) {
        REG.blockItem(block, Compat::blockDescription).tab(tab);
    }

    /**
     * The charger's block icon is {@code block_steel} (particles only); its item is 1.7's {@code ItemRenderBase}:
     * inventory {@code T(0,-7,0) S(10)}, common {@code S(2) T(0.5,0,0)}, parts Base + Slide.
     */
    private static void chargerItem(Reg.Handle<?> handle) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", "hbm:models/blocks/charger.obj");
        model.addProperty("texture", "hbm:textures/models/machines/charger.png");
        JsonArray parts = new JsonArray();
        parts.add("Base");
        parts.add("Slide");
        model.add("parts", parts);
        model.addProperty("scale", 2.0);
        model.addProperty("yaw", 0.0);
        model.addProperty("rawLegacyTransform", true);
        model.add("itemTranslate", numbers(1, 0, 0)); // S(2) then T(0.5,0,0)
        model.add("guiTranslate", numbers(0, -7, 0));
        model.addProperty("guiScale", 10.0);
        handle.icon(d -> d.shares("hbm:item/machine_obj_base"));
        Dejson.itemDefinition("hbm:item/" + handle.path()).add("model", model);
        Dejson.itemDefinition("hbm:item/" + handle.path()).addProperty("oversized_in_gui", true);
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }

    public static Block[] cableBoxes() {
        Block[] blocks = new Block[5];
        for (int i = 0; i < 5; i++) blocks[i] = red_cable_box[i].get();
        return blocks;
    }

    public static void init() {}
}
