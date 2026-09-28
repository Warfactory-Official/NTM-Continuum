package com.hbm.client;

import com.hbm.blocks.EnergyBedrockDeclarations;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.PowerCableBoxRenderer;
import com.hbm.client.render.RenderCharger;
import com.hbm.tileentity.ModEnergyBedrockBlockEntities;
import dev.golem.core.render.Sprites;

/** Block-entity renderers of the energy extras, and the computed bedrock ore grade sprites. */
public final class EnergyBedrockClientRegistrations {

    private EnergyBedrockClientRegistrations() {}

    /**
     * 1.7 {@code BedrockOreType} light/dark ends, by ordinal: the band 1.7's
     * {@code RGBMutatorInterpolatedComponentRemap(0xFFFFFF, 0x505050, light, dark)} mapped the greyscale art into.
     */
    private static final int[][] TYPE_BANDS = {
            {0xFFFFFF, 0x353535}, {0x868686, 0x000000}, {0xE6E6B6, 0x1C1C00},
            {0xC1C7BD, 0x2B3227}, {0xAFAFAF, 0x0F0F0F}, {0xE2FFFA, 0x1E8A77}};

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModEnergyBedrockBlockEntities.ENERGY_CABLE_BOX.get(), PowerCableBoxRenderer::new);
        HbmBlockEntityRenderers.regGem(sink, ModEnergyBedrockBlockEntities.CHARGER.get(), RenderCharger::new);
        // harness: the charge of whatever battery the player holds (a charge has no damage value to read)
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_battery", (mc, params) -> {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            if (mc.player == null) return json;
            net.minecraft.world.item.ItemStack held = mc.player.getMainHandItem();
            json.addProperty("id", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
            if (held.getItem() instanceof com.hbm.api.energymk2.IBatteryItem battery) {
                json.addProperty("charge", battery.getCharge(held));
                json.addProperty("max", battery.getMaxCharge(held));
            }
            return json;
        });
    }

    /**
     * One remap per grade prefix and type: 1.7 registered a mutable atlas icon per grade and type, each recolouring
     * that type's greyscale {@code bedrock_ore_<prefix>_<type>} art. Client only, before the first resource load.
     */
    public static void installSprites() {
        for (String prefix : EnergyBedrockDeclarations.PREFIXES) {
            for (int t = 0; t < EnergyBedrockDeclarations.TYPES.length; t++) {
                Sprites.remap(Sprites.Atlas.ITEMS)
                        .texture("hbm:items/bedrock_ore/" + prefix + "_" + EnergyBedrockDeclarations.TYPES[t])
                        .permutation(EnergyBedrockDeclarations.REMAPPED, TYPE_BANDS[t][0], TYPE_BANDS[t][1])
                        .declare();
            }
        }
    }
}
