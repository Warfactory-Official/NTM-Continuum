package com.hbm.client.jetpack;

import com.google.gson.JsonObject;
import com.hbm.client.crucible.CrucibleEquipModel;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.JetpackHandler;
import com.hbm.items.armor.WingsMurk;
import com.mojang.blaze3d.vertex.PoseStack;
import com.hbm.client.render.armor.ArmorRenderer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.FrameTime;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.PlasmaGlowRenderType;
import dev.golem.core.render.StaticMeshes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.render.HeatDistortionRenderType;
import dev.golem.core.render.Particles;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.Compat;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client-side jetpack model selection, exhaust spawning, and the two HUD compositions.
 */
public final class JetpackClientVisuals {

    public static final String GLIDER = "glider";
    public static final String BJ = "bj";
    public static final String DNS = "dns";

    static final Identifier JETPACK_MODEL = Ids.of("hbm", "models/armor/jetpack.obj");
    static final Identifier BJ_MODEL = Ids.of("hbm", "models/armor/bj.obj");
    static final Identifier WINGS_MODEL = Ids.of("hbm", "models/armor/murk.obj");
    private static final Identifier MASK = Ids.of("hbm", "textures/models/bfg/fresnel_ms.png");
    private static final Identifier NOISE = Ids.of("hbm", "textures/misc/noise_2.png");

    private static JetpackColladaModel gliderModel;
    /**
     * Harness only, see {@link #init}.
     */
    private static long pinnedGliderMs = -1L;
    private static boolean pinnedGliderReverse;
    private static boolean harnessNoExhaust;

    private static final Map<UUID, Effects> EFFECTS = new HashMap<>();
    private static final Map<UUID, GliderAnimation> GLIDER_ANIMATIONS = new HashMap<>();
    private static ClientLevel activeLevel;

    private JetpackClientVisuals() {
    }

    public static void init() {
        PlasmaGlowRenderType.init();
        StaticMeshes.init();
        WorldEffects.init();
        // Harness only: pin the worn glider's Collada playhead ({@code glider_ms}, {@code glider_reverse}; -1 lets it
        // run) and the first-person crucible's deploy ({@code crucible_deploy_ms}), so frames of a moving animation can
        // be compared exactly.
        HarnessQueryRegistry.register("hbm_static_models", (mc, params) -> {
            if (params.has("glider_ms")) pinnedGliderMs = params.get("glider_ms").getAsLong();
            if (params.has("glider_reverse")) pinnedGliderReverse = params.get("glider_reverse").getAsBoolean();
            // no exhaust: the effects cleared and none spawned, so frames show the model alone
            if (params.has("glider_no_exhaust")) {
                harnessNoExhaust = params.get("glider_no_exhaust").getAsBoolean();
                if (harnessNoExhaust) for (Effects effects : EFFECTS.values()) effects.clear();
            }
            if (params.has("crucible_deploy_ms"))
                CrucibleEquipModel.pinnedElapsed = params.get("crucible_deploy_ms").getAsLong();
            JsonObject json = new JsonObject();
            json.addProperty("ok", true);
            json.addProperty("glider_ms", pinnedGliderMs);
            json.addProperty("glider_reverse", pinnedGliderReverse);
            json.addProperty("crucible_deploy_ms", CrucibleEquipModel.pinnedElapsed);
            // read-only: the live exhaust effects over every player (positive signal that the exhaust is drawn)
            int plasma = 0, heat = 0, brightness = 0, trails = 0;
            for (Effects effects : EFFECTS.values()) {
                plasma += effects.plasma.size();
                heat += effects.heat.size();
                brightness += effects.brightness.size();
                for (JetpackTrail trail : effects.trails) if (trail != null) trails++;
            }
            json.addProperty("exhaust_plasma", plasma);
            json.addProperty("exhaust_heat", heat);
            json.addProperty("exhaust_brightness", brightness);
            json.addProperty("exhaust_trails", trails);
            return json;
        });
        // The client texture manager is not constructed during mod-container construction. The render
        // type installs its scene texture lazily on the first submitted heat particle.
    }

    /**
     * Drops client-only state when a world or resource set is replaced.
     */
    public static void clear() {
        EFFECTS.clear();
        GLIDER_ANIMATIONS.clear();
        activeLevel = null;
        gliderModel = null;
        JetpackGliderItemModel.clear();
        JetpackWornModels.clear();
        JetpackSound.clearAll();
    }

    /**
     * Invalidates model instances after a client resource reload.
     */
    public static void reload() {
        clear();
    }

    public static String kind(Player player) {
        if (player == null) return null;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        String direct = itemKind(chest);
        if (direct != null) return direct;
        ItemStack module = ArmorModHandler.pryMod(chest, ArmorModHandler.plate_only);
        return itemKind(module);
    }

    public static String kindFromStack(ItemStack stack) {
        return itemKind(stack);
    }

    public static String path(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null
                : BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    public static ItemStack sourceStack(ItemStack chest) {
        if (chest == null || chest.isEmpty()) return ItemStack.EMPTY;
        return itemKind(chest) == null
                ? ArmorModHandler.pryMod(chest, ArmorModHandler.plate_only) : chest;
    }

    private static String itemKind(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String path = path(stack);
        if (path.equals("jetpack_glider")) return GLIDER;
        if (path.equals("bj_plate_jetpack")) return BJ;
        if (path.equals("dns_plate")) return DNS;
        if (path.equals("wings_murk") || path.equals("wings_limp")) return "wings";
        if (path.equals("jetpack_fly") || path.equals("jetpack_break")
                || path.equals("jetpack_vector") || path.equals("jetpack_boost")) return "standard";
        return null;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != activeLevel) {
            clear();
            activeLevel = minecraft.level;
        }
        if (minecraft.level == null) {
            return;
        }
        Set<UUID> tracked = new HashSet<>();
        for (Player player : minecraft.level.players()) {
            tracked.add(player.getUUID());
            Effects effects = EFFECTS.computeIfAbsent(player.getUUID(), ignored -> new Effects());
            tickHeat(effects);
            String kind = kind(player);
            JetpackClientBridge.State state = JetpackClientBridge.state(player);
            if (kind == null) JetpackSound.clear(player.getUUID());
            else JetpackSound.tick(player, state);
            if (!GLIDER.equals(kind)) {
                effects.clear();
                EFFECTS.remove(player.getUUID());
                GLIDER_ANIMATIONS.remove(player.getUUID());
                continue;
            }
            if (GLIDER.equals(kind) && active(state) && !player.onGround() && state.thrust() > 0.05F) {
                if (!harnessNoExhaust) spawnExhaust(player, state, effects);
            }
            if (effects.empty() && !trailEligible(player, state, fuel(player))) EFFECTS.remove(player.getUUID());
        }
        Iterator<Map.Entry<UUID, Effects>> stale = EFFECTS.entrySet().iterator();
        while (stale.hasNext()) {
            UUID id = stale.next().getKey();
            if (!tracked.contains(id)) {
                JetpackSound.clear(id);
                GLIDER_ANIMATIONS.remove(id);
                stale.remove();
            }
        }
        GLIDER_ANIMATIONS.keySet().removeIf(id -> !tracked.contains(id));
    }

    private static boolean active(JetpackClientBridge.State state) {
        return state != null && state.active() && state.failureTicks() <= 0;
    }

    private static void tickHeat(Effects effects) {
        effects.plasma.removeIf(particle -> !particle.tick());
        effects.brightness.removeIf(particle -> !particle.tick());
        Iterator<ParticleHeatDistortion> iterator = effects.heat.iterator();
        while (iterator.hasNext()) if (!iterator.next().tick()) iterator.remove();
        if (effects.trails[0] != null && !effects.trails[0].tick()) effects.trails[0] = null;
        if (effects.trails[1] != null && !effects.trails[1].tick()) effects.trails[1] = null;
    }

    private static void spawnExhaust(Player player, JetpackClientBridge.State state, Effects effects) {
        float thrust = state.thrust() - 0.4F;
        float speed = -1F - 2F * thrust;
        float scale = 4F + 2F * thrust;
        JetpackGradient gradient = gradient(fuelType(player));
        float[] brightness = brightnessColor(fuelType(player));
        int count = 3;
        for (int i = 0; i < count; i++) {
            float fraction = i / (float) count;
            for (int side : new int[]{-1, 1}) {
                Vec3 motion = new Vec3(player.getRandom().nextGaussian() * 0.05F + side * 0.1F,
                        speed, player.getRandom().nextGaussian() * 0.05F);
                effects.plasma.add(new ParticleRocketPlasma(new Vec3(side * 1.8F, fraction * speed, 4F),
                        motion, scale, gradient));
            }
        }
        if (player.level().getGameTime() % (2 - player.level().getRandom().nextInt(2)) == 0) {
            float alpha = Mth.clamp(0.05F + thrust * 0.1F, 0F, 1F);
            for (int side : new int[]{-1, 1})
                effects.brightness.add(new ParticleFakeBrightness(
                        new Vec3(side * 1.8F, -1F, 4F), 20F + thrust * 10F,
                        6 + player.getRandom().nextInt(2), brightness, alpha));
        }
        if (player.level().getRandom().nextBoolean()) {
            effects.heat.add(new ParticleHeatDistortion(1.8F, -1F, 4F, 5F + thrust,
                    1.5F + thrust * 3F, 5, player.level().getRandom().nextFloat() * 20F,
                    0.1F, speed * 0.5F, 0F));
        }
        if (player.level().getRandom().nextBoolean()) {
            effects.heat.add(new ParticleHeatDistortion(-1.8F, -1F, 4F, 5F + thrust,
                    1.5F + thrust * 3F, 5, player.level().getRandom().nextFloat() * 20F,
                    -0.1F, speed * 0.5F, 0F));
        }
    }

    private static boolean trailEligible(Player player, JetpackClientBridge.State state,
                                         JetpackClientBridge.Fuel fuel) {
        double horizontalSpeed = player.getDeltaMovement().x * player.getDeltaMovement().x
                + player.getDeltaMovement().z * player.getDeltaMovement().z;
        return active(state) && !state.hover() && !player.onGround() && !player.isFallFlying()
                && state.thrust() > 0.05F && horizontalSpeed > 1.5D * 1.5D
                && fuel != null && fuel.amount() > 0;
    }

    /**
     * Handles the common gameplay packet for the ordinary/BJ/DNS families.
     */
    public static void spawnExhaustCue(Level level, int playerId, int mode) {
        if (!(level instanceof ClientLevel clientLevel)) return;
        if (Particles.quality() == Particles.Quality.MINIMAL) return;
        Entity entity = clientLevel.getEntity(playerId);
        if (!(entity instanceof Player player)) return;
        double baseY = mode == JetpackHandler.EXHAUST_BJ ? player.getY() + 1.62D + 0.12D - 0.9375D
                : mode == JetpackHandler.EXHAUST_DNS ? player.getY()
                : player.getEyeY() - 1D;
        float yaw = (float) Math.toRadians(player.yBodyRot);
        Vec3 side = new Vec3(Math.cos(yaw), 0D, Math.sin(yaw));
        Vec3 back = new Vec3(Math.sin(yaw), 0D, -Math.cos(yaw));
        double backOffset = mode == JetpackHandler.EXHAUST_REGULAR
                || mode == JetpackHandler.EXHAUST_VECTOR ? 0.25D
                : mode == JetpackHandler.EXHAUST_BJ ? 0.3125D : 0D;
        Vec3 base = new Vec3(player.getX(), baseY, player.getZ()).add(back.scale(backOffset));
        if (mode == JetpackHandler.EXHAUST_REGULAR
                || mode == JetpackHandler.EXHAUST_VECTOR) {
            Vec3 look = player.getViewVector(1F);
            Vec3 motion = player.getDeltaMovement();
            Vec3 flameVelocity = mode == JetpackHandler.EXHAUST_REGULAR
                    ? new Vec3(Mth.clamp(motion.x, -5D, 5D), Mth.clamp(motion.y, -5D, 5D), Mth.clamp(motion.z, -5D, 5D))
                    : player.getDeltaMovement().subtract(look.scale(0.2D));
            Vec3 smokeVelocity = mode == JetpackHandler.EXHAUST_REGULAR
                    ? new Vec3(Mth.clamp(motion.x, -10D, 10D), Mth.clamp(motion.y, -10D, 10D),
                    Mth.clamp(motion.z, -10D, 10D))
                    : player.getDeltaMovement().subtract(look.scale(0.3D));
            for (int sign : new int[]{1, -1}) {
                Vec3 position = base.add(side.scale(sign * 0.125D));
                Particles.spawn(ParticleTypes.FLAME, position.x, position.y,
                        position.z, flameVelocity.x, flameVelocity.y, flameVelocity.z);
            }
            if (Particles.quality() == Particles.Quality.ALL) for (int sign : new int[]{1, -1}) {
                Vec3 position = base.add(side.scale(sign * 0.125D));
                Particles.spawn(ParticleTypes.SMOKE, position.x, position.y,
                        position.z, smokeVelocity.x, smokeVelocity.y, smokeVelocity.z);
            }
            Vec3 thrust = mode == JetpackHandler.EXHAUST_REGULAR ? new Vec3(0D, -1D, 0D) : look.scale(-1D).normalize();
            if (Particles.quality() == Particles.Quality.ALL) spawnKickup(clientLevel, base, thrust);
        } else {
            float red = mode == JetpackHandler.EXHAUST_BJ ? 0.8F : 0.01F;
            float green = mode == JetpackHandler.EXHAUST_BJ ? 0.5F : 1F;
            float blue = 1F;
            for (int sign : new int[]{1, -1}) {
                Vec3 position = base.add(side.scale(sign * 0.125D));
                Particles.dust(clientLevel, position.x, position.y, position.z, red, green, blue,
                        1F, player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z);
            }
            if (Particles.quality() == Particles.Quality.ALL) spawnKickup(clientLevel, base, new Vec3(0D, -1D, 0D));
        }
    }

    /**
     * 1.7 casts 10 blocks along the thrust and kicks up dust only off a block's top face.
     */
    private static void spawnKickup(ClientLevel level, Vec3 origin, Vec3 thrust) {
        Vec3 target = origin.add(thrust.scale(10D));
        BlockHitResult hit = Compat.clip(level, origin, target,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK
                || hit.getDirection() != Direction.UP) return;
        Vec3 location = hit.getLocation();
        double distance = origin.distanceTo(location);
        int count = Math.max(0, (int) (10D - distance));
        Vec3 velocity = new Vec3(Math.max(0D, 0.75D - distance * 0.075D), 0D, 0D);
        var state = level.getBlockState(hit.getBlockPos());
        for (int i = 0; i < count; i++) {
            velocity = velocity.yRot(level.getRandom().nextFloat() * (float) (Math.PI * 2D));
            level.addParticle(new BlockParticleOption(
                            ParticleTypes.BLOCK, state), location.x,
                    location.y + 0.1D, location.z, velocity.x, 0.1D, velocity.z);
        }
    }

    private static String fuelType(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack module = itemKind(chest) == null ? ArmorModHandler.pryMod(chest, ArmorModHandler.plate_only) : chest;
        JetpackClientBridge.Fuel fuel = JetpackClientBridge.fuel(module);
        return fuel == null ? "KEROSENE" : fuel.type();
    }

    public static JetpackClientBridge.Fuel fuel(Player player) {
        if (player == null) return null;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack module = itemKind(chest) == null ? ArmorModHandler.pryMod(chest, ArmorModHandler.plate_only) : chest;
        return JetpackClientBridge.fuel(module);
    }

    public static void submitGlider(PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
                                    boolean inventory, boolean opening, long animTime) {
        if (gliderModel == null) gliderModel = JetpackColladaModel.load(true);
        if (!inventory) {
            pose.pushPose();
            applyGliderRoot(pose, parts.sneaking && !flying(parts.player()));
            GliderAnimation animation = gliderAnimation(parts.player(), opening, animTime);
            if (pinnedGliderMs >= 0L) gliderModel.submit(pose, sink, light, pinnedGliderMs, pinnedGliderReverse);
            else gliderModel.submit(pose, sink, light, animation.elapsed(), !animation.opening());
            pose.popPose();
        } else {
            pose.pushPose();
            pose.translate(-5F, -6F, 0F);
            pose.scale(1.5F, 1.5F, 1.5F);
            gliderModel.submit(pose, sink, light, gliderModelLength(), false);
            pose.popPose();
        }
    }

    /**
     * The glider with its wings fully spread and no wearer: SilentYeti's bobblehead.
     */
    public static void submitGliderOpen(PoseStack pose, SubmitSink sink, int light) {
        if (gliderModel == null) gliderModel = JetpackColladaModel.load(true);
        gliderModel.submit(pose, sink, light, gliderModelLength(), false);
    }

    private static GliderAnimation gliderAnimation(Player player, boolean opening, long gameplayStart) {
        long now = System.currentTimeMillis();
        if (player == null) {
            long elapsed = gameplayStart == 0L ? gliderModelLength() : now - gameplayStart;
            return new GliderAnimation(opening, Math.max(0L, Math.min(gliderModelLength(), elapsed)));
        }
        GliderAnimation animation = GLIDER_ANIMATIONS.get(player.getUUID());
        if (animation == null) {
            long elapsed = gameplayStart == 0L ? gliderModelLength() : now - gameplayStart;
            animation = new GliderAnimation(opening, now, Math.max(0L, Math.min(gliderModelLength(), elapsed)));
            GLIDER_ANIMATIONS.put(player.getUUID(), animation);
        } else if (animation.opening != opening) {
            // State changes begin at the previous endpoint. Ignore later duplicate packet timestamps.
            animation = new GliderAnimation(opening, now, 0L);
            GLIDER_ANIMATIONS.put(player.getUUID(), animation);
        }
        return new GliderAnimation(animation.opening, Math.min(gliderModelLength(),
                animation.initialElapsed + Math.max(0L, now - animation.start)));
    }

    private static long gliderModelLength() {
        return 1000L;
    }

    public static void submitStandard(PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
                                      String kind) {
        JetpackWornModels.standard().submit(pose, sink, light, parts.body(), JetpackWornModels.pack(kind));
    }

    public static void submitBJ(PoseStack pose, SubmitSink sink, int light, HumanoidPose parts) {
        JetpackWornModels.bj().submit(pose, sink, light, parts.body());
    }

    /**
     * The wings flap on a sine of the wearer's own tick count, with fixed poses for standing and for a
     * limp pair that has lost its murk.
     */
    public static void submitWings(PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
                                   ItemStack source) {
        Player player = parts.player();
        if (player == null) return;
        boolean limp = source != null && source.getItem() instanceof WingsMurk wings
                && !wings.murk;
        float sweep = (float) Math.sin(player.tickCount * 0.2D) * 20F;
        float trail = (float) Math.sin(player.tickCount * 0.2D - Math.PI * 0.5D) * 50F + 30F;
        boolean relax = relaxWings(player);
        if (!limp && relax) {
            sweep = 20F;
            trail = 160F;
        }
        if (limp) {
            if (relax) {
                sweep = 30F;
                trail = -30F;
            } else if (player.getDeltaMovement().y < -0.1D) {
                sweep = 0F;
                trail = 10F;
            } else {
                sweep = 30F;
                trail = 20F;
            }
        }
        JetpackWornModels.wings().submit(pose, sink, light, parts.body(), null, sweep, trail);
    }

    /**
     * 1.7 {@code RenderAccessoryUtility.renderWings(event, mode)}: {@code ModelArmorWings} types 2 (Bob) and 3
     * (the_NCR), which flap like murk wings in a skin of their own.
     */
    public static void submitWings(PoseStack pose, SubmitSink sink, int light, HumanoidPose parts,
                                   Surface skin) {
        Player player = parts.player();
        if (player == null) return;
        float sweep = (float) Math.sin(player.tickCount * 0.2D) * 20F;
        float trail = (float) Math.sin(player.tickCount * 0.2D - Math.PI * 0.5D) * 50F + 30F;
        if (relaxWings(player)) {
            sweep = 20F;
            trail = 160F;
        }
        JetpackWornModels.wings().submit(pose, sink, light, parts.body(), skin, sweep, trail);
    }

    /**
     * hbm-space {@code ModelArmorWings}: the wings also fold while the wearer rides something
     * ({@code relaxWings = onGround || ridingEntity != null}; ADDON-API-INVENTORY V-184). Off in plain HBM.
     */
    public static boolean relaxWhileRiding = false;

    private static boolean relaxWings(Player player) {
        return player.onGround() || relaxWhileRiding && player.isPassenger();
    }

    public static void submitLocalEffects(PoseStack pose, CameraInfo camera,
                                          boolean sneaking, Player player) {
        if (!WorldEffects.isCollecting() || player == null || !GLIDER.equals(kind(player))) return;
        Effects effects = EFFECTS.get(player.getUUID());
        if (effects == null) return;
        float partial = FrameTime.partialTick();
        pose.pushPose();
        applyGliderRoot(pose, sneaking && !flying(player));
        for (ParticleHeatDistortion heat : effects.heat) {
            Vec3 point = heat.position(partial);
            float size = 0.1F * heat.scale;
            if (size <= 0F) continue;
            Vec3[] corners = billboardCorners(pose, camera, point, size);
            float amount = heat.heatAmount * heat.fade(partial) * 0.15F;
            float timeOffset = heat.timeOffset / 20F;
            WorldEffects.submit(pose, HeatDistortionRenderType.of(MASK, NOISE), 0,
                    (drawPose, buffer) -> {
                        HeatDistortionRenderType.vertex(buffer, drawPose, (float) corners[0].x,
                                (float) corners[0].y, (float) corners[0].z, 1F, 1F, amount, timeOffset);
                        HeatDistortionRenderType.vertex(buffer, drawPose, (float) corners[1].x,
                                (float) corners[1].y, (float) corners[1].z, 1F, 0F, amount, timeOffset);
                        HeatDistortionRenderType.vertex(buffer, drawPose, (float) corners[2].x,
                                (float) corners[2].y, (float) corners[2].z, 0F, 0F, amount, timeOffset);
                        HeatDistortionRenderType.vertex(buffer, drawPose, (float) corners[3].x,
                                (float) corners[3].y, (float) corners[3].z, 0F, 1F, amount, timeOffset);
                    });
        }
        for (ParticleRocketPlasma plasma : effects.plasma)
            submitGlow(pose, 1, camera, plasma.position(partial), plasma.size(partial), plasma.color(partial));
        for (ParticleFakeBrightness brightness : effects.brightness)
            submitGlow(pose, 2, camera, brightness.position, brightness.size(partial), brightness.color);
        JetpackClientBridge.Fuel fuel = fuel(player);
        JetpackClientBridge.State state = JetpackClientBridge.state(player);
        if (trailEligible(player, state, fuel)
                && player.level().getGameTime() != effects.lastTrailCaptureTick
        ) {
            if (effects.trails[0] == null) effects.trails[0] = new JetpackTrail();
            if (effects.trails[1] == null) effects.trails[1] = new JetpackTrail();
            effects.trails[0].tryAddNewPos(worldPoint(pose, camera, 5.25F, 0.2F, 2.75F));
            effects.trails[1].tryAddNewPos(worldPoint(pose, camera, -5.25F, 0.2F, 2.75F));
            effects.lastTrailCaptureTick = player.level().getGameTime();
        } else if (!trailEligible(player, state, fuel)) {
            effects.trails[0] = null;
            effects.trails[1] = null;
        }
        if (effects.trails[0] != null) effects.trails[0].submit(pose, camera);
        if (effects.trails[1] != null) effects.trails[1].submit(pose, camera);
        pose.popPose();
    }

    /**
     * Captures a local glider nozzle after the player pose/root and converts it to world space.
     */
    private static void submitGlow(PoseStack pose, int order, CameraInfo camera, Vec3 center, float size,
                                   float[] color) {
        if (size <= 0F || color[3] <= 0F) return;
        Vec3[] corners = billboardCorners(pose, camera, center, size);
        WorldEffects.submit(pose, PlasmaGlowRenderType.of(MASK), order, (drawPose, buffer) -> {
            for (int i = 0; i < 4; i++) {
                Vec3 vertex = corners[i];
                Vertexes.vertex(buffer, drawPose, (float) vertex.x, (float) vertex.y,
                        (float) vertex.z, color[0], color[1], color[2], color[3], i < 2 ? 1F : 0F,
                        i == 0 || i == 3 ? 1F : 0F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
            }
        });
    }

    private static Vec3 worldPoint(PoseStack pose, CameraInfo camera, float x, float y, float z) {
        Vector3f world = camera.renderToWorld(new Vector3f(x, y, z).mulPosition(pose.last().pose()));
        return camera.pos.add(world.x(), world.y(), world.z());
    }

    private static Vec3[] billboardCorners(PoseStack pose, CameraInfo camera, Vec3 point, float size) {
        Matrix4f localToView = new Matrix4f(pose.last().pose());
        Matrix4f viewToLocal = new Matrix4f(localToView).invert();
        Vector3f center = new Vector3f((float) point.x, (float) point.y, (float) point.z)
                .mulPosition(localToView);
        // Entity layer matrices end in camera-relative WORLD space. The camera rotation lives in
        // the global view matrix; treating this as view space turns plumes edge-on at other yaws.
        Vector3f right = camera.worldToRender(new Vector3f(size, 0F, 0F).rotate(camera.orientation));
        Vector3f up = camera.worldToRender(new Vector3f(0F, size, 0F).rotate(camera.orientation));
        return new Vec3[]{
                localPoint(viewToLocal, center.x - right.x - up.x, center.y - right.y - up.y,
                        center.z - right.z - up.z),
                localPoint(viewToLocal, center.x - right.x + up.x, center.y - right.y + up.y,
                        center.z - right.z + up.z),
                localPoint(viewToLocal, center.x + right.x + up.x, center.y + right.y + up.y,
                        center.z + right.z + up.z),
                localPoint(viewToLocal, center.x + right.x - up.x, center.y + right.y - up.y, center.z + right.z - up.z)
        };
    }

    private static Vec3 localPoint(Matrix4f viewToLocal, float x, float y, float z) {
        Vector3f point = new Vector3f(x, y, z).mulPosition(viewToLocal);
        return new Vec3(point.x(), point.y(), point.z());
    }

    private static void applyGliderRoot(PoseStack pose, boolean sneaking) {
        pose.translate(0F, 0.9F, 1.25F);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        pose.mulPose(Axis.ZP.rotationDegrees(180F));
        pose.scale(0.25F, 0.25F, 0.25F);
        if (sneaking) {
            pose.translate(0F, 5.4F, 1F);
            pose.mulPose(Axis.XP.rotation(0.5F));
            pose.translate(0F, -4F, 0F);
        }
    }

    /**
     * Applies the old in-flight body tilt before vanilla submits the humanoid model.
     */
    public static void applyPlayerPose(Player player, PoseStack pose, float partialTick) {
        if (!flying(player)) return;
        JetpackClientBridge.State state = JetpackClientBridge.state(player);
        if (state == null) return;
        if (state.hover()) {
            float motionX = (float) (player.getX() - player.xo);
            float motionZ = (float) (player.getZ() - player.zo);
            float angle = (float) (Math.atan2(motionX, motionZ) + Math.PI * 0.5D);
            float amount = Mth.clamp((float) Math.sqrt(motionX * motionX + motionZ * motionZ), 0F, 2F);
            Quaternionf rotation = new Quaternionf().fromAxisAngleRad(
                    (float) Math.sin(angle), 0F, (float) Math.cos(angle), amount * (float) Math.toRadians(22.5D));
            pose.mulPose(rotation);
            return;
        }
        JetpackClientBridge.Fuel fuel = fuel(player);
        if (fuel == null || fuel.amount() <= 0) return;
        float renderYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180F - renderYaw));
        float time = state.jetpackFlyTime() + partialTick;
        float mult = Mth.clamp(time * time * 0.5F / 100F, 0F, 1F);
        pose.mulPose(Axis.XP.rotationDegrees((-player.getXRot() - 90F) * mult));
        Vec3 look = player.getViewVector(partialTick);
        float lookLength = (float) Math.sqrt(look.x * look.x + look.z * look.z);
        if (lookLength > 0F) {
            float lookX = (float) look.x / lookLength;
            float lookZ = (float) look.z / lookLength;
            float bodyX = (float) Math.cos(Math.toRadians(renderYaw + 90F));
            float bodyZ = (float) Math.sin(Math.toRadians(renderYaw + 90F));
            float dot = Math.max(lookX * bodyX + lookZ * bodyZ, 0F);
            float angle = (float) Math.acos(dot);
            if (!Float.isNaN(angle)) {
                float cross = lookZ * bodyX - bodyZ * lookX;
                pose.mulPose(Axis.YP.rotationDegrees(
                        (float) Math.toDegrees(angle) * Math.signum(cross) * mult));
            }
        }
        pose.mulPose(Axis.YP.rotationDegrees(-(180F - renderYaw)));
    }

    private static boolean flying(Player player) {
        if (player == null || !GLIDER.equals(kind(player)) || player.onGround() || player.isFallFlying()) return false;
        JetpackClientBridge.Fuel fuel = fuel(player);
        return active(JetpackClientBridge.state(player)) && fuel != null && fuel.amount() > 0;
    }

    public static boolean freezesPlayerPose(Player player) {
        return flying(player);
    }

    /**
     * Addons may opt into render-only directional pack poses without changing travel or collision.
     */
    public static boolean directionalArmorPose = false;

    public static boolean directionalRenderPose(Player player) {
        return directionalArmorPose && JetpackHandler.directionalFlight(player);
    }

    /**
     * Freeze the flight silhouette, not grounded walking/crouching or hover-mode head tracking.
     */
    public static void applyPlayerModelPose(HumanoidPose parts) {
        if (!flying(parts.player())) return;
        parts.body().resetPose();
        parts.leftArm().resetPose();
        parts.rightArm().resetPose();
        parts.leftLeg().resetPose();
        parts.rightLeg().resetPose();
        float headPitch = parts.head().xRot;
        float headYaw = parts.head().yRot;
        parts.head().resetPose();
        boolean hover = JetpackClientBridge.state(parts.player()).hover();
        parts.head().xRot = hover ? headPitch : -(float) Math.PI / 4F;
        parts.head().yRot = hover ? headYaw : 0F;
    }

    /**
     * The old third-person camera offset, used only for regular glider flight.
     */
    public static boolean lowerThirdPersonCamera(Player player) {
        JetpackClientBridge.State state = JetpackClientBridge.state(player);
        return GLIDER.equals(kind(player)) && active(state) && !state.hover() && !player.onGround()
                && !player.isFallFlying();
    }

    private static JetpackGradient gradient(String fuel) {
        if (fuel != null && fuel.toUpperCase().contains("REFORM")) {
            return new JetpackGradient(new float[]{1F, .95F, .85F, 1F, 0F},
                    new float[]{.55F, .75F, 1F, 1F, .15F},
                    new float[]{.25F, .55F, .9F, 1F, .4F},
                    new float[]{.15F, .3F, .7F, 1F, .8F},
                    new float[]{.1F, .05F, .2F, 0F, 1F});
        }
        if (fuel != null && fuel.toUpperCase().contains("BALEFIRE")) {
            return new JetpackGradient(new float[]{1F, .95F, .279F, 1F, 0F}, new float[]{1F, .9F, .1F, 1F, .122F},
                    new float[]{.013F, 1F, .068F, 1F, .389F}, new float[]{.2F, 1F, .3F, 1F, .891F},
                    new float[]{0F, 1F, .4F, 0F, 1F});
        }
        if (fuel != null && fuel.toUpperCase().contains("NITAN")) {
            return new JetpackGradient(new float[]{.845F, .779F, 1F, 1F, 0F}, new float[]{1F, .3F, 1F, 1F, .122F},
                    new float[]{.7F, .4F, 1F, 1F, .389F}, new float[]{.35F, .2F, 1F, 1F, .891F},
                    new float[]{.1F, .1F, 1F, 0F, 1F});
        }
        return new JetpackGradient(new float[]{1F, .918F, .882F, 1F, 0F}, new float[]{.887F, 1F, 0F, 1F, .177F},
                new float[]{1F, .19F, 0F, 1F, .336F}, new float[]{1F, .14F, 0F, 1F, .85F},
                new float[]{1F, .14F, 0F, 0F, 1F});
    }

    private static float[] brightnessColor(String fuel) {
        if (fuel != null && fuel.toUpperCase().contains("BALEFIRE")) return new float[]{.4F, 1F, .7F};
        if (fuel != null && fuel.toUpperCase().contains("NITAN")) return new float[]{1F, .5F, 1F};
        if (fuel != null && fuel.toUpperCase().contains("REFORM")) return new float[]{.55F, .75F, .95F};
        return new float[]{1F, .6F, .5F};
    }

    public static int fuelColor(String fuel) {
        float[] color = brightnessColor(fuel);
        return 0xFF000000 | ((int) (color[0] * 255F) << 16) | ((int) (color[1] * 255F) << 8)
                | (int) (color[2] * 255F);
    }

    private static final class Effects {
        private final List<ParticleRocketPlasma> plasma = new ArrayList<>();
        private final List<ParticleFakeBrightness> brightness = new ArrayList<>();
        private final List<ParticleHeatDistortion> heat = new ArrayList<>();
        private final JetpackTrail[] trails = new JetpackTrail[2];
        private long lastTrailCaptureTick = Long.MIN_VALUE;

        private void clear() {
            plasma.clear();
            brightness.clear();
            heat.clear();
            trails[0] = null;
            trails[1] = null;
        }

        private boolean empty() {
            return heat.isEmpty() && plasma.isEmpty() && brightness.isEmpty() && trails[0] == null && trails[1] == null;
        }
    }

    private static final class GliderAnimation {
        private final boolean opening;
        private final long start;
        private final long initialElapsed;

        private GliderAnimation(boolean opening, long elapsed) {
            this(opening, 0L, elapsed);
        }

        private GliderAnimation(boolean opening, long start, long initialElapsed) {
            this.opening = opening;
            this.start = start;
            this.initialElapsed = initialElapsed;
        }

        private boolean opening() {
            return opening;
        }

        private long elapsed() {
            return initialElapsed;
        }
    }
}
