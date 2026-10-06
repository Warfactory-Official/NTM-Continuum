package com.hbm.handler;

import com.hbm.items.ModDataComponents;
import com.hbm.capability.HbmLivingProps;
import com.hbm.items.armor.ArmorBJJetpack;
import com.hbm.items.armor.ArmorDNT;
import com.hbm.items.armor.JetpackBase;
import com.hbm.items.armor.JetpackFueledBase;
import com.hbm.items.gear.JetpackGlider;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.packet.JetpackExhaustPayload;
import com.hbm.packet.JetpackInputPayload;
import com.hbm.packet.JetpackSyncPayload;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.data.Tags;
import dev.golem.core.world.Movement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Common flight state and motion rules for HBM's jetpacks, glider and wings. */
public final class JetpackHandler {

    public static final String JETPACK_NBT = "hbmJetpackAdvanced";

    public static final int EXHAUST_REGULAR = 0;
    public static final int EXHAUST_VECTOR = 1;
    public static final int EXHAUST_BJ = 2;
    public static final int EXHAUST_DNS = 3;
    private static final long INPUT_TIMEOUT_TICKS = 5L;
    private static final long SMOKE_TIMEOUT_TICKS = 200L;

    private static final Map<StateKey, JetpackInfo> STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, SmokeSession> SMOKE_SESSIONS = new ConcurrentHashMap<>();
    private static volatile BiConsumer<Level, JetpackSyncPayload> clientSyncCallback;
    private static volatile BiConsumer<Level, JetpackExhaustPayload> clientExhaustCallback;

    private JetpackHandler() {}

    /** Returns the state for a player, loading the advanced state carried by a worn glider if present. */
    public static JetpackInfo getState(Player player) {
        if (player == null) return null;
        StateKey key = stateKey(player);
        JetpackInfo state = STATES.computeIfAbsent(key, ignored -> stateFromStack(player));
        refreshSource(player, state);
        return state;
    }

    /** Alias retained for the old handler's public client contract. */
    public static JetpackInfo get(Player player) {
        return getState(player);
    }

    public static void put(Player player, JetpackInfo state) {
        if (player != null && state != null) {
            ItemStackAccess source = findJetpack(player);
            state.sourceContainer = player.level().isClientSide() || source.stack() == null ? null
                    : source.outer() == null ? source.stack() : source.outer();
            state.sourceItem = source.stack() == null ? null : source.stack().getItem();
            STATES.put(stateKey(player), state);
        }
    }

    /** UUID form for client render code that already has a stable player identity. */
    public static JetpackInfo getState(UUID player) {
        if (player == null) return null;
        JetpackInfo server = null;
        for (Map.Entry<StateKey, JetpackInfo> entry : STATES.entrySet()) {
            if (!entry.getKey().uuid().equals(player)) continue;
            if (entry.getKey().client()) return entry.getValue();
            server = entry.getValue();
        }
        return server;
    }

    /** Exact side/world UUID form for callers that have a level but not its Player object. */
    public static JetpackInfo getState(Level level, UUID player) {
        if (level == null || player == null) return null;
        return STATES.get(new StateKey(level.isClientSide(), player, level));
    }

    public static void remove(Player player) {
        if (player != null) STATES.remove(stateKey(player));
    }

    /** Clears both client/server state for one world during level teardown or client disconnect. */
    public static void clear(Level level) {
        if (level == null) return;
        for (SmokeSession session : SMOKE_SESSIONS.values()) {
            if (session.level() == level) stopSmoke(session.player());
        }
        STATES.keySet().removeIf(key -> key.level() == level);
    }

    /** Clears client-side state when the local connection has no active level. */
    public static void clearClientState() {
        STATES.keySet().removeIf(StateKey::client);
    }

    /** Removes server states for players who disconnected or changed dimensions. */
    public static void cleanup(Level level) {
        if (!(level instanceof ServerLevel server)) return;
        for (SmokeSession session : SMOKE_SESSIONS.values()) {
            ServerPlayer player = session.player();
            boolean gone = server.getServer().getPlayerList().getPlayer(player.getUUID()) != player;
            boolean changedLevel = player.level() != session.level();
            boolean dead = !player.isAlive() || player.isRemoved();
            boolean expired = player.level().getGameTime() >= session.expiresAt();
            if (session.level() == level && (gone || changedLevel || dead || expired)) stopSmoke(player);
        }
        STATES.entrySet().removeIf(entry -> entry.getKey().level() == level
                && server.players().stream().noneMatch(player -> player.getUUID().equals(entry.getKey().uuid())));
    }

    /** Starts the bounded development smoke fixture while retaining the player's exact chest state. */
    public static void startSmoke(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        stopSmoke(player);
        Map<EquipmentSlot, ItemStack> originalEquipment = new java.util.EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            originalEquipment.put(slot, player.getItemBySlot(slot).copy());
        SmokeSession session = new SmokeSession(player, level,
                originalEquipment, new java.util.HashMap<>(), player.isNoGravity(), player.onGround(),
                player.getDeltaMovement(), player.isShiftKeyDown(), "water".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"))
                        || "occlusion".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"))
                        || "crouch".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"))
                        || "flight_crouch".equals(System.getenv("HBM_JETPACK_CHECK_GEAR")) ? player.position() : null,
                level.getGameTime() + SMOKE_TIMEOUT_TICKS
                        * ("reload".equals(System.getenv("HBM_JETPACK_CHECK_GEAR")) ? 2 : 1));
        SMOKE_SESSIONS.put(player.getUUID(), session);

        ItemStack glider = new ItemStack(com.hbm.items.ModArmorItems.jetpack_glider.get());
        com.hbm.items.gear.JetpackGlider jetpack = (com.hbm.items.gear.JetpackGlider) glider.getItem();
        jetpack.tryFill(com.hbm.inventory.fluid.Fluids.KEROSENE, 20_000, glider);
        ItemStack chest = new ItemStack(com.hbm.items.ModArmorItems.t51_plate.get());
        ArmorModHandler.applyMod(chest, glider);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setOnGround(false);
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        handleKeybind(player, EnumKeybind.JETPACK, true);
        handleKeybind(player, EnumKeybind.JETPACK, false);
        if (!"glide".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"))) toggleHoverState(player);
        // Optional visual fixture variants still share this session's bounded equipment restoration.
        String gear = System.getenv("HBM_JETPACK_CHECK_GEAR");
        if ("dns".equals(gear) || "bj".equals(gear) || "crouch".equals(gear)) {
            String armor = "crouch".equals(gear) ? "bj" : gear;
            String[] suffixes = {"helmet", "plate", "legs", "boots"};
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (int i = 0; i < slots.length; i++) {
                String name = armor + "_" + suffixes[i];
                if ("bj".equals(armor) && i == 1) name = "bj_plate_jetpack";
                player.setItemSlot(slots[i], new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                        dev.golem.core.Ids.of("hbm", name))));
            }
            if ("crouch".equals(gear)) {
                BlockPos platform = findEmptySky(level, player, 0);
                if (platform == null) {
                    stopSmoke(player);
                    throw new IllegalStateException("Crouch visual fixture could not find an empty sky platform");
                }
                for (BlockPos position : BlockPos.betweenClosed(platform.offset(-1, 0, -1), platform.offset(1, 0, 1))) {
                    var platformState = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
                    level.setBlock(position, platformState, 3);
                    session.temporaryBlocks().put(position, platformState);
                }
                player.connection.teleport(platform.getX() + 0.5D, platform.getY() + 1D,
                        platform.getZ() + 0.5D, 0F, 10F);
                player.setNoGravity(false);
                player.setOnGround(true);
                player.setShiftKeyDown(true);
            }
        } else if ("flight_crouch".equals(gear)) {
            BlockPos position = findEmptySky(level, player, 2);
            if (position == null) {
                stopSmoke(player);
                throw new IllegalStateException("Flight crouch fixture could not find an empty sky volume");
            }
            player.connection.teleport(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0F, 0F);
        } else if ("regular".equals(gear)) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(com.hbm.items.ModArmorItems.jetpack_fly.get()));
        } else if ("murk".equals(gear) || "limp".equals(gear)) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack("murk".equals(gear)
                    ? com.hbm.items.ModArmorItems.wings_murk.get() : com.hbm.items.ModArmorItems.wings_limp.get()));
        } else if ("bomb".equals(gear)) {
            player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            BlockPos position = player.blockPosition().offset(0, -1, 3);
            if (level.getBlockState(position).isAir()) {
                var bomb = com.hbm.blocks.bomb.ModBombBlocks.nuke_man.get().defaultBlockState()
                        .setValue(com.hbm.blocks.bomb.NukeMan.FACING, 2);
                level.setBlock(position, bomb, 3);
                session.temporaryBlocks().put(position, bomb);
                com.hbm.Constants.LOG.info("Bomb visual fixture at {} (player {}), block entity {}",
                        position, player.position(), level.getBlockEntity(position));
            } else {
                com.hbm.Constants.LOG.warn("Bomb visual fixture skipped occupied position {}", position);
            }
        } else if ("water".equals(gear) || "occlusion".equals(gear)) {
            BlockPos center = new BlockPos(player.getBlockX(), 240, player.getBlockZ());
            if (level.isOutsideBuildHeight(center)) {
                stopSmoke(player);
                throw new IllegalStateException("Water visual fixture requires overworld build height");
            }
            for (BlockPos position : BlockPos.betweenClosed(center.offset(-7, -1, -7), center.offset(7, 0, 7))) {
                if (!level.getBlockState(position).isAir()) {
                    stopSmoke(player);
                    throw new IllegalStateException("Water visual fixture requires an empty 15x2x15 volume");
                }
            }
            // A contained, source-only basin: no flowing water escapes the owned fixture positions.
            for (int y = -1; y <= 0; y++) for (int x = -7; x <= 7; x++) for (int z = -7; z <= 7; z++) {
                BlockPos position = center.offset(x, y, z);
                var state = y == -1 || Math.abs(x) == 7 || Math.abs(z) == 7
                        ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
                if (!level.setBlock(position, state, 3)) {
                    stopSmoke(player);
                    throw new IllegalStateException("Failed to place water visual fixture at " + position);
                }
                session.temporaryBlocks().put(position, state);
            }
            player.connection.teleport(center.getX() + 0.5D, center.getY() + 2D, center.getZ() + 0.5D,
                    0F, "occlusion".equals(gear) ? 0F : 60F);
            com.hbm.Constants.LOG.info("Water visual fixture: {} contained source blocks at {} beneath player", 13 * 13, center);
        }
    }

    private static BlockPos findEmptySky(ServerLevel level, ServerPlayer player, int verticalRadius) {
        for (int attempt = 1; attempt <= 16; attempt++) {
            BlockPos candidate = new BlockPos(player.getBlockX() + attempt * 64, 240, player.getBlockZ());
            boolean empty = true;
            for (BlockPos position : BlockPos.betweenClosed(candidate.offset(-1, -verticalRadius, -1),
                    candidate.offset(1, verticalRadius, 1))) {
                if (!level.getBlockState(position).isAir()) {
                    empty = false;
                    break;
                }
            }
            if (empty) return candidate;
        }
        return null;
    }

    /** Stops the development smoke fixture and restores only the state owned by its start command. */
    public static boolean stopSmoke(ServerPlayer player) {
        if (player == null) return false;
        SmokeSession session = SMOKE_SESSIONS.remove(player.getUUID());
        if (session == null) return false;
        // Drain the basin before removing its walls/floor so no scheduled flow escapes during cleanup.
        session.temporaryBlocks().forEach((position, state) -> {
            if (state.is(net.minecraft.world.level.block.Blocks.WATER)
                    && session.level().getBlockState(position).equals(state)) session.level().setBlock(position,
                            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        });
        session.temporaryBlocks().forEach((position, state) -> {
            if (session.level().getBlockState(position).equals(state)) session.level().removeBlock(position, false);
        });
        if (!session.temporaryBlocks().isEmpty()) {
            long remaining = session.temporaryBlocks().keySet().stream()
                    .filter(position -> !session.level().getBlockState(position).isAir()).count();
            com.hbm.Constants.LOG.info("Visual fixture cleanup: {} positions, {} non-air remaining",
                    session.temporaryBlocks().size(), remaining);
        }
        if (!player.isRemoved()) {
            if (session.originalPosition() != null && player.level() == session.level()) {
                Vec3 original = session.originalPosition();
                player.connection.teleport(original.x, original.y, original.z, player.getYRot(), player.getXRot());
            }
            session.originalEquipment().forEach((slot, stack) -> player.setItemSlot(slot, stack.copy()));
            player.setNoGravity(session.originalNoGravity());
            player.setOnGround(session.originalOnGround());
            player.setDeltaMovement(session.originalMotion());
            player.setShiftKeyDown(session.originalSneaking());
        }
        STATES.keySet().removeIf(key -> !key.client() && key.uuid().equals(player.getUUID()));
        return true;
    }

    /** Client-side source identity: equivalent network snapshots of one item are not equipment swaps. */
    public static boolean isEquivalentClientSource(net.minecraft.world.item.ItemStack previous,
                                                   net.minecraft.world.item.ItemStack current) {
        if (previous == null || current == null || previous.isEmpty() || current.isEmpty()) {
            return previous != null && current != null && previous.isEmpty() && current.isEmpty();
        }
        return previous.getItem() == current.getItem();
    }

    private static StateKey stateKey(Player player) {
        return new StateKey(player.level().isClientSide(), player.getUUID(), player.level());
    }

    private static final class StateKey {
        private final boolean client;
        private final UUID uuid;
        private final Level level;

        private StateKey(boolean client, UUID uuid, Level level) {
            this.client = client;
            this.uuid = uuid;
            this.level = level;
        }

        private boolean client() { return client; }
        private UUID uuid() { return uuid; }
        private Level level() { return level; }

        @Override
        public boolean equals(Object object) {
            return object instanceof StateKey other && client == other.client
                    && uuid.equals(other.uuid) && level == other.level;
        }

        @Override
        public int hashCode() {
            int result = Boolean.hashCode(client);
            result = 31 * result + uuid.hashCode();
            return 31 * result + System.identityHashCode(level);
        }
    }

    private record SmokeSession(ServerPlayer player, ServerLevel level, Map<EquipmentSlot, ItemStack> originalEquipment,
                                Map<BlockPos, net.minecraft.world.level.block.state.BlockState> temporaryBlocks,
                                 boolean originalNoGravity, boolean originalOnGround, Vec3 originalMotion, boolean originalSneaking,
                                 Vec3 originalPosition,
                                long expiresAt) {}

    private static JetpackInfo stateFromStack(Player player) {
        JetpackInfo state = new JetpackInfo();
        state.backpackEnabled = HbmLivingProps.getEnableBackpack(player);
        state.hudEnabled = HbmLivingProps.getEnableHUD(player);
        ItemStackAccess glider = findGlider(player);
        if (glider.stack() != null && ModDataComponents.JETPACK_STATE.has(glider.stack())) {
            CompoundTag tag = ModDataComponents.JETPACK_STATE.getOr(glider.stack(), new CompoundTag());
            state.readFromTag(tag);
        }
        return state;
    }

    private static void refreshSource(Player player, JetpackInfo state) {
        ItemStackAccess source = findJetpack(player);
        net.minecraft.world.item.ItemStack container = source.stack() == null ? null
                : source.outer() == null ? source.stack() : source.outer();
        net.minecraft.world.item.Item item = source.stack() == null ? null : source.stack().getItem();
        boolean sameSource = player.level().isClientSide()
                ? sameClientSource(state.sourceItem, item)
                : state.sourceContainer == container && state.sourceItem == item;
        if (sameSource) return;

        state.sourceContainer = player.level().isClientSide() ? null : container;
        state.sourceItem = item;
        state.resetFlightState();
        if (source.stack() != null && item instanceof JetpackGlider
                && ModDataComponents.JETPACK_STATE.has(source.stack())) {
            state.readFromTag(ModDataComponents.JETPACK_STATE.getOr(source.stack(), new CompoundTag()));
        }
    }

    /** Whether any direct or armor-module jetpack is worn in the chest slot. */
    public static boolean hasJetpack(Player player) {
        return findJetpack(player).stack() != null;
    }

    public static boolean hasGlider(Player player) {
        return findGlider(player).stack() != null;
    }

    private static boolean hasPoweredFlight(Player player) {
        Object item = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        return item instanceof ArmorBJJetpack || item instanceof ArmorDNT;
    }

    private static boolean hasFlightGear(Player player) {
        return hasJetpack(player) || hasPoweredFlight(player);
    }

    /** General C-toggle state used by ordinary jetpacks, wings and powered armor. */
    public static boolean isBackpackEnabled(Player player) {
        JetpackInfo state = getState(player);
        return state != null && state.backpackEnabled;
    }

    /** General V-toggle state used by the HUD; the glider's U mode is separate. */
    public static boolean isHudEnabled(Player player) {
        JetpackInfo state = getState(player);
        return state != null && state.hudEnabled;
    }

    /** The old J activation state for ordinary gear and powered suits. Gliders use their delayed opening state. */
    public static boolean isJetpackActive(Player player) {
        JetpackInfo state = getState(player);
        if (state == null) return false;
        // The advanced glider has its own J/H/U controller and deliberately ignores the general C toggle.
        if (hasGlider(player)) return state.opening && System.currentTimeMillis() - state.animTime > 1000L;
        if (!state.backpackEnabled) return false;
        return state.jetpackKeyDown;
    }

    public static boolean jetpackActive(Player player) {
        return isJetpackActive(player);
    }

    public static boolean isHovering(Player player) {
        JetpackInfo state = getState(player);
        return state != null && state.hover;
    }

    public static boolean isOpening(Player player) {
        JetpackInfo state = getState(player);
        return state != null && state.opening;
    }

    public static boolean useCompactHUD(Player player) {
        JetpackInfo state = getState(player);
        return state != null && state.useCompactHUD;
    }

    public static long getAnimTime(Player player) {
        JetpackInfo state = getState(player);
        return state == null ? 0L : state.animTime;
    }

    public static int getFuel(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.getItem() instanceof JetpackFueledBase) return JetpackFueledBase.getFuel(stack);
        if (stack.getItem() instanceof JetpackGlider glider) return glider.getFill(stack);
        return 0;
    }

    public static int getFuel(Player player) {
        ItemStackAccess jetpack = findJetpack(player);
        return jetpack.stack() == null ? 0 : getFuel(jetpack.stack());
    }

    public static int getMaxFuel(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.getItem() instanceof JetpackFueledBase jetpack) return jetpack.maxFuel;
        if (stack.getItem() instanceof JetpackGlider glider) return glider.capacity;
        return 0;
    }

    public static int getMaxFuel(Player player) {
        ItemStackAccess jetpack = findJetpack(player);
        return jetpack.stack() == null ? 0 : getMaxFuel(jetpack.stack());
    }

    public static com.hbm.inventory.fluid.FluidType getFuelType(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (stack.getItem() instanceof JetpackFueledBase jetpack) return jetpack.fuel;
        if (stack.getItem() instanceof JetpackGlider glider) return glider.getFirstFluidType(stack);
        return null;
    }

    public static com.hbm.inventory.fluid.FluidType getFuelType(Player player) {
        ItemStackAccess jetpack = findJetpack(player);
        return jetpack.stack() == null ? null : getFuelType(jetpack.stack());
    }

    public static com.hbm.inventory.fluid.tank.FluidTankNTM getTank(Player player) {
        ItemStackAccess glider = findGlider(player);
        return glider.stack() == null ? new com.hbm.inventory.fluid.tank.FluidTankNTM(
                com.hbm.inventory.fluid.Fluids.NONE, 0) : ((JetpackGlider) glider.stack().getItem()).getTank(glider.stack());
    }

    public static void setTank(Player player, com.hbm.inventory.fluid.tank.FluidTankNTM tank) {
        ItemStackAccess glider = findGlider(player);
        if (glider.stack() == null) return;
        ((JetpackGlider) glider.stack().getItem()).setTank(glider.stack(), tank);
        if (glider.outer() != null) ArmorModHandler.applyMod(glider.outer(), glider.stack());
    }

    public static float getSpeed(com.hbm.inventory.fluid.FluidType type) {
        if (type == null) return 0F;
        if (type == com.hbm.inventory.fluid.Fluids.KEROSENE) return 0.3F;
        if (type == com.hbm.inventory.fluid.Fluids.KEROSENE_REFORM) return 0.4F;
        if (type == com.hbm.inventory.fluid.Fluids.NITAN) return 0.5F;
        if (type == com.hbm.inventory.fluid.Fluids.BALEFIRE) return 1.5F;
        return 0F;
    }

    /** Drain is intentionally one for every supported fuel; thrust provides the scaling. */
    public static int getDrain(com.hbm.inventory.fluid.FluidType type) {
        return type == com.hbm.inventory.fluid.Fluids.KEROSENE
                || type == com.hbm.inventory.fluid.Fluids.KEROSENE_REFORM
                || type == com.hbm.inventory.fluid.Fluids.NITAN
                || type == com.hbm.inventory.fluid.Fluids.BALEFIRE ? 1 : 0;
    }

    /** Resets the server's vanilla floating check through Core's loader/access seam. */
    public static void resetFlightTime(Player player) {
        if (player instanceof ServerPlayer serverPlayer) Movement.resetFloatingTickCount(serverPlayer);
    }

    /** Handles both the server's authoritative key edge and the client's local prediction. */
    public static void handleKeybind(Player player, HbmKeybinds.EnumKeybind keybind, boolean state) {
        if (player == null || keybind == null) return;
        JetpackInfo info = getState(player);
        if (info == null) return;

        switch (keybind) {
            case JETPACK -> {
                if (state && !info.jetpackKeyDown && hasGlider(player)
                        && System.currentTimeMillis() - info.animTime > 1000L) toggleOpenState(player);
                info.jetpackKeyDown = state;
            }
            case JETPACK_HOVER -> {
                if (state && !info.hoverKeyDown && hasGlider(player)) toggleHoverState(player);
                info.hoverKeyDown = state;
            }
            case JETPACK_HUD -> {
                if (state && !info.hudKeyDown && hasGlider(player)) toggleHUDState(player);
                info.hudKeyDown = state;
            }
            case TOGGLE_JETPACK -> {
                if (state && !info.backpackToggleKeyDown) {
                    info.backpackEnabled = !info.backpackEnabled;
                    HbmLivingProps.setEnableBackpack(player, info.backpackEnabled);
                }
                info.backpackToggleKeyDown = state;
                info.dirty = true;
            }
            case TOGGLE_HEAD -> {
                if (state && !info.headToggleKeyDown) {
                    info.hudEnabled = !info.hudEnabled;
                    HbmLivingProps.setEnableHUD(player, info.hudEnabled);
                }
                info.headToggleKeyDown = state;
                info.dirty = true;
            }
            default -> { return; }
        }
        info.dirty = true;
        if (player instanceof ServerPlayer serverPlayer) sendState(serverPlayer, info);
    }

    public static void toggleOpenState(Player player) {
        JetpackInfo info = getState(player);
        if (info == null) return;
        info.animTime = System.currentTimeMillis();
        info.opening = !info.opening;
        info.jetpackFlyTime = info.opening ? 0 : -1;
        info.dirty = true;
    }

    public static void toggleHoverState(Player player) {
        JetpackInfo info = getState(player);
        if (info == null) return;
        info.hover = !info.hover;
        if (!info.hover && info.opening) info.jetpackFlyTime = 0;
        info.dirty = true;
    }

    public static void toggleHUDState(Player player) {
        JetpackInfo info = getState(player);
        if (info == null) return;
        info.useCompactHUD = !info.useCompactHUD;
        info.dirty = true;
    }

    public static void setThrust(Player player, float thrust) {
        JetpackInfo info = getState(player);
        if (info != null && info.thrust != thrust) {
            info.thrust = thrust;
            info.dirty = true;
        }
    }

    /** Supplies the client-side movement input without importing a client movement-input class into common code. */
    public static void setInput(Player player, float forward, float strafe, boolean jump,
                                boolean sneak, boolean sprint) {
        JetpackInputPayload input = new JetpackInputPayload(forward, strafe, jump, sneak, sprint);
        JetpackInfo info = getState(player);
        if (info == null) return;
        info.forward = input.forward();
        info.strafe = input.strafe();
        info.jump = input.jump();
        info.sneak = input.sneak();
        info.sprint = input.sprint();
    }

    /** Client seam: applies local prediction and sends the bounded input to the authenticated server player. */
    public static void sendInput(Player player, float forward, float strafe, boolean jump,
                                 boolean sneak, boolean sprint) {
        if (player == null) return;
        JetpackInputPayload input = new JetpackInputPayload(forward, strafe, jump, sneak, sprint);
        setInput(player, input.forward(), input.strafe(), input.jump(), input.sneak(), input.sprint());
        if (player.level().isClientSide()) GolemNetwork.sendToServer(JetpackInputPayload.CODEC, input);
    }

    /** Server receiver; the transport context, never a payload field, supplies the authenticated player. */
    public static void handleInput(ServerPlayer player, JetpackInputPayload input) {
        if (player == null || input == null) return;
        setInput(player, input.forward(), input.strafe(), input.jump(), input.sneak(), input.sprint());
        JetpackInfo info = getState(player);
        if (info != null) {
            info.inputLastTick = player.level().getGameTime();
            info.inputReceived = true;
        }
    }

    public static float getForwardInput(Player player) {
        JetpackInfo info = getState(player);
        if (info != null) expireInput(player, info);
        return info == null ? 0F : info.forward;
    }

    public static float getStrafeInput(Player player) {
        JetpackInfo info = getState(player);
        if (info != null) expireInput(player, info);
        return info == null ? 0F : info.strafe;
    }

    /** Applies the advanced glider formula with the last input supplied by the client seam. */
    public static void tickGlider(Player player) {
        JetpackInfo info = getState(player);
        if (info == null) return;
        expireInput(player, info);
        tickGlider(player, info.forward, info.strafe, info.jump, info.sneak, info.sprint);
    }

    /** Applies the advanced glider formula from a loader/client input event. */
    public static void tickGlider(Player player, float forward, float strafe, boolean jump,
                                  boolean sneak, boolean sprint) {
        JetpackInfo info = getState(player);
        ItemStackAccess found = findGlider(player);
        if (info == null || found.stack() == null) return;
        info.forward = forward;
        info.strafe = strafe;
        info.jump = jump;
        info.sneak = sneak;
        info.sprint = sprint;

        info.prevThrust = info.thrust;
        info.prevJetpackFlyTime = info.jetpackFlyTime;
        boolean active = isJetpackActive(player);
        if (active && player.isInWater()) info.failureTicks = 80;
        else if (info.failureTicks > 0) info.failureTicks--;

        if (active && !player.onGround() && info.failureTicks <= 0 && getFuel(found.stack()) > 0) {
            player.getAbilities().flying = false;
            float speed = getSpeed(getFuelType(found.stack()));
            Vec3 motion = player.getDeltaMovement();
            if (info.hover) {
                float moveForward = forward * (sprint ? 0.17F : 0.1F) * speed;
                float moveStrafe = strafe * 0.1F * speed;
                float yaw = (float) Math.toRadians(player.getYHeadRot());
                motion = motion.add(-Math.sin(yaw) * moveForward, 0, Math.cos(yaw) * moveForward);
                float sideYaw = (float) Math.toRadians(player.getYHeadRot() - 90F);
                motion = motion.add(-Math.sin(sideYaw) * moveStrafe, 0, Math.cos(sideYaw) * moveStrafe);
                double y = motion.y * 0.75D + 0.05D;
                float extraY = 0F;
                if (jump) extraY += 0.3F * speed;
                if (sneak) extraY -= Math.min(0.3F, 0.3F * speed);
                motion = new Vec3(motion.x, y + extraY, motion.z);
                float desired = (Math.abs(moveForward) + Math.abs(moveStrafe) + extraY + 0.4F);
                setThrust(player, info.thrust + (desired - info.thrust) * 0.3F);
                player.fallDistance = 0;
            } else {
                float boost = jump ? speed : 0F;
                float desired = Math.max(0F, Math.min(2F, boost * 2F));
                setThrust(player, info.thrust + (desired - info.thrust) * 0.2F);
                Vec3 look = player.getViewVector(1.0F);
                if (jump) {
                    motion = motion.add(look.scale(boost * 0.5F));
                    if (look.y > 0) motion = motion.add(0, look.y * 0.08F, 0);
                }
                if (motion.y < -0.08D) {
                    double amount = motion.y * -0.4D;
                    motion = motion.add(look.x * amount, amount * (1D - Math.abs(look.y))
                            + look.y * amount * (1D - Math.abs(look.y)), look.z * amount);
                }
            }
            player.setDeltaMovement(motion);
            // Opening, landing and hover reset this timer; every new glide must start it again.
            info.jetpackFlyTime = info.hover ? -1 : Math.max(0, info.jetpackFlyTime) + 1;
        } else {
            setThrust(player, info.thrust + (-info.thrust) * 0.3F);
            info.prevJetpackFlyTime = info.jetpackFlyTime;
            info.jetpackFlyTime = -1;
        }
        info.dirty = true;
    }

    /** Server tick: drain glider fuel, persist its state and replicate state to tracking clients. */
    public static void serverTick(ServerPlayer player) {
        // Bob, the_NCR and Pheo fly on their own wings (SpecialPlayers) and keep the C/jump state without gear
        if (player == null || player.isRemoved() || !hasFlightGear(player) && !SpecialPlayers.hasInnateFlight(player)) {
            remove(player);
            return;
        }

        JetpackInfo info = getState(player);
        info.backpackEnabled = HbmLivingProps.getEnableBackpack(player);
        info.hudEnabled = HbmLivingProps.getEnableHUD(player);
        expireInput(player, info);
        ItemStackAccess glider = findGlider(player);
        if (glider.stack() != null && isJetpackActive(player) && getFuel(glider.stack()) > 0) {
            int drain = (int) Math.ceil(getDrain(getFuelType(glider.stack())) * info.thrust);
            if (info.thrust < 0.0001F) drain = 0;
            if (drain > 0) drainGlider(glider, drain);
        }
        if (hasFlightGear(player) && player.getDeltaMovement().y > -0.5D) player.fallDistance = 0;
        if (glider.stack() != null) {
            if (saveStateToStack(glider.stack(), info) && glider.outer() != null) {
                ArmorModHandler.applyMod(glider.outer(), glider.stack());
            }
        }
        sendState(player, info);
    }

    private static void expireInput(Player player, JetpackInfo info) {
        if (!player.level().isClientSide()
                && info.inputReceived
                && player.level().getGameTime() - info.inputLastTick > INPUT_TIMEOUT_TICKS) info.clearInput();
    }

    private static void drainGlider(ItemStackAccess found, int amount) {
        JetpackGlider glider = (JetpackGlider) found.stack().getItem();
        com.hbm.inventory.fluid.FluidType type = glider.getFirstFluidType(found.stack());
        int drained = glider.tryEmpty(type, amount, found.stack());
        if (drained > 0 && found.outer() != null) ArmorModHandler.applyMod(found.outer(), found.stack());
    }

    private static boolean saveStateToStack(net.minecraft.world.item.ItemStack stack, JetpackInfo info) {
        CompoundTag next = info.writeToTag(new CompoundTag());
        if (ModDataComponents.JETPACK_STATE.has(stack)
                && ModDataComponents.JETPACK_STATE.getOr(stack, new CompoundTag()).equals(next)) return false;
        ModDataComponents.JETPACK_STATE.set(stack, next);
        return true;
    }

    private static void sendState(ServerPlayer player, JetpackInfo info) {
        JetpackSyncPayload payload = info.toPayload(player.getId());
        GolemNetwork.sendToPlayer(player, JetpackSyncPayload.CODEC, payload);
        if (player.level() instanceof ServerLevel level) {
            GolemNetwork.sendToTracking(level, BlockPos.containing(player.getX(), player.getY(), player.getZ()),
                    JetpackSyncPayload.CODEC, payload);
        }
        info.dirty = false;
    }

    /** Sends the remote plume cue; the client installs its renderer through the callback below. */
    public static void sendExhaust(Player player, int mode) {
        if (!(player instanceof ServerPlayer) || !(player.level() instanceof ServerLevel level)) return;
        GolemNetwork.sendToTracking(level, BlockPos.containing(player.getX(), player.getY(), player.getZ()),
                JetpackExhaustPayload.CODEC, new JetpackExhaustPayload(player.getId(), mode));
    }

    public static void setClientSyncCallback(BiConsumer<Level, JetpackSyncPayload> callback) {
        clientSyncCallback = callback;
    }

    public static void setClientExhaustCallback(BiConsumer<Level, JetpackExhaustPayload> callback) {
        clientExhaustCallback = callback;
    }

    /** Common receiver: updates state first, then gives the client-owned visual layer its callback. */
    public static void dispatchClientSync(Level level, JetpackSyncPayload payload) {
        if (level != null) {
            Entity entity = level.getEntity(payload.playerId());
            if (entity instanceof Player player) {
                JetpackInfo state = getState(player);
                state.setFromServer(payload);
                HbmLivingProps.setEnableBackpack(player, payload.backpackEnabled());
                HbmLivingProps.setEnableHUD(player, payload.hudEnabled());
            }
        }
        BiConsumer<Level, JetpackSyncPayload> callback = clientSyncCallback;
        if (callback != null) callback.accept(level, payload);
    }

    public static void dispatchClientExhaust(Level level, JetpackExhaustPayload payload) {
        BiConsumer<Level, JetpackExhaustPayload> callback = clientExhaustCallback;
        if (callback != null) callback.accept(level, payload);
    }

    private static ItemStackAccess findJetpack(Player player) {
        net.minecraft.world.item.ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof JetpackBase) return new ItemStackAccess(chest, null);
        net.minecraft.world.item.ItemStack module = ArmorModHandler.pryMod(chest, ArmorModHandler.plate_only);
        return module.getItem() instanceof JetpackBase || module.getItem() instanceof JetpackGlider
                ? new ItemStackAccess(module, chest) : new ItemStackAccess(null, null);
    }

    private static boolean sameClientSource(net.minecraft.world.item.Item previous,
                                            net.minecraft.world.item.Item current) {
        return previous == current;
    }

    private static ItemStackAccess findGlider(Player player) {
        ItemStackAccess jetpack = findJetpack(player);
        return jetpack.stack() != null && jetpack.stack().getItem() instanceof JetpackGlider ? jetpack : new ItemStackAccess(null, null);
    }

    private record ItemStackAccess(net.minecraft.world.item.ItemStack stack,
                                   net.minecraft.world.item.ItemStack outer) {}

    public static final class JetpackInfo {
        public boolean opening;
        public boolean hover;
        public float thrust;
        public float prevThrust;
        public int jetpackFlyTime;
        public int prevJetpackFlyTime;
        public int failureTicks;
        public boolean useCompactHUD;
        public long animTime;
        public boolean dirty = true;
        public boolean backpackEnabled = true;
        public boolean hudEnabled = true;

        private boolean jetpackKeyDown;
        private boolean hoverKeyDown;
        private boolean hudKeyDown;
        private boolean backpackToggleKeyDown;
        private boolean headToggleKeyDown;
        private float forward;
        private float strafe;
        private boolean jump;
        private boolean sneak;
        private boolean sprint;
        private ItemStack sourceContainer;
        private net.minecraft.world.item.Item sourceItem;
        private long inputLastTick = Long.MIN_VALUE;
        private boolean inputReceived;

        public JetpackInfo() {}

        /** Compatibility constructor; client-only particles live behind the callback seam now. */
        public JetpackInfo(boolean client) {}

        private void resetFlightState() {
            opening = false;
            hover = false;
            thrust = 0F;
            prevThrust = 0F;
            jetpackFlyTime = 0;
            prevJetpackFlyTime = 0;
            failureTicks = 0;
            useCompactHUD = false;
            animTime = 0L;
            dirty = true;
        }

        private void clearInput() {
            forward = 0F;
            strafe = 0F;
            jump = false;
            sneak = false;
            sprint = false;
            inputReceived = false;
            inputLastTick = Long.MIN_VALUE;
        }

        public CompoundTag writeToTag(CompoundTag tag) {
            tag.putBoolean("opening", opening);
            tag.putBoolean("hover", hover);
            tag.putFloat("thrust", thrust);
            tag.putInt("flyTime", jetpackFlyTime);
            tag.putBoolean("compact", useCompactHUD);
            tag.putInt("failureTicks", failureTicks);
            return tag;
        }

        public CompoundTag writeToNBT(CompoundTag tag) {
            return writeToTag(tag);
        }

        public void readFromTag(CompoundTag tag) {
            opening = Tags.getBooleanOr(tag, "opening", false);
            hover = Tags.getBooleanOr(tag, "hover", false);
            thrust = Tags.getFloatOr(tag, "thrust", 0F);
            jetpackFlyTime = Tags.getIntOr(tag, "flyTime", 0);
            useCompactHUD = Tags.getBooleanOr(tag, "compact", false);
            failureTicks = Tags.getIntOr(tag, "failureTicks", 0);
            dirty = true;
        }

        public void readFromNBT(CompoundTag tag) {
            readFromTag(tag);
        }

        public JetpackSyncPayload toPayload(int playerId) {
            return new JetpackSyncPayload(playerId, opening, hover, thrust, jetpackFlyTime,
                    useCompactHUD, failureTicks, backpackEnabled, hudEnabled);
        }

        public void setFromServer(JetpackSyncPayload payload) {
            if (opening != payload.opening()) animTime = System.currentTimeMillis();
            prevThrust = thrust;
            prevJetpackFlyTime = jetpackFlyTime;
            opening = payload.opening();
            hover = payload.hover();
            thrust = payload.thrust();
            jetpackFlyTime = payload.jetpackFlyTime();
            useCompactHUD = payload.useCompactHUD();
            failureTicks = payload.failureTicks();
            backpackEnabled = payload.backpackEnabled();
            hudEnabled = payload.hudEnabled();
        }
    }
}
