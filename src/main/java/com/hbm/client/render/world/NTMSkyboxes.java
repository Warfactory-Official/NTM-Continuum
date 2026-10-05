package com.hbm.client.render.world;

import com.hbm.blocks.ModGlassBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.handler.ImpactWorldHandler;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.render.Sky;
import dev.golem.core.render.Skyboxes;
import dev.golem.core.world.Skies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7's sky-renderer slot and the {@code ModEventHandlerClient.onClientTickLast} logic that fills it
 * ({@code GeneralConfig.enableSkyboxes}): the overworld provider's {@code IRenderHandler} is emulated as {@link Slot},
 * one per client level (1.7 kept it on the {@code WorldProvider}, which a new {@code WorldClient} replaced), and drawn
 * through Golem Core's {@link Skyboxes}: {@link RenderNTMSkyboxImpact} replaces vanilla's sun, moon and stars, and
 * {@link RenderNTMSkyboxChainloader} draws after the sky.
 *
 * <p>Two consequences of 1.7's tick logic are deliberately NOT ported (user decision, wave 2, History §19ek-visual):
 * <ul>
 *   <li>1.7 alternated the slot every client tick while dust or fire was above 0 (install the impact renderer and
 *       return, then wrap it in a chainloader, then replace that again), so the chainloader's objects flickered on
 *       every other tick. Here the overworld holds one stable state: the impact sky plus the chainloader objects,
 *       drawn every frame, for as long as dust or fire is above 0.</li>
 *   <li>1.7 never removed the impact renderer, so the impact sky outlived the impact until the next world load. Here
 *       the slot returns to the vanilla sky plus the chainloader objects as soon as dust and fire are both 0.</li>
 * </ul>
 * The lode star test runs every tick, at most every 200 ms of wall time, as in 1.7.
 */
public final class NTMSkyboxes implements Skyboxes.Skybox, Skies.Modifier {

    /** What 1.7's {@code world.provider.getSkyRenderer()} held. */
    private enum Slot { NONE, CHAINLOADER, CHAINLOADER_IMPACT }

    private static ClientLevel slotLevel;
    private static Slot slot = Slot.NONE;

    /** 1.7 {@code ModEventHandlerClient.renderLodeStar}: the player is looking through polarized glass at the lode star. */
    public static boolean renderLodeStar = false;
    /** 1.7 {@code ModEventHandlerClient.lastStarCheck}. */
    public static long lastStarCheck = 0L;

    private NTMSkyboxes() {}

    public static void register() {
        NTMSkyboxes instance = new NTMSkyboxes();
        Skyboxes.register(instance);
        Skies.addModifier(instance);
        // 1.7: ClientTickEvent at LOWEST priority, phase START.
        ClientTicks.onStartTick(NTMSkyboxes::onClientTickLast);
    }

    /** The slot's state as a word, for the {@code hbm_impact} harness query. */
    public static String state(Level level) {
        return level != null && level == slotLevel ? slot.name().toLowerCase(java.util.Locale.ROOT) : "none";
    }

    private static boolean impactInstalled(Level level) {
        return level == slotLevel && slot == Slot.CHAINLOADER_IMPACT;
    }

    private static void onClientTickLast() {
        Minecraft mc = Minecraft.getInstance();
        if (!GeneralConfig.enableSkyboxes) {
            slot = Slot.NONE;
            return;
        }

        ClientLevel world = mc.level;
        if (world == null) return;
        if (world != slotLevel) {
            slotLevel = world;
            slot = Slot.NONE;
        }

        // world.provider.dimensionId == 0 / WorldProviderSurface: the overworld gets the chainloader, and the impact
        // renderer underneath it while Tom's dust or fire is above 0 (no 1.7 alternation, no 1.7 persistence).
        if (world.dimension() == Level.OVERWORLD) {
            boolean impact = ImpactWorldHandler.getDustForClient(world) > 0 || ImpactWorldHandler.getFireForClient(world) > 0;
            slot = impact ? Slot.CHAINLOADER_IMPACT : Slot.CHAINLOADER;
        } else {
            slot = Slot.NONE;
        }

        LocalPlayer player = mc.player;
        long millis = System.currentTimeMillis();

        if (lastStarCheck + 200 < millis) {
            renderLodeStar = false;
            lastStarCheck = millis;

            if (player != null) {
                // 1.7's client player posY is its eye height (yOffset 1.62), whatever the pose.
                Vec3 pos = new Vec3(player.getX(), player.getY() + 1.62, player.getZ());
                // new Vec3NT(0, 0, -1).rotateAroundXDeg(-15).multiply(25)
                double a = Math.toRadians(-15.0);
                Vec3 heading = new Vec3(0, (0 * Math.cos(a) + -1 * Math.sin(a)) * 25, (-1 * Math.cos(a) - 0 * Math.sin(a)) * 25);
                Vec3 nextPos = pos.add(heading);
                // func_147447_a(pos, next, stopOnLiquid false, ignoreBlockWithoutBoundingBox true, returnLast false)
                BlockHitResult mop = world.clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (mop.getType() == HitResult.Type.BLOCK
                        && world.getBlockState(mop.getBlockPos()).is(ModGlassBlocks.glass_polarized.get())) {
                    renderLodeStar = true;
                }
            }
        }
    }

    // ======================================================================== drawing (Golem Core Skyboxes)

    @Override
    public boolean replacesCelestial(ClientLevel level) {
        return impactInstalled(level);
    }

    @Override
    public void celestial(Sky sky) {
        RenderNTMSkyboxImpact.render(sky);
    }

    @Override
    public void afterSky(Sky sky) {
        if (sky.level() == slotLevel && (slot == Slot.CHAINLOADER || slot == Slot.CHAINLOADER_IMPACT)) {
            RenderNTMSkyboxChainloader.render(sky);
        }
    }

    // ======================================================================== the impact renderer has no sunrise glow

    @Override
    public boolean active(Level level) {
        return level.isClientSide() && impactInstalled(level);
    }

    /** 1.7's impact renderer never draws {@code calcSunriseSunsetColors}' fan. */
    @Override
    public float[] sunriseColor(Level level, float[] rgba) {
        return null;
    }
}
