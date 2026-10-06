package com.hbm.handler;

import com.hbm.items.ModArmorSetBItems;
import com.hbm.potion.ModEffects;
import com.hbm.util.ShadyUtil;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.Effects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The per-player effects of 1.7's {@code ModEventHandler} that key on {@link ShadyUtil}: {@code onWingFlop} (Bob,
 * the_NCR and Pheo fly without gear, Peep sprints faster), the Pu-238 aura of {@code onPlayerTick} and
 * Dr_Nostalgia's {@code onPlayerRespawn} kit. Elsewhere: Pu-238's resistance in {@link HazmatRegistry}, the powered
 * step in {@code ArmorFSB.handleTick}, Tankish's crucible in {@code ItemCrucible}, Bob's book in
 * {@code ModEventHandlerMobs}, and the capes and models client-side in {@code com.hbm.client.render.util}.
 *
 * <p>{@link #tick} runs on both sides from the living tick, as 1.7's {@code PlayerTickEvent} did.
 */
public final class SpecialPlayers {

    private SpecialPlayers() {}

    /** 1.7 {@code fopSpeed} ({@code e5a8c95d-c7a0-4ecf-8126-76fb8c949389}, "FOP SPEED"). */
    private static final String FOP_SPEED = "hbm:fop_speed";

    public static void init() {
        Lifecycle.onPlayerCopy((oldPlayer, newPlayer, alive) -> onPlayerRespawn(newPlayer));
    }

    public static boolean isBob(Player player) {
        return ShadyUtil.is(player, ShadyUtil.HbMinecraft, "HbMinecraft");
    }

    public static boolean isNcr(Player player) {
        return ShadyUtil.is(player, ShadyUtil.the_NCR, "the_NCR");
    }

    public static boolean isPheo(Player player) {
        return ShadyUtil.is(player, ShadyUtil.Barnaby99_x, "pheo7");
    }

    /** Whether this player flies on their own wings now: one of the three, with no chestplate. */
    public static boolean hasInnateFlight(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && (isBob(player) || isNcr(player) || isPheo(player));
    }

    public static void tick(Player player) {
        onWingFlop(player);
        if (ShadyUtil.is(player.getUUID(), ShadyUtil.Pu_238)) puRadiation(player);
    }

    /** 1.7 {@code onWingFlop}, phase START. */
    private static void onWingFlop(Player player) {
        if (player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && !player.onGround()) {

            if (isPheo(player)) {
                JetpackHandler.resetFlightTime(player);
                Vec3 motion = player.getDeltaMovement();

                if (JetpackHandler.isJetpackActive(player)) {
                    if (motion.y < 0.4D) motion = motion.add(0, 0.1D, 0);
                    Vec3 look = player.getViewVector(1.0F);
                    if (motion.length() < 2) {
                        motion = motion.add(look.scale(0.2D));
                        if (look.y > 0) player.fallDistance = 0;
                    }
                } else if (JetpackHandler.isBackpackEnabled(player) && !player.isShiftKeyDown()) {
                    if (motion.y < -0.2D) motion = motion.add(0, 0.075D, 0);
                    if (player.fallDistance > 0) player.fallDistance = 0;
                }
                player.setDeltaMovement(motion);
            }

            boolean isBob = isBob(player);
            boolean isOther = isNcr(player);

            if (isBob || isOther) {
                JetpackHandler.resetFlightTime(player);
                if (player.fallDistance > 0) player.fallDistance = 0;

                Vec3 motion = player.getDeltaMovement();
                if (motion.y < -0.4D) motion = new Vec3(motion.x, -0.4D, motion.z);
                boolean backpack = JetpackHandler.isBackpackEnabled(player);

                if (isBob || player.getFoodData().getFoodLevel() > 6) {

                    if (JetpackHandler.isJetpackActive(player)) {
                        double cap = isBob ? 0.8D : 0.4D;
                        if (motion.y < cap) motion = motion.add(0, 0.15D, 0);
                        else motion = new Vec3(motion.x, cap + 0.15D, motion.z);

                        if (isOther) {
                            // burn up saturation so that super-saturating foods have no effect
                            if (player.getFoodData().getSaturationLevel() > 0F) player.causeFoodExhaustion(4F);
                            else player.causeFoodExhaustion(0.2F); // 4:1 -> 0.05 hunger per tick or 1 per second
                        }

                    } else if (backpack && !player.isShiftKeyDown()) {
                        double y = motion.y;
                        if (y < -1) y += 0.4D;
                        else if (y < -0.1) y += 0.2D;
                        else if (y < 0) y = 0;
                        motion = new Vec3(motion.x, y, motion.z);

                        if (isOther && !player.onGround()) {
                            if (player.getFoodData().getSaturationLevel() > 0F) player.causeFoodExhaustion(4F);
                            else player.causeFoodExhaustion(0.04F);
                        }

                    } else if (!backpack && player.isShiftKeyDown()) {
                        if (motion.y < -0.08) {
                            double mo = motion.y * (isBob ? -0.6 : -0.4);
                            motion = motion.add(0, mo, 0).add(player.getViewVector(1.0F).scale(mo));
                        }
                    }
                }

                Vec3 orig = player.getViewVector(1.0F);
                Vec3 look = new Vec3(orig.x, 0, orig.z);
                if (look.lengthSqr() > 0D) look = look.normalize();
                double mod = backpack ? (isBob ? 0.5D : 0.25D) : 0.125D;
                float forward = JetpackHandler.getForwardInput(player);
                float strafe = JetpackHandler.getStrafeInput(player);

                if (forward != 0) motion = motion.add(look.x * 0.35 * forward * mod, 0, look.z * 0.35 * forward * mod);
                if (strafe != 0) {
                    // Vec3.rotateAroundY(PI / 2)
                    motion = motion.add(look.z * 0.15 * strafe * mod, 0, -look.x * 0.15 * strafe * mod);
                }
                player.setDeltaMovement(motion);
            }
        }

        // Peep: half again the base speed while sprinting (1.7 operation 1); 1.7 ran this with or without a chestplate
        if (ShadyUtil.is(player, ShadyUtil.LePeeperSauvage, "LePeeperSauvage")) {
            var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (player.isSprinting()) Attributes.setTransient(speed, FOP_SPEED, 0.5D, Attributes.Op.MULTIPLY_BASE);
            else Attributes.removeTransient(speed, FOP_SPEED);
        }
    }

    /** 1.7 {@code onPlayerTick}: PU RADIATION on the server, and the {@code townaura} motes on the client. */
    private static void puRadiation(Player player) {
        if (!player.level().isClientSide()) {
            for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(3, 3, 3))) {
                if (entity != player) Effects.addRegistered(entity, ModEffects.RADIATION.get(), 300, 2);
            }
        } else if (!player.isInvisible() && !player.isShiftKeyDown()) {
            RandomSource rand = player.getRandom();
            double radius = 3 * rand.nextDouble();
            double z = rand.nextDouble() * Math.PI;      // rotateAroundZ
            double y = rand.nextDouble() * Math.PI * 2;  // rotateAroundY
            double vx = radius * Math.cos(z);
            double vy = -radius * Math.sin(z);
            player.level().addParticle(ParticleTypes.MYCELIUM,
                    player.getX() + vx * Math.cos(y), player.getY() + 1 + vy, player.getZ() - vx * Math.sin(y),
                    0.0, 0.0, 0.0);
        }
    }

    /** 1.7 {@code onPlayerRespawn}: Dr_Nostalgia gets the hat and Beta back. */
    private static void onPlayerRespawn(ServerPlayer player) {
        if (!ShadyUtil.is(player, ShadyUtil.Dr_Nostalgia, "Dr_Nostalgia")) return;
        give(player, new ItemStack(ModArmorSetBItems.nossy_hat.get()));
        give(player, new ItemStack(ModArmorSetBItems.beta.get()));
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().contains(stack)) player.getInventory().add(stack);
    }
}
