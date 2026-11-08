package com.hbm.client.anim;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.config.ClientConfig;
import dev.golem.core.client.CameraHooks;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * The client consumer for Sedna's camera recoil. 1.7 drew the view between the last two ticks' angles, so each tick's
 * turn eased in over the next; the player's camera reads raw angles now, so it is held back by the part still to come.
 */
public final class GunRecoil {

    private static boolean registered;
    /** This tick's net turn, in degrees. */
    private static float turnPitch;
    private static float turnYaw;

    private GunRecoil() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ClientTicks.onEndTick(GunRecoil::tick);
        CameraHooks.PITCH.register(entity -> pending(entity, turnPitch));
        CameraHooks.YAW.register(entity -> pending(entity, turnYaw));
    }

    /** First person only: a detached camera is placed from the raw angles and mirrors pitch in front view. */
    private static float pending(Entity entity, float turn) {
        Minecraft mc = Minecraft.getInstance();
        if (turn == 0F || entity != mc.player || !mc.options.getCameraType().isFirstPerson()) return 0F;
        return -turn * (1F - Frames.partialTick());
    }

    /** Applies the old pitch/yaw kick, then decays and rebounds it in that exact order. */
    public static void tick() {
        turnPitch = 0F;
        turnYaw = 0F;
        if (!ClientConfig.GUN_VISUAL_RECOIL) {
            ItemGunBaseNT.clearRecoil();
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        float pitch = player.getXRot();
        float yaw = player.getYRot();

        ItemGunBaseNT.offsetVertical += ItemGunBaseNT.recoilVertical;
        ItemGunBaseNT.offsetHorizontal += ItemGunBaseNT.recoilHorizontal;
        player.setXRot(player.getXRot() - ItemGunBaseNT.recoilVertical);
        player.setYRot(player.getYRot() - ItemGunBaseNT.recoilHorizontal);

        ItemGunBaseNT.recoilVertical *= ItemGunBaseNT.recoilDecay;
        ItemGunBaseNT.recoilHorizontal *= ItemGunBaseNT.recoilDecay;
        float dV = ItemGunBaseNT.offsetVertical * ItemGunBaseNT.recoilRebound;
        float dH = ItemGunBaseNT.offsetHorizontal * ItemGunBaseNT.recoilRebound;
        ItemGunBaseNT.offsetVertical -= dV;
        ItemGunBaseNT.offsetHorizontal -= dH;
        player.setXRot(player.getXRot() + dV);
        player.setYRot(player.getYRot() + dH);
        turnPitch = player.getXRot() - pitch;
        turnYaw = player.getYRot() - yaw;
    }
}
