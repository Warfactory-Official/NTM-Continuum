package com.hbm.client.jetpack;

import com.hbm.Constants;
import com.hbm.handler.HbmKeybinds;
import com.hbm.handler.JetpackHandler;
import com.hbm.packet.KeybindPayload;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.platform.Platform;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Development-only driver for a real joined-world jetpack render smoke. */
public final class JetpackSmokeClient {

    private static boolean sent;
    private static int diagnostics;
    private static Vec3 bombTarget;

    private JetpackSmokeClient() {}

    public static void register() {
        if (!Platform.isDevelopment() || System.getenv("HBM_JETPACK_SMOKE") == null) return;
        ClientTicks.onEndTick(JetpackSmokeClient::tick);
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (System.getenv("HBM_TITLE_CHECK") != null) {
            if (minecraft.level != null) throw new IllegalStateException("Normal launch unexpectedly joined a world");
            if (minecraft.screen instanceof TitleScreen && minecraft.getOverlay() == null) {
                if (++diagnostics == 20) Screenshot.grab(minecraft.gameDirectory,
                        minecraft.getMainRenderTarget(), message -> Constants.LOG.info(
                                "Title-screen visual capture: {}", message.getString()));
                if (diagnostics == 40) minecraft.stop();
            }
            return;
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || minecraft.getOverlay() != null) return;
        // First-person rendering skips the local player's humanoid layers, so it cannot exercise the
        // equipment model or the scene-sampling heat path.
        boolean bomb = "bomb".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"));
        boolean crouch = "crouch".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"))
                || "flight_crouch".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"));
        if (crouch) minecraft.options.keyShift.setDown(true);
        if ("flight_crouch".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"))) {
            minecraft.player.setDeltaMovement(Vec3.ZERO);
            minecraft.player.setOnGround(false);
        }
        minecraft.options.setCameraType(bomb ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
        boolean occlusion = "occlusion".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"));
        if (occlusion) {
            minecraft.options.setCameraType(diagnostics < 90 ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
            minecraft.player.setXRot(diagnostics < 90 ? 0F : 60F);
            minecraft.player.setYRot(0F);
        }
        if ("water".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"))) {
            minecraft.player.setXRot(60F);
            minecraft.player.setYRot(0F);
        }
        if (bomb) {
            if (bombTarget == null) bombTarget = Vec3.atCenterOf(
                    minecraft.player.blockPosition().offset(0, -1, 3));
            var delta = bombTarget.subtract(minecraft.player.getEyePosition());
            minecraft.player.setYRot((float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90F
                    + (diagnostics < 60 ? 0F : 35F));
            minecraft.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)))
                    - (diagnostics < 60 ? 0F : 28F));
        }
        if (!sent) {
            Constants.LOG.info("Jetpack visual check gear={}", System.getenv("HBM_JETPACK_CHECK_GEAR"));
            String test = System.getenv("HBM_RUN_TEST");
            String command = System.getenv("HBM_RUN_COMMAND");
            if (command != null && !command.isBlank()) {
                for (String request : command.split("\n")) minecraft.player.connection.sendCommand(request);
            } else {
                minecraft.player.connection.sendCommand(test == null || test.isBlank() ? "hbm jetpack_smoke" : "test run " + test);
            }
            sent = true;
            return;
        }
        if (++diagnostics % 20 == 0 && diagnostics <= 100) {
            JetpackClientBridge.State state = JetpackClientBridge.state(minecraft.player);
            Constants.LOG.info("[HBM] Jetpack smoke state kind={} state={} active={} hover={} thrust={} onGround={}",
                    JetpackClientVisuals.kind(minecraft.player), state == null ? "null" : "present",
                    state != null && state.active(), state != null && state.hover(),
                    state == null ? -1F : state.thrust(), minecraft.player.onGround());
            if (bomb) Constants.LOG.info("Bomb visual camera player position {}", minecraft.player.position());
        }
        if (System.getenv("HBM_JETPACK_SCREENSHOTS") != null) {
            boolean reload = "reload".equals(System.getenv("HBM_JETPACK_CHECK_GEAR"));
            if (reload && diagnostics == 80) minecraft.reloadResourcePacks().thenRun(
                    () -> Constants.LOG.info("Jetpack visual resource reload completed"));
            if (diagnostics == 60) {
                JetpackHandler.toggleHUDState(minecraft.player);
                GolemNetwork.sendToServer(KeybindPayload.CODEC,
                        new KeybindPayload(HbmKeybinds.EnumKeybind.JETPACK_HUD.ordinal(), true));
            }
            if (diagnostics == 61) {
                GolemNetwork.sendToServer(KeybindPayload.CODEC,
                        new KeybindPayload(HbmKeybinds.EnumKeybind.JETPACK_HUD.ordinal(), false));
            }
            if ((!reload && !occlusion && diagnostics == 100) || diagnostics == 160) {
                JetpackHandler.toggleOpenState(minecraft.player);
                GolemNetwork.sendToServer(KeybindPayload.CODEC,
                        new KeybindPayload(HbmKeybinds.EnumKeybind.JETPACK.ordinal(), true));
            }
            if (diagnostics == 101 || diagnostics == 161) {
                GolemNetwork.sendToServer(KeybindPayload.CODEC,
                        new KeybindPayload(HbmKeybinds.EnumKeybind.JETPACK.ordinal(), false));
            }
            if (diagnostics == 40 || diagnostics == 80
                    || diagnostics >= 99 && diagnostics <= 105
                    || diagnostics >= 159 && diagnostics <= 165) {
                Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(),
                        message -> Constants.LOG.info("Jetpack visual capture: {}", message.getString()));
            }
            if (diagnostics == 180) {
                minecraft.options.keyShift.setDown(false);
                minecraft.player.connection.sendCommand("hbm jetpack_smoke stop");
            }
            if (diagnostics == 220) minecraft.stop();
        }
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (System.getenv("HBM_JETPACK_SCREENSHOTS") == null) return;
        String[] items = {"t51_helmet", "t51_plate", "bj_helmet", "bj_plate_jetpack", "dns_helmet",
                "dns_plate", "dns_legs", "dns_boots", "jetpack_glider", "jetpack_fly"};
        for (int i = 0; i < items.length; i++) {
            int x = 8 + i * 20;
            GuiDraw.fill(graphics, x - 1, 7, x + 17, 25, 0xFF303030);
            GuiDraw.item(graphics, new ItemStack(
                    BuiltInRegistries.ITEM.getValue(
                            Ids.of("hbm", items[i]))), x, 8);
        }
    }
}
