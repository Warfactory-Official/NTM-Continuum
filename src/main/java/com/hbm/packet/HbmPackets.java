package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.IBufPacketReceiver;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.net.GolemNetwork;
import io.netty.buffer.Unpooled;
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

    /** Declares HBM's play payloads once; loaders flush these declarations at their own hook. */
    public static void declare() {
        GolemNetwork.declarePlayToClient(BlockEntitySyncPayload.CODEC);
        GolemNetwork.declarePlayToClient(EntityPropsSyncPayload.CODEC);
        GolemNetwork.registerPlayToServer(ControlEventPayload.CODEC,
                (payload, player) -> handleControl(player, payload));
        GolemNetwork.registerPlayToServer(AnvilCraftPayload.CODEC,
                (payload, player) -> handleAnvilCraft(player, payload));
        GolemNetwork.declarePlayToClient(GunAnimationPayload.CODEC);
        GolemNetwork.registerPlayToServer(KeybindPayload.CODEC,
                (payload, player) -> com.hbm.handler.HbmKeybindsServer.onPressedServer(player,
                        com.hbm.handler.HbmKeybinds.EnumKeybind.byOrdinal(payload.keybind()), payload.state()));
    }

    /** Tells one player's client that their gun played an animation cue. */
    public static void sendGunAnimation(ServerPlayer player, com.hbm.items.weapon.sedna.GunAnimation animation,
            int configIndex) {
        GolemNetwork.sendToPlayer(player, GunAnimationPayload.CODEC,
                new GunAnimationPayload(animation.ordinal(), 0, configIndex));
    }

    /** Attaches client handlers only after the physical client is available. */
    public static void registerClientHandlers() {
        GolemNetwork.registerPlayToClient(BlockEntitySyncPayload.CODEC,
                (payload, context) -> handleSync(context.level(), payload));
        GolemNetwork.registerPlayToClient(EntityPropsSyncPayload.CODEC,
                (payload, context) -> handlePropsSync(context.player(), payload));
        GolemNetwork.registerPlayToClient(GunAnimationPayload.CODEC,
                (payload, context) -> com.hbm.client.anim.GunAnimationHandler.handle(payload));
    }

    /**
     * Applies a received {@link BlockEntitySyncPayload}: resolves the tile in the given (client) level
     * and replays the byte blob into its {@link IBufPacketReceiver#deserialize}. Must run on the main
     * client thread (each loader's receiver is responsible for that). Failures are swallowed with a
     * warning — a malformed/underflowing buffer must never tear down the netty pipeline.
     */
    public static void handleSync(Level level, BlockEntitySyncPayload payload) {
        handleSync(level, payload.pos(), payload.data());
    }

    public static void handleSync(Level level, net.minecraft.core.BlockPos pos, byte[] data) {
        if (level == null) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IBufPacketReceiver receiver) {
            var buf = GolemBuffers.wrap(Unpooled.wrappedBuffer(data), level);
            try {
                receiver.deserialize(buf);
            } catch (Exception e) {
                Constants.LOG.warn("Failed to apply BlockEntitySyncPayload at {}: {}", pos, e.toString());
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
        handlePropsSync(player, payload.data());
    }

    public static void handlePropsSync(Player player, byte[] data) {
        if (player == null) return;
        try {
            HbmLivingProps.getData(player).deserialize(Unpooled.wrappedBuffer(data));
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
        handleAnvilCraft(player, payload.recipeIndex(), payload.mode());
    }

    public static void handleAnvilCraft(ServerPlayer player, int recipeIndex, int mode) {
        if (player == null) return;
        java.util.List<com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe> recipes =
                com.hbm.inventory.recipes.anvil.AnvilRecipes.getConstruction();
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;
        if (!(player.containerMenu instanceof com.hbm.inventory.container.ContainerAnvil anvil)) return;

        com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe recipe = recipes.get(recipeIndex);
        if (!recipe.isTierValid(anvil.tier)) return;

        int count = mode == 1 ? 64 : 1;
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
        handleControl(player, payload.pos(), payload.data());
    }

    public static void handleControl(ServerPlayer player, net.minecraft.core.BlockPos pos, net.minecraft.nbt.CompoundTag data) {
        if (player == null) return;
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IControlReceiver receiver
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0
                && receiver.hasPermission(player)) {
            try {
                receiver.receiveControl(data);
            } catch (Exception e) {
                Constants.LOG.warn("Failed to apply ControlEventPayload at {}: {}", pos, e.toString());
            }
        }
    }
}
