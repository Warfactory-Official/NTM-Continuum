package com.hbm.client.anim;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.config.ClientConfig;
import dev.golem.core.client.ClientTicks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** The small client-only consumer for Sedna's upstream camera recoil accumulator. */
public final class GunRecoil {

    private static boolean registered;

    private GunRecoil() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ClientTicks.onEndTick(GunRecoil::tick);
    }

    /** Applies the old pitch/yaw kick, then decays and rebounds it in that exact order. */
    public static void tick() {
        if (!ClientConfig.GUN_VISUAL_RECOIL) {
            ItemGunBaseNT.clearRecoil();
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

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
    }
}
