package com.hbm.client.crucible;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.items.weapon.ItemSwordCutter;
import com.hbm.items.weapon.MobSlicer;
import com.hbm.lib.ModDamageSources;
import com.hbm.packet.CrucibleAnimPayload;
import com.hbm.packet.CrucibleClickPayload;
import com.hbm.packet.MobSlicerPayload;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.physics.V3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.client.AttackInput;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.EquipProgress;
import dev.golem.core.client.Frames;
import dev.golem.core.client.PlayerSkins;
import dev.golem.core.data.Tags;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.render.Bloom;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.LineRenderTypes;
import dev.golem.core.render.NoiseDisplacedRenderType;
import dev.golem.core.render.Poses;
import dev.golem.core.render.ScreenDistortion;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationSequence;
import dev.golem.core.render.display.DisplaySlot;
import dev.golem.core.render.display.Pose17;
import dev.golem.core.world.Compat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The client half of 1.12.2's sword cutters and crucible, in one place:
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

    // ItemSwordCutter statics
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
        // the left click that confirms a drawn cut (use key still held) only swings: no attack packet, no block breaking,
        // whatever is under the crosshair; the swing sends the cut (swingClient)
        AttackInput.consumeWhile(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            return player != null && INSTANCE.cutting(player);
        });
        WorldEffects.init();
        WorldEffects.onBegin(camera -> frame());
        ClientResourceReloads.register(Constants.MOD_ID, "crucible_equip", () -> {
            CrucibleEquipModel.reset();
            resetModelCaches();
            CrucibleLightning.CpuReference.reset();
        });
        SlicedMobs.register();
        HbmEffectNT.VanillaBurst_BlockDust.setHandler(CrucibleClient::blockDustBurst);
        HarnessQueryRegistry.register("hbm_crucible", (mc, params) -> {
            harnessControl(mc, params);
            return harnessQuery(mc);
        });
    }

    /** Harness only: draw the lightning through the CPU reference instead of the GPU shader. */
    static boolean lightningCpu;
    /** Harness only: leave out the blade's bloom and distortion passes (animated), to compare lightning pixels. */
    static boolean isolate;
    /** Harness only: the last End fight snapshot (taken on the server thread). */
    private static volatile JsonObject endFight = new JsonObject();
    /** Harness only: the 1.7 swing pinned at this many ms ({@code -1}: live), with this offset. */
    static int freezeSwingMs = -1, freezeSwingOffset;
    /** Harness only: draw the 1.7 renderer's first-person crucible (reference for the swing mapping). */
    static boolean reference17;
    /** Harness only: the last server-side swing snapshot of the local player. */
    private static volatile JsonObject serverSwing = new JsonObject();

    /** Server thread: the fight's state and the podium's egg/portal blocks, reading only loaded chunks. */
    private static JsonObject endFightSnapshot(ServerLevel end) {
        JsonObject json = new JsonObject();
        int alive = 0;
        for (var dragon : end.getDragons()) if (dragon.isAlive()) alive++;
        json.addProperty("dragons", alive);
        var fight = end.getDragonFight();
        json.addProperty("fight", fight != null);
        if (fight != null) json.addProperty("previously_killed", fight.hasPreviouslyKilledDragon());
        int eggs = 0, portal = 0;
        boolean loaded = true;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            BlockPos column = new BlockPos(x, 64, z);
            if (!end.hasChunkAt(column)) { loaded = false; continue; }
            for (int y = 30; y <= 110; y++) {
                var state = end.getBlockState(new BlockPos(x, y, z));
                if (state.is(Blocks.DRAGON_EGG)) eggs++;
                else if (state.is(Blocks.END_PORTAL)) portal++;
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
    private static void harnessControl(Minecraft mc, JsonObject params) {
        if (params.has("lightning_mode")) lightningCpu = "cpu".equals(params.get("lightning_mode").getAsString());
        if (params.has("lightning_clear")) FirstPersonParticles.PARTICLES.clear();
        if (params.has("isolate")) isolate = params.get("isolate").getAsBoolean();
        // End fight check (single player): cut every dragon in the End through the slicer's kill, and/or snapshot the
        // fight (dragons, previously killed, egg and exit-portal blocks around the podium) for the next query
        if (params.has("slice_dragons") || params.has("end_fight_probe")) {
            boolean slice = params.has("slice_dragons");
            var server = mc.getSingleplayerServer();
            if (server != null) server.execute(() -> {
                ServerLevel end = server.getLevel(Level.END);
                if (end == null) return;
                if (slice) {
                    for (var dragon : List.copyOf(end.getDragons())) {
                        MobSlicer.kill(dragon,
                                ModDamageSources.source(end, ModDamageSources.CRUCIBLE));
                    }
                }
                endFight = endFightSnapshot(end);
            });
        }
        // the server's view of this player's swing (single player): vanilla's swing state there means the swing was not
        // cancelled, so the server broadcast it to every tracking player
        if (params.has("server_swing_probe")) {
            var server = mc.getSingleplayerServer();
            UUID id = mc.player == null ? null : mc.player.getUUID();
            if (server != null && id != null) server.execute(() -> {
                var sp = server.getPlayerList().getPlayer(id);
                if (sp == null) return;
                JsonObject json = new JsonObject();
                json.addProperty("swinging", sp.swinging);
                json.addProperty("swing_time", sp.swingTime);
                serverSwing = json;
            });
        }
        // the 1.7 swing pinned at one moment (offset, ms), so a screenshot shows exactly that keyframe; "swing17_release"
        // goes back to the live bus. "reference17" draws the 1.7 OBJ crucible in its own 1.7 frame instead of the scene.
        if (params.has("swing17_ms")) {
            freezeSwingMs = params.get("swing17_ms").getAsInt();
            freezeSwingOffset = params.has("swing17_offset") ? params.get("swing17_offset").getAsInt() : 0;
        }
        if (params.has("swing17_release")) freezeSwingMs = -1;
        if (params.has("reference17")) reference17 = params.get("reference17").getAsBoolean();
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
    private static JsonObject harnessQuery(Minecraft mc) {
        JsonObject json = new JsonObject();
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
        json.add("server_swing", serverSwing);
        // the 1.7 swing: its bus values now, the time into it and the offset it was started with
        double[] rot = transformation("SWING_ROT", InteractionHand.MAIN_HAND);
        double[] trans = transformation("SWING_TRANS", InteractionHand.MAIN_HAND);
        JsonArray swing = new JsonArray();
        for (int i = 0; i < 3; i++) swing.add(rot[i]);
        for (int i = 0; i < 3; i++) swing.add(trans[i]);
        json.add("swing17", swing);
        json.addProperty("swing17_offset", lastSwingOffset);
        json.addProperty("swing17_ms", anim != null && anim.animation != null && anim.animation.getBus("SWING_ROT") != null
                ? now - anim.startMillis : -1);
        json.addProperty("attack_clicks_consumed", AttackInput.consumedClicks());
        json.addProperty("third_person_draws", thirdPersonDraws);
        json.addProperty("third_person_bolts", thirdPersonBoltDraws);
        int others = 0;
        for (List<FirstPersonParticles.Lightning> list : OTHER_BOLTS.values()) others += list.size();
        json.addProperty("other_bolts", others);
        thirdPersonDraws = 0;
        thirdPersonBoltDraws = 0;
        return json;
    }

    public static void registerPacketHandlers() {
        GolemNetwork.registerPlayToClient(CrucibleAnimPayload.CODEC, (payload, context) -> animation(payload.mode()));
    }

    // Animations

    private static int slot(Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND) return 9;
        int slot = Compat.selectedSlot(player);
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
                // The standard attack in first person is the 1.7 crucible's swing
                // (1.7 ItemCrucible.getAnimation(SWING)), not 1.12.2's cSwing re-tuning of it
                // (60 - offset, 60 - offset, -55 / trans 0, -10, 0). The cue, its condition and the sound stay 1.12.2's.
                int offset = player.level().getRandom().nextInt(80) - 20;
                lastSwingOffset = offset;
                overlay(slot, held, swing17(offset), now);
            }
        }
    }

    /** The offset of the last 1.7 swing started (harness: to recompute its keyframes). */
    static int lastSwingOffset;

    /**
     * 1.7 {@code ItemCrucible.getAnimation(ToolAnimation.SWING)}: {@code offset = itemRand.nextInt(80) - 20},
     * SWING_ROT {@code (90 - offset, 90 - offset, 35)} at 75 ms, {@code (90 + offset, 90 - offset, -45)} 150 ms later,
     * back to 0 over 500 ms; SWING_TRANS {@code (-3, 0, 0)}, {@code (8, 0, 0)}, {@code 0} on the same clock. The values are
     * the 1.7 OBJ crucible's own frame; {@link #renderFirstPerson} applies them there (see {@link #swingInScene}).
     */
    static BusAnimation swing17(int offset) {
        return new BusAnimation()
                .addBus("SWING_ROT", new BusAnimationSequence()
                        .addPos(90 - offset, 90 - offset, 35, 75)
                        .addPos(90 + offset, 90 - offset, -45, 150)
                        .addPos(0, 0, 0, 500))
                .addBus("SWING_TRANS", new BusAnimationSequence()
                        .addPos(-3, 0, 0, 75)
                        .addPos(8, 0, 0, 150)
                        .addPos(0, 0, 0, 500));
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

    /** {@code RenderGameOverlayEvent} ALL: drop finished buses, and a deploy whose item is not held. */
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

    // ItemSwordCutter.Client

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
    public boolean cutting(Player player) {
        return player == Minecraft.getInstance().player && clicked
                && player.getMainHandItem().getItem() instanceof ItemSwordCutter;
    }

    @Override
    public boolean busAnimationPlaying(Player player) {
        Anim anim = relevant(InteractionHand.MAIN_HAND);
        return anim != null && anim.animation != null;
    }

    // Ticks and frames

    private static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            clicked = false;
            planeNormal = null;
            clickSentOnce = false;
        } else {
            // MouseEvent: releasing the right button allows the next cut
            if (!mc.options.keyUse.isDown()) canClick = true;
            updateClient(player);
        }
        // 1.12 order: ItemCrucible.onUpdate (updateClient) runs in the world tick and spawns a bolt at age 0, then
        // ClientTickEvent END ticks firstPersonAuxParticles (ModEventHandlerClient.java:326-337), so a bolt is first drawn
        // at age 1 and drawn for ten ticks (ages 1..10), not eleven
        FirstPersonParticles.tick();
        tickOtherBolts(mc);
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
        // 1.12.2 sent AuxButton 1000 every tick; the server only keeps the last value, so a change is enough
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
        nextSlot = 1;
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
     * The per-frame side effects of {@code ItemRenderCrucible}'s first-person branch: the deploy wrapper's STAY and empty
     * clocks, and the 50 sparks for each of keyframes 13..20 the blade passes.
     */
    private static void firstPersonFrame(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ItemCrucible)) return;
        // the hand is held raised by Core at the hand renderer's tick (ItemCrucible.compatHoldsRaised and
        // compatIsSameHeldItem); pinning it from here was one tick late, so the first frame of each tick dipped
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

    // First-person draw

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
        // 1.7 ItemRenderCrucible: player.isSwingInProgress = false in its first-person branch. The swing itself is
        // vanilla's again (ItemCrucible does not cancel it, so F5 and other players see the arm swing); in first person
        // the 1.7 swing bus is the attack animation, so the vanilla arm swing never advances here. The swing starts with
        // swingTime -1 and attackAnim 0, so clearing it before the next tick leaves no vanilla swing frame on screen.
        player.swinging = false;
        if (reference17) {
            renderReference17(pose, sink, light, overlay);
            return;
        }
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

        CrucibleEquipModel model = CrucibleEquipModel.get();
        CrucibleEquipModel.Playhead head = model.playhead(deployElapsed(anim, stack, model));
        Matrix4f swing = swingInScene(model, head);
        if (swing != null) Poses.mulPose(pose, swing);
        Identifier skin = PlayerSkins.texture((AbstractClientPlayer) player);
        float partial = Frames.partialTick();
        model.walk(head, pose, (name, nodePose) -> {
            if (name.startsWith("Guard")) {
                model.submitPart(sink, nodePose, GUARD, name, light, overlay);
            } else if (name.equals("Hilt")) {
                model.submitPart(sink, nodePose, HILT, name, light, overlay);
            } else if (name.equals("Blade")) {
                blade(sink, nodePose, model, head.diffN(), partial, overlay);
            } else {
                model.submitPart(sink, nodePose, skin, name, light, overlay);
            }
        });
    }

    /**
     * The deploy clock as the draw reads it: {@code now - wrapperStart}, with 1.12's two overrides applied at the same
     * instant. 1.12 set {@code startTime = now - 400} (empty) and the STAY clamp inside {@code renderByItem}, right before
     * {@code renderAnimated} read the clock again, so both reads were the same millisecond. Here the wrapper is updated in
     * {@link #firstPersonFrame} at the start of the world frame and the hand is drawn after the whole level, tens of
     * milliseconds later; reading the clock again would give the empty crucible 400..4xx ms, a different point of the
     * still-moving deploy every frame (jitter). A charged crucible is unaffected because the playhead clamps past the
     * last keyframe.
     */
    static long deployElapsed(Anim anim, ItemStack stack, CrucibleEquipModel model) {
        if (ItemCrucible.getCharges(stack) == 0) return 400;
        return Math.min(System.currentTimeMillis() - anim.wrapperStart, model.length);
    }

    /**
     * The 1.7 swing ({@link #swing17}) as a transform of the 1.12.2 scene, or null while no swing plays.
     *
     * <p>1.7 drew the OBJ crucible alone and applied {@code translate(SWING_TRANS) rotate(x) rotate(z) rotate(y)} right
     * before the OBJ, i.e. in the crucible's own model frame, pivoting about the OBJ origin (the hilt's base line).
     * Here the crucible is the Collada scene's {@code Hilt} node holding the same mesh, with the arms around it, and the
     * swing is applied in that frame: {@code N W N^-1} at the scene root, where {@code N} is the hilt node's scene matrix
     * at this playhead times {@link CrucibleEquipModel#OBJ_IN_HILT} (OBJ to node coordinates). The whole scene moves
     * rigidly, so the arms keep holding the hilt; translations stay in OBJ units (scaled with the model) and rotations
     * about the OBJ axes, as in 1.7.
     */
    static @Nullable Matrix4f swingInScene(CrucibleEquipModel model, CrucibleEquipModel.Playhead head) {
        Matrix4f w = swing17Matrix();
        if (w == null) return null;
        Matrix4f hilt = model.nodeMatrix(head, "Hilt");
        if (hilt == null) return null;
        Matrix4f n = hilt.mul(CrucibleEquipModel.OBJ_IN_HILT);
        return new Matrix4f(n).mul(w).mul(new Matrix4f(n).invert());
    }

    /**
     * 1.7's {@code translate(SWING_TRANS) rotate(SWING_ROT x, 1, 0, 0) rotate(z, 0, 0, 1) rotate(y, 0, 1, 0)} now (or at
     * the harness's pinned moment); null while no swing plays.
     */
    private static @Nullable Matrix4f swing17Matrix() {
        double[] sRot, sTrans;
        if (freezeSwingMs >= 0) {
            BusAnimation pinned = swing17(freezeSwingOffset);
            sRot = pinned.getTimedTransformation("SWING_ROT", freezeSwingMs);
            sTrans = pinned.getTimedTransformation("SWING_TRANS", freezeSwingMs);
        } else {
            sRot = transformation("SWING_ROT", InteractionHand.MAIN_HAND);
            sTrans = transformation("SWING_TRANS", InteractionHand.MAIN_HAND);
        }
        if (sRot == null || sTrans == null) return null;
        if (sRot[0] == 0 && sRot[1] == 0 && sRot[2] == 0 && sTrans[0] == 0 && sTrans[1] == 0 && sTrans[2] == 0) return null;
        return new Matrix4f()
                .translate((float) sTrans[0], (float) sTrans[1], (float) sTrans[2])
                .rotateX((float) Math.toRadians(sRot[0]))
                .rotateZ((float) Math.toRadians(sRot[2]))
                .rotateY((float) Math.toRadians(sRot[1]));
    }

    private static com.hbm.client.model.@Nullable WavefrontObj obj17;

    /**
     * Harness reference: the 1.7 {@code ItemRenderCrucible} first-person body (charged, guards open, blade cutout) in the
     * 1.7 weapon frame, reached from the {@code teisr} frame the pose is in: {@code teisr^-1 * weapon}, both frames being
     * maps from the same hand pose ({@code Pose17} ITEM_RENDER_WEAPON and 1.12.2's first-person display).
     */
    private static void renderReference17(PoseStack pose, SubmitSink sink, int light, int overlay) {
        if (obj17 == null) obj17 = com.hbm.client.model.WavefrontObj.load(Ids.of("hbm", "models/weapons/crucible.obj"));
        Matrix4f teisr = new Matrix4f()
                .translate(0.5F, 0.5F, 0.5F)
                .translate(1.13F / 16F, 3.2F / 16F, 1.13F / 16F)
                .rotateXYZ(0F, (float) Math.toRadians(-90), (float) Math.toRadians(25))
                .scale(0.68F)
                .translate(-0.5F, -0.5F, -0.5F);
        Matrix4f weapon = Pose17.frame(Pose17.Klass.ITEM_RENDER_WEAPON,
                DisplaySlot.FIRST_PERSON_RIGHT_HAND);
        Poses.mulPose(pose, teisr.invert().mul(weapon));
        pose.translate(1.5D, -0.3D, 0D);
        pose.scale(0.3F, 0.3F, 0.3F);
        pose.mulPose(Axis.ZP.rotationDegrees(45F));
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        Matrix4f w = swing17Matrix();
        if (w != null) Poses.mulPose(pose, w);
        com.hbm.client.model.WavefrontObj obj = obj17;
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutCull(HILT), (p, b) -> obj.renderPart("Hilt", p, b, light, overlay));
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutCull(GUARD), (p, b) -> obj.renderPart("GuardLeft", p, b, light, overlay));
        sink.submitCustomGeometry(pose, CompatRenderTypes.entityCutoutCull(GUARD), (p, b) -> obj.renderPart("GuardRight", p, b, light, overlay));
        pose.translate(0.005D, 0D, 0D);
        sink.submitCustomGeometry(pose, CompatRenderTypes.flatCutout(BLADE), (p, b) -> obj.renderPart("Blade", p, b, 0xF000F0, overlay));
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
            NoiseDisplacedRenderType.localMatrix(m);
            sink.submitCustomGeometry(pose,
                    NoiseDisplacedRenderType.of(LIGHTNING_SHADER, cpu ? NOISE_FLAT : NOISE, false), (p, buffer) -> {
                for (FirstPersonParticles.Particle particle : particles) {
                    if (particle instanceof FirstPersonParticles.Lightning bolt) {
                        CrucibleLightning.render(bolt.node, playerPos, 0.0015F, bolt.shaderTime(), p, buffer, cpu);
                    }
                }
            });
            // 1.12 drew the lightning into bloomData in the same onRender, before the blade wrote depth (bloomData shares
            // the main depth attachment): order 0, or the depth-written 0.7 blade (order 1) hides the bolt's bloom
            if (!isolate) sink.submitCustomGeometry(pose,
                    NoiseDisplacedRenderType.of(LIGHTNING_SHADER, cpu ? NOISE_FLAT : NOISE, true), (p, buffer) -> {
                for (FirstPersonParticles.Particle particle : particles) {
                    if (particle instanceof FirstPersonParticles.Lightning bolt) {
                        CrucibleLightning.render(bolt.node, playerPos, 0.0015F, bolt.shaderTime(), p, buffer, cpu);
                    }
                }
            });
            // the sparks' bloom pass too (ItemRenderCrucible.java:167-171: same onRender loop, before the blade)
            if (!isolate) sink.submitCustomGeometry(pose, Bloom.of(GUARD), (p, buffer) -> {
                for (FirstPersonParticles.Particle particle : particles) {
                    if (particle instanceof FirstPersonParticles.Spark spark) spark.render(p, buffer, playerPos, partial, SPARK_BLOOM_COLOR);
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
    private static void clippedBlade(PoseStack.Pose pose, VertexConsumer buffer, float[] tri,
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

    private static void emit(PoseStack.Pose pose, VertexConsumer buffer, float[] v, int color,
                             int overlay) {
        Vertexes.vertex(buffer, pose, v[0], v[1], v[2], color, v[3], v[4], overlay, 0xF000F0, v[5], v[6], v[7]);
    }

    // Third person (port addition)

    /**
     * The lightning of other players' crucibles, by entity id. The local player's bolts are the first-person ones
     * ({@code updateClient} spawns them whatever the camera), so F5 shows the very bolts first person would; another
     * player's client state is unknown here, so their charged crucible spawns bolts at the same rate (1 in 20 per tick)
     * from the same positions, as if fully deployed.
     */
    private static final Map<Integer, List<FirstPersonParticles.Lightning>> OTHER_BOLTS = new HashMap<>();
    /** Next free {@code NoiseDisplacedRenderType} slot this frame; slot 0 is first person's. */
    private static int nextSlot = 1;
    /** Harness: third-person effect draws since the last query. */
    static int thirdPersonDraws, thirdPersonBoltDraws;

    /** The 1.12 Collada blade node in the OBJ crucible's frame: {@code OBJ_IN_HILT^-1 * Hilt^-1 * Blade}. */
    private static @Nullable Matrix4f bladeNodeInObj;

    private static Matrix4f bladeNodeInObj() {
        if (bladeNodeInObj == null) {
            CrucibleEquipModel model = CrucibleEquipModel.get();
            CrucibleEquipModel.Playhead head = model.playhead(0);
            Matrix4f hilt = model.nodeMatrix(head, "Hilt");
            Matrix4f blade = model.nodeMatrix(head, "Blade");
            bladeNodeInObj = hilt == null || blade == null ? new Matrix4f()
                    : new Matrix4f(CrucibleEquipModel.OBJ_IN_HILT).invert().mul(hilt.invert()).mul(blade);
        }
        return bladeNodeInObj;
    }

    static void resetModelCaches() {
        bladeNodeInObj = null;
    }

    /** Client tick: other players' bolts age, and a charged main-hand crucible may spawn one. */
    private static void tickOtherBolts(Minecraft mc) {
        if (mc.level == null) {
            OTHER_BOLTS.clear();
            return;
        }
        Set<Integer> present = new HashSet<>();
        var rand = mc.level.getRandom();
        for (Player other : mc.level.players()) {
            if (other == mc.player) continue;
            ItemStack held = other.getMainHandItem();
            if (!(held.getItem() instanceof ItemCrucible) || ItemCrucible.getCharges(held) <= 0) continue;
            present.add(other.getId());
            List<FirstPersonParticles.Lightning> bolts = OTHER_BOLTS.computeIfAbsent(other.getId(), id -> new ArrayList<>());
            if (!FirstPersonParticles.frozen) {
                // spawn, then age (the local player's order: updateClient, then the END-phase particle tick)
                if (rand.nextInt(20) == 0) {
                    bolts.add(new FirstPersonParticles.Lightning(mc.level.getGameTime(), rand, 0,
                            (rand.nextFloat() - 0.5F) * 0.2F, 0.7F - rand.nextFloat() * 0.25F).lifetime(10));
                }
                bolts.removeIf(bolt -> {
                    bolt.tick();
                    return !bolt.alive;
                });
            }
        }
        OTHER_BOLTS.keySet().retainAll(present);
    }

    /** Whether {@code owner}'s crucible in this hand gets the shader effects in third person: a charged main hand. */
    public static boolean thirdPersonEffects(ItemStack stack, @Nullable LivingEntity owner, boolean mainHand) {
        return owner instanceof Player && mainHand && stack.getItem() instanceof ItemCrucible
                && ItemCrucible.getCharges(stack) > 0;
    }

    /**
     * The first-person blade's effects on the third-person OBJ crucible (port addition; 1.12.2 drew them in first person
     * only): the blade translucent at alpha 0.7 (order 1), the lightning before it (order 0, both the main-target and
     * the bloom pass, through this player's own local-matrix slot) and after it the blade's bloom and, above 60% of the
     * deploy, its heat-distortion shell (order 2), as {@link #renderFirstPerson}'s blade does. {@code pose} is the OBJ
     * crucible's space with the blade's 0.005 nudge. The OBJ blade is the Collada blade node's mesh at
     * {@link #bladeNodeInObj()}: the shell is the node's {@code (1.15, 1.15, 1.05)} about its centre, and the bolts are
     * the first-person bolts (same seed, same main chain) drawn in that node's space with their forks following the
     * chain ({@code Lightning.thirdPersonNode}). 1.12's forks all leave along the node's {@code -y}, which first person
     * sees head-on but which points at the ground in the held third-person pose.
     */
    public static void renderThirdPerson(LivingEntity owner, PoseStack pose, SubmitSink sink, int overlay,
                                         com.hbm.client.model.WavefrontObj crucible) {
        Minecraft mc = Minecraft.getInstance();
        boolean local = owner == mc.player;
        float diffN = 1F;
        List<FirstPersonParticles.Lightning> bolts = new ArrayList<>();
        if (local) {
            Anim w = relevant(InteractionHand.MAIN_HAND);
            if (w != null && w.blender) {
                diffN = CrucibleEquipModel.get().playhead(System.currentTimeMillis() - w.wrapperStart).diffN();
            }
            for (FirstPersonParticles.Particle p : FirstPersonParticles.PARTICLES) {
                if (p instanceof FirstPersonParticles.Lightning bolt) bolts.add(bolt);
            }
        } else {
            List<FirstPersonParticles.Lightning> list = OTHER_BOLTS.get(owner.getId());
            if (list != null) bolts.addAll(list);
        }
        thirdPersonDraws++;

        pose.pushPose();
        Poses.mulPose(pose, bladeNodeInObj());
        if (!bolts.isEmpty() && nextSlot < NoiseDisplacedRenderType.SLOTS) {
            int slot = nextSlot++;
            Matrix4f m = new Matrix4f(pose.last().pose());
            Vector4f eye = new Matrix4f(m).invert().transform(new Vector4f(0, 0, 0, 1));
            V3 eyePos = new V3(eye.x, eye.y, eye.z);
            NoiseDisplacedRenderType.localMatrix(slot, m);
            boolean cpu = lightningCpu;
            SubmitSink.CustomGeometry draw = (p, buffer) -> {
                for (FirstPersonParticles.Lightning bolt : bolts) {
                    CrucibleLightning.render(bolt.thirdPersonNode, eyePos, 0.0015F, bolt.shaderTime(), p, buffer, cpu);
                }
            };
            Identifier noise = cpu ? NOISE_FLAT : NOISE;
            sink.submitCustomGeometry(pose, NoiseDisplacedRenderType.of(LIGHTNING_SHADER, noise, false, slot), draw);
            if (!isolate) sink.submitCustomGeometry(pose, NoiseDisplacedRenderType.of(LIGHTNING_SHADER, noise, true, slot), draw);
            thirdPersonBoltDraws += bolts.size();
        }
        boolean distort = diffN > 0.6F;
        pose.popPose();

        sink.order(1).submitCustomGeometry(pose, CompatRenderTypes.flatTranslucentCull(BLADE),
                (p, buffer) -> crucible.renderPart("Blade", p, buffer, 0xF000F0, overlay, BLADE_TINT));
        SubmitSink layers = sink.order(2);
        layers.submitCustomGeometry(pose, Bloom.of(BLADE_BLOOM),
                (p, buffer) -> crucible.renderPart("Blade", p, buffer, 0xF000F0, overlay, 0xFFFFFFFF));
        if (distort) {
            // the node's scale (1.15, 1.15, 1.05) about its origin (the blade's centre), in OBJ space
            pose.pushPose();
            Matrix4f node = bladeNodeInObj();
            Poses.mulPose(pose, new Matrix4f(node).scale(1.15F, 1.15F, 1.05F).mul(new Matrix4f(node).invert()));
            layers.submitCustomGeometry(pose, ScreenDistortion.of(BLADE_BLOOM, NOISE),
                    (p, buffer) -> crucible.renderPart("Blade", p, buffer, 0xF000F0, overlay, DISTORTION_AMOUNT));
            pose.popPose();
        }
    }

    // The killing hit's dust

    /** 1.12.2 {@code VanillaBurst_BlockDust}: exact motion {@code gaussian * motion} (+0.2 up), life 50..99. */
    private static void blockDustBurst(Level level, double x, double y, double z,
                                       CompoundTag data) {
        if (!(level instanceof ClientLevel)) return;
        double motion = Tags.getDoubleOr(data, "motion", 0D);
        int count = Tags.getIntOr(data, "count", 0);
        var block = BuiltInRegistries.BLOCK.byId(Tags.getIntOr(data, "block", 0));
        var option = new BlockParticleOption(ParticleTypes.BLOCK,
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
