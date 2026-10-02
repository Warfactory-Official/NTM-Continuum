package com.hbm.client.crucible;

import com.hbm.items.weapon.ItemCrucible;
import com.hbm.items.weapon.ItemSwordCutter;
import com.hbm.packet.CrucibleAnimPayload;
import com.hbm.packet.CrucibleClickPayload;
import com.hbm.packet.MobSlicerPayload;
import com.hbm.packet.SpecialDeathPayload;
import com.hbm.physics.V3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.EquipProgress;
import dev.golem.core.client.Frames;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.render.Bloom;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.LineRenderTypes;
import dev.golem.core.render.ScreenDistortion;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The client half of old-hbm's sword cutters and crucible, in one place:
 *
 * <ul>
 *   <li>{@code ItemSwordCutter}'s client statics ({@code startPos}, {@code clicked}, {@code canClick},
 *       {@code planeNormal}, {@code prevAngle}, {@code yaw}/{@code pitch}) and its right-click/swing halves;</li>
 *   <li>{@code HbmAnimations.hotbar} for these items: per slot either a plain bus animation or a
 *       {@code BlenderAnimation} (the Collada deploy, held on its last frame) with an optional bus over it, cleared
 *       each frame as {@code RenderGameOverlayEvent} did;</li>
 *   <li>{@code ModEventHandlerClient}'s cutter parts: the rotation clamp while drawing a cut ({@code renderTick}), the
 *       guide line and plane normal ({@code renderWorld}), and the aux particle tick;</li>
 *   <li>{@code ItemCrucible.updateClient} (the AuxButton 1000 state and the lightning) and the first-person draw of
 *       {@code ItemRenderCrucible}.</li>
 * </ul>
 */
public final class CrucibleClient implements ItemSwordCutter.Client {

    private static final Identifier HILT = Ids.of("hbm", "textures/models/weapons/crucible_hilt.png");
    private static final Identifier GUARD = Ids.of("hbm", "textures/models/weapons/crucible_guard.png");
    private static final Identifier BLADE = Ids.of("hbm", "textures/models/weapons/crucible_blade.png");
    private static final Identifier BLADE_BLOOM = Ids.of("hbm", "textures/models/weapons/crucible_blade_bloom.png");
    private static final Identifier NOISE = Ids.of("hbm", "textures/misc/noise_2.png");
    /** Only the harness's CPU reference draws with it: a flat noise, so the shader adds no displacement of its own. */
    private static final Identifier NOISE_FLAT = Ids.of("hbm", "textures/misc/noise_flat.png");
    private static final String LIGHTNING_SHADER = "hbm:crucible_lightning";

    // ---- ItemSwordCutter statics ----
    static @Nullable Vec3 startPos;
    static boolean clicked;
    static boolean canClick = true;
    static @Nullable Vec3 planeNormal;
    static double prevAngle;
    static float yaw, pitch;
    static final float MAX_DYAW = 120;
    static final float MAX_DPITCH = 120;

    /** {@code HbmAnimations.hotbar} for the cutters: nine hotbar slots and the off hand (9). */
    static final Anim[] HOTBAR = new Anim[10];

    /** One slot of {@code HbmAnimations.hotbar}. */
    static final class Anim {
        final Item key;
        long startMillis;
        @Nullable BusAnimation animation;
        /** A {@code BlenderAnimation}: the deploy's wrapper, with its own (mutable) start time. */
        final boolean blender;
        long wrapperStart;
        int prevFrame;

        Anim(Item key, long startMillis, @Nullable BusAnimation animation, boolean blender) {
            this.key = key;
            this.startMillis = startMillis;
            this.animation = animation;
            this.blender = blender;
            this.wrapperStart = startMillis;
        }
    }

    private static final CrucibleClient INSTANCE = new CrucibleClient();
    private static boolean lastClickSent;
    private static boolean clickSentOnce;

    private CrucibleClient() {}

    /** Client setup: the item hook, ticks and the per-frame pass. */
    public static void register() {
        ItemSwordCutter.client = INSTANCE;
        ClientTicks.onEndTick(CrucibleClient::clientTick);
        WorldEffects.init();
        WorldEffects.onBegin(camera -> frame());
        dev.golem.core.client.ClientResourceReloads.register(com.hbm.Constants.MOD_ID, "crucible_equip", () -> {
            CrucibleEquipModel.reset();
            CrucibleLightning.CpuReference.reset();
        });
        SlicedMobs.register();
        com.hbm.particle.helper.HbmEffectNT.VanillaBurst_BlockDust.setHandler(CrucibleClient::blockDustBurst);
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_crucible", (mc, params) -> {
            harnessControl(mc, params);
            return harnessQuery(mc);
        });
    }

    /** Harness only: draw the lightning through the CPU reference instead of the GPU shader. */
    static boolean lightningCpu;
    /** Harness only: leave out the blade's bloom and distortion passes (animated), to compare lightning pixels. */
    static boolean isolate;
    /** Harness only: the last End fight snapshot (taken on the server thread). */
    private static volatile com.google.gson.JsonObject endFight = new com.google.gson.JsonObject();

    /** Server thread: the fight's state and the podium's egg/portal blocks, reading only loaded chunks. */
    private static com.google.gson.JsonObject endFightSnapshot(net.minecraft.server.level.ServerLevel end) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        int alive = 0;
        for (var dragon : end.getDragons()) if (dragon.isAlive()) alive++;
        json.addProperty("dragons", alive);
        var fight = end.getDragonFight();
        json.addProperty("fight", fight != null);
        if (fight != null) json.addProperty("previously_killed", fight.hasPreviouslyKilledDragon());
        int eggs = 0, portal = 0;
        boolean loaded = true;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            net.minecraft.core.BlockPos column = new net.minecraft.core.BlockPos(x, 64, z);
            if (!end.hasChunkAt(column)) { loaded = false; continue; }
            for (int y = 30; y <= 110; y++) {
                var state = end.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                if (state.is(net.minecraft.world.level.block.Blocks.DRAGON_EGG)) eggs++;
                else if (state.is(net.minecraft.world.level.block.Blocks.END_PORTAL)) portal++;
            }
        }
        json.addProperty("podium_loaded", loaded);
        json.addProperty("eggs", eggs);
        json.addProperty("end_portal", portal);
        return json;
    }

    /**
     * The query's controls: {@code lightning_mode} {@code cpu}/{@code gpu}; {@code lightning_freeze} true replaces the aux
     * particles by one fixed bolt (seed 4815162342, age 4, from (0, 0.05, 0.6) along -z) and stops ticking and spawning,
     * false resumes.
     */
    private static void harnessControl(Minecraft mc, com.google.gson.JsonObject params) {
        if (params.has("lightning_mode")) lightningCpu = "cpu".equals(params.get("lightning_mode").getAsString());
        if (params.has("lightning_clear")) FirstPersonParticles.PARTICLES.clear();
        if (params.has("isolate")) isolate = params.get("isolate").getAsBoolean();
        // End fight check (single player): cut every dragon in the End through the slicer's kill, and/or snapshot the
        // fight (dragons, previously killed, egg and exit-portal blocks around the podium) for the next query
        if (params.has("slice_dragons") || params.has("end_fight_probe")) {
            boolean slice = params.has("slice_dragons");
            var server = mc.getSingleplayerServer();
            if (server != null) server.execute(() -> {
                net.minecraft.server.level.ServerLevel end = server.getLevel(net.minecraft.world.level.Level.END);
                if (end == null) return;
                if (slice) {
                    for (var dragon : List.copyOf(end.getDragons())) {
                        com.hbm.items.weapon.MobSlicer.kill(dragon,
                                com.hbm.lib.ModDamageSources.source(end, com.hbm.lib.ModDamageSources.CRUCIBLE));
                    }
                }
                endFight = endFightSnapshot(end);
            });
        }
        if (params.has("lightning_freeze")) {
            boolean freeze = params.get("lightning_freeze").getAsBoolean();
            FirstPersonParticles.frozen = freeze;
            FirstPersonParticles.PARTICLES.clear();
            if (freeze) {
                FirstPersonParticles.Lightning bolt = new FirstPersonParticles.Lightning(4815162342L, false, 0, 0.05, 0.6)
                        .lifetime(10);
                bolt.age = 4;
                FirstPersonParticles.PARTICLES.add(bolt);
            }
        }
    }

    /**
     * The {@code hbm_crucible} harness query: the held crucible's charge and damage, the slot's animation (deploy
     * elapsed, bus playing, the swing buses now), the cut being drawn, the aux particles, the hand's equip progress and
     * the cut-mob counters. Inert unless the harness runs.
     */
    private static com.google.gson.JsonObject harnessQuery(Minecraft mc) {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        LocalPlayer player = mc.player;
        if (player == null) return json;
        ItemStack held = player.getMainHandItem();
        json.addProperty("held_crucible", held.getItem() instanceof ItemCrucible);
        json.addProperty("charges", held.getItem() instanceof ItemCrucible ? ItemCrucible.getCharges(held) : -1);
        json.addProperty("damage", held.getDamageValue());
        Anim anim = relevant(InteractionHand.MAIN_HAND);
        long now = System.currentTimeMillis();
        json.addProperty("blender", anim != null && anim.blender);
        json.addProperty("deploy_ms", anim != null && anim.blender ? now - anim.wrapperStart : -1);
        json.addProperty("bus", anim != null && anim.animation != null);
        json.addProperty("swing_rot_x", transformation("SWING_ROT", InteractionHand.MAIN_HAND)[0]);
        json.addProperty("swing_x", transformation("SWING", InteractionHand.MAIN_HAND)[0]);
        json.addProperty("clicked", clicked);
        json.addProperty("plane_normal", planeNormal == null ? "" : planeNormal.toString());
        json.addProperty("can_click", canClick);
        int sparks = 0, bolts = 0;
        for (FirstPersonParticles.Particle p : FirstPersonParticles.PARTICLES) {
            if (p instanceof FirstPersonParticles.Spark) sparks++;
            else bolts++;
        }
        json.addProperty("fp_sparks", sparks);
        json.addProperty("fp_lightning", bolts);
        json.addProperty("equip", EquipProgress.mainHand());
        json.addProperty("equip_prev", EquipProgress.previousMainHand());
        json.addProperty("swinging", player.swinging);
        json.addProperty("sliced_spawned", SlicedMobs.slicedSpawned);
        json.addProperty("blood_spawned", SlicedMobs.bloodSpawned);
        json.addProperty("sparks_spawned", SlicedMobs.sparksSpawned);
        json.addProperty("decals_spawned", SlicedMobs.decalsSpawned);
        json.addProperty("last_mesh_cubes", SlicedMobs.lastMeshCubes);
        int[] live = SlicedMobs.live();
        json.addProperty("sliced_live", live[0]);
        json.addProperty("blood_live", live[1]);
        json.addProperty("sparks_live", live[2]);
        json.addProperty("decals_live", live[3]);
        json.add("end_fight", endFight);
        return json;
    }

    public static void registerPacketHandlers() {
        GolemNetwork.registerPlayToClient(CrucibleAnimPayload.CODEC, (payload, context) -> animation(payload.mode()));
        GolemNetwork.registerPlayToClient(SpecialDeathPayload.CODEC, (payload, context) -> SlicedMobs.handle(payload));
    }

    // ------------------------------------------------------------------ animations

    private static int slot(Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) return 9;
        int slot = dev.golem.core.world.Compat.selectedSlot(player);
        return slot >= 0 && slot < 9 ? slot : Math.abs(slot) % 9;
    }

    /** {@code getRelevantAnim(hand)}: the slot's animation if it was started for the item now held. */
    static @Nullable Anim relevant(InteractionHand hand) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return null;
        Anim anim = HOTBAR[slot(player, hand)];
        return anim != null && anim.key == stack.getItem() ? anim : null;
    }

    /** {@code getRelevantTransformation(bus, hand)}; identity when nothing plays. */
    static double[] transformation(String bus, InteractionHand hand) {
        Anim anim = relevant(hand);
        if (anim != null && anim.animation != null) {
            double[] t = anim.animation.getTimedTransformation(bus, (int) (System.currentTimeMillis() - anim.startMillis));
            if (t != null) return t;
        }
        return new double[]{0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 1, 2};
    }

    /** The {@code Anim} effect's {@code "equip"} and {@code "cSwing"} modes (main hand). */
    static void animation(byte mode) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        int slot = slot(player, InteractionHand.MAIN_HAND);
        Item held = player.getMainHandItem().getItem();
        long now = System.currentTimeMillis();
        if (mode == CrucibleAnimPayload.EQUIP) {
            HOTBAR[slot] = new Anim(held, now, null, true);
        } else if (mode == CrucibleAnimPayload.CSWING) {
            if (transformation("SWING_ROT", InteractionHand.MAIN_HAND)[0] == 0) {
                int offset = player.level().getRandom().nextInt(80) - 20;
                BusAnimation animation = new BusAnimation()
                        .addBus("SWING_ROT", new BusAnimationSequence()
                                .addPos(60 - offset, 60 - offset, -55, 75)
                                .addPos(60 + offset, 60 - offset, -45, 150)
                                .addPos(0, 0, 0, 500))
                        .addBus("SWING_TRANS", new BusAnimationSequence()
                                .addPos(-0, -10, 0, 75)
                                .addPos(0, -10, 0, 150)
                                .addPos(0, 0, 0, 500));
                overlay(slot, held, animation, now);
            }
        }
    }

    /** The {@code "swing"} mode, played locally by {@code swingClient}: the cutting swing on the blade. */
    private static void cuttingSwing(Player player) {
        BusAnimation animation = new BusAnimation()
                .addBus("SWING", new BusAnimationSequence()
                        .addPos(120, 0, 0, 150)
                        .addPos(0, 0, 0, 500));
        overlay(slot(player, InteractionHand.MAIN_HAND), player.getMainHandItem().getItem(), animation,
                System.currentTimeMillis());
    }

    /** A bus over a Blender animation (restarting its clock), or a plain animation in the slot. */
    private static void overlay(int slot, Item held, BusAnimation animation, long now) {
        Anim current = HOTBAR[slot];
        if (current != null && current.blender) {
            current.animation = animation;
            current.startMillis = now;
        } else {
            HOTBAR[slot] = new Anim(held, now, animation, false);
        }
    }

    /** {@code RenderGameOverlayEvent} ALL: drop finished buses, and a deploy whose item is no longer held. */
    private static void cleanUp(LocalPlayer player) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < HOTBAR.length; i++) {
            Anim anim = HOTBAR[i];
            if (anim == null) continue;
            long time = now - anim.startMillis;
            if (anim.blender) {
                InteractionHand hand = i < 9 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                if (player.getItemInHand(hand).getItem() != anim.key) HOTBAR[i] = null;
                if (anim.animation != null && time > anim.animation.getDuration()) anim.animation = null;
            } else if (anim.animation == null || time > anim.animation.getDuration()) {
                HOTBAR[i] = null;
            }
        }
    }

    // ------------------------------------------------------------------ ItemSwordCutter.Client

    @Override
    public void rightClick(Player player) {
        if (player == Minecraft.getInstance().player && !clicked && canClick) {
            startPos = player.getViewVector(1F);
            planeNormal = null;
            clicked = true;
            canClick = false;
            yaw = player.getYRot();
            pitch = player.getXRot();
        }
    }

    @Override
    public void swing(LivingEntity entity, ItemStack stack) {
        if (clicked && planeNormal != null && startPos != null && entity instanceof Player player) {
            Vec3 pos = startPos.add(entity.getEyePosition(1F));
            byte tex = stack.getItem() instanceof ItemSwordCutter cutter ? cutter.getTexId() : 0;
            GolemNetwork.sendToServer(MobSlicerPayload.CODEC,
                    new MobSlicerPayload(pos.x, pos.y, pos.z, planeNormal.x, planeNormal.y, planeNormal.z, tex));
            planeNormal = null;
            clicked = false;
            cuttingSwing(player);
        }
    }

    @Override
    public boolean busAnimationPlaying(Player player) {
        Anim anim = relevant(InteractionHand.MAIN_HAND);
        return anim != null && anim.animation != null;
    }

    // ------------------------------------------------------------------ ticks and frames

    private static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        FirstPersonParticles.tick();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            clicked = false;
            planeNormal = null;
            clickSentOnce = false;
            return;
        }
        // MouseEvent: releasing the right button allows the next cut
        if (!mc.options.keyUse.isDown()) canClick = true;
        updateClient(player);
    }

    /** {@code ItemCrucible.onUpdate} on the client, for the selected, charged crucible. */
    private static void updateClient(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemCrucible) || ItemCrucible.getCharges(stack) <= 0) {
            clickSentOnce = false;
            return;
        }
        int slot = slot(player, InteractionHand.MAIN_HAND);
        Anim anim = HOTBAR[slot];
        boolean special = clicked || (anim != null && anim.animation != null && anim.animation.getBus("SWING") != null);
        // old-hbm sent AuxButton 1000 every tick; the server only keeps the last value, so a change is enough
        if (!clickSentOnce || special != lastClickSent) {
            GolemNetwork.sendToServer(CrucibleClickPayload.CODEC, new CrucibleClickPayload(special));
            lastClickSent = special;
            clickSentOnce = true;
        }
        boolean flag = anim != null && anim.blender
                && System.currentTimeMillis() - anim.wrapperStart > CrucibleEquipModel.get().length * 0.7F;
        var level = player.level();
        if (flag && !FirstPersonParticles.frozen && level.getRandom().nextInt(20) == 0) {
            FirstPersonParticles.PARTICLES.add(new FirstPersonParticles.Lightning(level.getGameTime(), level.getRandom(),
                    0, (level.getRandom().nextFloat() - 0.5F) * 0.2F, 0.7F - level.getRandom().nextFloat() * 0.25F)
                    .lifetime(10));
        }
    }

    /** Once per world frame, before the hand: cleanup, the cut's clamp and guide line, and the deploy's clock. */
    private static void frame() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        cleanUp(player);
        if (player.getMainHandItem().getItem() instanceof ItemSwordCutter && clicked) clampRotation(player);
        guideLine(player);
        firstPersonFrame(player);
    }

    /** {@code renderTick}: while drawing, the view may turn at most 120 degrees from where the cut began. */
    private static void clampRotation(LocalPlayer player) {
        float oldPitch = player.getXRot();
        float oldYaw = player.getYRot();
        float y = player.getYRot() - yaw;
        if (y > MAX_DYAW) player.setYRot(yaw + MAX_DYAW);
        if (y < -MAX_DYAW) player.setYRot(yaw - MAX_DYAW);
        float p = player.getXRot() - pitch;
        if (p > MAX_DPITCH) player.setXRot(pitch + MAX_DPITCH);
        if (p < -MAX_DPITCH) player.setXRot(pitch - MAX_DPITCH);
        player.yRotO += player.getYRot() - oldYaw;
        player.xRotO += player.getXRot() - oldPitch;
    }

    /**
     * {@code renderWorld}: while the use key is held after a cut began, a black line from the start direction to the
     * current one (both one block from the eye), and the cut's plane normal {@code start x look}; letting go, or an
     * empty crucible, abandons the cut.
     */
    private static void guideLine(LocalPlayer player) {
        if (!(player.getMainHandItem().getItem() instanceof ItemSwordCutter) || !clicked) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyUse.isDown() && startPos != null) {
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof ItemCrucible && ItemCrucible.getCharges(held) == 0)) {
                Vec3 pos1 = startPos;
                Vec3 pos2 = player.getViewVector(Frames.partialTick());
                Vec3 norm = startPos.cross(pos2);
                PoseStack pose = new PoseStack();
                WorldEffects.submit(pose, LineRenderTypes.lines(1F), 0, (p, buffer) -> {
                    Vec3 d = pos2.subtract(pos1).normalize();
                    Vertexes.line(buffer, p, (float) pos1.x, (float) pos1.y, (float) pos1.z, 0xFF000000,
                            (float) d.x, (float) d.y, (float) d.z, 1F);
                    Vertexes.line(buffer, p, (float) pos2.x, (float) pos2.y, (float) pos2.z, 0xFF000000,
                            (float) d.x, (float) d.y, (float) d.z, 1F);
                });
                if (norm.lengthSqr() > 0.001F) planeNormal = norm.normalize();
            } else {
                clicked = false;
                planeNormal = null;
            }
        } else {
            clicked = false;
            planeNormal = null;
        }
    }

    /**
     * The per-frame side effects of {@code ItemRenderCrucible}'s first-person branch: the hand held fully raised
     * ({@code RenderSpecificHandEvent} drew the crucible at equip progress 0), the deploy wrapper's STAY and empty
     * clocks, and the 50 sparks for each of keyframes 13..20 the blade passes.
     */
    private static void firstPersonFrame(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemCrucible)) return;
        if (!EquipProgress.mainHandSwitching()) {
            EquipProgress.pinMainHand(1.0F, 1.0F);
        } else if (EquipProgress.mainHand() < EquipProgress.previousMainHand()) {
            EquipProgress.pinMainHand(0.0F, 0.0F);
        }
        Anim w = relevant(InteractionHand.MAIN_HAND);
        if (w == null || !w.blender) return;
        long now = System.currentTimeMillis();
        CrucibleEquipModel model = CrucibleEquipModel.get();
        if (ItemCrucible.getCharges(stack) == 0) w.wrapperStart = now - 400;
        if (now - w.wrapperStart > model.length) w.wrapperStart = now - model.length; // EndType.STAY
        CrucibleEquipModel.Playhead head = model.playhead(now - w.wrapperStart);
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson() && !FirstPersonParticles.frozen) {
            int currentFrame = head.first();
            int prevFrame = w.prevFrame;
            if (currentFrame <= 20) {
                for (int f = 13; f <= 20; f++) {
                    if (currentFrame >= f && prevFrame < f) {
                        var rand = player.level().getRandom();
                        for (int i = 0; i < 50; i++) {
                            FirstPersonParticles.PARTICLES.add(new FirstPersonParticles.Spark(2, 0.0025F, 0,
                                    (rand.nextFloat() - 0.5F) * 0.2F, 0.6F - rand.nextFloat() * 0.75F, 0, 0,
                                    -0.01F * rand.nextFloat()).lifetime(6 + (int) rand.nextGaussian() * 10));
                        }
                    }
                }
            }
            w.prevFrame = currentFrame;
        }
    }

    // ------------------------------------------------------------------ first-person draw

    /** {@code GlStateManager.color(1, 0.5, 0.5, 1)} for the sparks' first pass and {@code (1, 0.2, 0.2)} for bloom. */
    private static final int SPARK_COLOR = 0xFFFF7F7F;
    private static final int SPARK_BLOOM_COLOR = 0xFFFF3333;
    /** {@code color(1, 1, 1, 0.7F)} on the blade. */
    private static final int BLADE_TINT = (Math.round(0.7F * 255F) << 24) | 0xFFFFFF;
    /** {@code distort(0.5F, ...)}: the amount rides the vertex colour's red. */
    private static final int DISTORTION_AMOUNT = 0xFF000000 | (Math.round(0.5F * 255F) << 16);

    /** {@code ItemRenderCrucible.renderByItem} FIRST_PERSON_RIGHT_HAND, in the teisr frame's space. */
    public static void renderFirstPerson(ItemStack stack, PoseStack pose, SubmitSink sink, int light, int overlay) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean depleted = ItemCrucible.getCharges(stack) == 0;
        pose.scale(5, 5, 5);
        pose.translate(0.2, -1.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.mulPose(Axis.XP.rotationDegrees(-20));
        pose.mulPose(Axis.ZP.rotationDegrees(5));

        Anim anim = relevant(InteractionHand.MAIN_HAND);
        if (startPos != null && anim != null && (clicked || anim.animation != null)) {
            double[] swingRot = transformation("SWING", InteractionHand.MAIN_HAND);
            V3 v = new V3(startPos.x, startPos.y, startPos.z)
                    .rotateYaw((float) Math.toRadians(player.getYRot() + 180))
                    .rotatePitch((float) Math.toRadians(-player.getXRot()));
            double angle = Math.toDegrees(Math.atan2(v.y(), v.x())) - 80;
            float oX = 0.4F;
            float oY = -1.55F;
            float oZ = 0;
            boolean flag = false;
            if (anim.animation != null) {
                angle = prevAngle;
                long time = System.currentTimeMillis() - anim.startMillis;
                if (anim.animation.getDuration() - time < 400) flag = true;
            } else {
                prevAngle = angle;
            }
            if (!flag) {
                pose.translate(0.3F, -0.1F, 0);
                pose.translate(-oX, -oY, -oZ);
                pose.mulPose(Axis.ZP.rotationDegrees((float) -angle));
                pose.translate(oX, oY, oZ);
                pose.translate(0F, -0.2F, 0F);
                pose.mulPose(Axis.YP.rotationDegrees(10));
            }
            pose.mulPose(Axis.XP.rotationDegrees((float) swingRot[0]));
        }

        if (anim == null || !anim.blender) return; // AnimationWrapper.EMPTY: nothing is drawn
        if (depleted) pose.translate(-0.1, -0.25, 0.1);
        double[] sRot = transformation("SWING_ROT", InteractionHand.MAIN_HAND);
        double[] sTrans = transformation("SWING_TRANS", InteractionHand.MAIN_HAND);
        pose.translate(sTrans[0], sTrans[1], sTrans[2]);
        pose.mulPose(Axis.XP.rotationDegrees((float) sRot[0]));
        pose.mulPose(Axis.ZP.rotationDegrees((float) sRot[2]));
        pose.mulPose(Axis.YP.rotationDegrees((float) sRot[1]));

        CrucibleEquipModel model = CrucibleEquipModel.get();
        CrucibleEquipModel.Playhead head = model.playhead(System.currentTimeMillis() - anim.wrapperStart);
        Identifier skin = dev.golem.core.client.PlayerSkins.texture((AbstractClientPlayer) player);
        float partial = Frames.partialTick();
        model.walk(head, pose, (name, nodePose) -> {
            if (name.startsWith("Guard")) {
                part(sink, nodePose, model, CompatRenderTypes.entityCutoutCull(GUARD), name, light, overlay);
            } else if (name.equals("Hilt")) {
                part(sink, nodePose, model, CompatRenderTypes.entityCutoutCull(HILT), name, light, overlay);
            } else if (name.equals("Blade")) {
                blade(sink, nodePose, model, head.diffN(), partial, overlay);
            } else {
                part(sink, nodePose, model, CompatRenderTypes.entityCutoutCull(skin), name, light, overlay);
            }
        });
    }

    private static void part(SubmitSink sink, PoseStack pose, CrucibleEquipModel model,
                             net.minecraft.client.renderer.rendertype.RenderType type, String name, int light, int overlay) {
        sink.submitCustomGeometry(pose, type, (p, buffer) -> model.model.renderPart(name, p, buffer, light, overlay));
    }

    /**
     * The blade node's {@code onRender}/{@code postRender}: the aux particles (sparks textured with the guard texture
     * still bound, {@code SRC_ALPHA/ONE}, then again in the bloom colour into the bloom layer), the clip plane
     * {@code (0, 0, 1, remap(diffN, 0.6, 0.7, -0.7, 0.7))}, the blade at alpha 0.7 fullbright, its bloom, and above
     * 60% of the deploy the heat-distortion shell scaled {@code (1.15, 1.15, 1.05)}. The clip plane is set before
     * that scale, so the shell is clipped against the unscaled plane.
     */
    private static void blade(SubmitSink sink, PoseStack pose, CrucibleEquipModel model, float diffN, float partial,
                              int overlay) {
        Matrix4f m = new Matrix4f(pose.last().pose());
        Vector4f eye = new Matrix4f(m).invert().transform(new Vector4f(0, 0, 0, 1));
        V3 playerPos = new V3(eye.x, eye.y, eye.z);
        List<FirstPersonParticles.Particle> particles = new ArrayList<>(FirstPersonParticles.PARTICLES);
        if (!particles.isEmpty()) {
            sink.submitCustomGeometry(pose, CompatRenderTypes.plasmaGlow(GUARD), (p, buffer) -> {
                for (FirstPersonParticles.Particle particle : particles) {
                    if (particle instanceof FirstPersonParticles.Spark spark) spark.render(p, buffer, playerPos, partial, SPARK_COLOR);
                }
            });
            // the crucible_lightning shader pair, SRC_ALPHA / ONE, in the blade's space (its offsets are object-space)
            boolean cpu = lightningCpu;
            dev.golem.core.render.NoiseDisplacedRenderType.localMatrix(m);
            sink.submitCustomGeometry(pose,
                    dev.golem.core.render.NoiseDisplacedRenderType.of(LIGHTNING_SHADER, cpu ? NOISE_FLAT : NOISE, false), (p, buffer) -> {
                for (FirstPersonParticles.Particle particle : particles) {
                    if (particle instanceof FirstPersonParticles.Lightning bolt) {
                        CrucibleLightning.render(bolt.node, playerPos, 0.0015F, bolt.shaderTime(), p, buffer, cpu);
                    }
                }
            });
            SubmitSink layers = sink.order(2);
            if (!isolate) layers.submitCustomGeometry(pose, Bloom.of(GUARD), (p, buffer) -> {
                for (FirstPersonParticles.Particle particle : particles) {
                    if (particle instanceof FirstPersonParticles.Spark spark) spark.render(p, buffer, playerPos, partial, SPARK_BLOOM_COLOR);
                }
            });
            if (!isolate) layers.submitCustomGeometry(pose,
                    dev.golem.core.render.NoiseDisplacedRenderType.of(LIGHTNING_SHADER, cpu ? NOISE_FLAT : NOISE, true), (p, buffer) -> {
                for (FirstPersonParticles.Particle particle : particles) {
                    if (particle instanceof FirstPersonParticles.Lightning bolt) {
                        CrucibleLightning.render(bolt.node, playerPos, 0.0015F, bolt.shaderTime(), p, buffer, cpu);
                    }
                }
            });
        }

        float d = remap(diffN, 0.6F, 0.7F, -0.7F, 0.7F);
        float[] blade = model.model.part("Blade").vertices();
        // order 1: the blade after the sparks and lightning (1.12 drew them in onRender, before the blade, so they show
        // through its 0.7 alpha); its bloom and shell after it (postRender)
        sink.order(1).submitCustomGeometry(pose, CompatRenderTypes.flatTranslucentCull(BLADE),
                (p, buffer) -> clippedBlade(p, buffer, blade, 1F, d, BLADE_TINT, overlay));
        SubmitSink layers = sink.order(2);
        if (isolate) return;
        layers.submitCustomGeometry(pose, Bloom.of(BLADE_BLOOM),
                (p, buffer) -> clippedBlade(p, buffer, blade, 1F, d, 0xFFFFFFFF, overlay));
        if (diffN > 0.6) {
            pose.pushPose();
            pose.scale(1.15F, 1.15F, 1.05F);
            layers.submitCustomGeometry(pose, ScreenDistortion.of(BLADE_BLOOM, NOISE),
                    (p, buffer) -> clippedBlade(p, buffer, blade, 1.05F, d, DISTORTION_AMOUNT, overlay));
            pose.popPose();
        }
    }

    private static float remap(float num, float min1, float max1, float min2, float max2) {
        return ((num - min1) / (max1 - min1)) * (max2 - min2) + min2;
    }

    /**
     * The blade's triangles clipped by {@code GL_CLIP_PLANE0 = (0, 0, 1, d)} in the blade node's space: kept where
     * {@code z * zScale + d >= 0} ({@code zScale} is the shell's own z scale, applied after the plane was set). Each
     * kept piece is a triangle, emitted as a degenerate quad.
     */
    private static void clippedBlade(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, float[] tri,
                                     float zScale, float d, int color, int overlay) {
        for (int t = 0; t + 24 <= tri.length; t += 24) {
            float[][] in = new float[3][];
            for (int k = 0; k < 3; k++) {
                int o = t + k * 8;
                in[k] = new float[]{tri[o], tri[o + 1], tri[o + 2], tri[o + 3], tri[o + 4], tri[o + 5], tri[o + 6], tri[o + 7]};
            }
            List<float[]> out = clip(in, zScale, d);
            for (int i = 1; i + 1 < out.size(); i++) {
                emit(pose, buffer, out.get(0), color, overlay);
                emit(pose, buffer, out.get(i), color, overlay);
                emit(pose, buffer, out.get(i + 1), color, overlay);
                emit(pose, buffer, out.get(i + 1), color, overlay);
            }
        }
    }

    /** Sutherland-Hodgman against one plane: the polygon's part with {@code z * zScale + d >= 0}. */
    private static List<float[]> clip(float[][] poly, float zScale, float d) {
        List<float[]> out = new ArrayList<>(4);
        for (int i = 0; i < poly.length; i++) {
            float[] a = poly[i];
            float[] b = poly[(i + 1) % poly.length];
            float da = a[2] * zScale + d;
            float db = b[2] * zScale + d;
            if (da >= 0) out.add(a);
            if ((da >= 0) != (db >= 0)) {
                float s = da / (da - db);
                float[] c = new float[8];
                for (int k = 0; k < 8; k++) c[k] = a[k] + (b[k] - a[k]) * s;
                out.add(c);
            }
        }
        return out;
    }

    private static void emit(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, float[] v, int color,
                             int overlay) {
        Vertexes.vertex(buffer, pose, v[0], v[1], v[2], color, v[3], v[4], overlay, 0xF000F0, v[5], v[6], v[7]);
    }

    // ------------------------------------------------------------------ the killing hit's dust

    /** old-hbm {@code VanillaBurst_BlockDust}: exact motion {@code gaussian * motion} (+0.2 up), life 50..99. */
    private static void blockDustBurst(net.minecraft.world.level.Level level, double x, double y, double z,
                                       net.minecraft.nbt.CompoundTag data) {
        if (!(level instanceof net.minecraft.client.multiplayer.ClientLevel)) return;
        double motion = dev.golem.core.data.Tags.getDoubleOr(data, "motion", 0D);
        int count = dev.golem.core.data.Tags.getIntOr(data, "count", 0);
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.byId(dev.golem.core.data.Tags.getIntOr(data, "block", 0));
        var option = new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                block.defaultBlockState());
        var rand = level.getRandom();
        for (int i = 0; i < count; i++) {
            double mX = rand.nextGaussian() * motion;
            double mY = rand.nextGaussian() * motion;
            double mZ = rand.nextGaussian() * motion;
            var particle = Minecraft.getInstance().particleEngine.createParticle(option, x, y, z, mX, mY + 0.2, mZ);
            if (particle == null) return;
            particle.setParticleSpeed(mX, mY + 0.2, mZ);
            particle.setLifetime(50 + rand.nextInt(50));
        }
    }
}
