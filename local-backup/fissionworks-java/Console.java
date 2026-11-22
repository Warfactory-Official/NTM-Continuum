package fissionworks;

import net.minecraft.world.entity.player.Player;

/** A block entity that takes commands from a console screen. */
public interface Console {
    void command(Player player, int op, int a, int b);
}
