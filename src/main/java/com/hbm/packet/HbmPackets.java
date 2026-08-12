package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.IBufPacketReceiver;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Loader-agnostic handling for HBM's custom packets. Each loader registers
 * {@link BlockEntitySyncPayload#TYPE}/{@link BlockEntitySyncPayload#CODEC} with its own networking API
 * and routes received payloads here on the client main thread.
 */
public final class HbmPackets {

    private HbmPackets() {}

    /**
     * Applies a received {@link BlockEntitySyncPayload}: resolves the tile in the given (client) level
     * and replays the byte blob into its {@link IBufPacketReceiver#deserialize}. Must run on the main
     * client thread (each loader's receiver is responsible for that). Failures are swallowed with a
     * warning — a malformed/underflowing buffer must never tear down the netty pipeline.
     */
    public static void handleSync(Level level, BlockEntitySyncPayload payload) {
        if (level == null) return;
        BlockEntity be = level.getBlockEntity(payload.pos());
        if (be instanceof IBufPacketReceiver receiver) {
            RegistryFriendlyByteBuf buf =
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), level.registryAccess());
            try {
                receiver.deserialize(buf);
            } catch (Exception e) {
                Constants.LOG.warn("Failed to apply BlockEntitySyncPayload at {}: {}", payload.pos(), e.toString());
            }
        }
    }

    /**
     * Applies a received {@link ControlEventPayload} server-side: resolves the tile, checks the sender is
     * within reach (and the tile's own {@link IControlReceiver#hasPermission permission}), then hands it
     * the control tag. Must run on the server main thread (each loader enqueues it there). The 64-block
     * reach gate mirrors the 1.12.2 {@code isUseableByPlayer} check so a client can't poke distant tiles.
     */
    /**
     * Applies a received {@link EntityPropsSyncPayload} to the local (client) player's duck props.
     * Must run on the client main thread. {@code player} is the client player handed over by the
     * loader's receiver context.
     */
    public static void handlePropsSync(Player player, EntityPropsSyncPayload payload) {
        if (player == null) return;
        try {
            HbmLivingProps.getData(player).deserialize(Unpooled.wrappedBuffer(payload.data()));
        } catch (Exception e) {
            Constants.LOG.warn("Failed to apply EntityPropsSyncPayload: {}", e.toString());
        }
    }

    /**
     * Applies a received {@link AnvilCraftPayload} server-side (the anvil's construction crafting).
     * Everything the client claims is re-validated here: the recipe index, that the player actually has
     * an anvil menu open, and the anvil's tier (read from the server's own world via the container).
     * Ingredients are pulled from — and outputs given to — the player's inventory, like the original.
     */
    public static void handleAnvilCraft(ServerPlayer player, AnvilCraftPayload payload) {
        if (player == null) return;
        java.util.List<com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe> recipes =
                com.hbm.inventory.recipes.anvil.AnvilRecipes.getConstruction();
        if (payload.recipeIndex() < 0 || payload.recipeIndex() >= recipes.size()) return;
        if (!(player.containerMenu instanceof com.hbm.inventory.container.ContainerAnvil anvil)) return;

        com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe recipe = recipes.get(payload.recipeIndex());
        if (!recipe.isTierValid(anvil.tier)) return;

        int count = payload.mode() == 1 ? 64 : 1;
        for (int i = 0; i < count; i++) {
            if (com.hbm.util.InventoryUtil.doesPlayerHaveAStacks(player, recipe.input, true)) {
                com.hbm.util.InventoryUtil.giveChanceStacksToPlayer(player, recipe.output);
            } else {
                break;
            }
        }

        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    public static void handleControl(ServerPlayer player, ControlEventPayload payload) {
        if (player == null) return;
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(payload.pos());
        if (be instanceof IControlReceiver receiver
                && player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5, payload.pos().getZ() + 0.5) <= 64.0
                && receiver.hasPermission(player)) {
            try {
                receiver.receiveControl(payload.data());
            } catch (Exception e) {
                Constants.LOG.warn("Failed to apply ControlEventPayload at {}: {}", payload.pos(), e.toString());
            }
        }
    }
}
