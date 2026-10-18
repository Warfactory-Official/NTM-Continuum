package com.hbm.uninos.networkproviders;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.HbmEffectNT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** The shared body of 1.7's {@code PowerNetProvider} / {@code FluidNetProvider.spawnDebugParticles}. */
final class PortDebugParticles {

    private PortDebugParticles() {}

    static void spawn(ServerLevel level, DirPos pos, boolean receive, String mode, int color) {
        ForgeDirection dir = pos.getDir();
        int x = pos.getPos().getX() + dir.offsetX;
        int y = pos.getPos().getY() + dir.offsetY;
        int z = pos.getPos().getZ() + dir.offsetZ;
        double motion = receive ? -0.1 : 0.1;
        if (!receive) {
            x -= dir.offsetX;
            y -= dir.offsetY;
            z -= dir.offsetZ;
        }
        CompoundTag data = new CompoundTag();
        data.putString("mode", mode);
        if (color >= 0) data.putInt("color", color);
        double posX = x + 0.5 - dir.offsetX * 0.5 + level.getRandom().nextDouble() * 0.5 - 0.25;
        double posY = y + 0.5 - dir.offsetY * 0.5 + level.getRandom().nextDouble() * 0.5 - 0.25;
        double posZ = z + 0.5 - dir.offsetZ * 0.5 + level.getRandom().nextDouble() * 0.5 - 0.25;
        data.putDouble("mX", dir.offsetX * motion);
        data.putDouble("mY", dir.offsetY * motion);
        data.putDouble("mZ", dir.offsetZ * motion);
        HbmEffectNT.Network.send(level, posX, posY, posZ, data, 25);
    }
}
