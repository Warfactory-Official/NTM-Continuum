package com.hbm.client.gun;

import com.hbm.Constants;
import com.hbm.client.HbmKeybindsClient;
import com.hbm.client.render.weapon.WeaponRenderers;
import com.hbm.inventory.ModGunSupportMenus;
import com.hbm.inventory.container.ContainerWeaponTable;
import com.hbm.inventory.container.MenuItemBox;
import com.hbm.blocks.machine.ModWeaponTableBlocks;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModGunSupportItems;
import com.hbm.items.ModWeaponModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumModTest;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.ContainerClicks;
import dev.golem.core.client.MouseInput;
import dev.golem.core.data.Inventories;
import dev.golem.core.world.Compat;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Development-only driver for a real joined-world gun render smoke.
 *
 * <p>The fixture presses the same mappings as a player. The server therefore owns reload, firing,
 * animation packets, ammunition consumption and projectile creation; this class only waits for those
 * results before taking a capture.
 */
public final class GunSmokeClient {

    private static final boolean ENABLED = Boolean.getBoolean("hbm.gunSmokeTest");
    private static final int TIMEOUT_TICKS = 20 * 60;
    private static final int FIRING_CAPTURE_FRAMES = 6;
    private static final List<Case> CASES = List.of(
            new Case("g3", "gun_g3", "ammo_standard_r556_sp"),
            new Case("uzi", "gun_uzi", "ammo_standard_p9_sp"),
            new Case("akimbo", "gun_uzi_akimbo", "ammo_standard_p9_sp"),
            new Case("shotgun", "gun_autoshotgun", "ammo_standard_g12"),
            new Case("laser", "gun_laser_pistol", "ammo_standard_capacitor"));

    private static boolean registered;
    private static boolean started;
    private static boolean restored;
    private static boolean shutdownPending;
    private static volatile boolean serverReady;
    private static volatile boolean serverMutationPending;
    private static volatile boolean restoreComplete;
    private static volatile boolean restoreQueued;
    private static volatile boolean restoreRequested;
    private static volatile @Nullable String serverFailure;
    private static int ticks;
    private static int stageTicks;
    private static int caseIndex;
    private static Stage stage = Stage.WAITING;
    private static long observedShot;
    private static int observedAmmo;
    private static int observedProjectileCount;
    private static Set<Integer> fixtureEntities = Set.of();
    private static final Set<UUID> baselineProjectileUuids = new HashSet<>();
    private static final Set<UUID> fixtureProjectileUuids = new HashSet<>();
    private static @Nullable String pendingCapture;
    private static int pendingCaptureFrames;
    private static int pendingCaptureIndex;
    private static @Nullable Minecraft client;
    private static @Nullable MinecraftServer server;
    private static @Nullable ServerPlayer serverPlayer;
    private static Inventories.@Nullable PlayerSnapshot originalInventory;
    private static @Nullable CameraType previousCamera;
    private static @Nullable Integer previousFov;
    private static @Nullable Double previousX;
    private static @Nullable Double previousY;
    private static @Nullable Double previousZ;
    private static @Nullable Float previousYaw;
    private static @Nullable Float previousPitch;
    private static @Nullable BlockPos tablePos;
    private static @Nullable BlockState tableOriginalState;
    private static boolean tablePlaced;

    private GunSmokeClient() {}

    public static void register() {
        if (!ENABLED || !dev.golem.core.platform.Platform.isDevelopment() || registered) return;
        registered = true;
        ClientTicks.onEndTick(GunSmokeClient::tick);
    }

    public static boolean enabled() {
        return ENABLED;
    }

    /** Called by the loader's logging-out hook so an in-flight fixture is restored before disconnect. */
    public static void onDisconnect() {
        if (ENABLED && started && !restored) beginRestore();
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        client = mc;
        if (shutdownPending) {
            mc.stop();
            return;
        }
        if (restored) return;
        if (++ticks > TIMEOUT_TICKS && stage != Stage.RESTORE_WAIT) {
            Constants.LOG.error("[GUN-SMOKE] timed out at case={} stage={}", caseIndex, stage);
            beginRestore();
            return;
        }
        if (stage == Stage.RESTORE_WAIT) {
            if (restoreComplete) {
                finishRestore();
            } else if (serverFailure != null) {
                Constants.LOG.error("[GUN-SMOKE] inventory restoration unavailable: {}", serverFailure);
                finishRestore();
            } else if (!serverMutationPending && !restoreQueued) {
                beginRestore();
            }
            return;
        }
        if (serverFailure != null) {
            Constants.LOG.error("[GUN-SMOKE] server fixture failed: {}", serverFailure);
            beginRestore();
            return;
        }
        if (mc.player == null || mc.level == null) {
            if (started) beginRestore();
            return;
        }
        if (mc.getOverlay() != null) return;

        if (!started) {
            saveState(mc);
            if (!logGunResources(mc)) {
                Constants.LOG.error("[GUN-SMOKE] refusing visual run because the 70-gun resource audit failed");
                finishRestore();
                return;
            }
            fixtureEntities = entityIds(mc);
            if (!startServerFixture(mc)) return;
            started = true;
            stage = Stage.BAG_USE;
            stageTicks = 0;
            return;
        }

        trackFixtureProjectiles(mc);

        if (runMenuFixture(mc)) return;

        if (mc.screen != null) return;

        if (stage == Stage.COMMANDS) {
            if (!serverReady || serverMutationPending) return;
            if (!selectingCase(mc)) return;
            ItemStack readyStack = mc.player.getMainHandItem();
            if (!(readyStack.getItem() instanceof ItemGunBaseNT)
                    || ItemGunBaseNT.getState(readyStack, 0) != ItemGunBaseNT.GunState.IDLE) return;
            stage = Stage.RELOAD_PRESS;
            stageTicks = 0;
            press(HbmKeybindsClient.reloadKey);
            return;
        }

        if (!selectingCase(mc)) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return;

        stageTicks++;
        switch (stage) {
            case RELOAD_PRESS -> {
                if (stageTicks >= 2) {
                    release(HbmKeybindsClient.reloadKey);
                    stage = Stage.RELOAD_WAIT;
                    stageTicks = 0;
                }
            }
            case RELOAD_WAIT -> {
                if (gun.getPrimaryMagazine(stack, 0) != null
                        && gun.getPrimaryMagazine(stack, 0).getAmount(stack, null) > 0
                        && ItemGunBaseNT.getState(stack, 0) == ItemGunBaseNT.GunState.IDLE) {
                    capture(mc, "rest");
                    stage = Stage.FIRE_PRESS;
                    stageTicks = 0;
                    observedShot = gun.lastShotByHand[net.minecraft.world.InteractionHand.MAIN_HAND.ordinal()][0];
                    observedAmmo = gun.getPrimaryMagazine(stack, 0).getAmount(stack, null);
                    observedProjectileCount = fixtureProjectileUuids.size();
                    press(mc.options.keyAttack);
                }
            }
            case FIRE_PRESS -> {
                int ammo = gun.getPrimaryMagazine(stack, 0) == null ? -1
                        : gun.getPrimaryMagazine(stack, 0).getAmount(stack, null);
                boolean ammoPositive = observedAmmo >= 0 && ammo >= 0 && ammo < observedAmmo;
                boolean entityPositive = fixtureProjectileUuids.size() > observedProjectileCount;
                long shot = gun.lastShotByHand[net.minecraft.world.InteractionHand.MAIN_HAND.ordinal()][0];
                if (shot != observedShot && (ammoPositive || entityPositive)) {
                    observedShot = shot;
                    Constants.LOG.info("[GUN-SMOKE] server fire confirmed case={} item={} shotStamp={} ammo={} evidence={}",
                            current().name, current().gun, observedShot,
                            ammo, ammoPositive && entityPositive ? "ammo+entity"
                                    : ammoPositive ? "ammo" : "entity");
                    captureBurst(mc, "firing", FIRING_CAPTURE_FRAMES);
                    release(mc.options.keyAttack);
                    stage = Stage.SMOKE_WAIT;
                    stageTicks = 0;
                } else if (stageTicks >= 20) {
                    Constants.LOG.error("[GUN-SMOKE] no server fire confirmation for case={} item={}; no screenshots claimed",
                            current().name, current().gun);
                    beginRestore();
                }
            }
            case SMOKE_WAIT -> {
                if (stageTicks >= 6) {
                    if (!gun.getConfig(stack, 0).getSmokeNodes(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty()) {
                        Constants.LOG.info("[GUN-SMOKE] smoke confirmed case={} nodes={}", current().name,
                                gun.getConfig(stack, 0).getSmokeNodes(net.minecraft.world.InteractionHand.MAIN_HAND).size());
                        capture(mc, "smoke");
                    } else {
                        Constants.LOG.info("[GUN-SMOKE] case={} has no source smoke layer; no smoke capture claimed",
                                current().name);
                    }
                    stage = Stage.ADS_PRESS;
                    stageTicks = 0;
                    if (supportsAiming(gun, stack)) {
                        press(HbmKeybindsClient.gunTertiaryKey);
                    } else {
                        Constants.LOG.info("[GUN-SMOKE] case={} has no source ADS handler; no ADS capture claimed",
                                current().name);
                        nextCase(mc);
                    }
                }
            }
            case ADS_PRESS -> {
                if (stageTicks >= 2) {
                    release(HbmKeybindsClient.gunTertiaryKey);
                    stage = Stage.ADS_WAIT;
                    stageTicks = 0;
                }
            }
            case ADS_WAIT -> {
                if (ItemGunBaseNT.getIsAiming(stack) && ItemGunBaseNT.aimingProgress >= 1F) {
                    capture(mc, "ads");
                    nextCase(mc);
                } else if (stageTicks >= 20) {
                    Constants.LOG.error("[GUN-SMOKE] no server ADS confirmation for case={}; no ADS screenshot claimed",
                            current().name);
                    nextCase(mc);
                }
            }
            default -> { }
        }
    }

    private static void saveState(Minecraft mc) {
        previousCamera = mc.options.getCameraType();
        previousFov = mc.options.fov().get();
        previousX = mc.player.getX();
        previousY = mc.player.getY();
        previousZ = mc.player.getZ();
        previousYaw = mc.player.getYRot();
        previousPitch = mc.player.getXRot();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.fov().set(90);
    }

    private static boolean startServerFixture(Minecraft mc) {
        MinecraftServer integrated = mc.getSingleplayerServer();
        if (integrated == null) {
            Constants.LOG.error("[GUN-SMOKE] aborting: this fixture requires an integrated singleplayer server");
            finishRestore();
            return false;
        }
        server = integrated;
        UUID playerId = mc.player.getUUID();
        queueServerMutation("snapshot and initialize", actual -> {
            serverPlayer = actual;
            originalInventory = Inventories.snapshot(actual);
            previousX = actual.getX();
            previousY = actual.getY();
            previousZ = actual.getZ();
            previousYaw = actual.getYRot();
            previousPitch = actual.getXRot();
            net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) actual.level();
            tablePos = findTablePosition(level, actual.blockPosition());
            tableOriginalState = tablePos == null ? null : level.getBlockState(tablePos);
            if (tablePos == null || tableOriginalState == null) {
                throw new IllegalStateException("no empty table fixture position near player");
            }
            for (Entity entity : level.getAllEntities()) {
                if (entity == null) continue;
                if (entity instanceof com.hbm.entity.projectile.EntityBulletBaseMK4
                        || entity instanceof com.hbm.entity.projectile.EntityBulletBeamBase)
                    baselineProjectileUuids.add(entity.getUUID());
            }
            Compat.setSelectedSlot(actual, 0);
            actual.getInventory().setItem(0, new ItemStack(ModGunSupportItems.ammo_bag.get()));
            actual.getInventory().setItem(1, ItemStack.EMPTY);
            if (restoreRequested) {
                if (!Inventories.restore(actual, originalInventory))
                    throw new IllegalStateException("inventory component/slot verification failed after early abort");
                syncInventory(actual);
                restoreComplete = true;
                Constants.LOG.info("[GUN-SMOKE] inventory restoration verified after early abort slots={} selected={}",
                        originalInventory.slots().size(), originalInventory.selectedSlot());
            } else {
                syncInventory(actual);
                serverReady = true;
                Constants.LOG.info("[GUN-SMOKE] inventory snapshot captured slots={} selected={}",
                        originalInventory.slots().size(), originalInventory.selectedSlot());
            }
        }, playerId);
        return true;
    }

    /** Finds an empty block position without replacing a block entity from the user's save. */
    private static @Nullable BlockPos findTablePosition(net.minecraft.server.level.ServerLevel level, BlockPos origin) {
        for (int radius = 2; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos candidate = origin.offset(dx, 0, dz);
                    if (level.getBlockState(candidate).isAir() && level.getBlockEntity(candidate) == null) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    /** Drives real client menu opening packets and menu click packets before the gun smoke cases. */
    private static boolean runMenuFixture(Minecraft mc) {
        switch (stage) {
            case BAG_USE -> {
                if (!serverReady || serverMutationPending || mc.screen != null) return true;
                if (++stageTicks == 1) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    stage = Stage.BAG_WAIT;
                    stageTicks = 0;
                }
                return true;
            }
            case BAG_WAIT -> {
                if (mc.screen != null && mc.player.containerMenu instanceof MenuItemBox menu
                        && menu.getType() == ModGunSupportMenus.AMMO_BAG.get()) {
                    capture(mc, "ammo_bag");
                    stage = Stage.BAG_CLOSE;
                    stageTicks = 0;
                } else if (++stageTicks > 40) {
                    failMenu("ammo bag did not open through the use-item packet");
                }
                return true;
            }
            case BAG_CLOSE -> {
                if (++stageTicks == 2 && mc.screen != null) mc.player.closeContainer();
                if (mc.screen == null && !serverMutationPending) {
                    queueServerMutation("place weapon table fixture", actual -> {
                        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) actual.level();
                        level.setBlock(tablePos, ModWeaponTableBlocks.MACHINE_WEAPON_TABLE.get().defaultBlockState(), 3);
                        tablePlaced = true;
                        actual.getInventory().setItem(0, ItemStack.EMPTY);
                        actual.getInventory().setItem(1, ItemStack.EMPTY);
                        syncInventory(actual);
                        Constants.LOG.info("[GUN-SMOKE] weapon table fixture placed pos={} original={}",
                                tablePos, tableOriginalState);
                    }, actualPlayerId());
                    stage = Stage.TABLE_SETUP_WAIT;
                    stageTicks = 0;
                }
                return true;
            }
            case TABLE_SETUP_WAIT -> {
                if (!serverMutationPending && tablePlaced) {
                    stage = Stage.TABLE_USE;
                    stageTicks = 0;
                } else if (++stageTicks > 80) {
                    failMenu("weapon table fixture placement did not complete");
                }
                return true;
            }
            case TABLE_USE -> {
                if (serverMutationPending || mc.screen != null || tablePos == null) return true;
                if (++stageTicks == 1) {
                    Vec3 hit = Vec3.atCenterOf(tablePos);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(hit, Direction.UP, tablePos, false));
                    stage = Stage.TABLE_WAIT;
                    stageTicks = 0;
                }
                return true;
            }
            case TABLE_WAIT -> {
                if (mc.screen != null && mc.player.containerMenu instanceof ContainerWeaponTable) {
                    stage = Stage.TABLE_POPULATE;
                    stageTicks = 0;
                } else if (++stageTicks > 40) {
                    failMenu("weapon table did not open through the use-block packet");
                }
                return true;
            }
            case TABLE_POPULATE -> {
                if (serverMutationPending) return true;
                queueServerMutation("populate weapon table UI fixture", actual -> {
                    actual.getInventory().setItem(0, new ItemStack(ModGunItems.gun_light_revolver_dani.get()));
                    actual.getInventory().setItem(1, ModWeaponModItems.weapon_mod_test.stack(EnumModTest.FIRERATE));
                    syncInventory(actual);
                }, actualPlayerId());
                stage = Stage.TABLE_POPULATE_WAIT;
                stageTicks = 0;
                return true;
            }
            case TABLE_POPULATE_WAIT -> {
                if (mc.screen != null && mc.player.containerMenu instanceof ContainerWeaponTable menu
                        && menu.getSlot(35).hasItem() && menu.getSlot(36).hasItem()) {
                    ContainerClicks.send(mc.gameMode, menu.containerId, 35, 0,
                            ContainerClicks.Action.PICKUP, mc.player);
                    ContainerClicks.send(mc.gameMode, menu.containerId, ContainerWeaponTable.GUN_SLOT, 0,
                            ContainerClicks.Action.PICKUP, mc.player);
                    stage = Stage.TABLE_GUN_WAIT;
                    stageTicks = 0;
                } else if (++stageTicks > 80) {
                    failMenu("weapon table UI fixture inventory did not synchronize");
                }
                return true;
            }
            case TABLE_GUN_WAIT -> {
                if (mc.player.containerMenu instanceof ContainerWeaponTable menu
                        && menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().is(ModGunItems.gun_light_revolver_dani.get())) {
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    stage = Stage.TABLE_CONFIG_WAIT;
                    stageTicks = 0;
                } else if (++stageTicks > 40) {
                    failMenu("weapon table gun pickup packet did not arrive");
                }
                return true;
            }
            case TABLE_CONFIG_WAIT -> {
                if (mc.player.containerMenu instanceof ContainerWeaponTable menu && menu.configIndex() == 1) {
                    ContainerClicks.send(mc.gameMode, menu.containerId, 36, 0,
                            ContainerClicks.Action.PICKUP, mc.player);
                    ContainerClicks.send(mc.gameMode, menu.containerId, 0, 0,
                            ContainerClicks.Action.PICKUP, mc.player);
                    stage = Stage.TABLE_MOD_WAIT;
                    stageTicks = 0;
                } else if (++stageTicks > 40) {
                    failMenu("weapon table configuration button packet did not synchronize");
                }
                return true;
            }
            case TABLE_MOD_WAIT -> {
                if (mc.player.containerMenu instanceof ContainerWeaponTable menu
                        && ModWeaponModItems.weapon_mod_test.typeOf(menu.getSlot(0).getItem()) != null) {
                    capture(mc, "weapon_table");
                    ContainerClicks.send(mc.gameMode, menu.containerId, ContainerWeaponTable.GUN_SLOT, 0,
                            ContainerClicks.Action.QUICK_MOVE, mc.player);
                    stage = Stage.TABLE_EXTRACT_WAIT;
                    stageTicks = 0;
                } else if (++stageTicks > 40) {
                    failMenu("weapon table modifier pickup packet did not arrive");
                }
                return true;
            }
            case TABLE_EXTRACT_WAIT -> {
                if (mc.player.containerMenu instanceof ContainerWeaponTable menu
                        && menu.getSlot(ContainerWeaponTable.GUN_SLOT).getItem().isEmpty()
                        && findConfiguredGun(mc.player) != null) {
                    queueServerMutation("verify weapon table outgoing gun", actual -> {
                        ItemStack outgoing = findConfiguredGun(actual);
                        if (outgoing == null || !XWeaponModManager.hasUpgrade(outgoing, 1, 0)) {
                            throw new IllegalStateException("outgoing gun lost config-1 FIRERATE modifier");
                        }
                        Constants.LOG.info("[GUN-SMOKE] weapon table UI verified outgoing gun preserves config=1 modifier=0");
                    }, actualPlayerId());
                    stage = Stage.TABLE_CLOSE;
                    stageTicks = 0;
                } else if (++stageTicks > 80) {
                    failMenu("weapon table quick-move did not preserve the configured gun");
                }
                return true;
            }
            case TABLE_CLOSE -> {
                if (++stageTicks == 2 && mc.screen != null) mc.player.closeContainer();
                if (mc.screen == null && !serverMutationPending) {
                    queueServerMutation("restore weapon table block", actual -> {
                        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) actual.level();
                        if (tablePlaced) {
                            level.setBlock(tablePos, tableOriginalState, 3);
                            tablePlaced = false;
                        }
                        if (!level.getBlockState(tablePos).equals(tableOriginalState)) {
                            throw new IllegalStateException("weapon table fixture block state did not restore");
                        }
                        Constants.LOG.info("[GUN-SMOKE] weapon table fixture restored pos={} state={}",
                                tablePos, tableOriginalState);
                    }, actualPlayerId());
                    stage = Stage.TABLE_RESTORE_WAIT;
                    stageTicks = 0;
                }
                return true;
            }
            case TABLE_RESTORE_WAIT -> {
                if (!serverMutationPending && !tablePlaced) {
                    stage = Stage.COMMANDS;
                    stageTicks = 0;
                } else if (++stageTicks > 80) {
                    failMenu("weapon table block restoration did not complete");
                }
                return true;
            }
            default -> { return false; }
        }
    }

    private static UUID actualPlayerId() {
        if (serverPlayer == null) throw new IllegalStateException("fixture player has not been captured");
        return serverPlayer.getUUID();
    }

    private static void failMenu(String message) {
        Constants.LOG.error("[GUN-SMOKE] {}", message);
        beginRestore();
    }

    private static @Nullable ItemStack findConfiguredGun(net.minecraft.world.entity.player.Player player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModGunItems.gun_light_revolver_dani.get())
                    && XWeaponModManager.hasUpgrade(stack, 1, 0)) return stack;
        }
        return null;
    }

    private static void queueServerMutation(String description, java.util.function.Consumer<ServerPlayer> action,
                                             UUID playerId) {
        if (server == null || serverMutationPending) return;
        serverMutationPending = true;
        try {
            server.execute(() -> {
                try {
                    ServerPlayer actual = server.getPlayerList().getPlayer(playerId);
                    if (actual == null) throw new IllegalStateException("singleplayer player is unavailable");
                    action.accept(actual);
                } catch (Throwable error) {
                    serverFailure = description + ": " + error;
                    if (description.startsWith("restore")) restoreQueued = false;
                } finally {
                    serverMutationPending = false;
                }
            });
        } catch (Throwable error) {
            serverMutationPending = false;
            serverFailure = description + ": could not queue server task: " + error;
            if (description.startsWith("restore")) restoreQueued = false;
        }
    }

    private static ItemStack fixtureStack(String path, int count) {
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, path));
        if (item == null) throw new IllegalStateException("missing fixture item hbm:" + path);
        return new ItemStack(item, count);
    }

    private static void syncInventory(ServerPlayer player) {
        player.inventoryMenu.sendAllDataToRemote();
        Compat.syncSelectedSlot(player);
    }

    private static boolean selectingCase(Minecraft mc) {
        Case current = current();
        String actual = BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).getPath();
        if (current.gun.equals(actual)) return true;
        if (stage == Stage.WAITING) return false;
        if (!serverMutationPending) {
            UUID playerId = mc.player.getUUID();
            queueServerMutation("select " + current.name, actualPlayer -> {
                Compat.setSelectedSlot(actualPlayer, 0);
                actualPlayer.getInventory().setItem(0, fixtureStack(current.gun, 1));
                actualPlayer.getInventory().setItem(35, fixtureStack(current.ammo, 64));
                actualPlayer.setXRot(-80F);
                syncInventory(actualPlayer);
            }, playerId);
        }
        return false;
    }

    private static void nextCase(Minecraft mc) {
        release(mc.options.keyAttack);
        release(HbmKeybindsClient.reloadKey);
        release(HbmKeybindsClient.gunTertiaryKey);
        caseIndex++;
        if (caseIndex >= CASES.size()) {
            beginRestore();
        } else {
            stage = Stage.COMMANDS;
        }
        stageTicks = 0;
    }

    private static void capture(Minecraft mc, String state) {
        // Tick callbacks run before the frame is rendered. Defer the grab to the HUD render callback so
        // the target contains the frame that consumed the real server fire timestamp.
        pendingCapture = current().name + "|" + state;
        pendingCaptureFrames = 1;
        pendingCaptureIndex = 0;
    }

    private static void captureBurst(Minecraft mc, String state, int frames) {
        // Capture consecutive rendered frames; the effect keeps its authored lifetime and is never held
        // open for the fixture.
        pendingCapture = current().name + "|" + state;
        pendingCaptureFrames = Math.max(1, frames);
        pendingCaptureIndex = 0;
    }

    private static void beginRestore() {
        if (restored || restoreQueued) return;
        restoreRequested = true;
        release(client == null ? null : client.options.keyAttack);
        release(HbmKeybindsClient.reloadKey);
        release(HbmKeybindsClient.gunTertiaryKey);
        if (originalInventory == null || server == null || serverPlayer == null) {
            Constants.LOG.info("[GUN-SMOKE] aborted before snapshot restore; no inventory mutation to restore");
            finishRestore();
            return;
        }
        stage = Stage.RESTORE_WAIT;
        stageTicks = 0;
        if (serverMutationPending) return;
        serverFailure = null;
        restoreQueued = true;
        UUID playerId = serverPlayer.getUUID();
        queueServerMutation("restore player inventory", actual -> {
            boolean verified = Inventories.restore(actual, originalInventory);
            if (!verified) throw new IllegalStateException("inventory component/slot verification failed");
            syncInventory(actual);
            int removed = 0;
            net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) actual.level();
            for (Entity entity : level.getAllEntities()) {
                if (entity == null) continue;
                if (fixtureProjectileUuids.contains(entity.getUUID())
                        || entity instanceof com.hbm.entity.projectile.EntityBulletBaseMK4 bullet
                        && bullet.thrower == actual && !baselineProjectileUuids.contains(entity.getUUID())
                        || entity instanceof com.hbm.entity.projectile.EntityBulletBeamBase beam
                        && beam.thrower == actual && !baselineProjectileUuids.contains(entity.getUUID())) {
                    entity.discard();
                    removed++;
                }
            }
            if (tablePlaced && tablePos != null && tableOriginalState != null) {
                level.setBlock(tablePos, tableOriginalState, 3);
                tablePlaced = false;
                Constants.LOG.info("[GUN-SMOKE] weapon table fixture restored during final cleanup pos={} state={}",
                        tablePos, tableOriginalState);
            }
            if (previousX != null && previousY != null && previousZ != null
                    && previousYaw != null && previousPitch != null) {
                actual.connection.teleport(previousX, previousY, previousZ, previousYaw, previousPitch);
            }
            restoreComplete = true;
            Constants.LOG.info("[GUN-SMOKE] inventory restoration verified slots={} selected={}",
                    originalInventory.slots().size(), originalInventory.selectedSlot());
            Constants.LOG.info("[GUN-SMOKE] fixture projectile cleanup complete projectilesRemoved={}", removed);
        }, playerId);
    }

    private static void finishRestore() {
        if (restored) return;
        restored = true;
        Minecraft mc = client;
        if (mc != null) {
            release(mc.options.keyAttack);
            release(HbmKeybindsClient.reloadKey);
            release(HbmKeybindsClient.gunTertiaryKey);
            if (previousCamera != null) mc.options.setCameraType(previousCamera);
            if (previousFov != null) mc.options.fov().set(previousFov);
            mc.options.save();
        }
        shutdownPending = true;
    }

    /** Render callback paired with {@link #capture}; unlike a tick callback it sees the current frame. */
    public static void render(net.minecraft.client.gui.GuiGraphicsExtractor ignored) {
        if (pendingCapture == null || client == null || client.level == null || restored) return;
        String[] capture = pendingCapture.split("\\|", 2);
        int frame = pendingCaptureIndex++;
        if (--pendingCaptureFrames <= 0) pendingCapture = null;
        net.minecraft.client.Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(),
                message -> Constants.LOG.info("[GUN-SMOKE] capture case={} state={} frame={} path={}",
                        capture[0], capture.length > 1 ? capture[1] : "unknown", frame, message.getString()));
    }

    /** Called by the weapon effect seam to prove that a transient muzzle geometry was submitted. */
    public static void noteMuzzleSubmission(String item, long shotStamp, long age, int duration) {
        if (!ENABLED || restored || started && (stage != Stage.FIRE_PRESS && stage != Stage.SMOKE_WAIT)) return;
        if (!started || !current().gun.equals(item) || age < 0L || age >= duration) return;
        Constants.LOG.info("[GUN-SMOKE] muzzle submitted case={} item={} now={} shotStamp={} ageMs={} durationMs={}",
                current().name, item, com.hbm.util.GameTime.now(), shotStamp, age, duration);
    }

    private static void trackFixtureProjectiles(Minecraft mc) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!fixtureEntities.contains(entity.getId())
                    && (entity instanceof com.hbm.entity.projectile.EntityBulletBaseMK4
                    || entity instanceof com.hbm.entity.projectile.EntityBulletBeamBase)) {
                fixtureProjectileUuids.add(entity.getUUID());
            }
        }
    }

    private static Set<Integer> entityIds(Minecraft mc) {
        Set<Integer> ids = new HashSet<>();
        for (Entity entity : mc.level.entitiesForRendering()) ids.add(entity.getId());
        return ids;
    }

    private static boolean logGunResources(Minecraft mc) {
        List<String> guns = new ArrayList<>();
        boolean valid = true;
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!Constants.MOD_ID.equals(id.getNamespace()) || !id.getPath().startsWith("gun_")
                    || !(item instanceof ItemGunBaseNT)) continue;
            Identifier definition = Ids.of(Constants.MOD_ID, "items/" + id.getPath() + ".json");
            String renderer = "<missing>";
            boolean definitionPresent = false;
            boolean basePresent = false;
            try {
                @Nullable Resource resource = mc.getResourceManager().getResource(definition).orElse(null);
                if (resource != null) {
                    definitionPresent = true;
                    try (BufferedReader reader = resource.openAsReader()) {
                        var root = com.google.gson.JsonParser.parseReader(reader);
                        var model = root.getAsJsonObject().getAsJsonObject("model");
                        String type = model.get("type").getAsString();
                        if ("hbm:gun".equals(type)) {
                            renderer = model.get("weapon").getAsString();
                            WeaponRenderers.byName(renderer);
                            Identifier base = Ids.parse(model.get("base").getAsString());
                            basePresent = mc.getResourceManager().getResource(
                                    Ids.of(base.getNamespace(), "models/" + base.getPath() + ".json")).isPresent();
                        } else if ("minecraft:model".equals(type)) {
                            // Upstream's PA ranged gun is intentionally a plain sprite, not a weapon renderer.
                            renderer = "sprite";
                            Identifier modelId = Ids.parse(model.get("model").getAsString());
                            basePresent = mc.getResourceManager().getResource(
                                    Ids.of(modelId.getNamespace(), "models/" + modelId.getPath() + ".json")).isPresent();
                        } else {
                            throw new IllegalArgumentException("unsupported item model type " + type);
                        }
                    }
                }
            } catch (Exception error) {
                renderer = "ERROR:" + error.getClass().getSimpleName();
            }
            valid &= definitionPresent && basePresent && !renderer.startsWith("ERROR:")
                    && !renderer.equals("<missing>");
            guns.add(id + " renderer=" + renderer + " item_def=" + definitionPresent
                    + " base_model=" + basePresent);
        }
        guns.sort(Comparator.naturalOrder());
        Constants.LOG.info("[GUN-SMOKE] registry gun resources count={} expected=70", guns.size());
        for (String gun : guns) Constants.LOG.info("[GUN-SMOKE] {}", gun);
        if (guns.size() != 70) {
            Constants.LOG.error("[GUN-SMOKE] refusing visual claim: expected 70 guns, got {}", guns.size());
            valid = false;
        }
        return valid;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.5f", value);
    }

    private static void press(net.minecraft.client.@Nullable KeyMapping mapping) {
        if (client != null && mapping == client.options.keyAttack) {
            MouseInput.fireButton(0, true);
            return;
        }
        if (mapping != null) mapping.setDown(true);
    }

    private static void release(net.minecraft.client.@Nullable KeyMapping mapping) {
        if (client != null && mapping == client.options.keyAttack) {
            MouseInput.fireButton(0, false);
            return;
        }
        if (mapping != null) mapping.setDown(false);
    }

    private static Case current() {
        return CASES.get(caseIndex);
    }

    private static boolean supportsAiming(ItemGunBaseNT gun, ItemStack stack) {
        var config = gun.getConfig(stack, 0);
        return config.getPressTertiary(stack) != null || config.getReleaseTertiary(stack) != null;
    }

    private enum Stage {
        WAITING, BAG_USE, BAG_WAIT, BAG_CLOSE, TABLE_SETUP_WAIT, TABLE_USE, TABLE_WAIT, TABLE_POPULATE,
        TABLE_POPULATE_WAIT, TABLE_GUN_WAIT, TABLE_CONFIG_WAIT, TABLE_MOD_WAIT, TABLE_EXTRACT_WAIT,
        TABLE_CLOSE, TABLE_RESTORE_WAIT, COMMANDS, RELOAD_PRESS, RELOAD_WAIT, FIRE_PRESS, SMOKE_WAIT,
        ADS_PRESS, ADS_WAIT, RESTORE_WAIT
    }

    private record Case(String name, String gun, String ammo) {}
}
