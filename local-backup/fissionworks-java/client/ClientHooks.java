package fissionworks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Only called on the client; keeps client classes out of the common block code. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void open(BlockPos panel) {
        Minecraft.getInstance().setScreen(new ConsoleScreen(panel));
    }

    public static void openZirnox(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ZirnoxScreen(pos));
    }
}
