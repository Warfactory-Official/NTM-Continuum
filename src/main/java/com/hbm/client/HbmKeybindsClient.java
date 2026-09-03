package com.hbm.client;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.packet.KeybindPayload;
import com.mojang.blaze3d.platform.InputConstants;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.KeyMappings;
import dev.golem.core.client.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The client half of HBM's keybinds: registers the mappings, watches them, and reports every change to
 * the server.
 *
 * <p>Ported from the client parts of 1.12.2 {@code HbmKeybinds}. The design point is that this reports
 * <em>state</em>, never intent — "the trigger is now down", not "fire a shot". Everything else is the
 * server's decision, so a modified client cannot fire faster than the gun cycles.
 *
 * <p>Default bindings match upstream: primary is the vanilla attack button, secondary and tertiary are
 * the right and middle mouse buttons (upstream's {@code -99}/{@code -98} are 1.12's spelling of exactly
 * those), and reload is R.
 */
public final class HbmKeybindsClient {

    private static final String CATEGORY = "hbm";

    public static KeyMappings.Mapping reloadKey;
    public static KeyMappings.Mapping gunSecondaryKey;
    public static KeyMappings.Mapping gunTertiaryKey;
    public static KeyMappings.Mapping qmawKey;

    /** Last reported state per keybind, so only edges go over the wire. */
    private static final boolean[] LAST_STATE = new boolean[EnumKeybind.VALUES.length];

    private HbmKeybindsClient() {}

    public static void register() {
        reloadKey = KeyMappings.register(CATEGORY, "key.hbm.reload", InputConstants.KEY_R, CATEGORY);
        gunSecondaryKey = KeyMappings.register(CATEGORY, "key.hbm.gunSecondary",
                InputConstants.Type.MOUSE, 1, CATEGORY);
        gunTertiaryKey = KeyMappings.register(CATEGORY, "key.hbm.gunTertiary",
                InputConstants.Type.MOUSE, 2, CATEGORY);
        qmawKey = KeyMappings.register(CATEGORY, "key.hbm.qmaw", InputConstants.KEY_F1, CATEGORY);

        MouseInput.onButton(HbmKeybindsClient::onMouseButton);
        ClientTicks.onEndTick(HbmKeybindsClient::onEndTick);
    }

    private static void onEndTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            // Leaving a world must not leave the server believing a trigger is still held.
            java.util.Arrays.fill(LAST_STATE, false);
            return;
        }

        boolean holdingGun = isGun(player.getMainHandItem()) || isGun(player.getOffhandItem());
        // A screen is open: report everything released, so opening the inventory mid-burst stops it.
        boolean suppressed = mc.screen != null || !holdingGun;

        // The vanilla attack mapping is deliberately drained below, so poll its physical mouse button.
        // Keyboard rebindings retain their ordinary mapping state.
        boolean primary = !suppressed && KeyMappings.isHeld(mc.options.keyAttack);
        report(EnumKeybind.GUN_PRIMARY, primary);
        // isHeld, not isDown: on 1.20.1 vanilla tracks only one binding per key, so a bind sharing
        // a mouse button with Use or Pick Block never receives state at all (see KeyMappings.Mapping).
        report(EnumKeybind.GUN_SECONDARY, !suppressed && gunSecondaryKey.isHeld());
        report(EnumKeybind.GUN_TERTIARY, !suppressed && gunTertiaryKey.isHeld());
        report(EnumKeybind.RELOAD, !suppressed && reloadKey.isHeld());

        if (holdingGun && mc.screen == null) {
            suppressAttack(mc);
            updateAiming(player);
        }
    }

    /**
     * Stops the attack button from also mining, swinging and cracking blocks while a gun is held.
     *
     * <p>Upstream does this by reflecting into {@code KeyMapping}'s private static binding map and
     * unregistering the conflicting key. That reflection cannot survive the port, so the key is simply
     * held down-cleared every tick and its buffered clicks are drained instead — same visible result,
     * no reflection. The item's own {@code compatCanDestroyBlock} is the server-side backstop.
     */
    private static void suppressAttack(Minecraft mc) {
        drain(mc.options.keyAttack);
        // Clear other vanilla actions only while they actually overlap a gun binding. This preserves
        // user rebindings while retaining the old mod's "gun action wins" behavior.
        if (overlapsGunBinding(mc.options.keyUse, mc)) drain(mc.options.keyUse);
        if (overlapsGunBinding(mc.options.keyPickItem, mc)) drain(mc.options.keyPickItem);
    }

    /** Drains overlaps immediately after mappings update, before vanilla handles the buffered click. */
    private static void onMouseButton(int button, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null
                || (!isGun(player.getMainHandItem()) && !isGun(player.getOffhandItem()))) {
            return;
        }

        boolean primary = KeyMappings.matchesMouse(mc.options.keyAttack, button);
        boolean secondary = KeyMappings.matchesMouse(gunSecondaryKey, button);
        boolean tertiary = KeyMappings.matchesMouse(gunTertiaryKey, button);
        boolean reload = KeyMappings.matchesMouse(reloadKey, button);
        if (!primary && !secondary && !tertiary && !reload) return;

        if (KeyMappings.matchesMouse(mc.options.keyAttack, button)) drain(mc.options.keyAttack);
        if (KeyMappings.matchesMouse(mc.options.keyUse, button)) drain(mc.options.keyUse);
        if (KeyMappings.matchesMouse(mc.options.keyPickItem, button)) drain(mc.options.keyPickItem);

        if (primary) report(EnumKeybind.GUN_PRIMARY, pressed);
        if (secondary) report(EnumKeybind.GUN_SECONDARY, pressed);
        if (tertiary) report(EnumKeybind.GUN_TERTIARY, pressed);
        if (reload) report(EnumKeybind.RELOAD, pressed);
    }

    /** Clears a vanilla bind's held state and any clicks it buffered this tick. */
    private static void drain(KeyMapping mapping) {
        mapping.setDown(false);
        while (mapping.consumeClick()) {
            // A click buffered this tick would otherwise still fire on the next one.
        }
    }

    private static boolean overlapsGunBinding(KeyMapping mapping, Minecraft mc) {
        return mapping.same(mc.options.keyAttack)
                || mapping.same(gunSecondaryKey)
                || mapping.same(gunTertiaryKey)
                || mapping.same(reloadKey);
    }

    /** Advances the aim transition — upstream runs this from the item's client-side update. */
    private static void updateAiming(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!isGun(held)) held = player.getOffhandItem();
        if (!isGun(held)) return;

        ItemGunBaseNT.prevAimingProgress = ItemGunBaseNT.aimingProgress;
        boolean aiming = ItemGunBaseNT.getIsAiming(held);
        float aimSpeed = 0.25F;
        if (aiming && ItemGunBaseNT.aimingProgress < 1F) ItemGunBaseNT.aimingProgress += aimSpeed;
        if (!aiming && ItemGunBaseNT.aimingProgress > 0F) ItemGunBaseNT.aimingProgress -= aimSpeed;
        ItemGunBaseNT.aimingProgress = Math.max(0F, Math.min(1F, ItemGunBaseNT.aimingProgress));
    }

    private static void report(EnumKeybind keybind, boolean state) {
        if (LAST_STATE[keybind.ordinal()] == state) return;
        LAST_STATE[keybind.ordinal()] = state;
        GolemNetwork.sendToServer(KeybindPayload.CODEC, new KeybindPayload(keybind.ordinal(), state));
    }

    private static boolean isGun(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemGunBaseNT;
    }
}
