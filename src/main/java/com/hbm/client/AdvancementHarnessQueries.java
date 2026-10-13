package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.gametest.AdvancementCommandGameTests;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.world.Advancements;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.TimeUnit;

/**
 * Harness queries for the advancement + command family, inert unless the harness listener runs:
 * <ul>
 * <li>{@code hbm_advancements}: the integrated server's view of which {@code hbm:} advancements the local player has
 *     completed ({@code done}) and how many are loaded ({@code loaded});</li>
 * <li>{@code hbm_suggest} ({@code input}: a command line without the slash): the server dispatcher's tab-completion
 *     suggestions for it, as the player would get them.</li>
 * </ul>
 */
public final class AdvancementHarnessQueries {

    private AdvancementHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_advancements", (mc, params) -> advancements(mc));
        HarnessQueryRegistry.register("hbm_suggest", AdvancementHarnessQueries::suggest);
    }

    private static ServerPlayer serverPlayer(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return null;
        return server.getPlayerList().getPlayer(mc.player.getUUID());
    }

    private static JsonObject advancements(Minecraft mc) {
        JsonObject json = new JsonObject();
        ServerPlayer player = serverPlayer(mc);
        if (player == null) return json;
        JsonArray done = new JsonArray();
        int loaded = 0;
        for (String path : AdvancementCommandGameTests.IDS) {
            Boolean state = Advancements.isDone(player, Constants.MOD_ID + ":" + path);
            if (state == null) continue;
            loaded++;
            if (state) done.add(Constants.MOD_ID + ":" + path);
        }
        json.addProperty("loaded", loaded);
        json.add("done", done);
        return json;
    }

    private static JsonObject suggest(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        ServerPlayer player = serverPlayer(mc);
        if (player == null || !params.has("input")) return json;
        String input = params.get("input").getAsString();
        var dispatcher = player.level().getServer().getCommands().getDispatcher();
        CommandSourceStack source = player.createCommandSourceStack();
        JsonArray list = new JsonArray();
        try {
            ParseResults<CommandSourceStack> parse = dispatcher.parse(input, source);
            Suggestions suggestions = dispatcher.getCompletionSuggestions(parse).get(5, TimeUnit.SECONDS);
            for (Suggestion suggestion : suggestions.getList()) list.add(suggestion.getText());
        } catch (Exception e) {
            json.addProperty("error", String.valueOf(e));
        }
        json.add("suggestions", list);
        return json;
    }
}
