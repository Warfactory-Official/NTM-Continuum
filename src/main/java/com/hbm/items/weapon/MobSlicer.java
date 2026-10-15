package com.hbm.items.weapon;

import com.hbm.lib.ModDamageSources;
import com.hbm.packet.SpecialDeathPayload;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.Compat;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The server half of 1.12.2 {@code PacketMobSlicer.Handler}: which living things a drawn cut hits, and their deaths.
 *
 * <p>Rays from the eyes, the look direction blended towards the cut's start direction in steps of 0.1. 1.12.2 stepped
 * a float ({@code i <= 1} with {@code i += 0.1F}), which reaches 1.0000001 after ten steps, so it casts ten rays
 * (0 .. 0.9) and never the start direction itself; the loop is kept as written. Each ray is three blocks long ({@code Library.rayTraceIncludeEntities}: blocks first, then living, alive entities whose box
 * grown by 0.3 the shortened ray crosses, nearest first). A crucible needs a charge and spends one if anything was hit.
 *
 * <p>Each victim: marked {@code killedByMobSlicer} (revive modules skip it), its combat tracker told of a lethal hit,
 * health 0, {@code onDeath} with {@code crucible} (tex 1) or {@code slicer} damage, then removed; and every client
 * tracking it is sent the cut ({@link SpecialDeathPayload}, effect 3). 1.12 called {@code setDead()} before
 * {@code onDeath}; a modern {@code die} ignores a removed entity, so the removal comes after (see {@link #kill}): loot
 * dropped, no death animation, no experience (nothing hit it as a player).
 *
 * <p>Server trust: 1.12 took the packet as sent. Here a cut is only accepted when its numbers are finite, its point
 * lies near the eye (the client sends {@code eye + look}, one block out) and the normal is not degenerate; the normal
 * is re-normalised, the cap texture and damage come from the held cutter rather than the packet, and one player's
 * cuts are rate-limited ({@link ItemSwordCutter#acceptCut}). An honest client sends exactly such cuts, so nothing it
 * does changes.
 */
public final class MobSlicer {

    /**
     * 1.12 {@code getEntityData().setBoolean("killedByMobSlicer", true)}, read only by the revive handler during this
     * very death ("we don't want to make a Schrodinger death"); a weak set holds the same fact without NBT.
     */
    private static final Set<LivingEntity> SLICED =
            Collections.newSetFromMap(new WeakHashMap<>());

    /** Whether {@code entity} is dying (or died) by a cut: revives must not bring it back. */
    public static boolean killedByMobSlicer(LivingEntity entity) {
        return SLICED.contains(entity);
    }

    private MobSlicer() {}

    /** One victim and the cut data its clients were sent. */
    public record Cut(LivingEntity victim, float[] data) {}

    /**
     * The checked entry point for a client's cut: drops a non-finite or degenerate cut, or one arriving faster than
     * {@link ItemSwordCutter#acceptCut} allows, and takes the texture id from the held cutter. Returns null when dropped.
     */
    public static @Nullable List<Cut> sliceFromClient(ServerPlayer player, Vec3 pos, Vec3 norm) {
        if (!(player.getMainHandItem().getItem() instanceof ItemSwordCutter cutter)) return null;
        if (!finite(pos) || !finite(norm)) return null;
        double length = norm.length();
        if (!(length > 1.0E-6D) || !Double.isFinite(length)) return null;
        // eye + start look (a unit vector); a few blocks of slack for movement between the client's frame and now
        if (pos.distanceToSqr(player.getEyePosition(1F)) > 4D * 4D) return null;
        if (!ItemSwordCutter.acceptCut(player)) return null;
        return slice(player, pos, norm.scale(1D / length), cutter.getTexId());
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    public static List<Cut> slice(ServerPlayer player, Vec3 pos, Vec3 norm, byte tex) {
        ItemStack heldStack = player.getMainHandItem();
        List<LivingEntity> attack = new ArrayList<>();
        Vec3 eye = player.getEyePosition(1F);
        Vec3 v1 = player.getViewVector(1F);
        Vec3 v2 = pos.subtract(eye);
        for (float i = 0; i <= 1; i += 0.1F) {
            Vec3 dir = new Vec3(v1.x + (v2.x - v1.x) * i, v1.y + (v2.y - v1.y) * i, v1.z + (v2.z - v1.z) * i).normalize();
            LivingEntity hit = rayTraceLiving(player, eye, eye.add(dir.scale(3)));
            if (hit != null && !attack.contains(hit)) attack.add(hit);
        }
        List<Cut> cuts = new ArrayList<>();
        // the server also starts its record of the cutting swing ("swing": SWING 150 + 500 ms) the client plays
        ItemSwordCutter.busStarted(player, 650);
        if (heldStack.getItem() instanceof ItemCrucible) {
            if (ItemCrucible.getCharges(heldStack) == 0) return cuts;
            // the free hit that may have opened this cut (doSpecialClick) is settled by the cut itself
            ItemCrucible.cutArrived(player);
            if (!attack.isEmpty()) {
                ItemCrucible.discharge(heldStack);
                if (ItemCrucible.getCharges(heldStack) == 0) ItemCrucible.clearSpecialClick(player);
            }
        }
        ServerLevel level = (ServerLevel) player.level();
        for (LivingEntity victim : attack) {
            SLICED.add(victim);
            Vec3 rel = pos.subtract(victim.getX(), victim.getY(), victim.getZ());
            float[] data = new float[]{(float) norm.x, (float) norm.y, (float) norm.z, -(float) norm.dot(rel),
                    Float.intBitsToFloat(tex)};
            DamageSource source = ModDamageSources.source(level, tex == 1 ? ModDamageSources.CRUCIBLE : ModDamageSources.SLICER);
            // 1.12's setDead() only flagged the entity, so the cut reached the clients before the removal did; a modern
            // removal is broadcast at once, so the cut is sent first
            sendToTrackers(level, victim, new SpecialDeathPayload(victim.getId(), SpecialDeathPayload.SLICED, data));
            kill(victim, source);
            cuts.add(new Cut(victim, data));
        }
        return cuts;
    }

    /**
     * {@code trackDamage(source, health, health); setDead(); setHealth(0); onDeath(source); onKillCommand()}.
     *
     * <ul>
     *   <li>{@code onKillCommand} was a no-op for anything already at zero health, except the ender dragon:
     *       {@code EntityDragon.onKillCommand} removed it and told its fight ({@code processDragonDeath}: exit portal,
     *       gateway, egg on the first kill). A modern dragon tells its fight only from the end of its death animation
     *       ({@code tickDeath}, which a removed dragon never reaches) or from its {@code kill} override, which is the
     *       same {@code remove(KILLED)} plus {@code setDragonKilled}. So a dragon is removed through {@code kill}.</li>
     *   <li>1.12 removed everything else ({@code setDead}) while its health was still above zero, so a large slime
     *       did not split. Modern {@code Slime.remove} splits whenever {@code isDeadOrDying()}, at any removal reason,
     *       on every node; so the removal runs at the old health and the health is settled at zero afterwards.</li>
     * </ul>
     */
    public static void kill(LivingEntity victim, DamageSource source) {
        float health = victim.getHealth();
        victim.getCombatTracker().recordDamage(source, health);
        victim.setHealth(0);
        victim.die(source);
        if (victim instanceof Player) return;
        if (victim instanceof EnderDragon dragon && victim.level() instanceof ServerLevel level) {
            Compat.kill(dragon, level);
            return;
        }
        victim.setHealth(health);
        victim.discard();
        victim.setHealth(0);
    }

    /**
     * {@code sendToAllTracking(victim)} plus the victim itself if it is a player: every player within the victim type's
     * tracking range ({@code GolemNetwork.sendToPlayer} skips a connection that never negotiated the channel).
     */
    public static void sendToTrackers(ServerLevel level, LivingEntity victim, SpecialDeathPayload payload) {
        double range = victim.getType().clientTrackingRange() * 16D;
        for (ServerPlayer player : level.players()) {
            if (player != victim && player.distanceToSqr(victim) > range * range) continue;
            GolemNetwork.sendToPlayer(player, SpecialDeathPayload.CODEC, payload);
        }
    }

    /** {@code Library.rayTraceIncludeEntities} for living, alive entities, the player excluded. */
    static @Nullable LivingEntity rayTraceLiving(ServerPlayer player, Vec3 start, Vec3 end) {
        BlockHitResult block = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB area = new AABB(start, end).inflate(2.3D);
        for (Entity entity : player.level().getEntities(player, area,
                e -> e instanceof LivingEntity living && living.isAlive())) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.3D).clip(start, end);
            if (hit.isEmpty()) continue;
            double dist = start.distanceToSqr(hit.get());
            if (dist < bestDist) {
                bestDist = dist;
                best = (LivingEntity) entity;
            }
        }
        if (best == null) return null;
        if (block.getType() != HitResult.Type.MISS && start.distanceToSqr(block.getLocation()) <= bestDist) return null;
        return best;
    }
}
