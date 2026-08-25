package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IPlayerProcessor;
import java.util.HashMap;
import java.util.Map.Entry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pushes players away from the blast.
 *
 * <p>Upstream sent its own {@code ExplosionKnockbackPacket} because a player's motion lives on their
 * client, so a server-side velocity change would be overwritten by the next movement packet. Modern
 * Minecraft has that channel built in: setting {@code hurtMarked} makes the server send
 * {@code ClientboundSetEntityMotionPacket} at the end of the tick. Same effect, one fewer payload.
 */
public class PlayerProcessorStandard implements IPlayerProcessor {

    @Override
    public void process(ExplosionVNT explosion, Level world, double x, double y, double z, HashMap<Player, Vec3> affectedPlayers) {

        for (Entry<Player, Vec3> entry : affectedPlayers.entrySet()) {
            Player player = entry.getKey();
            player.setDeltaMovement(player.getDeltaMovement().add(entry.getValue()));
            player.hurtMarked = true;
        }
    }
}
