package com.hbm.client.render.entity;

import com.hbm.Constants;
import com.hbm.commands.EntityRenderSmoke;
import dev.golem.core.client.ClientTicks;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Development-only driver for a real joined-world render smoke of the ported entities: asks the server to spawn
 * the roster, parks the camera over the grid, walks the view across it taking screenshots, and reports which
 * renderer each visible ported entity resolved to. A renderer that throws takes the client down, which the
 * runner sees as a non-zero exit; a renderer that draws nothing is caught only by looking at the captures.
 *
 * <p>Enabled by {@code HBM_ENTITY_SMOKE=1} in a development client; see {@code port-tools/run_entity_render_check.ps1}.
 */
public final class EntityRenderSmokeClient {

    private static boolean sent;
    private static boolean landed;
    private static int waited;
    private static int ticks;
    private static double startX, startZ;
    private static double originX, originY, originZ;

    private EntityRenderSmokeClient() {}

    public static void register() {
        if (System.getenv("HBM_ENTITY_SMOKE") == null) return;
        ClientTicks.onEndTick(EntityRenderSmokeClient::tick);
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || minecraft.getOverlay() != null) return;

        if (System.getenv("HBM_ENTITY_SMOKE_CLEAN") != null) {
            // Maintenance mode: strip every non-player entity from the save (an earlier fixture left its roster
            // at spawn before the runner learned to restore the save), then leave.
            if (!sent) {
                minecraft.player.connection.sendCommand("kill @e[type=!minecraft:player]");
                sent = true;
            } else if (++ticks == 60) {
                Constants.LOG.info("[HBM] Entity render smoke: save cleaned");
                minecraft.stop();
            }
            return;
        }
        if (!sent) {
            startX = minecraft.player.getX();
            startZ = minecraft.player.getZ();
            minecraft.player.connection.sendCommand("hbm entity_smoke");
            sent = true;
            return;
        }
        if (!landed) {
            // The command teleports the player 4000 blocks onto fresh ground, which can take a while to generate;
            // the clock starts once that has landed.
            if (Math.abs(minecraft.player.getX() - startX) + Math.abs(minecraft.player.getZ() - startZ) < 1000D) {
                if (++waited > 1200) {
                    Constants.LOG.error("[HBM] Entity render smoke: teleport never landed");
                    minecraft.stop();
                }
                return;
            }
            landed = true;
            originX = minecraft.player.getX();
            originZ = minecraft.player.getZ();
            // The server stood the player one block above the ground it measured; the client's own heightmap may
            // not have that chunk yet.
            originY = minecraft.player.getY() - 1D;
            Constants.LOG.info("[HBM] Entity render smoke origin {}, {}, {}", originX, originY, originZ);
        }
        ticks++;

        // Camera: first person, hovering over the grid's near edge looking down its rows; the yaw sweeps
        // across the grid so every column passes through the middle of a capture.
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        List<String> roster = EntityRenderSmoke.roster();
        int rows = (roster.size() + EntityRenderSmoke.columns() - 1) / EntityRenderSmoke.columns();
        double gridDepth = 8D + rows * EntityRenderSmoke.spacingZ();
        double camY, camZ, pitch;
        float sweep = ((ticks % 150) - 60) / 80F; // -0.75 .. +1.1
        if (EntityRenderSmoke.closeUp()) {
            // A hand-picked subset: eye level, a few blocks back, panning slowly across the little grid; the
            // first capture looks up at whatever is still falling.
            camY = originY + 2.2D;
            camZ = originZ - 1D;
            pitch = ticks < 20 ? -20D : 14D;
            sweep *= 0.6F;
        } else {
            // Pass 1 (ticks 20..140): high and far, the whole grid. Pass 2 (160..280): low and close, the first rows.
            boolean close = ticks >= 150;
            camY = originY + (close ? 3D : 18D);
            camZ = originZ + (close ? 2D : -6D);
            pitch = close ? 12D : Math.toDegrees(Math.atan2(camY - originY, gridDepth * 0.5D));
        }
        minecraft.player.setPos(originX, camY, camZ);
        minecraft.player.setDeltaMovement(0, 0, 0);
        minecraft.player.setXRot((float) pitch);
        minecraft.player.setYRot(sweep * 35F);

        if (ticks == 8 || ticks == 40 || ticks == 80 || ticks == 120 || ticks == 190 || ticks == 230 || ticks == 270) {
            Screenshot.grab(minecraft.gameDirectory, minecraft.getMainRenderTarget(),
                    message -> Constants.LOG.info("[HBM] Entity render capture: {}", message.getString()));
        }

        if (ticks == 100) report(minecraft);
        if (ticks == 300) {
            Constants.LOG.info("[HBM] Entity render smoke finished");
            minecraft.stop();
        }
    }

    /** One line per ported entity in view range: its type, its renderer class, and its distance. */
    private static void report(Minecraft minecraft) {
        TreeMap<String, String> seen = new TreeMap<>();
        List<String> unbound = new ArrayList<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            String id = EntityType.getKey(entity.getType()).toString();
            if (!id.startsWith(Constants.MOD_ID + ":")) continue;
            Object renderer = null;
            try {
                renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
            } catch (RuntimeException ignored) {
                // reported below as unbound
            }
            if (renderer == null) {
                unbound.add(id);
                continue;
            }
            seen.put(id, renderer.getClass().getSimpleName());
        }
        for (var entry : seen.entrySet()) {
            Constants.LOG.info("[HBM] Entity render smoke: {} -> {}", entry.getKey(), entry.getValue());
        }
        Constants.LOG.info("[HBM] Entity render smoke: {} ported entity types in view, {} without a renderer{}",
                seen.size(), unbound.size(), unbound.isEmpty() ? "" : " " + unbound);
        int missing = 0;
        List<String> roster = EntityRenderSmoke.roster();
        for (String id : roster) {
            if (!seen.containsKey(Constants.MOD_ID + ":" + id)) {
                missing++;
                Constants.LOG.warn("[HBM] Entity render smoke: roster entity {} is not in view", id);
            }
        }
        Constants.LOG.info("[HBM] Entity render smoke: {} of {} roster entities in view", roster.size() - missing,
                roster.size());
    }
}
