package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.inventory.gui.GUIAnvil;
import dev.golem.core.harness.HarnessQueryRegistry;
import net.minecraft.client.Minecraft;

/** The {@code hbm_anvil} harness query: the open anvil screen's selected construction recipe and page. */
public final class AnvilHarnessQueries {

    private AnvilHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_anvil", AnvilHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
                boolean open = mc.screen instanceof GUIAnvil;
        json.addProperty("open", open);
        if (open) {
            GUIAnvil anvil = (GUIAnvil) mc.screen;
            json.addProperty("selected", anvil.selectedRecipeId());
            json.addProperty("page", anvil.pageIndex());
        }
        return json;
    }
}
