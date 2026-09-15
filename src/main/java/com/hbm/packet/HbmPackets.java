package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.IBufPacketReceiver;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.net.GolemNetwork;
import io.netty.buffer.Unpooled;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
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
        dev.golem.core.net.GolemSync.install();
        com.hbm.particle.helper.CasingCreator.setSender((level, event) -> {
            if (!(level instanceof ServerLevel server)) return;
            if (event.gun()) {
                sendGunCasing(server, new GunCasingEjectPayload(event.x(), event.y(), event.z(), event.mX(),
                        event.mY(), event.mZ(), event.yaw(), event.pitch(), event.mPitch(), event.mYaw(),
                        event.casing(), event.smoking(), event.smokeLife(), event.smokeLift(), event.nodeLife(),
                        event.legacy()));
            } else {
                sendCasing(server, new CasingEjectPayload(event.x(), event.y(), event.z(), event.mX(), event.mY(),
                        event.mZ(), event.yaw(), event.pitch(), event.mPitch(), event.mYaw(), event.casing(),
                        event.smoking(), event.smokeLife(), event.smokeLift(), event.nodeLife()));
            }
        });
        GolemNetwork.declarePlayToClient(BlockEntitySyncPayload.CODEC);
        GolemNetwork.declarePlayToClient(EntityPropsSyncPayload.CODEC);
        GolemNetwork.registerPlayToServer(ControlEventPayload.CODEC,
                (payload, player) -> handleControl(player, payload));
        GolemNetwork.registerPlayToServer(AnvilCraftPayload.CODEC,
                (payload, player) -> handleAnvilCraft(player, payload));
        GolemNetwork.declarePlayToClient(GunAnimationPayload.CODEC);
        GolemNetwork.declarePlayToClient(PlasmaBlastPayload.CODEC);
        GolemNetwork.declarePlayToClient(MuzzleFlashPayload.CODEC);
        GolemNetwork.declarePlayToClient(GunSoundPayload.CODEC);
        GolemNetwork.declarePlayToClient(CasingEjectPayload.CODEC);
        GolemNetwork.declarePlayToClient(GunCasingEjectPayload.CODEC);
        GolemNetwork.declarePlayToClient(ParticlePayload.CODEC);
        GolemNetwork.declarePlayToClient(JetpackSyncPayload.CODEC);
        GolemNetwork.declarePlayToClient(JetpackExhaustPayload.CODEC);
        GolemNetwork.declarePlayToClient(ProjectileStatePayload.CODEC);
        GolemNetwork.registerPlayToServer(JetpackInputPayload.CODEC,
                (payload, player) -> com.hbm.handler.JetpackHandler.handleInput(player, payload));
        GolemNetwork.registerPlayToServer(KeybindPayload.CODEC,
                (payload, player) -> {
                    com.hbm.handler.HbmKeybinds.EnumKeybind keybind =
                            com.hbm.handler.HbmKeybinds.EnumKeybind.byOrdinal(payload.keybind());
                    if (keybind != null) {
                        com.hbm.handler.HbmKeybindsServer.onPressedServer(player, keybind, payload.state());
                    }
                });
        GolemNetwork.registerPlayToServer(CraneKeyPayload.CODEC,
                (payload, player) -> handleCraneKey(player, payload));
        GolemNetwork.registerPlayToServerOffThread(NetworkProbePayload.CODEC,
                NetworkProbePayload::received);
    }

    /** Tells one player's client that their gun played an animation cue. */
    public static void sendGunAnimation(ServerPlayer player, net.minecraft.world.item.ItemStack stack,
            com.hbm.items.weapon.sedna.GunAnimation animation, int configIndex) {
        sendGunAnimation(player, stack, animation, InteractionHand.MAIN_HAND, 0, configIndex);
    }

    /** Sends an animation cue while retaining the source receiver/rail and config index. */
    public static void sendGunAnimation(ServerPlayer player, net.minecraft.world.item.ItemStack stack,
            com.hbm.items.weapon.sedna.GunAnimation animation, int receiver, int configIndex) {
        sendGunAnimation(player, stack, animation, handOf(player, stack), receiver, configIndex);
    }

    /** Sends an animation cue for the exact hand that owns {@code stack}. */
    public static void sendGunAnimation(ServerPlayer player, net.minecraft.world.item.ItemStack stack,
            com.hbm.items.weapon.sedna.GunAnimation animation, InteractionHand hand, int receiver,
            int configIndex) {
        GolemNetwork.sendToPlayer(player, GunAnimationPayload.CODEC,
                new GunAnimationPayload(animation.ordinal(), receiver, configIndex, hand,
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
    }

    private static InteractionHand handOf(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        if (player.getItemInHand(InteractionHand.OFF_HAND) == stack) return InteractionHand.OFF_HAND;
        return InteractionHand.MAIN_HAND;
    }

    /** Sends a seeded gun sound to the same tracking population as the source gun packet. */
    public static void sendGunSound(ServerLevel level, boolean legacy, SoundEvent sound, SoundSource source,
            double x, double y, double z, float volume, float pitch, long seed) {
        GunSoundPayload.send(level, legacy, sound, source, x, y, z, volume, pitch, seed);
    }

    /** Sends the observer muzzle edge; the firing player's own animation packet handles the local gun. */
    public static void sendMuzzleFlash(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            MuzzleFlashPayload payload = new MuzzleFlashPayload(entity.getId());
            double radiusSquared = 100D * 100D;
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(entity) <= radiusSquared)
                    GolemNetwork.sendToPlayer(player, MuzzleFlashPayload.CODEC, payload);
            }
        }
    }

    /** Sends an ordinary casing snapshot to the tracking population. */
    public static void sendCasing(ServerLevel level, CasingEjectPayload payload) {
        sendToRadius(level, payload.x(), payload.y(), payload.z(), 50D, CasingEjectPayload.CODEC, payload);
    }

    /** Sends a mode-filtered gun casing snapshot to the tracking population. */
    public static void sendGunCasing(ServerLevel level, GunCasingEjectPayload payload) {
        sendToRadius(level, payload.x(), payload.y(), payload.z(), 50D, GunCasingEjectPayload.CODEC, payload);
    }

    private static <P extends dev.golem.core.net.GolemPayload> void sendToRadius(ServerLevel level,
            double x, double y, double z, double radius, dev.golem.core.net.GolemCodec<P> codec, P payload) {
        double radiusSquared = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= radiusSquared) GolemNetwork.sendToPlayer(player, codec, payload);
        }
    }

    /** Attaches client handlers only after the physical client is available. */
    public static void registerClientHandlers() {
        GolemNetwork.registerPlayToClient(BlockEntitySyncPayload.CODEC,
                (payload, context) -> handleSync(context.level(), payload));
        GolemNetwork.registerPlayToClient(EntityPropsSyncPayload.CODEC,
                (payload, context) -> handlePropsSync(context.player(), payload));
        GolemNetwork.registerPlayToClient(GunAnimationPayload.CODEC,
                (payload, context) -> com.hbm.client.anim.GunAnimationHandler.handle(payload));
        GolemNetwork.registerPlayToClient(PlasmaBlastPayload.CODEC,
                (payload, context) -> handlePlasmaBlast(context.level(), payload));
        GolemNetwork.registerPlayToClient(MuzzleFlashPayload.CODEC,
                (payload, context) -> com.hbm.client.GunEffectState.markMuzzleFlash(payload));
        GolemNetwork.registerPlayToClient(GunSoundPayload.CODEC, GunSoundPayload::handleClient);
        GolemNetwork.registerPlayToClient(CasingEjectPayload.CODEC,
                (payload, context) -> com.hbm.client.GunEffectState.handleCasing(payload));
        GolemNetwork.registerPlayToClient(GunCasingEjectPayload.CODEC,
                (payload, context) -> {
                    if (payload.legacy() == com.hbm.config.ClientConfig.GUN_ANIMS_LEGACY)
                        com.hbm.client.GunEffectState.handleGunCasing(payload);
                });
        GolemNetwork.registerPlayToClient(ParticlePayload.CODEC,
                (payload, context) -> handleParticle(context.level(), payload));
        GolemNetwork.registerPlayToClient(JetpackSyncPayload.CODEC,
                (payload, context) -> com.hbm.handler.JetpackHandler.dispatchClientSync(context.level(), payload));
        GolemNetwork.registerPlayToClient(JetpackExhaustPayload.CODEC,
                (payload, context) -> com.hbm.handler.JetpackHandler.dispatchClientExhaust(context.level(), payload));
        GolemNetwork.registerPlayToClient(ProjectileStatePayload.CODEC,
                (payload, context) -> payload.apply(context.level()));
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
     * Applies a received {@link ParticlePayload}: resolves the named effect and runs its client
     * handler. Must run on the main client thread (each loader's receiver is responsible for that).
     *
     * <p>An unknown effect name is ignored rather than logged — a server running a build with more
     * effects than this client knows about is a version skew, not a bug worth spamming the log for.
     * A handler that throws is caught and warned about: a bad particle must never tear down the netty
     * pipeline, which is the same rule {@link #handleSync} follows.
     */
    public static void handleParticle(Level level, ParticlePayload payload) {
        if (level == null) return;
        com.hbm.particle.helper.HbmEffectNT effect =
                com.hbm.particle.helper.HbmEffectNT.byName(payload.effect());
        if (effect == null) return;
        try {
            effect.summonParticle(level, payload.x(), payload.y(), payload.z(), payload.data());
        } catch (Exception e) {
            Constants.LOG.warn("Failed to play particle effect {}: {}", payload.effect(), e.toString());
        }
    }

    /** Rebuilds the source shockwave using Core's oriented additive particle path. */
    public static void handlePlasmaBlast(Level level, PlasmaBlastPayload payload) {
        if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel client)) return;
        dev.golem.core.render.Particles.add(new com.hbm.client.particle.ParticlePlasmaBlast(client,
                payload.x(), payload.y(), payload.z(), payload.r(), payload.g(), payload.b(),
                payload.pitch(), payload.yaw(), payload.scale()));
    }

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

    /**
     * Applies a received {@link ControlEventPayload} server-side: first enforces the 24-block absolute
     * envelope, then lets the receiver apply its own permission policy and marks accepted changes dirty.
     */
    public static void handleControl(ServerPlayer player, ControlEventPayload payload) {
        handleControl(player, payload.pos(), payload.data());
    }

    public static void handleCraneKey(ServerPlayer player, CraneKeyPayload payload) {
        if (player == null || payload.key() < 0 || payload.key() >= com.hbm.handler.EnumCraneKey.VALUES.length) return;
        com.hbm.extprop.HbmPlayerProps.getData(player).setCraneKeyPressed(
                com.hbm.handler.EnumCraneKey.VALUES[payload.key()], payload.pressed());
    }

    public static void handleControl(ServerPlayer player, net.minecraft.core.BlockPos pos, net.minecraft.nbt.CompoundTag data) {
        if (player == null || pos == null || data == null || data.sizeInBytes() > 16 * 1024
                || containsNestedCompound(data)) return;
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 24D * 24D) return;
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IControlReceiver receiver && receiver.hasPermission(player)) {
            try {
                receiver.receiveControl(data);
                be.setChanged();
            } catch (Exception e) {
                Constants.LOG.warn("Failed to apply ControlEventPayload at {}: {}", pos, e.toString());
            }
        }
    }

    /** Control packets are flat key/value schemas; nested tags have no receiver contract. */
    private static boolean containsNestedCompound(net.minecraft.nbt.CompoundTag data) {
        for (String key : dev.golem.core.data.Tags.keys(data)) {
            if (data.get(key) instanceof net.minecraft.nbt.CompoundTag
                    || data.get(key) instanceof net.minecraft.nbt.ListTag) return true;
        }
        return false;
    }
}
