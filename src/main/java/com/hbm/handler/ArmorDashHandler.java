package com.hbm.handler;

import com.hbm.interfaces.IArmorModDash;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.sound.ModSounds;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 {@code EntityEffectHandler.handleDashing} with the {@code HbmPlayerProps} dash fields it drives: the
 * bismuth suit's (and dash modules') stamina-metered sideways dash.
 *
 * <p>Upstream ran this every tick on <em>both</em> sides from the same key state, which the client set
 * locally as well as sending it: the dash impulse that actually moves the player is the client's, the
 * server keeps its own stamina bookkeeping and plays the sound for everyone else. The port keeps one state
 * per side for the same reason. The key edge is cleared only while the cooldown runs, so a held key dashes
 * once; the stamina bar's refill "boop" rises a twelfth of an octave per recharged dash.
 *
 * <p>Oddities kept: stamina is clamped down to the current capacity every tick (taking the suit off empties
 * it), and while the cooldown runs the key is forced up, so a press during it is lost.
 */
public final class ArmorDashHandler {

    private ArmorDashHandler() {}

    /** 1.7 {@code HbmPlayerProps.dashCooldownLength}. */
    public static final int DASH_COOLDOWN_LENGTH = 5;
    /** Stamina per dash. */
    public static final int PER_DASH = 30;

    /** One side's copy of upstream's dash props. */
    public static final class State {
        public boolean keyPressed;
        public int dashCooldown;
        public int totalDashCount;
        public int stamina;
    }

    private static final Map<UUID, State> CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, State> SERVER = new ConcurrentHashMap<>();

    public static State state(Player player) {
        return (player.level().isClientSide() ? CLIENT : SERVER).computeIfAbsent(player.getUUID(), id -> new State());
    }

    /** The key state as reported by the keybind (server) or read locally (client). */
    public static void setKeyPressed(Player player, boolean pressed) {
        state(player).keyPressed = pressed;
    }

    public static void tick(Player player) {
        State props = state(player);
        props.totalDashCount = 0;

        int armorDashCount = 0;
        int armorModDashCount = 0;

        if (ArmorFSB.hasFSBArmor(player)) {
            ArmorFSB chestplate = ArmorFSB.chestplate(player);
            if (chestplate != null) armorDashCount = chestplate.dashCount;
        }

        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
                EquipmentSlot.HEAD}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty() || !ArmorModHandler.hasMods(armor)) continue;
            for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                if (!mod.isEmpty() && mod.getItem() instanceof IArmorModDash dash) armorModDashCount += dash.getDashes();
            }
        }

        int dashCount = armorDashCount + armorModDashCount;
        boolean dashActivated = props.keyPressed;

        if (dashCount * PER_DASH < props.stamina) props.stamina = dashCount * PER_DASH;

        if (dashCount <= 0) return;

        int stamina = props.stamina;
        props.totalDashCount = dashCount;

        if (props.dashCooldown <= 0) {
            if (dashActivated && stamina >= PER_DASH) {
                Vec3 lookingIn = player.getLookAngle();
                // Vec3.rotateAroundY(PI/2): x' = x cos + z sin, z' = z cos - x sin
                Vec3 strafeVec = new Vec3(lookingIn.z, lookingIn.y, -lookingIn.x);

                int forward = (int) Math.signum(player.zza);
                int strafe = (int) Math.signum(player.xxa);
                if (forward == 0 && strafe == 0) forward = 1;

                Vec3 motion = player.getDeltaMovement().add(
                        lookingIn.x * forward + strafeVec.x * strafe, 0,
                        lookingIn.z * forward + strafeVec.z * strafe);
                player.setDeltaMovement(motion.x, 0, motion.z);
                player.fallDistance = 0F;
                player.playSound(ModSounds.WEAPON_ROCKET_FLAME.get(), 1.0F, 1.0F);

                props.dashCooldown = DASH_COOLDOWN_LENGTH;
                stamina -= PER_DASH;
            }
        } else {
            props.dashCooldown--;
            props.keyPressed = false;
        }

        if (stamina < props.totalDashCount * PER_DASH) {
            stamina++;
            if (stamina % PER_DASH == PER_DASH - 1) {
                player.playSound(ModSounds.TECH_BOOP.get(), 1.0F, 1.0F + ((1F / 12F) * (stamina / PER_DASH)));
                stamina++;
            }
        }

        props.stamina = stamina;
    }

    /** Forgets every player's state, as leaving a world forgot the old props. */
    public static void clear() {
        CLIENT.clear();
        SERVER.clear();
    }
}
