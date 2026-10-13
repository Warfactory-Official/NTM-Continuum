package com.hbm.client;

import com.google.gson.JsonObject;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.LegacyMeta;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.machine.ItemFluidIDMulti;
import com.hbm.items.special.ItemBookLore;
import com.hbm.items.special.ItemClayTablet;
import com.hbm.items.special.ItemHolotapeImage;
import com.hbm.items.tool.ItemGuideBook;
import com.hbm.items.tool.ItemLeadBox;
import dev.golem.core.data.ItemContents;
import dev.golem.core.harness.HarnessQueryRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The {@code hbm_utility} harness query. Without parameters it reports, from the integrated server, the
 * local player's main-hand stack and the utility tools' stack data (pipette, settings tool, containment box,
 * multi identifier, pager channel, Coltass deposit, lore book) plus the hazard system's radiation for the
 * stack. With {@code give} it hands the player a stack that has no command spelling common to every version:
 * {@code lore:<key>}, {@code guide:<variant>}, {@code holotape:<variant>}, {@code tablet:<variant>}.
 * Inert unless the harness listener is running.
 */
public final class UtilityToolHarnessQueries {

    private UtilityToolHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_utility", UtilityToolHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return json;
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
                result.complete(player == null ? new JsonObject() : onServer(player, params));
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("error", t.toString());
                result.complete(error);
            }
        });
        try {
            return result.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            json.addProperty("error", e.toString());
            return json;
        }
    }

    private static JsonObject onServer(ServerPlayer player, JsonObject params) {
        JsonObject json = new JsonObject();
        if (params.has("give")) {
            String[] give = params.get("give").getAsString().split(":", 2);
            ItemStack stack = switch (give[0]) {
                case "lore" -> {
                    for (Object[] v : ItemBookLore.VARIANTS) {
                        if (v[0].equals(give[1])) {
                            ItemStack book = ItemBookLore.createBook((String) v[0], (Integer) v[1], (Integer) v[2], (Integer) v[3]);
                            if (((String) v[0]).startsWith("book_")) ItemBookLore.addArgs(book, (Integer) v[1] - 1, "5");
                            yield book;
                        }
                    }
                    yield ItemStack.EMPTY;
                }
                case "guide" -> ItemGuideBook.of(ItemGuideBook.BookType.getType(Integer.parseInt(give[1])));
                case "holotape" -> ModUtilityToolItems.holotape_image.stack(
                        ItemHolotapeImage.EnumHoloImage.values()[Integer.parseInt(give[1])]);
                case "tablet" -> ItemClayTablet.of(Integer.parseInt(give[1]));
                case "fluid_multi" -> {
                    ItemStack id = new ItemStack(ModUtilityToolItems.fluid_identifier_multi.get());
                    String[] types = give[1].split(",");
                    ItemFluidIDMulti.setType(id, Fluids.fromName(types[0]), true);
                    ItemFluidIDMulti.setType(id, Fluids.fromName(types[1]), false);
                    yield id;
                }
                default -> ItemStack.EMPTY;
            };
            json.addProperty("given", !stack.isEmpty() && player.getInventory().add(stack));
            player.inventoryMenu.broadcastChanges();
            return json;
        }

        ItemStack held = player.getMainHandItem();
        json.addProperty("id", held.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
        json.addProperty("count", held.getCount());
        json.addProperty("variant", LegacyMeta.of(held)); // 1.7 metadata, from the item
        json.addProperty("radiation", HazardSystem.getHazardLevelFromStack(held, HazardRegistry.RADIATION));
        json.addProperty("digamma_hazard", HazardSystem.getHazardLevelFromStack(held, HazardRegistry.DIGAMMA));
        ModUtilityToolItems.PIPETTE.get(held).ifPresent(t -> json.addProperty("pipette", t.toString()));
        ModUtilityToolItems.SETTINGS.get(held).ifPresent(t -> json.addProperty("settings", t.toString()));
        ModUtilityToolItems.FLUID_MULTI.get(held).ifPresent(t -> json.addProperty("fluid_multi", t.toString()));
        ModUtilityToolItems.PAGER_CHANNEL.get(held).ifPresent(t -> json.addProperty("pager", t));
        ModUtilityToolItems.COLTASS.get(held).ifPresent(t -> json.addProperty("coltass", t.toString()));
        ModUtilityToolItems.BOOK_LORE.get(held).ifPresent(t -> json.addProperty("book_lore", t.toString()));
        ModUtilityToolItems.TABLET_SEED.get(held).ifPresent(t -> json.addProperty("tablet_seed", t));
        if (held.getItem() instanceof ItemLeadBox) {
            int filled = 0;
            for (ItemStack s : ModUtilityToolItems.CONTAINMENT.getOr(held, ItemContents.EMPTY).copies()) {
                if (!s.isEmpty()) filled++;
            }
            json.addProperty("box_filled", filled);
        }
        return json;
    }

}
