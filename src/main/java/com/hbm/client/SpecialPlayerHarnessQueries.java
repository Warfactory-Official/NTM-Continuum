package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.client.render.util.RenderAccessoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * The {@code hbm_special} harness query: the cape the local player's cape layer reads (Core {@code PlayerSkins.cape})
 * next to the one 1.7's table names for them, so a scenario checks the installed cape, not just the absence of an
 * error. Inert unless the harness listener runs.
 */
public final class SpecialPlayerHarnessQueries {

    private SpecialPlayerHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_special", SpecialPlayerHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        if (mc.player == null) return json;
        Identifier cape = dev.golem.core.client.PlayerSkins.cape(mc.player);
        Identifier expected = RenderAccessoryUtility.getCloakFromPlayer(mc.player.getUUID(), mc.player.getName().getString());
        json.addProperty("name", mc.player.getName().getString());
        json.addProperty("uuid", mc.player.getUUID().toString());
        json.addProperty("cape", cape == null ? "" : cape.toString());
        json.addProperty("expected", expected == null ? "" : expected.toString());
        // the skinned bobbleheads: whether each person's skin has been fetched and registered
        JsonObject skins = new JsonObject();
        for (var type : com.hbm.blocks.generic.BlockBobble.BobbleType.VALUES) {
            if (type.skinUuid == null) continue;
            var skin = dev.golem.core.client.ProfileSkins.get(java.util.UUID.fromString(type.skinUuid));
            skins.addProperty(type.name(), skin == null ? "" : skin.texture().toString());
        }
        json.add("bobble_skins", skins);
        return json;
    }
}
