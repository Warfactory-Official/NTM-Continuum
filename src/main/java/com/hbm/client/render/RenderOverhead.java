package com.hbm.client.render;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm.entity.mob.siege.EntitySiegeCraft;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.LineRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 1.7 {@code RenderOverhead}'s markers and tags: block outlines drawn through the world, each with an optional label
 * that floats (at most sixteen blocks out along the line of sight) in front of the marked block, gaining the distance
 * ("12m") while the player looks straight at it. Reusable: anything can queue a {@link Marker} for a position; the
 * server does it with {@code OverheadMarkers.send} (the {@code marker} aux effect).
 *
 * <p>1.7 drew in {@code RenderWorldLastEvent} with the depth test off; here it is Core's late world-effects pass with
 * see-through lines and see-through text, which is the same thing. Lines are two pixels wide - 1.7 set no width of
 * its own and inherited the {@code glLineWidth(2)} vanilla left behind from the block selection box.
 */
public final class RenderOverhead {

    /** Markers added since the last frame; merged at the start of the next, as 1.7's queue was. */
    private static final Map<BlockPos, Marker> QUEUED = new HashMap<>();
    private static final Map<BlockPos, Marker> MARKERS = new HashMap<>();
    private static boolean initialized;
    private static int lastThermalBoxes;
    private static int lastHealthTags;

    private RenderOverhead() {}

    public static void register() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        WorldEffects.onBegin(RenderOverhead::renderMarkers);
        WorldEffects.onBegin(RenderOverhead::renderArmorSight);
    }

    /** 1.7 {@code queuedMarkers.put}. Client thread. */
    public static void queue(BlockPos pos, Marker marker) {
        QUEUED.put(pos.immutable(), marker);
    }

    /** Markers currently shown (for the harness). */
    public static int count() {
        return MARKERS.size() + QUEUED.size();
    }

    /** The labels currently shown, for the harness. */
    public static List<String> labels() {
        List<String> out = new ArrayList<>();
        for (Marker marker : MARKERS.values()) if (marker.label != null) out.add(marker.label);
        for (Marker marker : QUEUED.values()) if (marker.label != null) out.add(marker.label);
        return out;
    }

    /** Thermal boxes and health tags drawn in the last frame, for the harness. */
    public static int[] lastSight() {
        return new int[]{lastThermalBoxes, lastHealthTags};
    }

    public static void clear() {
        QUEUED.clear();
        MARKERS.clear();
    }

    /** 1.7 {@code renderMarkers}. */
    private static void renderMarkers(CameraInfo camera) {
        // 1.7 draws markers only with the HUD on; queued ones wait
        LocalPlayer viewer = Minecraft.getInstance().player;
        if (viewer != null && !HbmLivingProps.getEnableHUD(viewer)) return;
        MARKERS.putAll(QUEUED);
        QUEUED.clear();
        if (MARKERS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            MARKERS.clear();
            return;
        }
        float partial = Frames.partialTick();
        // 1.7 measured from the interpolated player (its eyes: an EntityPlayerSP's posY), drawn in camera space
        double x = camera.pos.x;
        double y = camera.pos.y;
        double z = camera.pos.z;

        List<Map.Entry<BlockPos, Marker>> tagList = new ArrayList<>();
        List<float[]> boxes = new ArrayList<>();
        Iterator<Map.Entry<BlockPos, Marker>> it = MARKERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Marker> entry = it.next();
            BlockPos pos = entry.getKey();
            Marker marker = entry.getValue();
            boxes.add(new float[]{
                    (float) (pos.getX() + marker.minX - x), (float) (pos.getY() + marker.minY - y), (float) (pos.getZ() + marker.minZ - z),
                    (float) (pos.getX() + marker.maxX - x), (float) (pos.getY() + marker.maxY - y), (float) (pos.getZ() + marker.maxZ - z),
                    Float.intBitsToFloat(marker.color)});
            tagList.add(entry);

            if (marker.expire > 0 && System.currentTimeMillis() > marker.expire) {
                it.remove();
            } else if (marker.maxDist > 0) {
                // 1.7: the block origin plus HALF THE SIZE (not min + half) - kept
                double aX = pos.getX() + (marker.maxX - marker.minX) / 2D;
                double aY = pos.getY() + (marker.maxY - marker.minY) / 2D;
                double aZ = pos.getZ() + (marker.maxZ - marker.minZ) / 2D;
                if (new Vec3(x - aX, y - aY, z - aZ).length() > marker.maxDist) it.remove();
            }
        }

        PoseStack pose = new PoseStack();
        WorldEffects.submit(pose, LineRenderTypes.seeThrough(2F), 10, (last, buffer) -> {
            for (float[] box : boxes) outline(last, buffer, box);
        });

        // port: 1.7's 100 blocks plus the depth its world floor (y 0) lies above this one's, so deposits keep their labels
        int labelRange = 100 - Math.min(0, mc.level.getMinY());
        Vec3 look = player.getViewVector(partial);
        float viewY = player.getViewYRot(partial);
        float viewX = player.getViewXRot(partial);
        for (Map.Entry<BlockPos, Marker> entry : tagList) {
            BlockPos pos = entry.getKey();
            Marker marker = entry.getValue();
            double aX = pos.getX() + (marker.maxX - marker.minX) / 2D;
            double aY = pos.getY() + (marker.maxY - marker.minY) / 2D;
            double aZ = pos.getZ() + (marker.maxZ - marker.minZ) / 2D;
            double vx = aX - x, vy = aY - y, vz = aZ - z;
            double len = vx * vx + vy * vy + vz * vz;
            double sqrt = Math.sqrt(len);
            double mult = Math.min(sqrt, 16D);
            vx *= mult / sqrt;
            vy *= mult / sqrt;
            vz *= mult / sqrt;
            double nx = vx / mult, ny = vy / mult, nz = vz / mult;
            String label = marker.label == null ? "" : marker.label;
            if (Math.abs(look.x - nx) + Math.abs(look.y - ny) + Math.abs(look.z - nz) < 0.15) {
                label += (!label.isEmpty() ? " " : "") + ((int) sqrt) + "m";
            }
            if (!label.isEmpty()) {
                drawTag(1F, len, label, vx, vy, vz, labelRange, true, marker.color, marker.color, viewY, viewX);
            }
        }
    }

    /**
     * 1.7 {@code ModEventHandlerClient}, with the HUD on: a full working {@code ArmorFSB} set with VATS tags every other
     * living entity with a health bar, and thermal sight (the set's, or a fully aimed gun's) outlines entities within 64
     * blocks through walls.
     */
    private static void renderArmorSight(CameraInfo camera) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        lastThermalBoxes = 0;
        lastHealthTags = 0;
        if (player == null || mc.level == null || !HbmLivingProps.getEnableHUD(player)) return;
        ArmorFSB set = ArmorFSB.hasFSBArmor(player)
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorFSB chestplate ? chestplate : null;
        boolean thermal = set != null && set.thermal;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemGunBaseNT gun && ItemGunBaseNT.aimingProgress == 1F) {
            for (int i = 0; i < gun.getConfigCount(); i++) {
                if (gun.getConfig(held, i).hasThermalSights(held)) thermal = true;
            }
        }
        if (thermal) renderThermalSight(mc, player, camera);
        if (set != null && set.vats) renderHealthTags(mc, player, camera, set.thermal);
    }

    /** 1.7 {@code renderThermalSight}: each entity's box, coloured by kind, black once dead. */
    private static void renderThermalSight(Minecraft mc, LocalPlayer player, CameraInfo camera) {
        List<float[]> boxes = new ArrayList<>();
        for (Entity ent : mc.level.entitiesForRendering()) {
            if (ent == player || ent.distanceToSqr(player) > 4096D) continue;
            int color;
            if (isBoss(ent)) color = 0xFF8000;
            else if (ent instanceof Enemy) color = 0xFF0000;
            else if (ent instanceof Player) color = 0xFF00FF;
            else if (ent instanceof Mob) color = 0x00FF00;
            else if (ent instanceof ItemEntity) color = 0xFFFF80;
            else if (ent instanceof ExperienceOrb) color = player.tickCount % 10 < 5 ? 0xFFFF80 : 0x80FF80;
            else continue;
            if (ent instanceof LivingEntity living && living.getHealth() <= 0F) color = 0x000000;
            AABB bb = ent.getBoundingBox();
            boxes.add(new float[]{
                    (float) (bb.minX - camera.pos.x), (float) (bb.minY - camera.pos.y), (float) (bb.minZ - camera.pos.z),
                    (float) (bb.maxX - camera.pos.x), (float) (bb.maxY - camera.pos.y), (float) (bb.maxZ - camera.pos.z),
                    Float.intBitsToFloat(color)});
        }
        lastThermalBoxes = boxes.size();
        if (boxes.isEmpty()) return;
        WorldEffects.submit(new PoseStack(), LineRenderTypes.seeThrough(2F), 10, (last, buffer) -> {
            for (float[] box : boxes) outline(last, buffer, box);
        });
    }

    /** 1.7's {@code IBossDisplayData} entities. */
    private static boolean isBoss(Entity ent) {
        return ent instanceof EnderDragon || ent instanceof WitherBoss || ent instanceof EntityBOTPrimeHead
                || ent instanceof EntityHunterChopper || ent instanceof EntityMaskMan || ent instanceof EntityQuackos
                || ent instanceof EntityUFO || ent instanceof EntitySiegeCraft;
    }

    /**
     * 1.7 {@code preRenderEvent} VATS: a bar of up to 100 strokes over each other living entity within name-tag range
     * (32 while it sneaks), red for its health; seen through walls when the set also has thermal sight.
     */
    private static void renderHealthTags(Minecraft mc, LocalPlayer player, CameraInfo camera, boolean throughWalls) {
        if (mc.options.hideGui) return;
        Entity viewer = mc.getCameraEntity() == null ? player : mc.getCameraEntity();
        float partial = Frames.partialTick();
        float viewY = player.getViewYRot(partial);
        float viewX = player.getViewXRot(partial);
        for (Entity ent : mc.level.entitiesForRendering()) {
            if (!(ent instanceof LivingEntity living) || ent == viewer || ent.isInvisibleTo(player)
                    || ent.isVehicle()) continue;
            double distSq = ent.distanceToSqr(viewer);
            float range = ent.isDiscrete() ? 32F : 64F;
            if (distSq >= range * range) continue;
            int count = (int) Math.min(living.getMaxHealth(), 100F);
            int bars = (int) Math.ceil(living.getHealth() * count / living.getMaxHealth());
            StringBuilder bar = new StringBuilder(ChatFormatting.RED.toString());
            for (int i = 0; i < count; i++) {
                if (i == bars) bar.append(ChatFormatting.RESET);
                bar.append('|');
            }
            Vec3 at = ent.getPosition(partial).subtract(camera.pos);
            double y = living.isSleeping() ? at.y - 1.5D : at.y;
            drawTag(ent.getBbHeight() + 0.75F, distSq, bar.toString(), at.x, y, at.z, 64, throughWalls, -1, 553648127,
                    viewY, viewX);
            lastHealthTags++;
        }
    }

    /** 1.7's 24 GL_LINES vertices: the twelve edges of the box, opaque in the marker colour. */
    private static void outline(PoseStack.Pose pose, VertexConsumer buffer, float[] b) {
        int color = 0xFF000000 | Float.floatToRawIntBits(b[6]);
        float x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        edge(pose, buffer, color, x0, y1, z0, x0, y0, z0);
        edge(pose, buffer, color, x0, y1, z0, x1, y1, z0);
        edge(pose, buffer, color, x1, y1, z0, x1, y0, z0);
        edge(pose, buffer, color, x0, y0, z0, x1, y0, z0);
        edge(pose, buffer, color, x1, y0, z0, x1, y0, z1);
        edge(pose, buffer, color, x1, y1, z1, x1, y1, z0);
        edge(pose, buffer, color, x1, y1, z1, x1, y0, z1);
        edge(pose, buffer, color, x0, y1, z0, x0, y1, z1);
        edge(pose, buffer, color, x0, y1, z1, x0, y0, z1);
        edge(pose, buffer, color, x0, y1, z1, x1, y1, z1);
        edge(pose, buffer, color, x0, y0, z1, x1, y0, z1);
        edge(pose, buffer, color, x0, y0, z0, x0, y0, z1);
    }

    private static void edge(PoseStack.Pose pose, VertexConsumer buffer, int color,
                             float ax, float ay, float az, float bx, float by, float bz) {
        float nx = bx - ax, ny = by - ay, nz = bz - az;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        Vertexes.line(buffer, pose, ax, ay, az, color, nx, ny, nz, 2F);
        Vertexes.line(buffer, pose, bx, by, bz, color, nx, ny, nz, 2F);
    }

    /**
     * 1.7 {@code drawTag}: a nameplate {@code 1.6 / 60} blocks per pixel, turned to the player's view, over a black
     * quarter-alpha backing; the first (shadow-colour) pass ignores depth when {@code depthTest}, the second is
     * depth-tested, offset towards the camera so the backing's depth never hides it (1.7 wrote no depth there).
     */
    public static void drawTag(float offset, double distsq, String name, double x, double y, double z, int dist,
                               boolean depthTest, int color, int shadowColor, float viewY, float viewX) {
        if (distsq > (double) (dist * dist)) return;
        Font font = Minecraft.getInstance().font;
        float scale = 0.016666668F * 1.6F;
        PoseStack pose = new PoseStack();
        pose.translate((float) x, (float) y + offset, (float) z);
        pose.mulPose(Axis.YP.rotationDegrees(-viewY));
        pose.mulPose(Axis.XP.rotationDegrees(viewX));
        pose.scale(-scale, -scale, scale);
        int heightOffset = name.equals("deadmau5") ? -10 : 0;
        float left = -font.width(name) / 2F;
        WorldEffects.submitText(pose, 11, font, name, left, heightOffset, opaqueUnlessAlpha(shadowColor), false,
                depthTest ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0x40000000, 0xF000F0);
        WorldEffects.submitText(pose, 12, font, name, left, heightOffset, opaqueUnlessAlpha(color), false,
                Font.DisplayMode.POLYGON_OFFSET, 0, 0xF000F0);
    }

    /** 1.7 {@code FontRenderer}: a colour without alpha bits draws opaque, one with them keeps its alpha. */
    private static int opaqueUnlessAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    /** 1.7 {@code RenderOverhead.Marker}: a unit block box by default. */
    public static final class Marker {
        double minX = 0, minY = 0, minZ = 0, maxX = 1, maxY = 1, maxZ = 1;
        final int color;
        @Nullable String label;
        long expire;
        double maxDist;

        public Marker(int color) {
            this.color = color & 0xFFFFFF;
        }

        public Marker setExpire(long expire) {
            this.expire = expire;
            return this;
        }

        public Marker setDist(double maxDist) {
            this.maxDist = maxDist;
            return this;
        }

        public Marker withLabel(@Nullable String label) {
            this.label = label;
            return this;
        }
    }
}
